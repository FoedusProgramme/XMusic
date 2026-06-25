package com.xapps.media.xmusic.activity.controller;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.models.BottomSheetBehavior;
import com.xapps.media.xmusic.models.SquigglyProgress;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;
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
                    String songPath = RuntimeData.songs.get(controller.getCurrentMediaItemIndex()).path;
                    //activity.loadLyrics(songPath);
                }
                if (mediaItem != null) {
                    //activity.floatBnv(true);
					ActivityRootBinding binding = activity.getBinding();
                    int position = controller.getCurrentMediaItemIndex();
                    //activity.updateAdapters(position, controller.isPlaying());
                    //activity.progressDrawable.setAnimate(true);
                    //if (!binding.toggleView.isAnimating() && controller.isPlaying()) binding.toggleView.startAnimation();
                    //if (binding.toggleView.isAnimating() && !(controller.getPlaybackState() == Player.STATE_READY || controller.getPlaybackState() == Player.STATE_BUFFERING)) binding.toggleView.stopAnimation();
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
                //activity.updateProgress(controller.getCurrentPosition());
            }            
            
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    if (playWhenReady) {
                        //activity.getBinding().toggleView.startAnimation();
                        //activity.progressDrawable.setAnimate(true);
                    } else {
                        //activity.getBinding().toggleView.stopAnimation();
                        //activity.progressDrawable.setAnimate(false);
                    }
                }
            }
			
			@Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
					//activity.getBinding().toggleView.stopAnimation();
					//activity.progressDrawable.setAnimate(false);
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