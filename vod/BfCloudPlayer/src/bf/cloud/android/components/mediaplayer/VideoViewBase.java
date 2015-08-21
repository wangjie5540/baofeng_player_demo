package bf.cloud.android.components.mediaplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.LinkedList;

import bf.cloud.android.base.BFYEventBus;
import bf.cloud.android.components.mediaplayer.placeholder.BFYPlaceHolder;
import bf.cloud.android.components.mediaplayer.proxy.PlayProxy;
import bf.cloud.android.components.player.PlayerCommand;
import bf.cloud.android.events.PlayerEvent;
import bf.cloud.android.models.beans.BFYVideoInfo;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.modules.stat.StatInfo;

public abstract class VideoViewBase extends SurfaceView 
	implements SurfaceHolder.Callback, PlayerController.PlayerViewControl {

    private String TAG = VideoViewBase.class.getSimpleName();

    // 播放器当前的状态
    protected int mCurrentState = MediaPlayerConstant.STATE_IDLE;
    //调用VideoView方法如pause(),将要到达的状态
    protected int mTargetState = MediaPlayerConstant.STATE_IDLE;

    //以下是播放或显示视频信息需要的所有东西
    protected SurfaceHolder mSurfaceHolder = null;

    private static LinkedList<PlayerControllerListener> mPlayerControllerListeners = new LinkedList<PlayerControllerListener>();
    protected int mVideoWidth;
    protected int mVideoHeight;
    protected int mSurfaceWidth;
    protected int mSurfaceHeight;
    protected int mParentWidth;
    protected int mParentHeight;
    
    protected boolean mIsSeeking;
    protected int mSeekTmp = -1;

    //当前缓冲百分比
    protected int mCurrentBufferPercentage;
    //播放历史,毫秒
    protected int mSeekWhenPrepared = -1;
    //是否可以暂停、向前拖动、向后拖动
    protected boolean mCanPause;
    protected boolean mCanSeekBack;
    protected boolean mCanSeekForward;
    
    //主标题
    protected String mMainTitle;
    //子标题
    protected String mSubTitle;
    
    protected int mFastForwardIncrement = 30000;//30秒
    //当前播放事件
    protected PlayerEvent mPlayerEvent;
    //当前播放的视频数据代理
    protected PlayProxy mPlayProxy;

    //是否在退出状态
    protected boolean mQuitting;
    //是否正在执行onDestorying
    protected boolean mOnDestorying;
    //是否需要重新构造播放器
    protected boolean mNeedRecreatePlayer;
    //surface重建后是否需要创建播放器
    protected boolean mCreateMediaPlayer = true;

    //是否切换了剧集
    protected boolean mFirstVideo = true;
    //是否可以处理播放完毕的事件
    protected boolean mReceiveCompletion;

    protected int mComplation = MediaPlayerConstant.COMPLETION_AUTO;
    //onPause时是否执行了退出
    protected boolean mOnPauseQuitted;
    //是否解锁
    protected boolean mUserPresent = true;
    // 是否保持屏幕常亮
    protected boolean mKeepScreenOn = false;

    protected PlayerController mPlayerController;
    protected StatusController mStatusController;
    protected MediaController mMediaController;
    protected BFYPlaceHolder mPlaceHolder;    
    //当前播放的视频资源的总时长
    protected int mDuration;
    
    protected BFYVideoInfo mVideoInfo;

    protected PlayErrorListener mPlayErrorListener;

    protected View mAnchorView;
    
    // 统计信息
    protected StatInfo mStatInfo = new StatInfo();

    
    public VideoViewBase(Context context) {
        super(context);
        init();
    }

    public VideoViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoViewBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
    	mParentWidth = -1;
    	mParentHeight = -1;
        mVideoWidth = 0;
        mVideoHeight = 0;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = MediaPlayerConstant.STATE_IDLE;
        mTargetState = MediaPlayerConstant.STATE_IDLE;
        
        getHolder().addCallback(this); 

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // android 3.0
            if (null != getHolder()) {
            	getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }
    }
    
    /**
     * 视频播放完毕
     */
    protected void onCompletion() {
        BFYLog.d(TAG, "onCompletion");
        if (!mReceiveCompletion) {
            BFYLog.d(TAG, "onCompletion,!mReceviveCompletion");
            return;
        }
        //如果是手动拖动进度条完成,则不处理
        if (mComplation == MediaPlayerConstant.COMPLETION_MANUAL) return;

        postPlayComplete();
    }

    protected void postPlayComplete() {
        BFYLog.d(TAG, "postPlayComplete,start");
        if (mQuitting || mOnDestorying) return;

        mCurrentState = MediaPlayerConstant.STATE_PLAYBACK_COMPLETED;
        mTargetState = MediaPlayerConstant.STATE_PLAYBACK_COMPLETED;

        postDelayed(new Runnable() {
            @Override
            public void run() {
                int pos = getCurrentPosition();
                BFYLog.d(TAG, "postPlayComplete,mDuration=" + mDuration + ",pos=" + pos);
                PlayerCommand playerCommand = new PlayerCommand();
                playerCommand.setCommand(PlayerCommand.COMPLETE);
                PlayerEvent event = new PlayerEvent(playerCommand);
                BFYEventBus.getInstance().post(event);
            }
        }, 500);
        BFYLog.d(TAG, "postPlayComplete,end");
    }

    protected void setAppKeepScreenOn(boolean keepScreenOn) {
    	if (keepScreenOn != mKeepScreenOn) {
    		mKeepScreenOn = keepScreenOn;
    		
	    	Activity activity = (Activity)getContext();
	    	if (keepScreenOn) {
	    		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    	} else {
	    		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    	}
	
	    	BFYLog.d(TAG, "setAppKeepScreenOn:" + keepScreenOn);
    	}
    }
    
    protected void onPlayerStateChanged(int currentState) {
    	autoSetKeepScreenOn(currentState);
    }
    
    private void autoSetKeepScreenOn(int currentState) {
    	boolean keepScreenOn = (
    			currentState == MediaPlayerConstant.STATE_PREPARING ||
				currentState == MediaPlayerConstant.STATE_PREPARED ||
				currentState == MediaPlayerConstant.STATE_PLAYING ||
				currentState == MediaPlayerConstant.STATE_PAUSED
				);
    	
    	setAppKeepScreenOn(keepScreenOn);
    }

    @Override
    public boolean isSurfaceValid() {
        return null != mSurfaceHolder;
    }

    @Override
    public int getBufferPercentage() {
        if (mCurrentBufferPercentage <= 0) {
            mCurrentBufferPercentage = 0;
        }
        return mCurrentBufferPercentage;
    }

    @Override
    public boolean isPlayCompete() {
        return mCurrentState == MediaPlayerConstant.STATE_PLAYBACK_COMPLETED;
    }

    @Override
    public int getDuration() {
        BFYLog.d(TAG, "mDuration########=" + mDuration);
        return mDuration;
    }

    /**
     * 设置媒体播放控制器
     *
     * @param controller
     */
    @Override
    public void setMediaController(MediaController controller) {
        BFYLog.d(TAG, "setMediaController,(null == controller)=" + (null == controller));
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    /**
     * 将媒体播放控制器附加到视频播放视图上
     */
    protected void attachMediaController() {
        BFYLog.d(TAG, "attachMediaController,(null == mMediaController)=" + (null == mMediaController));
        if (mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            mMediaController.setAnchorView(mAnchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
        if (mPlaceHolder != null) {
            mPlaceHolder.setAnchorView(mAnchorView);
        }
    }

    @Override
    public void setStatusController(StatusController statusController) {
        BFYLog.d(TAG, "setStatusController");
        mStatusController = statusController;
        if (mStatusController != null) {
            if (isInPlaybackState() && !mIsSeeking) {
                mStatusController.hide();
            } else {
                mStatusController.show();
            }
        }
    }

    @Override
    public void orientationChanged() {
    }

    @Override
    public void onStopWindow() {
        BFYLog.d(TAG, "onStopWindow");
        mCreateMediaPlayer = true;
        mNeedRecreatePlayer = true;

        if (null != mPlayProxy) {
            mPlayProxy.onStop();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        BFYLog.d(TAG, "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BFYLog.d(TAG, "onDetachedFromWindow");
    }

    @Override
    public void hideStatusController() {
    }

    protected void setPlayerTitle() {
    }

    @Override
    public void setPlayProxy(PlayProxy playProxy) {
        mPlayProxy = playProxy;
    }

    @Override
    public void setVideoInfo(BFYVideoInfo videoInfo) {
    	mVideoInfo = videoInfo;
    }
    
    @Override
    public void setVideoTitle(String title) {
        mMainTitle = title;
        setPlayerTitle();
    }

    @Override
    public int getSeekPosition() {
        return mSeekWhenPrepared;
    }

    @Override
    public void setSeekPosition(int seekPos) {
        mSeekTmp = mSeekWhenPrepared = seekPos;
    }

    @Override
    public void setVideoSubTitle(String subTitle) {
        mSubTitle = subTitle;
        setPlayerTitle();
    }

    @Override
    public void setCurrentState(int state) {
    	BFYLog.d(TAG, "setCurrentState: " + state);
    	if (state != mCurrentState) {
    		mCurrentState = state;
    		onPlayerStateChanged(mCurrentState);
    	}
    }

    @Override
    public int getCurrentState() {
    	BFYLog.d(TAG, "getCurrentState: " + mCurrentState);
    	return mCurrentState;
    }
    
    @Override
    public void setTargetState(int state) {
        mTargetState = state;
    }

    @Override
    public void setIsSeeking(boolean seeking) {
        mIsSeeking = seeking;
    }

    @Override
    public void setFirstVideo(boolean fist) {
        mFirstVideo = fist;
    }

    @Override
    public void setReceiveCompletion(boolean canReceive) {
        mReceiveCompletion = canReceive;
    }

    @Override
    public void stop() {
        BFYLog.d(TAG, "stop");
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    @Override
    public Context getControlContext() {
        return getContext();
    }

    @Override
    public void setQuit(boolean quit) {
        mQuitting = quit;
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public void setPlayerController(PlayerController playerController) {
        mPlayerController = playerController;
    }

    @Override
    public void setPlaceHolder(BFYPlaceHolder holder) {
        mPlaceHolder = holder;
    }
    
    @Override
    public void setParentLayoutParams(int width, int height) {
    	mParentWidth = width;
    	mParentHeight = height;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l) {
    }

    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener l) {
    }

    /**
     * 视频加载完毕,准备播放时的监听器.
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
    }

    /**
     * 设置视频播放完毕后的监听器.
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l) {
    }

    /**
     * 设置播放器在播放或设置期间发生错误时监听器.
     */
    public void setPlayErrorListener(PlayErrorListener listener) {
        mPlayErrorListener = listener;
    }

    @Override
    public void setAnchorView(View anchorView) {
        mAnchorView = anchorView;
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BFYLog.d(TAG, "onKeyDown,keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mPlayerController.backToPortrait()) {
            	return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void unregisterPlayerListener(PlayerControllerListener l) {
        mPlayerControllerListeners.remove(l);

    }

    @Override
    public void registerPlayerListener(PlayerControllerListener l) {
        mPlayerControllerListeners.add(l);

    }
    
    @Override
    public StatInfo getStatInfo() {
    	return mStatInfo;
    }

    protected void sendOnCompletion(){
        for (PlayerControllerListener mPlayerControllerListener : mPlayerControllerListeners) {
            mPlayerControllerListener.onCompletion();
        }
    }

    protected void sendOnPrepare(){
        for (PlayerControllerListener mPlayerControllerListener : mPlayerControllerListeners) {
            mPlayerControllerListener.onPrepare();
        }
    }

    protected void sendOnVideoSizeChanged(){
        for (PlayerControllerListener mPlayerControllerListener : mPlayerControllerListeners) {
            mPlayerControllerListener.onVideoSizeChanged();
        }
    }

    protected void sendOnReadytoPlay(){
        for (PlayerControllerListener mPlayerControllerListener : mPlayerControllerListeners) {
            mPlayerControllerListener.onReadytoPlay();
        }
    }


}
