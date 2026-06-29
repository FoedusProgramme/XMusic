package com.xapps.media.xmusic.activity.manager;

import android.content.ComponentName;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.activity.controller.ActivityMediaController;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.service.XPlayerService;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LogicManager {
    private RootActivity activity;
    private ActivityRootBinding binding;
    private UIManager uiManager;
    private MediaController mediaController;
    private SessionToken sessionToken;
    private ActivityMediaController controller;

    private boolean test1 = false;
    private boolean test2 = true;

    public LogicManager(RootActivity activity, UIManager uiManager) {
        this.activity = activity;
        this.binding = activity.getBinding();
        this.uiManager = uiManager;
    }

    public void initLogic() {
        setupListeners();
        setupCallbacks();
    }

    private void setupListeners() {
        binding.bottomButton.setOnClickListener(
                v -> {
                    if (test1) {
                        if (test2) {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_TABS_BNV);
                        } else {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_FULL);
                        }
                        test1 = false;
                        binding.bottomButton.setText("Show Player");
                    } else {
                        if (test2) {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_FULL);
                        } else {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY);
                        }
                        test1 = true;
                        binding.bottomButton.setText("Hide Player");
                    }
                });

        binding.topButton.setOnClickListener(
                v -> {
                    if (test2) {
                        if (test1) {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY);
                        } else {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_FULL);
                        }
                        test2 = false;
                        binding.topButton.setText("Expand Card");
                    } else {
                        if (test1) {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_FULL);
                        } else {
                            uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_TABS_BNV);
                        }
                        test2 = true;
                        binding.topButton.setText("Collapse Card");
                    }
                });
    }

    private void setupCallbacks() {
        binding.miniPlayer.setupPredictiveBack(activity);
        binding.miniPlayer.addSliderCallback(
                new ExpressiveSliderLayout.SliderCallback() {
                    @Override
                    public void onStateChanged(int state) {
                        binding.miniPlayer
                                .getPredictiveBackCallback()
                                .setEnabled(
                                        !(state == ExpressiveSliderLayout.STATE_COLLAPSED
                                                || state == ExpressiveSliderLayout.STATE_HIDDEN));
                        if (state == ExpressiveSliderLayout.STATE_HIDDEN) {
                            uiManager.onPlayerHidden();
                            mediaController.stop();
                            // mediaController.clearMediaItems();
                        }
                    }

                    @Override
                    public void onSlide(float offset) {
                        binding.layoutScrim.setAlpha(Math.max(0f, offset) * 0.7f);
                        updateImageSize(offset);
                    }
                });
    }

    public void initController(
            FragmentActivity activity,
            Consumer<MediaController> onReady,
            Consumer<Throwable> onError,
            Runnable onRestore) {

        if (sessionToken == null) {
            sessionToken =
                    new SessionToken(activity, new ComponentName(activity, XPlayerService.class));
        }

        controller = new ActivityMediaController(activity, sessionToken);

        controller.initialize(
                c -> {
                    mediaController = c;
                    controller.setupListener((RootActivity) activity);
                    onReady.accept(c);
                },
                e -> onError.accept(e),
                onRestore);
    }

    public void playSong(int position) {
        if (mediaController.getPlaybackState() == Player.STATE_BUFFERING) return;

        if (position == mediaController.getCurrentMediaItemIndex()) {
            mediaController.seekBack();
        }

        String songPath = RuntimeData.songs.get(position).path;
        // loadLyrics(songPath);
        if (!samePlaylistByPath(mediaController, CallbackInterface.service().getMediaItems())) {
            mediaController.setMediaItems(CallbackInterface.service().getMediaItems(), position, 0);
            mediaController.play();
        } else {
            mediaController.seekTo(position, 0);
            mediaController.play();
        }
        binding.expandedPlayer.toggleView.forcePlayState();
    }

    private static boolean samePlaylistByPath(
            MediaController controller, List<MediaItem> serviceItems) {
        int count = controller.getMediaItemCount();
        if (count != serviceItems.size()) return false;

        for (int i = 0; i < count; i++) {
            MediaItem cItem = controller.getMediaItemAt(i);
            MediaItem sItem = serviceItems.get(i);

            String cPath = cItem.localConfiguration.uri.getPath();
            String sPath = sItem.localConfiguration.uri.getPath();

            if (!Objects.equals(cPath, sPath)) return false;
        }
        return true;
    }

    public void updateVumeters(boolean isPlaying) {
        activity.runOnUiThread(
                () -> {
                    if (CallbackInterface.mlFrag() != null)
                        CallbackInterface.mlFrag().updateVumeter(isPlaying);
                });
    }

    private void updateImageSize(float offset) {
        float clampedOffset = Math.max(0f, offset);
        // binding.collapsedPlayer.motionRoot.setAlpha(Math.max(0f, 1f - clampedOffset));
        binding.collapsedPlayer.motionRoot.setProgress(clampedOffset);
    }

    public void handleProgress(long progress) {
        if (true) activity.runOnUiThread(() -> updateProgress(progress));
        activity.runOnUiThread(() -> binding.expandedPlayer.xlyricsView.onProgress((int) progress));
    }

    public void updateProgress(long position) {
        binding.collapsedPlayer.musicProgress.setProgressCompat((int) position, true);
        binding.expandedPlayer.songSeekbar.setProgress((int) position, false);
        binding.expandedPlayer.currentDurationText.setText(XUtils.millisecondsToDuration(position));
    }
}
