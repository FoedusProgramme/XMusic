package com.xapps.media.xmusic.stats;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer;
import com.xapps.media.xmusic.models.Song;
import java.io.File;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

public final class StatsAudioAnalyzer {

    private static final String TAG = "STATS_LIFECYCLE";
    private static final long MIN_LISTEN_MS = 10000L;

    private final Context context;
    private final HandlerThread workerThread;
    private final Handler workerHandler;
    private final StatsDatabaseManager databaseManager;
    
    private ExoPlayer player;
    private StatsAudioProcessor processor;
    private Song extractingSong;
    private MessageDigest md5Digest;

    private Song currentSong;
    private Runnable watchDogRunnable;
    private long accumulatedTimeMs;
    private long lastStartTimeMs;
    private boolean isPlaying;
    private boolean isLogged;
    private boolean isFastPath;

    public StatsAudioAnalyzer(Context context) {
        this.context = context.getApplicationContext();
        this.databaseManager = new StatsDatabaseManager(this.context);
        this.workerThread = new HandlerThread("XMusicStatsBg");
        this.workerThread.start();
        this.workerHandler = new Handler(this.workerThread.getLooper());
        try {
            this.md5Digest = MessageDigest.getInstance("MD5");
        } catch (Exception ignored) {}
    }

    public void startAnalysis(Song song) {
        workerHandler.post(() -> {
            cancelTimer();
            if (song == null || song.path == null) return;

            currentSong = song;
            accumulatedTimeMs = 0;
            lastStartTimeMs = System.currentTimeMillis();
            isPlaying = true;
            isLogged = false;

            Log.d(TAG, "Triggered startAnalysis for track: " + song.title + " | Path: " + song.path);

            long lastMod = new File(song.path).lastModified();
            String hash = calculateTempHash(song.id, song.title, song.artist, song.album, song.path, song.size, lastMod);

            if (!databaseManager.shouldForceAudit(song.path, hash)) {
                isFastPath = true;
                Log.d(TAG, "Fast-path intercept hit! Metadata matches. Route: Waiting 10s to log shortcut.");
                startTimer(MIN_LISTEN_MS);
            } else {
                isFastPath = false;
                Log.d(TAG, "Fast-path missed or audit forced. Route: Commencing background decode pipeline immediately.");
                launchBackgroundExtraction(song);
            }
        });
    }

    public void resumeAnalysis() {
        workerHandler.post(() -> {
            if (currentSong == null || isPlaying || isLogged) return;
            
            lastStartTimeMs = System.currentTimeMillis();
            isPlaying = true;

            if (isFastPath) {
                long remainingTime = 10000 - accumulatedTimeMs;
                startTimer(remainingTime);
            } else if (player != null) {
                Log.d(TAG, "Resuming background decoder loop.");
                player.play();
            }
        });
    }

    public void pauseAnalysis() {
        workerHandler.post(() -> {
            if (currentSong == null || !isPlaying || isLogged) return;
            
            if (isFastPath) {
                cancelTimer();
                accumulatedTimeMs += (System.currentTimeMillis() - lastStartTimeMs);
            } else if (player != null) {
                Log.d(TAG, "Pausing background decoder loop.");
                player.pause();
            }
            isPlaying = false;
        });
    }

    public void stopAnalysis() {
        workerHandler.post(this::stopAnalysisInternal);
    }

    private void startTimer(long delayMs) {
        cancelTimer();
        if (delayMs <= 0) {
            triggerFastPathLogging();
            return;
        }
        watchDogRunnable = this::triggerFastPathLogging;
        workerHandler.postDelayed(watchDogRunnable, delayMs);
    }

    private void cancelTimer() {
        if (watchDogRunnable != null) {
            workerHandler.removeCallbacks(watchDogRunnable);
            watchDogRunnable = null;
        }
    }

    private void triggerFastPathLogging() {
        if (isLogged || currentSong == null) return;
        isLogged = true;
        Log.d(TAG, "10-second gate passed. Fast-path shortcut confirmed. Logging playback metrics.");
        databaseManager.processSongPlayback(currentSong.id, currentSong.title, currentSong.artist, currentSong.album, currentSong.path, currentSong.size, null);
    }

    private void launchBackgroundExtraction(Song songToProcess) {
        if (player != null) {
            if (extractingSong != null) {
                Log.w(TAG, "INTERRUPT: Active analysis session broken. User skipped or swapped track away from: " + extractingSong.path);
                Log.d(TAG, "Routing basic fallback metadata profile to database manager due to failure.");
                databaseManager.processSongPlayback(extractingSong.id, extractingSong.title, extractingSong.artist, extractingSong.album, extractingSong.path, extractingSong.size, null);
            }
            player.stop();
        }

        extractingSong = songToProcess;
        processor = new StatsAudioProcessor();

        AudioSink silentAudioSink = new DefaultAudioSink.Builder(context)
                .setAudioProcessors(new AudioProcessor[]{processor})
                .build();

        RenderersFactory isolatedRenderersFactory = (eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput) -> 
                new Renderer[]{
                        new FfmpegAudioRenderer(eventHandler, audioRendererEventListener, silentAudioSink)
                };

        if (player == null) {
            player = new ExoPlayer.Builder(context, isolatedRenderersFactory)
                    .setLooper(workerThread.getLooper())
                    .build();
        }

        MediaItem.ClippingConfiguration clipping = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(10_000)
                .setEndPositionMs(30_000)
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.fromFile(new File(extractingSong.path)))
                .setClippingConfiguration(clipping)
                .build();

        player.setMediaItem(mediaItem);
        player.setPlaybackParameters(new PlaybackParameters(2.0f));

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    if (!isLogged) {
                        isLogged = true;
                        Log.d(TAG, "Background playback state shifted to STATE_ENDED. Fetching frames...");
                        float[] samples = processor.getFinalMonoSamples();
                        int sampleRate = processor.getSampleRate();
                        Log.d(TAG, "PCM extraction completed. Total floating samples parsed: " + samples.length + " | Sample Rate: " + sampleRate);
                        
                        Log.d(TAG, "Running Fast Fourier Transform analysis and minting track signatures...");
                        List<Long> fingerprints = StatsFingerprinter.generateFingerprints(samples, sampleRate);
                        Log.d(TAG, "Generated " + fingerprints.size() + " unique fingerprint hashes. Handing over data payloads to DB manager.");
                        
                        databaseManager.processSongPlayback(extractingSong.id, extractingSong.title, extractingSong.artist, extractingSong.album, extractingSong.path, extractingSong.size, fingerprints);
                    }
                    cleanupDecoderFields();
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Log.e(TAG, "ExoPlayer pipeline encountered a critical playback exception.", error);
                if (!isLogged) {
                    isLogged = true;
                    Log.d(TAG, "Routing basic fallback metadata profile to database manager due to failure.");
                    databaseManager.processSongPlayback(extractingSong.id, extractingSong.title, extractingSong.artist, extractingSong.album, extractingSong.path, extractingSong.size, null);
                }
                cleanupDecoderFields();
            }
        });

        Log.d(TAG, "Preparing and launching isolated background decoder pipeline at 2x speed...");
        player.prepare();
        player.play();
    }

    private void stopAnalysisInternal() {
        cancelTimer();
        if (player != null && player.isPlaying()) {
            Log.w(TAG, "INTERRUPT: Active analysis session broken. Canceling pending metrics task.");
            player.stop();
        }
        cleanupDecoderFields();
        currentSong = null;
        isPlaying = false;
        isLogged = false;
        accumulatedTimeMs = 0;
    }

    private void cleanupDecoderFields() {
        processor = null;
        extractingSong = null;
    }

    private String calculateTempHash(long id, String t, String a, String al, String p, long s, long lm) {
        String raw = id + "|" + (t != null ? t : "") + "|" + (a != null ? a : "") + "|" + (al != null ? al : "") + "|" + p + "|" + s + "|" + lm;
        try {
            if (md5Digest == null) md5Digest = MessageDigest.getInstance("MD5");
            md5Digest.reset();
            byte[] bytes = md5Digest.digest(raw.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    public void release() {
        workerHandler.post(() -> {
            cancelTimer();
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
            cleanupDecoderFields();
            currentSong = null;
            Log.d(TAG, "Terminating analyzer background handler thread.");
            workerThread.quitSafely();
        });
    }
}
