package bf.cloud.android.modules.player.videoviewexo;

import com.google.android.exoplayer.ExoPlayer;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.mediaplayer.StatusController;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer.RendererBuilder;
import bf.cloud.android.utils.BFYWeakReferenceHandler;

public class VideoViewExo extends VideoViewBase implements ExoVideoPlayer.Listener {
	
    private final String TAG = VideoViewExo.class.getSimpleName();

    private ExoVideoPlayer mExoPlayer;
    private boolean mPlayerNeedsPrepare;
    private boolean mPlayerInitilized;
    private boolean mPlayerPrepared;
    private float mVideoAspectRatio;
    private boolean mIsSeeking;
    private boolean mIsFirstBuffering;
    private long mFirstBufferStartTime;
    
    private UiHandler mUiHandler;
    
    public VideoViewExo(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }
    
    private void initVideoView() {
    	mPlayerNeedsPrepare = false;
    	mPlayerPrepared = false;
    	mUiHandler = new UiHandler(this);
    }

	private void initPlayer() {
		if (mExoPlayer == null) {
			mExoPlayer = new ExoVideoPlayer(getRendererBuilder());
			mExoPlayer.addListener(this);
			mPlayerNeedsPrepare = true;
		}
		if (mPlayerNeedsPrepare) {
			mExoPlayer.prepare();
			mPlayerNeedsPrepare = false;
		}
		mExoPlayer.setSurface(this.getHolder().getSurface());
		mExoPlayer.setPlayWhenReady(false);

		mPlayerInitilized = true;
	}

	private void releasePlayer() {
		if (mExoPlayer != null) {
			mExoPlayer.release();
			mExoPlayer = null;
		}
		
		mPlayerInitilized = false;
	}
    
	private RendererBuilder getRendererBuilder() {
        String url = "";
        if (mPlayProxy != null) {
        	url = mPlayProxy.getVideoUrl();
        }
		
		String userAgent = "BfCloudPlayer";
		
		return new HlsRendererBuilder(userAgent, url);
	}
	
	private boolean isPlayToEnd() {
		int duration = getDuration();
		int curPos = getCurrentPosition();
		return duration > 0 && curPos >= duration;
	}
	
	private void doStatePreparing() {
		mPlayerPrepared = false;
		mIsFirstBuffering = true;
		mFirstBufferStartTime = System.currentTimeMillis();
	}
	
	private void doStateBuffering() {
		BFYLog.d(TAG, "player is buffering.");
		
		if (isPlayToEnd()) {
			doPlayComplete();
		} else {
			if (!mIsFirstBuffering && !mIsSeeking) {
				mStatInfo.breakCount++;
			}
			setStatusControllerVisible(true);
			if (!mPlayerPrepared) {
				mPlayerPrepared = true;
		    	onPrepared();
			}
		}
	}
	
	private void doStateReady() {
		BFYLog.d(TAG, "player is ready.");
		
		mIsSeeking = false;
		
		if (mIsFirstBuffering) {
			mStatInfo.firstBufferTime = (int)(System.currentTimeMillis() - mFirstBufferStartTime);
			mStatInfo.firstBufferSuccess = true;
			mIsFirstBuffering = false;
			
			if (mPlayerController != null) {
				mPlayerController.reportPlayProcessStatInfo();
			}
		}

		setStatusControllerVisible(false);
	}
	
	private void doStateEnded() {
		doPlayComplete();
	}

	private void doPlayComplete() {
		if (!isPlayCompete()) {
			onCompletion();
		}
	}
	
    @Override
	public void onStateChanged(boolean playWhenReady, int playbackState) {
		switch (playbackState) {
			case ExoPlayer.STATE_PREPARING:
				doStatePreparing();
				break;
			
			case ExoPlayer.STATE_BUFFERING:
				doStateBuffering();
				break;

			case ExoPlayer.STATE_READY:
				doStateReady();
				break;
			
			case ExoPlayer.STATE_ENDED:
				doStateEnded();
				break;

			default:
				break;
		}
	}
	
    @Override
	public void onError(Exception e) {
        BFYLog.d(TAG, "ExoPlayer Error: " + e.getMessage());
    	
		if (mIsFirstBuffering && mPlayerController != null) {
			mPlayerController.reportPlayProcessStatInfo();
		}

    	if (mPlayErrorListener != null) {
    		mPlayErrorListener.onError(PlayErrorManager.EXOPLAYER_DECODE_FAILED);
    	}
	}
	
    @Override
	public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
    	// ExoPlayer 在输出第一帧画面时尚未调整画面比例，造成画面抖动现象。此处修正这个问题。
    	setPlaceHolderVisible(false);
    	
    	setVideoWidthHeightRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
	}
	
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        BFYLog.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        BFYLog.d(TAG, "surfaceDestroyed");
		if (mExoPlayer != null) {
			mExoPlayer.blockingClearSurface();
		}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        BFYLog.d(TAG, "surfaceChanged,format=" + format + ",w=" + width + ",h=" + height);

        if (mExoPlayer != null) {
			mExoPlayer.setSurface(holder.getSurface());
		}
    }

    @Override
    public void orientationChanged() {
    }

    @Override
    public void onCreateWindow() {
    	setPlaceHolderVisible(true);
    }

    @Override
    public void onDestroyWindow() {
    	releasePlayer();
        mPlayProxy = null;
    	setCurrentState(MediaPlayerConstant.STATE_IDLE);
    }

    @Override
    public void onPauseWindow() {
        if (isInPlaybackState()) {
            BFYLog.d(TAG, "onPauseWindow,isInPlaybackState,mSeekWhenPrepared=" + mSeekWhenPrepared);
            int currentPosition = getCurrentPosition();
            BFYLog.d(TAG, "onPauseWindow,isInPlaybackState,currentPosition=" + currentPosition);
            if (currentPosition > 0) {
                mSeekWhenPrepared = currentPosition;
            }
        }

        pause();
    }

    @Override
    public void onResumeWindow() {
        if (isInPlaybackState()) {
            if (!isPlayCompete()) {
                start();
                
                if (mSeekWhenPrepared > 0) {
                	seekTo(mSeekWhenPrepared);
                	mSeekWhenPrepared = 0;
                }
            }
        }
    }
    
    @Override
    public void onRestartWindow() {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void start() {
    	if (mExoPlayer != null) {
    		mExoPlayer.getPlayerControl().start();
    		setCurrentState(MediaPlayerConstant.STATE_PLAYING);
    	}
    }

    @Override
    public void pause() {
    	if (mExoPlayer != null) {
    		mExoPlayer.getPlayerControl().pause();
    		setCurrentState(MediaPlayerConstant.STATE_PAUSED);
    	}
    }

    @Override
    public void stop() {
    	releasePlayer();
        if (null != mPlayProxy) {
            mPlayProxy.stopPlayback(null);
        }
        setCurrentState(MediaPlayerConstant.STATE_IDLE);
    }

    @Override
    public int getCurrentPosition() {
    	if (mExoPlayer != null) {
            if (isPlayCompete()) {
            	return mDuration;
            } else {
            	int curPos = (int) mExoPlayer.getCurrentPosition();
            	if (mDuration == 0) {  // live
            		return curPos;
            	} else {
            		return Math.min(curPos, mDuration);
            	}
            }
    	} else {
    		return 0;
    	}
    }

    @Override
    public void seekTo(int pos) {
    	if (mExoPlayer != null) {
    		mIsSeeking = true;
    		mExoPlayer.getPlayerControl().seekTo(pos);
    	}
    }

    @Override
    public boolean isPlaying() {
    	if (mExoPlayer != null) {
    		return mExoPlayer.getPlayerControl().isPlaying();
    	} else {
    		return false;
    	}
    }

    @Override
    public boolean isInPlaybackState() {
        return (mPlayerInitilized && mCurrentState != MediaPlayerConstant.STATE_ERROR &&
                mCurrentState != MediaPlayerConstant.STATE_IDLE &&
                mCurrentState != MediaPlayerConstant.STATE_PREPARING);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    @Override
    public void fastForward() {
        if (!isInPlaybackState() || mCurrentState == MediaPlayerConstant.STATE_PLAYBACK_COMPLETED)
        	return;

        int value = getCurrentPosition() + mFastForwardIncrement;
        int duration = getDuration();
        if (value > duration) {
            value = duration;
        }
        
        seekTo(value);
    }

    @Override
    public void fastBackward() {
        if (!isInPlaybackState()) return;

        int value = getCurrentPosition() - mFastForwardIncrement;
        if (value < 0) {
            value = 0;
        }

        seekTo(value);
    }

    @Override
    protected void setPlayerTitle() {
        String title = (mMainTitle == null) ? "" : mMainTitle;
        String subTitle = (mSubTitle == null) ? "" : mSubTitle;
        
        if (subTitle.length() > 0)
        	title = title + " " + subTitle;
        
        if (null != mUiHandler) {
            mUiHandler.obtainMessage(MediaPlayerConstant.UI_SET_TITLE, -1, -1, title).sendToTarget();
        }
    }
    
    @Override
    public void setStatusController(StatusController statusController) {
        mStatusController = statusController;
    }

    @Override
    public void hideStatusController() {
    	setStatusControllerVisible(false);
    }

    @Override
    public void showPlaceHolder() {
    	setPlaceHolderVisible(true);
    }

    @Override
    public void sendEmptyMessage(int what) {
        if (null != mUiHandler) {
            mUiHandler.sendEmptyMessage(what);
        }
    }

    @Override
    public void executePlay(long pos) {
    	initPlayer();
    	start();
    	setMediaControllerVisible(true);
    }

    @Override
    public void playPrepare(int historyPosition) {
        BFYLog.d(TAG, "playPrepare,pos=" + historyPosition);
        if (historyPosition < 0) return;

        mDuration = (int) mVideoInfo.getDuration();
        if (historyPosition > 0) {
            mSeekWhenPrepared = historyPosition;
        } else {
            mSeekWhenPrepared = 0;
        }
        BFYLog.d(TAG, "playPrepare,mSeekWhenPrepared=" + mSeekWhenPrepared + ",mDuration=" + mDuration);

        releasePlayer();

        if (null != mPlayerController) {
            mPlayerController.playPrepare(historyPosition);
        }
        
        setCurrentState(MediaPlayerConstant.STATE_PREPARED);
    }

    @Override
    public void keycodeHome() {
    }

	@Override
	public void onScreenOn() {
	}

	@Override
	public void onScreenOff() {
	}

	@Override
	public void onUserPresent() {
	}
	
	private void setPlaceHolderVisible(boolean visible) {
        mUiHandler.sendEmptyMessage(visible ? MediaPlayerConstant.UI_PLACEHOLDER_SHOW : MediaPlayerConstant.UI_PLACEHOLDER_HIDE);
	}
    
	private void setStatusControllerVisible(boolean visible) {
        mUiHandler.sendEmptyMessage(visible ? MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW : MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE);
	}
	
	private void setMediaControllerVisible(boolean visible) {
        mUiHandler.sendEmptyMessage(visible ? MediaPlayerConstant.UI_MEDIA_CONTROLLER_SHOW : MediaPlayerConstant.UI_MEDIA_CONTROLLER_HIDE);
	}
	
	private void setVideoWidthHeightRatio(float widthHeightRatio) {
		if (mVideoAspectRatio != widthHeightRatio) {
			mVideoAspectRatio = widthHeightRatio;
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    final float MAX_ASPECT_RATIO_DEFORMATION_PERCENT = 0.01f;

	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
	    int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		if (mVideoAspectRatio != 0) {
			float viewAspectRatio = (float) width / height;
			float aspectDeformation = mVideoAspectRatio / viewAspectRatio - 1;
			if (aspectDeformation > MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
				height = (int) (width / mVideoAspectRatio);
			} else if (aspectDeformation < -MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
				width = (int) (height * mVideoAspectRatio);
			}
		}
		setMeasuredDimension(width, height);
	}
	
	private void onPrepared() {
    	if (mSeekWhenPrepared > 0) {
    		seekTo(mSeekWhenPrepared);
    		mSeekWhenPrepared = 0;
    	}
	}
    
    static class UiHandler extends BFYWeakReferenceHandler<VideoViewExo> {

        public UiHandler(VideoViewExo reference) {
            super(reference);
        }

        public UiHandler(VideoViewExo reference, Looper looper) {
            super(reference, looper);
        }

        @Override
        protected void handleMessage(VideoViewExo reference, Message msg) {
            int what = msg.what;
            BFYLog.d(reference.TAG, "UiHandler,what=" + what);
            
            switch (what) {
	            case MediaPlayerConstant.UI_PLACEHOLDER_SHOW: {
	                if (null != reference.mPlaceHolder) {
	                    reference.mPlaceHolder.show();
	                }
	                break;
	            }
	            
	            case MediaPlayerConstant.UI_PLACEHOLDER_HIDE: {
	                if (null != reference.mPlaceHolder) {
	                    reference.mPlaceHolder.hide();
	                }
	                break;
	            }
	            
	            case MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW: {
	                if (null != reference.mStatusController) {
	                    reference.mStatusController.show();
	                }
	                break;
	            }

	            case MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE: {
	                if (null != reference.mStatusController) {
	                    reference.mStatusController.hide();
	                }
	                break;
	            }

	            case MediaPlayerConstant.UI_MEDIA_CONTROLLER_SHOW: {
	                if (null != reference.mMediaController) {
	                    reference.mMediaController.show();
	                }
	                break;
	            }

	            case MediaPlayerConstant.UI_MEDIA_CONTROLLER_HIDE: {
	                if (null != reference.mMediaController) {
	                    reference.mMediaController.hide();
	                }
	                break;
	            }

	            case MediaPlayerConstant.UI_SET_TITLE: {
	                if (null != reference.mMediaController) {
	                    reference.mMediaController.setTitle((String) msg.obj);
	                }
	                break;
	            }
            }
        }
    }

}