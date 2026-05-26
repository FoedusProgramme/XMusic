package com.xapps.media.xmusic.stats;

import android.content.Context;
import com.xapps.media.xmusic.utils.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StatsDatabaseManager {

    private static final String TAG = "STATS_DB";
    private static final String FILE_NAME = "xmusic_stats_v1.dat";
    private static final float MATCH_THRESHOLD = 0.15f;
    private static final int AUDIT_PLAY_INTERVAL = 5;

    private final Context context;
    private final ExecutorService diskExecutor;
    private File databaseFile;
    private HashMap<String, StatsSongModel> dbMap;
    private MessageDigest md5Digest;
    private boolean isLoaded;
    private int pendingSaveCount = 0;

    @SuppressWarnings("unchecked")
    public StatsDatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbMap = new HashMap<>();
        try {
            this.md5Digest = MessageDigest.getInstance("MD5");
        } catch (Exception ignored) {}
        this.diskExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "XMusicStatsDbWorker"));
        diskExecutor.execute(this::loadDatabaseInternal);
    }

    public synchronized boolean shouldForceAudit(String path, String currentWeakHash) {
        if (!isLoaded) return true;
        StatsSongModel model = dbMap.get(path);
        if (model == null) return true;
        if (!model.getWeakHash().equals(currentWeakHash)) return true;
        
        return (model.getPlayCount() - model.getLastVerifiedPlayCount()) >= AUDIT_PLAY_INTERVAL;
    }

    public void processSongPlayback(long mediaStoreId, String title, String artist, String album, String path, long size, List<Long> generatedFingerprints) {
        diskExecutor.execute(() -> {
            long currentLastModified = new File(path).lastModified();
            String currentWeakHash = calculateTemporaryWeakHash(mediaStoreId, title, artist, album, path, size, currentLastModified);

            synchronized (StatsDatabaseManager.this) {
                StatsSongModel existingByPath = dbMap.get(path);

                if (existingByPath != null) {
                    Log.d(TAG, "Path match found in DB for: " + path);
                    
                    if (generatedFingerprints == null || generatedFingerprints.isEmpty()) {
                        if (existingByPath.getWeakHash().equals(currentWeakHash)) {
                            existingByPath.incrementPlayCount();
                            Log.d(TAG, "MATCH: Weak hash matched perfectly! Incremented play count to " + existingByPath.getPlayCount() + " for: " + title);
                            checkAndSave();
                            return;
                        }
                    } else {
                        Log.d(TAG, "Evaluating fingerprints against the existing path's old fingerprint record...");
                        float score = getMatchScore(generatedFingerprints, existingByPath.getFingerprints());
                        Log.d(TAG, "Content similarity score with old record: " + (score * 100) + "% (Threshold=" + (MATCH_THRESHOLD * 100) + "%)");

                        if (score >= MATCH_THRESHOLD) {
                            Log.d(TAG, "MATCH: Fingerprint is within tolerance. Updating metadata attributes over same entry.");
                            existingByPath.setMediaStoreId(mediaStoreId);
                            existingByPath.setTitle(title);
                            existingByPath.setArtist(artist);
                            existingByPath.setAlbum(album);
                            existingByPath.setSize(size);
                            existingByPath.setLastModified(currentLastModified);
                            existingByPath.setFingerprints(generatedFingerprints);
                            existingByPath.updateWeakHash();
                            existingByPath.incrementPlayCount();
                            existingByPath.updateLastVerifiedPlayCount();
                            checkAndSave();
                            return;
                        } else {
                            Log.w(TAG, "ANTI-TAMPER TRIGGERED: Path matches but content fingerprint is completely different! Ghost entry detected.");
                            dbMap.remove(path);
                            dbMap.put(path, new StatsSongModel(mediaStoreId, title, artist, album, path, size, generatedFingerprints));
                            checkAndSave();
                            return;
                        }
                    }
                }

                if (generatedFingerprints == null || generatedFingerprints.isEmpty()) {
                    Log.w(TAG, "No fingerprints provided. Initializing basic fallback record.");
                    dbMap.put(path, new StatsSongModel(mediaStoreId, title, artist, album, path, size, java.util.Collections.emptyList()));
                    checkAndSave();
                    return;
                }

                Log.d(TAG, "Scanning entire database global pool to check if this file exists under a different path...");
                StatsSongModel matchedModel = null;
                for (StatsSongModel model : dbMap.values()) {
                    if (getMatchScore(generatedFingerprints, model.getFingerprints()) >= MATCH_THRESHOLD) {
                        matchedModel = model;
                        break;
                    }
                }

                if (matchedModel != null) {
                    int incomingCodecScore = getCodecPriorityScore(path);
                    int existingCodecScore = getCodecPriorityScore(matchedModel.getPath());
                    boolean takeIncomingPath = incomingCodecScore > existingCodecScore || (incomingCodecScore == existingCodecScore && size > matchedModel.getSize());

                    if (takeIncomingPath) {
                        dbMap.remove(matchedModel.getPath());
                        matchedModel.setPath(path);
                        matchedModel.setMediaStoreId(mediaStoreId);
                        matchedModel.setTitle(title);
                        matchedModel.setArtist(artist);
                        matchedModel.setAlbum(album);
                        matchedModel.setSize(size);
                        matchedModel.setLastModified(currentLastModified);
                        matchedModel.setFingerprints(generatedFingerprints);
                        matchedModel.updateWeakHash();
                        matchedModel.incrementPlayCount();
                        matchedModel.updateLastVerifiedPlayCount();
                        dbMap.put(path, matchedModel);
                    } else {
                        Log.d(TAG, "Shared statistics merged smoothly. Global play count incremented to: " + (matchedModel.getPlayCount() + 1));
                        matchedModel.incrementPlayCount();
                        matchedModel.updateLastVerifiedPlayCount();
                    }
                } else {
                    Log.d(TAG, "NEW ENTRY: No metadata or fingerprint matches exist anywhere. Creating a fresh tracking record for: " + title);
                    dbMap.put(path, new StatsSongModel(mediaStoreId, title, artist, album, path, size, generatedFingerprints));
                }
                checkAndSave();
            }
        });
    }

    private void checkAndSave() {
        pendingSaveCount++;
        if (pendingSaveCount >= 3) {
            pendingSaveCount = 0;
            saveDatabaseInternal();
        }
    }

    public void forceSave() {
        diskExecutor.execute(() -> {
            synchronized (StatsDatabaseManager.this) {
                saveDatabaseInternal();
            }
        });
    }

    private int getCodecPriorityScore(String path) {
        if (path == null) return 0;
        String lower = path.toLowerCase();
        if (lower.endsWith(".flac") || lower.endsWith(".alac")) return 3;
        if (lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp4")) return 2;
        if (lower.endsWith(".mp3") || lower.endsWith(".mpeg")) return 1;
        return 0;
    }

    private float getMatchScore(List<Long> src, List<Long> dest) {
        if (src == null || dest == null || src.isEmpty() || dest.isEmpty()) return 0f;
        HashSet<Long> srcSet = new HashSet<>(src);
        HashSet<Long> destSet = new HashSet<>(dest);
        int intersectionCount = 0;
        for (Long token : srcSet) {
            if (destSet.contains(token)) intersectionCount++;
        }
        return srcSet.isEmpty() ? 0f : (float) intersectionCount / srcSet.size();
    }

    private String calculateTemporaryWeakHash(long id, String t, String a, String al, String p, long s, long lm) {
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

    @SuppressWarnings("unchecked")
    private void loadDatabaseInternal() {
        databaseFile = new File(context.getDataDir().getAbsolutePath() + "/files", FILE_NAME);
        if (!databaseFile.exists()) {
            synchronized (this) { isLoaded = true; }
            Log.d(TAG, "No pre-existing local database file found on storage disk. Initializing clean memory map.");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(databaseFile))) {
            HashMap<String, StatsSongModel> loadedMap = (HashMap<String, StatsSongModel>) ois.readObject();
            synchronized (this) {
                dbMap = loadedMap != null ? loadedMap : new HashMap<>();
                isLoaded = true;
            }
            Log.d(TAG, "Database loaded successfully. Total records in memory: " + dbMap.size());
        } catch (Exception e) {
            synchronized (this) {
                dbMap = new HashMap<>();
                isLoaded = true;
            }
            Log.e(TAG, "Failed to load serialization file. Database map reset.", e);
        }
    }

    private void saveDatabaseInternal() {
        if (databaseFile == null) return;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(databaseFile))) {
            oos.writeObject(dbMap);
            Log.d(TAG, "Database changes successfully flushed and serialized to disk. Total records: " + dbMap.size());
        } catch (Exception e) {
            Log.e(TAG, "Critical error saving database updates to disk.", e);
        }
    }

    public synchronized Map<String, StatsSongModel> getInMemoryMap() {
        return new HashMap<>(dbMap);
    }
}
