package bf.cloud.android.modules.player.videoviewsd;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.mediaplayer.StatusController;
import bf.cloud.android.components.mediaplayer.proxy.PlayProxy;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYWeakReferenceHandler;

/**
 * 软解视频播放容器
 *
 * Created by gehuanfei on 2014/9/19.
 */
public class VideoViewStormSw extends VideoViewSw {
	
    private final String TAG = VideoViewStormSw.class.getSimpleName();

    private boolean mPlayerInitilized;
    private UiHandler mUiHandler;
    private boolean mSurfaceRecreate;

    private boolean mIsFirstBuffering;
    private long mFirstBufferStartTime;
    private long mBufferingTime;

    
    public VideoViewStormSw(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }

    private void initVideoView() {
        BFYLog.d(TAG, "initVideoView start");
        mUiHandler = new UiHandler(this);

        mVideoWidth = 0;
        mVideoHeight = 0;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        setVideoEventListener(mEventListener);
        BFYLog.d(TAG, "initVideoView end");
    }

    @Override
    public boolean isInPlaybackState() {
        return (mPlayerInitilized && mCurrentState != MediaPlayerConstant.STATE_ERROR &&
                mCurrentState != MediaPlayerConstant.STATE_IDLE &&
                mCurrentState != MediaPlayerConstant.STATE_PREPARING);
    }

    @Override
    public void orientationChanged() {
        BFYLog.d(TAG, "orientationChanged");
        if (mOnDestorying || mQuitting) return;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = FrameLayout.LayoutParams.MATCH_PARENT;
        params.height = FrameLayout.LayoutParams.MATCH_PARENT;
        setLayoutParams(params);

        executeSurfaceChanged();
    }

    @Override
    public void onPauseWindow() {
        BFYLog.d(TAG, "onPauseWindow");
        
        super.onPauseWindow();

        if (isInPlaybackState()) {
            BFYLog.d(TAG, "onPauseWindow,isInPlaybackState,mSeekWhenPrepared=" + mSeekWhenPrepared);
            int currentPosition = getCurrentPosition();
            BFYLog.d(TAG, "onPauseWindow,isInPlaybackState,currentPosition=" + currentPosition);
            if (currentPosition > 0) {
                mSeekWhenPrepared = currentPosition;
            }
        }
        if (mQuitting) {
            BFYLog.d(TAG, "onPauseWindow,mQuitting");
            mOnPauseQuitted = true;
            onPauseSw();
            quitPrepare();
            stopPlayback();
            return;
        }
        pause();
    }

    @Override
    public void onRestartWindow() {
        BFYLog.d(TAG, "onRestartWindow");
        super.onRestartWindow();
        if (null != mUiHandler) {
            mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW);
        }
    }

    @Override
    public void onResumeWindow() {
        BFYLog.d(TAG, "onResumeWindow");
        super.onResumeWindow();
        resumeMediaPlayer();
    }

    @Override
    public void onCreateWindow() {
        BFYLog.d(TAG, "onCreateWindow");
        super.onCreateWindow();
        mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_PLACEHOLDER_SHOW);
    }

    @Override
    public void onDestroyWindow() {
        BFYLog.d(TAG, "onDestroyWindow,mOnPauseQuitted=" + mOnPauseQuitted);
        super.onDestroyWindow();
        if (!mOnPauseQuitted) {
            quitPrepare();
            mOnDestorying = true;
            stopPlayback();
        } else {
            mOnDestorying = true;
        }
        mPlayProxy = null;
        mPlayerEvent = null;
    }

    private void quitPrepare() {
        BFYLog.d(TAG, "quitPrepare");
        if (mMediaController != null) {
        	mMediaController.reset();
        }
        //if (null != mPlayProxy)
            //mPlayProxy.recordHistory(getCurrentPosition());
    }

    public void stopPlayback() {
        BFYLog.d(TAG, "stopPlayback");
//        playerStop();
//        playerRelease();
        release(true);
//        mCurrentState = MediaPlayerConstant.STATE_IDLE;
//        mTargetState = MediaPlayerConstant.STATE_IDLE;

        if (null != mPlayProxy) {
            mPlayProxy.stopPlayback(callback);
        }
    }

    PlayProxy.CallbackOuter callback = new PlayProxy.CallbackOuter() {
        @Override
        public void playStopSuccess() {
            BFYLog.d(TAG, "CallbackOuter,playStopSuccess");
            mPlayProxy = null;

            mPlayerController = null;
            mStatusController = null;
            mMediaController = null;
            mPlaceHolder = null;
        }

        @Override
        public void playStopFailure() {

        }
    };
    
    @Override
    public void fastForward() {
        BFYLog.d(TAG, "fastForward start");
        if (mCurrentState == MediaPlayerConstant.STATE_PLAYBACK_COMPLETED) {
            BFYLog.d(TAG, "fastForward,mCurrentState == STATE_PLAYBACK_COMPLETED");
            return;
        }
        if (!isInPlaybackState()) return;

        int value = getCurrentPosition() + mFastForwardIncrement;
        int duration = getDuration();
        if (value > duration) {
            value = duration;
        }
        seekTo(value);
        BFYLog.d(TAG, "fastForward end");
    }

    @Override
    public void fastBackward() {
        BFYLog.d(TAG, "fastBackward start");
        if (!isInPlaybackState()) return;

        int value = getCurrentPosition() - mFastForwardIncrement;
        if (value < 0) {
            value = 0;
        }
        seekTo(value);
        BFYLog.d(TAG, "fastBackward end");
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
    public void setStatusController(StatusController controller) {
        mStatusController = controller;
        if (mStatusController != null) {
            if (mCurrentState == MediaPlayerConstant.STATE_PREPARING || mIsSeeking) {
                mStatusController.show();
            } else {
                mStatusController.hide();
            }
        }
    }

    @Override
    public void start() {
        BFYLog.d(TAG, "start,isInPlaybackState()=" + isInPlaybackState());
        setTargetState(MediaPlayerConstant.STATE_PLAYING);
        if (null == mSurfaceHolder) {
            BFYLog.d(TAG, "start,null == mSurfaceHolder");
            return;
        }

        if (isInPlaybackState()) {
            if (isPlayCompete()) {
                release(false);
                openVideo();
                return;
            }
            int total = (playerGetTotalLength() * 1000);
            BFYLog.d(TAG, "start,playerGetTotalLength()1=" + total);
            playerStart();
            total = (playerGetTotalLength() * 1000);
            BFYLog.d(TAG, "start,playerGetTotalLength()2=" + total + ",mIsSeeking=" + mIsSeeking + ",isPlayCompete()=" + isPlayCompete());            
            if (null != mPlayerController) {
            	if (mPlayerController.isLivePlayer()) {
            		total = playerGetPosition() * 1000;
            	}
            }
            if (!mIsSeeking && total > 0) {
                hideStatusController();
            } else {
                showStatus();
            }
            setCurrentState(MediaPlayerConstant.STATE_PLAYING);
            if (null != mMediaController) {
                mMediaController.startProgress();
            }
        }
    }

    private void onPauseSw() {
        BFYLog.d(TAG, "onPauseSw");
        if (null != mMediaController) {
            mMediaController.stopProgress();
        }
    }

    @Override
    public void pause() {
        BFYLog.d(TAG, "pause");
        if (mQuitting) {
            BFYLog.d(TAG, "pause,mQuitting");
            return;
        }

        if (isInPlaybackState()) {
            BFYLog.d(TAG, "pause,isInPlaybackState");
            if (mCurrentState != MediaPlayerConstant.STATE_PAUSED) {
                BFYLog.d(TAG, "pause,mCurrentState != MediaPlayerConstant.STATE_PAUSED");
                playerPause();
                setCurrentState(MediaPlayerConstant.STATE_PAUSED);
                if (null != mMediaController) {
                    mMediaController.stopProgress();
                }
            }
        }
        setTargetState(MediaPlayerConstant.STATE_PAUSED);

        if (null != mPlayProxy) {
            mPlayProxy.setLastFromEvent(false);
        }
    }

    @Override
    public void stop() {
        stopPlayback();
    }

    @Override
    public int getCurrentPosition() {
        if (mOnDestorying) return mSeekWhenPrepared;
        
        if (isPlayCompete()) {
            int duration = playerGetTotalLength() * 1000;
            int duration2 = getDuration();
            BFYLog.d(TAG, "getCurrentPosition,isPlayCompete,duration1=" + duration + ",duration2=" + duration2);
            return duration2;
        }

        if (!isInPlaybackState() || mIsSeeking) {
            return mSeekTmp;
        }

        return playerGetPosition() * 1000;
    }

	private void executeSurfaceChanged() {
        BFYLog.d(TAG, "executeSurfaceChanged");
        int width, height = 0;
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            BFYLog.d(TAG, "executeSurfaceChanged,Surface.ROTATION_90 || Surface.ROTATION_270");
            DisplayMetrics metrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                windowManager.getDefaultDisplay().getRealMetrics(metrics);
            } else {
                windowManager.getDefaultDisplay().getMetrics(metrics);
            }
            BFYLog.d(TAG, "executeSurfaceChanged,widthPixels=" + metrics.widthPixels + ",heightPixels=" + metrics.heightPixels);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        } else {
            BFYLog.d(TAG, "executeSurfaceChanged,Surface.ROTATION_0 || Surface.ROTATION_180");
            width = getWidth();
            height = getHeight();
        }
        BFYLog.d(TAG, "executeSurfaceChanged,width=" + width + ",height=" + height);
        if (width > 0 && height > 0) {
            if (null != getHolder()) {
                getHolder().setFixedSize(width, height);
            }
            //onSurfaceChanged(width, height);
        }
    }

    @Override
    public void seekTo(int msec) {
        BFYLog.d(TAG, "seekTo,msec=" + msec + ",all duration=" + getDuration());

        mIsSeeking = true;
        mSeekWhenPrepared = msec;
        mSeekTmp = msec;
        showStatus();
        
        if (isInPlaybackState()) {
            int seekPos = msec;
            if (seekPos > mVideoInfo.getDuration())
            	seekPos = (int) mVideoInfo.getDuration();
            
            playerSeekTo(seekPos);
            mSeekWhenPrepared = 0;

            BFYLog.d(TAG, "seekTo,end");
        }
    }

    @Override
    public void hideStatusController() {
        BFYLog.d(TAG, "hideStatus");
        if (null != mUiHandler) {
            mUiHandler.removeMessages(MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW);
            Message message = mUiHandler.obtainMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE);
            mUiHandler.sendMessage(message);
        }
    }

    private void showStatus() {
        BFYLog.d(TAG, "showStatus");
        if (null != mUiHandler) {
            mUiHandler.removeMessages(MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE);
            mUiHandler.removeMessages(MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW);
            Message message = mUiHandler.obtainMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW);
            mUiHandler.sendMessageDelayed(message, 350);
        }
    }

    private void hidePlaceHolder() {
        BFYLog.d(TAG, "hidePlaceHolder");
        if (null != mPlaceHolder) {
            mPlaceHolder.hide();
        }
    }

    @Override
    public void onScreenOn() {
        BFYLog.d(TAG, "onScreenOn");
    }

    @Override
    public void onScreenOff() {
        BFYLog.d(TAG, "onScreenOff");
        mUserPresent = false;
    }

    @Override
    public void onUserPresent() {
        BFYLog.d(TAG, "onUserPresent");
        mUserPresent = true;
        resumeMediaPlayer();
    }

    private void resumeMediaPlayer() {
        BFYLog.d(TAG, "resumeMediaPlayer,mUserPresent=" + mUserPresent);
        if (!mUserPresent) return;

        if (isInPlaybackState()) {
            BFYLog.d(TAG, "resumeMediaPlayer,isPlayCompete=" + isPlayCompete());
            if (!isPlayCompete()) {
                start();
            }
        } else {
            BFYLog.d(TAG, "resumeMediaPlayer,not isInPlaybackState()");
            showStatus();
        }
    }

    static class UiHandler extends BFYWeakReferenceHandler<VideoViewStormSw> {

        public UiHandler(VideoViewStormSw reference) {
            super(reference);
        }

        public UiHandler(VideoViewStormSw reference, Looper looper) {
            super(reference, looper);
        }

        @Override
        protected void handleMessage(VideoViewStormSw reference, Message msg) {
            int what = msg.what;
            BFYLog.d(reference.TAG, "UiHandler,what=" + what);
            if (what == MediaPlayerConstant.PROXY_OPEN_VIDEO) {
                BFYLog.d(reference.TAG, "UiHandler,PROXY_OPEN_VIDEO");
                reference.openVideo();
            } else if (what == MediaPlayerConstant.UI_SURFACE_INVISIBLE_VISIBLE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_SURFACE_INVISIBLE_VISIBLE,reference.mSurfaceRecreate=" + reference.mSurfaceRecreate);
                boolean recreate = true;
                if (reference.mSurfaceRecreate) {
                    if (null != reference.mSurfaceHolder) {
                        recreate = false;
                    }
                }
                BFYLog.d(reference.TAG, "UiHandler,UI_SURFACE_INVISIBLE_VISIBLE,recreate=" + recreate);
                if (recreate) {
                    reference.setVisibility(View.INVISIBLE);
                    reference.setVisibility(View.VISIBLE);
                }
            } else if (what == MediaPlayerConstant.START_FAILURE) {
                reference.hideStatusController();
            } else if (what == MediaPlayerConstant.UI_PLACEHOLDER_SHOW) {
                BFYLog.d(reference.TAG, "UiHandler,UI_PLACEHOLDER_SHOW");
                if (null != reference.mPlaceHolder) {
                    reference.mPlaceHolder.show();
                }
            } else if (what == MediaPlayerConstant.UI_PLACEHOLDER_HIDE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_PLACEHOLDER_HIDE");
                if (null != reference.mPlaceHolder) {
                    reference.mPlaceHolder.hide();
                }
            } else if (what == MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW) {
                BFYLog.d(reference.TAG, "UiHandler,UI_STATUS_CONTROLLER_SHOW");
                if (null != reference.mStatusController) {
                    reference.mStatusController.show();
                }
            } else if (what == MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_STATUS_CONTROLLER_HIDE");
                if (null != reference.mStatusController) {
                    reference.mStatusController.hide();
                }
            } else if (what == MediaPlayerConstant.UI_SET_TITLE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_SET_TITLE");
                if (null != reference.mMediaController) {
                    reference.mMediaController.setTitle((String) msg.obj);
                }
            } else if (what == MediaPlayerConstant.UI_P2P_START_FAILURE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_P2P_START_FAILURE");
            } else if (what == MediaPlayerConstant.UI_P2P_INIT_FAILURE) {
                BFYLog.d(reference.TAG, "UiHandler,UI_P2P_INIT_FAILURE");
//                if (null != reference.mPlayerController) {
//                    reference.mPlayerController.quit();
//                }
            }
        }
    }

    @Override
    public boolean isPlaying() {
        if (!mPlayerInitilized) return false;
        if (mCurrentState == MediaPlayerConstant.STATE_PAUSED) return false;
        if (mCurrentState == MediaPlayerConstant.STATE_PLAYING || mCurrentState == MediaPlayerConstant.STATE_PREPARED || mIsSeeking)
            return true;
        return false;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        playerSetVolume(leftVolume, rightVolume);
    }

    @Override
    public void showPlaceHolder() {
        BFYLog.d(TAG, "showPlaceHolder,(null==mUiHandler)=" + (null == mUiHandler));
//        release(true);
        if (null != mUiHandler) {
            mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_PLACEHOLDER_SHOW);
        }
    }

    /**
     * 播放视频
     *
     * @param historyPosition        视频播放位置
     */
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

        release(false);
        if (null != mPlayerController) {
            //请求PlayerController从videoIndex视频的pos处开始播放
            mPlayerController.playPrepare(historyPosition);
        }
    }

    @Override
    public void executePlay(long pos) {
        BFYLog.d(TAG, "executePlay,pos=" + pos);
        mSurfaceRecreate = (null == mSurfaceHolder);
        mCreateMediaPlayer = true;
        BFYLog.d(TAG, "executePlay,(null == mSurfaceHolder)=" + mSurfaceRecreate);
        if (null != mUiHandler) {
            mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_SURFACE_INVISIBLE_VISIBLE);
        }
    }

    private void openVideo() {
        BFYLog.d(TAG, "openVideo start");
        if (null == mSurfaceHolder || null == mPlayProxy || !mPlayProxy.isPlayValid())
            return;

        if (mNeedRecreatePlayer) {
            BFYLog.d(TAG, "openVideo,mMediaPlayerReCreate");
            //createMediaPlayer();
            mNeedRecreatePlayer = false;
            mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE);
            return;
        }
        if (mPlayerInitilized) {
            BFYLog.d(TAG, "openVideo,mPlayerInitilized");
            return;
        }
        createMediaPlayer();
        BFYLog.d(TAG, "openVideo end");
    }

    private void createMediaPlayer() {
        BFYLog.d(TAG, "createMediaPlayer start");
        mPlayerInitilized = true;
        String videoUrl;
        if (null != mPlayProxy) {
            videoUrl = mPlayProxy.getVideoUrl();
        } else {
            videoUrl = "valid path";
        }
        BFYLog.d(TAG, "createMediaPlayer,videoUrl=" + videoUrl);
        playerSetDataSource(videoUrl);
        setCurrentState(MediaPlayerConstant.STATE_PREPARING);
        attachMediaController();
        initPlayer();
        mHandler.sendEmptyMessage(CALLBACK_ONPREPARE);
        start();
        BFYLog.d(TAG, "createMediaPlayer end");
    }

    /**
     * 释放在任意状态MediaPlayer
     */
    private void release(boolean cleartargetstate) {
        BFYLog.d(TAG, "release start,mPlayerInitilized=" + mPlayerInitilized);
        if (!mPlayerInitilized) {
            return;
        }

        mReceiveCompletion = false;
        playerStop();
        playerRelease();
        if (!mIsSeeking) {
        	setCurrentState(MediaPlayerConstant.STATE_IDLE);
            if (cleartargetstate) {
            	setTargetState(MediaPlayerConstant.STATE_IDLE);
            }
        }
        mPlayerInitilized = false;
        BFYLog.d(TAG, "release end");
    }

    private void onPrepared() {
        BFYLog.d(TAG, "onPrepared");
        if (mOnDestorying || mQuitting) return;

        mUiHandler.sendEmptyMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_HIDE);
        hidePlaceHolder();
        setCurrentState(MediaPlayerConstant.STATE_PREPARED);

        mCanPause = mCanSeekBack = mCanSeekForward = true;

        if (mMediaController != null) {
            mMediaController.setEnabled(true);
        }
        mVideoWidth = getWidth();
        mVideoHeight = getHeight();

        //调用seekTo()方法可能会改变mSeekWhenPrepared值
        int seekToPosition = mSeekWhenPrepared;
        BFYLog.d(TAG, "onPrepared,mSeekWhenPrepared=" + mSeekWhenPrepared);
        if (seekToPosition > 0) {
            seekTo(seekToPosition);
        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            BFYLog.d(TAG, "onPrepared,mSurfaceWidth=" + mSurfaceWidth + ",mVideoWidth=" + mVideoWidth + ",mSurfaceHeight=" + mSurfaceHeight + ",mVideoHeight=" + mVideoHeight);
            if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                if (mTargetState == MediaPlayerConstant.STATE_PLAYING) {
                    BFYLog.d(TAG, "onPrepared,mTargetState == STATE_PLAYING");
                    start();
                    if (mMediaController != null) {
                        mMediaController.show();
                    }
                } else if (!isPlaying() &&
                        (seekToPosition != 0 || getCurrentPosition() > 0)) {
                    if (mMediaController != null) {
                        mMediaController.show(0);
                    }
                }
            }
        } else {
            if (mTargetState == MediaPlayerConstant.STATE_PLAYING) {
                start();
            }
        }
    }

    private void onVideoSizeChanged() {
        mVideoWidth = getWidth();
        mVideoHeight = getHeight();
        BFYLog.d(TAG, "onVideoSizeChanged,mVideoWidth=" + mVideoWidth + ",mVideoHeight=" + mVideoHeight + ",mIsSeeking=" + mIsSeeking);
        if (null != getHolder()) {
            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        }
        if (mIsSeeking) {
            int now = playerGetPosition() * 1000;
            BFYLog.d(TAG, "getCurrentPosition,now=" + now + ",mSeekTmp=" + mSeekTmp);
            if (now >= mSeekTmp) {
                mIsSeeking = false;
                mSeekTmp = 0;
                executeSurfaceChanged();
                hideStatusController();
            }
        }
    }

    private void onError() {
    	setCurrentState(MediaPlayerConstant.STATE_ERROR);
    	setTargetState(MediaPlayerConstant.STATE_ERROR);
        if (mMediaController != null) {
            mMediaController.hide();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        BFYLog.d(TAG, "surfaceCreated,createMediaPlayer=" + mCreateMediaPlayer);
        if (mCreateMediaPlayer && isPlayerSizeInited()) {
            createMediaPlayer(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        BFYLog.d(TAG, "surfaceDestroyed");
        mSurfaceHolder = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);

        if (mOnDestorying || mQuitting) return;

        mSurfaceHolder = holder;
        mSurfaceWidth = w;
        mSurfaceHeight = h;
        BFYLog.d(TAG, "surfaceChanged,format=" + format + ",mSurfaceWidth=" + mSurfaceWidth + ",mSurfaceHeight=" + mSurfaceHeight + ",mVideoWidth=" + mVideoWidth + ",mVideoHeight=" + mVideoHeight);
        boolean isValidState = (mTargetState == MediaPlayerConstant.STATE_PLAYING);
        boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
        BFYLog.d(TAG, "surfaceChanged,isValidState=" + isValidState + ",hasValidSize=" + hasValidSize + ",createMediaPlayer=" + mCreateMediaPlayer);

        if (mCreateMediaPlayer) {
            createMediaPlayer(holder);
            return;
        }
        if (isValidState && hasValidSize) {
            BFYLog.d(TAG, "surfaceChanged,mSeekWhenPrepared=" + mSeekWhenPrepared);
            if (mSeekWhenPrepared > 0) {
                seekTo(mSeekWhenPrepared);
            }
            start();
        }
    }

    private void createMediaPlayer(SurfaceHolder holder) {
        mCreateMediaPlayer = false;
        mSurfaceHolder = holder;
        openVideo();
    }

    private VideoViewSw.EventListener mEventListener = new VideoViewSw.EventListener() {
    	
    	@Override
		public void onStateChanged(int state) {
    		switch (state) {
    		case STATE_PREPARING:
    			doStatePreparing();
    			break;
    		case STATE_BUFFERING:
    			doStateBuffering();
    			break;
    		case STATE_READY:
    			doStateReady();
    			break;
    		case STATE_ENDED:
    			doStateEnded();
    			break;
    		default:
    			break;
    		}
    	}
    	
    	@Override
    	public void onError(int errorCode) {
            BFYLog.d(TAG, "error: " + errorCode);

    		if (mIsFirstBuffering && mPlayerController != null) {
    			mPlayerController.reportPlayProcessStatInfo();
    		}

            mHandler.sendEmptyMessage(CALLBACK_ONERROR);

        	if (mPlayErrorListener != null) {
        		mPlayErrorListener.onError(PlayErrorManager.SOFT_DECODE_FAILED);
        	}
    	}
    	
    	@Override
    	public void onVideoSizeChanged() {
            BFYLog.d(TAG, "onVideoSizeChanged");
            if (mOnDestorying) return;
            mHandler.sendEmptyMessage(CALLBACK_VIDEO_SIZE_CHANGED);
    	}
    	
    	private void doStatePreparing() {
            BFYLog.d(TAG, "doStatePreparing");
    		mIsFirstBuffering = true;
    		mFirstBufferStartTime = System.currentTimeMillis();
    	}
    	
    	private void doStateBuffering() {
            BFYLog.d(TAG, "doStateBuffering");
            
            mBufferingTime = System.currentTimeMillis();
			if (!mIsFirstBuffering && !mIsSeeking) {
				mStatInfo.breakCount++;
			}
			
            showStatus();
    	}
    	
    	private void doStateReady() {
            BFYLog.d(TAG, "doStateReady");
            mReceiveCompletion = true;
            mIsSeeking = false;
            mSeekTmp = 0;

            executeSurfaceChanged();
            hideStatusController();

    		if (mIsFirstBuffering) {
    			mStatInfo.firstBufferTime = (int)(System.currentTimeMillis() - mFirstBufferStartTime);
    			mStatInfo.firstBufferSuccess = true;
    			mIsFirstBuffering = false;
    			
    			if (mPlayerController != null) {
    				mPlayerController.reportPlayProcessStatInfo();
    			}
    		}
    		
			if (!mIsFirstBuffering && !mIsSeeking) {
				final int MIN_BREAK_MS = 500;
				if ((int)(System.currentTimeMillis() - mBufferingTime) < MIN_BREAK_MS) {
					mStatInfo.breakCount = Math.max(0, mStatInfo.breakCount - 1);
				}
			}
    	}
    	
    	private void doStateEnded() {
            BFYLog.d(TAG, "doStateEnded");
            if (mOnDestorying || mQuitting) return;
            if (mPlayerController.isLivePlayer()) return;

            if (mReceiveCompletion) {
                BFYLog.d(TAG, "playerCallbackOnCompletion,send CALLBACK_ONCOMPLETION msg");
                mHandler.sendEmptyMessage(CALLBACK_ONCOMPLETION);
            }
            mReceiveCompletion = true;
    	}
    };
    
    private static final int CALLBACK_VIDEO_SIZE_CHANGED = 0;
    private static final int CALLBACK_ONPREPARE = 1;
    private static final int CALLBACK_ONCOMPLETION = 2;
    private static final int CALLBACK_ONERROR = 3;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if (what == CALLBACK_VIDEO_SIZE_CHANGED) {
                onVideoSizeChanged();
            } else if (what == CALLBACK_ONPREPARE) {
                onPrepared();
            } else if (what == CALLBACK_ONCOMPLETION) {
                onCompletion();
            } else if (what == CALLBACK_ONERROR) {
                onError();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BFYLog.d(TAG, "onKeyDown,keyCode=" + keyCode);
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                BFYLog.d(TAG, "onKeyDown,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE");
                if (isInPlaybackState()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                if (isInPlaybackState()) {
                    BFYLog.d(TAG, "onKeyDown,KeyEvent.KEYCODE_MEDIA_STOP");
                    pause();
                    mMediaController.show();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                mMediaController.show(360000);
                mMediaController.onKeyDown(keyCode, event);
            } else if (keyCode == KeyEvent.ACTION_UP) {
                mMediaController.show();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void sendEmptyMessage(int what) {
        BFYLog.d(TAG, "sendEmptyMessage,what=" + what);
        if (null != mUiHandler) {
            mUiHandler.sendEmptyMessage(what);
        }
    }

    @Override
    public void keycodeHome() {
        BFYLog.d(TAG, "keycodeHome");
    }
}
