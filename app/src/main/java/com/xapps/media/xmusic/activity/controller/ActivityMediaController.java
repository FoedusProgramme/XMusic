package com.xapps.media.xmusic.activity.controller;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.ui.behavior.BottomSheetBehavior;
import com.xapps.media.xmusic.models.SquigglyProgress;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.ui.widget.ExpressiveSliderLayout;
import java.util.function.Consumer;

public class ActivityMediaController {

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private final Context context;
    private final SessionToken sessionToken;

    public ActivityMediaController(Context context, SessionToken token) {
        this.context = context;
        this.sessionToken = token;
    }

    public void initialize(Consumer<MediaController> onReady, Consumer<Exception> onError, Runnable onFinished) {
        if (controllerFuture != null || controller != null) {
            if (controller != null) {
                onReady.accept(controller);
                onFinished.run();
            }
            return;
        }

        controllerFuture = new MediaController.Builder(context, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                onReady.accept(controller);
            } catch (Exception e) {
                onError.accept(e);
            } finally {
                onFinished.run();
            }
        }, MoreExecutors.directExecutor());
    }
	
	public void setupListener(RootActivity activity) {
		controller.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {   
                if (controller.getPlaybackState() == Player.STATE_BUFFERING || controller.getPlaybackState() == Player.STATE_READY) {
                    if (RuntimeData.songs != null && !RuntimeData.songs.isEmpty()) {
                        int index = controller.getCurrentMediaItemIndex();
                        if (index >= 0 && index < RuntimeData.songs.size()) {
                            String songPath = RuntimeData.songs.get(index).path;
                            //activity.loadLyrics(songPath);
                        }
                    }
                }
                if (mediaItem != null) {
                    //activity.floatBnv(true);
					ActivityRootBinding binding = activity.getBinding();
                    int position = controller.getCurrentMediaItemIndex();
                    
                    //activity.updateAdapters(position, controller.isPlaying());
                    //activity.progressDrawable.setAnimate(true);
                    if (!binding.expandedPlayer.toggleView.isAnimating() && controller.isPlaying()) binding.expandedPlayer.toggleView.startAnimation();
                    if (binding.expandedPlayer.toggleView.isAnimating() && !(controller.getPlaybackState() == Player.STATE_READY || controller.getPlaybackState() == Player.STATE_BUFFERING)) binding.expandedPlayer.toggleView.stopAnimation();
                    //PlayerService.currentPosition = position;
                    //activity.seekbarFree = false;
                    /*binding.currentDurationText.setText(XUtils.millisecondsToDuration(0));
                    binding.songSeekbar.setProgress(0, true);
                    binding.musicProgress.setProgressCompat(0, true);
                    if (binding.expressiveBottomSheet.getState() == ExpressiveSliderLayout.STATE_HIDDEN) binding.expressiveBottomSheet.setState(ExpressiveSliderLayout.STATE_COLLAPSED);
                    //activity.syncPlayerUI(position);
                    //activity.saveUIState();
                    //activity.backgroundHandler.postDelayed(() -> {
                            //activity.seekbarFree = true;
                        //}, 150);
                    if (binding.expressiveBottomSheet.getState() == ExpressiveSliderLayout.STATE_EXPANDED) {
                        binding.miniPlayerBottomSheet.setProgress(1f);
                    } else {
                        binding.miniPlayerBottomSheet.setProgress(0f);
                    }
                    if (RuntimeData.songs.size() == 1) {
                        binding.previousButton.setActive(false);
                        binding.nextButton.setActive(false);
                    } else {
                        if (position == 0 && !(controller.getShuffleModeEnabled() || controller.getRepeatMode() == Player.REPEAT_MODE_ALL)) {
                            binding.previousButton.setActive(false);
                            binding.nextButton.setActive(true);
                        } else if (position == RuntimeData.songs.size() - 1 && !(controller.getShuffleModeEnabled() || controller.getRepeatMode() == Player.REPEAT_MODE_ALL)) {
                            binding.previousButton.setActive(true);
                            binding.nextButton.setActive(false);
                        } else {
                            binding.previousButton.setActive(true);
                            binding.nextButton.setActive(true);
                        }
                    }*/
                }
				
            }
            
            @Override
            public void onPositionDiscontinuity(Player.PositionInfo positionInfo, Player.PositionInfo positionInfo2, int i) {
                activity.onProgressChanged(controller.getCurrentPosition());
            }            
            
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    if (playWhenReady) {
                        activity.getBinding().expandedPlayer.toggleView.startAnimation();
                        activity.getBinding().expandedPlayer.songSeekbar.setAnimate(true);
                    } else {
                        activity.getBinding().expandedPlayer.toggleView.stopAnimation();
                        activity.getBinding().expandedPlayer.songSeekbar.setAnimate(false);
                    }
                }
            }
			
			@Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
					activity.getBinding().expandedPlayer.toggleView.stopAnimation();
					activity.getBinding().expandedPlayer.songSeekbar.setAnimate(false);
                } else if (playbackState == Player.STATE_IDLE) {
                    CallbackInterface.mlFrag().updateActiveItem(-1);
                }
            }
        });
	}

    public MediaController getController() {
        return controller;
    }

    public void release() {
        if (controller != null) {
            controller.release();
            controller = null;
        }

        controllerFuture = null;
    }
}