package com.xapps.media.xmusic.activity.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;

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
                if (mediaItem != null) {
					ActivityRootBinding binding = activity.getBinding();
                    if (!binding.expandedPlayer.toggleView.isAnimating() && controller.isPlaying()) binding.expandedPlayer.toggleView.startAnimation();
                    if (binding.expandedPlayer.toggleView.isAnimating() && !(controller.getPlaybackState() == Player.STATE_READY || controller.getPlaybackState() == Player.STATE_BUFFERING)) binding.expandedPlayer.toggleView.stopAnimation();

                }
            }
            
            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo positionInfo, @NonNull Player.PositionInfo positionInfo2, int i) {
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