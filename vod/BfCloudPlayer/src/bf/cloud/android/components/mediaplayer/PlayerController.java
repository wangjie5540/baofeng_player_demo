package bf.cloud.android.components.mediaplayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.BFYNetworkStatusData;
import bf.cloud.android.components.mediaplayer.brightness.BrightnessLayer;
import bf.cloud.android.components.mediaplayer.complete.PlayCompleteHolder;
import bf.cloud.android.components.mediaplayer.definition.DefinitionController;
import bf.cloud.android.components.mediaplayer.error.PlayErrorHolder;
import bf.cloud.android.components.mediaplayer.placeholder.BFYPlaceHolder;
import bf.cloud.android.components.mediaplayer.playprogress.ProgressLayer;
import bf.cloud.android.components.mediaplayer.proxy.P2pPlayProxy;
import bf.cloud.android.components.mediaplayer.proxy.PlayProxy;
import bf.cloud.android.components.mediaplayer.volume.VolumeLayer;
import bf.cloud.android.components.player.PlayerCommand;
import bf.cloud.android.events.BFYNetworkStatusReportEvent;
import bf.cloud.android.events.PlayerEvent;
import bf.cloud.android.models.beans.BFYVideoInfo;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.modules.p2p.MediaCenter;
import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.modules.stat.StatReporter;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.PlayTaskType;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYSysUtils;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.base.BFYEventBus;

public class PlayerController extends Controller implements BFYEventBus.OnEventListener {

    private final String TAG = PlayerController.class.getSimpleName();

    private Context mContext;

    // 视频播放视图root
    private View mRoot;
    private FrameLayout mPlayerViewRoot;
    // 视频容器
    private PlayerViewControl mPlayerView;

    private WeakReference<PlayerViewControl> mPlayerViewRef;

    // 视频播放控制器
    private MediaController mMediaController;

    private StatusController mStatusController;
    // 当前解码模式
    private DecodeMode mCurrentDecodeMode = BFYConst.DEFAULT_DECODE_MODE;
    // 第一层手势操作控制
    private GestureDetector mGestureDetector;
    // 音量状态
    private VolumeLayer mVolumeLayer;
    // 亮度状态
    private BrightnessLayer mBrightnessLayer;
    // 播放进度浮层
    private ProgressLayer mProgressLayer;
    //播放完成显示的视图
    private PlayCompleteHolder mPlayCompleteHolder;
    //出错时显示的视图
    private PlayErrorHolder mPlayErrorHolder;
    
    //播放视频的原始事件
    private PlayerEvent mPlayerEvent;
    //是否显示剧集按钮
    private boolean mMinfoButtonEnable;
    //视频的标题
    private String mTitle;
    //是否全屏
    private boolean mFullScreen;
    //是否自动全屏
    private boolean mAutoFullscreen = true;

    private PlayerHandler mHandler = new PlayerHandler(this);
    //是否正在退出
    private boolean mQuitting;
    //播放的视频数据代理
    protected PlayProxy mPlayProxy;
    private int mP2pStreamPort = 0;
    private BFYVideoInfo mVideoInfo;
    // activity是否在活动状态
    protected boolean mActivityRunning;
    //是否播放完毕
    private boolean mIsPlayComplete;
    //播放出错或等待
    private boolean mIsPlayWait;
    // 解码界面是否已加载
    private boolean mIsDecodeLoaded;

    //手机屏幕变化的广播接收器
    private ScreenBroadcastReceiver mScreenReceiver = new ScreenBroadcastReceiver();
    private ScreenStateListener mScreenStateListener;
    
    private PlayErrorListener mPlayErrorListener = null;
    private PlayerEventListener mPlayerEventListener = null;
    private PlayErrorManager mPlayErrorManager = null;
    
    private boolean mInited = false;
    private boolean mStarted = false;
    
    private PlayTaskType mPlayTaskType = PlayTaskType.VOD;
    private int mStartPlayTime;
    
    public int mScreenWidth;
    public int mScreenHeight;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mLandScape;
    private boolean mIsPlayerControolerStop = false;
    
    private OrientationChangedListener mOrientationChangedListener;
    
    public PlayerController(Context context) {
        super(context);
    }

    public PlayerController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Context context) {
        BFYLog.d(TAG, "init,threadId=" + Thread.currentThread().getId());
        
        if (!mInited) {
	        mContext = context;
	        registerScreenListener();
	        registerEvent();
	        mQuitting = false;
	        mGestureDetector = new GestureDetector(context, new PlayerControllerGestureListener());
	        mPlayErrorManager = new PlayErrorManager();
	        mStartPlayTime = 0;
	        mStarted = false;
	        mInited = true;
        }
    }
    
    private void uninit() {
    	if (mInited) {
	        unregisterScreenListener();
	        unregisterSensor();
	        unregisterEvent();
	
	        mContext = null;
	        mPlayerEvent = null;
	        mGestureDetector = null;
	        mPlayErrorManager = null;
	        mStarted = false;
	        mInited = false;
    	}
    }
    
    public void setVideoInfo(BFYVideoInfo videoInfo) {
    	mVideoInfo = videoInfo;
    }

    public void setPlayTaskType(PlayTaskType type) {
    	mPlayTaskType = type;
    }

    public PlayTaskType getPlayTaskType() {
        return mPlayTaskType;
    }
    
    public boolean isLivePlayer() {
    	return mPlayTaskType == PlayTaskType.LIVE;
    }

    public boolean isStarted() {
    	return mStarted;
    }
    
    public MediaController getMediaController(){
        return mMediaController;
    }

    /**
     * 停止screen状态监听
     */
    public void unregisterScreenListener() {
        BFYLog.d(TAG, "unregisterScreenListener");
        if (null != mContext) {
            mContext.unregisterReceiver(mScreenReceiver);
        }
    }

    /**
     * 注册screen状态广播接收器.屏幕点亮、屏幕关闭、用户解锁.
     */
    private void registerScreenListener() {
        BFYLog.d(TAG, "registerScreenListener");
        if (null != mContext) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            mContext.registerReceiver(mScreenReceiver, filter);
        }
    }

    /**
     * 注册屏幕旋转的监听器.
     */
    private void registerSensor() {
        BFYLog.d(TAG, "registerSensor");
        if (null == mOrientationEventListener) {
            mOrientationEventListener = new PlayerOrientationEventListener(getContext());
        }
        boolean canDetect = mOrientationEventListener.canDetectOrientation();
        BFYLog.d(TAG, "registerSensor,canDetect=" + canDetect);
        if (canDetect) {
            mOrientationEventListener.enable();
        }
    }

    private void unregisterSensor() {
        BFYLog.d(TAG, "unRegisterSensor");
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
            mOrientationEventListener = null;
        }
    }

    public void setOrientationChangedListener(OrientationChangedListener listener) {
    	mOrientationChangedListener = listener;
    }
    
    public StatInfo getStatInfo() {
    	if (mPlayerView != null) {
    		return mPlayerView.getStatInfo();
    	} else {
    		return null;
    	}
    }
    
    public void reloadDecodeView() {
    	loadDecodeView();
    }
    
    /**
     * 加载解码模块
     */
    private void loadDecodeView() {
        BFYLog.d(TAG, "loadDecode");
        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        //期望的SurfaceView宽度、高度
        int width, height;
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        }

        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {//横屏
        	BFYLog.d(TAG, "executeSurfaceChanged,Surface.ROTATION_90 || Surface.ROTATION_270");
        	BFYLog.d(TAG, "executeSurfaceChanged,widthPixels=" + metrics.widthPixels + ",heightPixels=" + metrics.heightPixels);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
            mScreenHeight = metrics.widthPixels;
            mScreenWidth = metrics.heightPixels;
            mLandScape = true;
            //mOrientationChangedListener.onOrientationChanged(true);
        } else {//竖屏
        	BFYLog.d(TAG, "executeSurfaceChanged,Surface.ROTATION_0 || Surface.ROTATION_180");
            width = metrics.widthPixels;
            height = (int) (metrics.widthPixels * BFYConst.DEFAULT_VIDEO_VIEW_ASPECT_RATIO);
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
            mLandScape = false;
        }
        
        frameParams.width = width;
        frameParams.height = height;
        BFYLog.d(TAG, "loadDecode,width=" + width + ",height=" + height);
        frameParams.gravity = Gravity.CENTER;

        if (mPlayerView != null) {
        	mPlayerView.onDestroyWindow();
        }
        
        removeAllViews();
        View v = makeDecodeView();
        if (null != v) {
            addView(v, frameParams);
        }
        
        mIsDecodeLoaded = true;

        BFYLog.d(TAG, "DecodeMode:" + mCurrentDecodeMode.toString());
    }

    /**
     * 生成解码视图
     *
     * @return
     */
    protected View makeDecodeView() {
        BFYLog.d(TAG, "makeDecodeView");
        if (null == mContext) {
            BFYLog.d(TAG, "makeDecodeView,null == mContext");
            return null;
        }

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mCurrentDecodeMode == DecodeMode.SOFT) {
            mRoot = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_player_view_sw"), null);
        } else if (mCurrentDecodeMode == DecodeMode.AUTO) {
        	if (BFYSysUtils.isExoPlayerUsable()) {
        		mRoot = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_player_view_exo"), null);
        	} else {
        		mRoot = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_player_view_sw"), null);
        	}
        }
        
        initPlayerView(mRoot);
        return mRoot;
    }

    private void initPlayerView(View v) {
        BFYLog.d(TAG, "initPlayerView start");        
        mPlayerViewRoot = (FrameLayout) v.findViewById(BFYResUtil.getId(getContext(), "playerViewRoot"));
        BFYPlaceHolder holder = (BFYPlaceHolder) v.findViewById(BFYResUtil.getId(getContext(), "placeholder"));
        mPlayerView = (PlayerViewControl) v.findViewById(BFYResUtil.getId(getContext(), "videoView"));
        mPlayerView.setPlaceHolder(holder);
        mPlayerView.setPlayErrorListener(mPlayErrorListener);        
        
        mScreenStateListener = mPlayerView;

        mMediaController = (MediaController) v.findViewById(BFYResUtil.getId(getContext(), "mediaController"));
        mMediaController.setPlayerController(this);
        mPlayerView.setAnchorView(mPlayerViewRoot);
        mPlayerView.setMediaController(mMediaController);
        mPlayerView.setPlayerController(this);

        mStatusController = (StatusController) v.findViewById(BFYResUtil.getId(getContext(), "statusController"));
        mPlayerView.setStatusController(mStatusController);

        mVolumeLayer = (VolumeLayer) v.findViewById(BFYResUtil.getId(getContext(), "volumeLayer"));
        mBrightnessLayer = (BrightnessLayer) v.findViewById(BFYResUtil.getId(getContext(), "brightnessLayer"));
        mProgressLayer = (ProgressLayer) v.findViewById(BFYResUtil.getId(getContext(), "progressLayer"));

        mPlayerView.setVideoTitle(mTitle);
        mPlayerViewRef = new WeakReference<PlayerViewControl>(mPlayerView);

        mPlayCompleteHolder = (PlayCompleteHolder) v.findViewById(BFYResUtil.getId(getContext(), "playComplete"));
        mPlayCompleteHolder.setPlayerController(this, mPlayerViewRoot);
        
        mPlayErrorHolder = (PlayErrorHolder) v.findViewById(BFYResUtil.getId(getContext(), "playError"));
        mPlayErrorHolder.setPlayerController(this, mPlayerViewRoot);
        
        BFYLog.d(TAG, "initPlayerView end");
    }


    public void incVolume(){
        if(null != mVolumeLayer){
            hideLayer(true);
            mVolumeLayer.incVolume();
        }
    }

    public void decVolume(){
        if(null != mVolumeLayer){
            hideLayer(true);
            mVolumeLayer.decVolume();
        }
    }

    public void setVolume(int value){
        if(null != mVolumeLayer){
            hideLayer(true);
            mVolumeLayer.setVolume(value);
        }
    }

    public int getCurrentVolume(){
        if(null != mVolumeLayer){
            return mVolumeLayer.getCurrentVolume();
        }
        return 0;
    }

    public int getMaxVolume(){
        if(null != mVolumeLayer){
            return mVolumeLayer.getStreamMaxVolume();
        }
        return 0;
    }

    public long getPlayerTime(){
        return 0L;
    }

    public void registerPlayerVideoEventListener(PlayerViewControl.PlayerControllerListener eventListener){
        mPlayerView.registerPlayerListener(eventListener);
    }

    public void unregisterPlayerVideoEventListener(PlayerViewControl.PlayerControllerListener eventListener){
        mPlayerView.unregisterPlayerListener(eventListener);
    }

    protected void registerEvent() {
        BFYLog.d(TAG, "registerEvent");
        BFYEventBus.getInstance().registerListener(this);
    }

    protected void unregisterEvent() {
        BFYLog.d(TAG, "unregisterEvent");
        BFYEventBus.getInstance().unregisterListener(this);
    }

    /**
     * 启动视频播放
     */
    public void start() {
        BFYLog.d(TAG, "start");
        if (null != mPlayerView) {
            mIsPlayComplete = false;
            mIsPlayWait = false;
            mErrorCode = PlayErrorManager.NO_ERROR;
            if (null != mPlayCompleteHolder) {
            	mPlayCompleteHolder.hide();
            }
            mPlayerView.start();
        }
    }
    
    public void stop() {
        BFYLog.d(TAG, "stop");
    	if (null != mPlayerView) {
    		mPlayerView.stop();
    	}
    }

    /**
     * 从头开始播放
     */
    public void startFromBeginning() {
        if (null != mPlayerView) {
            mIsPlayComplete = false;
            mIsPlayWait = false;
            mErrorCode = PlayErrorManager.NO_ERROR;
            if (null != mPlayCompleteHolder) {
            	mPlayCompleteHolder.hide();
            }         
        }
        
        BFYLog.d(TAG, "startFromBeginning");
        if (null != mPlayerEventListener) {
        	if (mPlayerEventListener.onRestartFromBeginning()) {
        		BFYLog.d(TAG, "network changed, restart all");
        		restartPlay();
        		return;
        	}
        }
        
        if (null != mPlayProxy) {
            if (null != mPlayerView) {
                mPlayerView.setFirstVideo(false);
            }
            mPlayProxy.playPrepare(0);
        }
    }

    public void seekTo(int ms) {
    	if (mPlayerView != null) {
    		mPlayerView.seekTo(ms);
    	}
    }
    
    /**
     * 出错后重新播放
     */
    public void restartPlay() {
    	BFYLog.d(TAG, "restartPlay");
    	if (mIsPlayerControolerStop)
    		return;
    	if (null != mPlayerView) {
    		if (null != mPlayErrorHolder) {
    			mPlayErrorHolder.hide();
    		}
    	}
    	if (mPlayerEventListener != null) {
    		mPlayerEventListener.onRestartPlay();
    	}
    }
    
    public void continuePlay() {
    	BFYLog.d(TAG, "continuePlay");
    	if (mPlayerEventListener != null) {
    		mPlayerEventListener.onContinuePlay();
    	}
    }
    
    public void hidTipsHolder() {
    	BFYLog.d(TAG, "hidTipsHolder");
    	if (mPlayErrorHolder != null) {
    		mPlayErrorHolder.hide();
    	}
    }    
    
    public boolean isPlayWaitState() {
    	return mIsPlayWait;
    }
    /*
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        BFYLog.d(TAG, "onError,what=" + what + ",extra=" + extra);
        if (mQuitting) return false;

        if (mCurrentDecodeMode == DecodeMode.HARD) {
            //当前为硬解码,设置为软解码
            //changeDecode();
        	BFYLog.d(TAG, "onError,硬解码失败");
        	if (null != mPlayResultListener) {
                mPlayResultListener.onError(what, extra);
        	}
            return true;
        } else if (mCurrentDecodeMode == DecodeMode.SOFT) {
            BFYLog.d(TAG, "onError,软解码失败");
            if (null != mPlayResultListener) {
                mPlayResultListener.onError(what, extra);
        	}
            return true;
        }
        return false;
    }
    */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BFYLog.d(TAG, "onKeyDown,keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            mMediaController.onKeyDown(keyCode, event);
        } else if (keyCode == KeyEvent.KEYCODE_HOME) {
            BFYLog.d(TAG, "onKeyDown,KeyEvent.KEYCODE_HOME");
            mPlayerView.keycodeHome();
        } else if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	if (backToPortrait()) {
        		return true;
        	}
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        BFYLog.d(TAG, "onInterceptTouchEvent,mIsPlayComplete=" + mIsPlayComplete);
        if (mIsPlayComplete || mIsPlayWait) return super.onInterceptTouchEvent(event);
        return !mMediaController.isShowing() || super.onInterceptTouchEvent(event);
    }
    
    public boolean backToPortrait() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
        	backPressed(false);
        	return true;
        } else {
        	return false;
        }
    }


    static final int MOVE_NONE = -1;
    static final int MOVE_LEFT = 1;
    static final int MOVE_RIGHT = 2;

    private float e1x;
    private float e1y;
    private MotionEvent motionEvent;

    private int moveDirection = MOVE_NONE;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        BFYLog.d(TAG, "onTouchEvent,mIsPlayComplete=" + mIsPlayComplete);
        int action = event.getAction();
        motionEvent = MotionEvent.obtain(event);
        mGestureDetector.onTouchEvent(motionEvent);

        if (mIsPlayComplete || mIsPlayWait || null == mPlayerView || !mPlayerView.isInPlaybackState()) {
            return true;
        }

        if (null != mMediaController && mMediaController.isShowing()) {
            if (action == MotionEvent.ACTION_UP) {
                mMediaController.show();
            }
            //控制界面显示时也可以手势操作
            //return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                BFYLog.d(TAG, "MotionEvent.ACTION_DOWN");
                e1x = event.getRawX();
                e1y = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE: {//左侧滑动更改亮度,右侧滑动调节音量,其它符合要求的滑动调节播放进度
                BFYLog.d(TAG, "MotionEvent.ACTION_MOVE");
                float e2x = event.getRawX();
                float e2y = event.getRawY();
                float e1e2x = Math.abs(e1x - e2x);
                float e1e2y = Math.abs(e1y - e2y);
                BFYLog.d(TAG, "e1(" + e1x + "," + e1y + "),e2(" + e2x + "," + e2y + ")" + ",e1e2x=" + e1e2x + ",e1e2y=" + e1e2y +
                        ",mPovitX=" + mPovitX + ",mMinX=" + mMinX + ",mMinY=" + mMinY);
                //音量、亮度x坐标是否在允许范围内;

                if (e1e2x < mMinX && e1e2y < mMinY) {
                    return true;
                }
                if (e1e2x > mMinX && e1e2y < mMinY) {//横向滑动
                    hideLayer(false);
                    moveDirection = e1x > e2x ? MOVE_LEFT : MOVE_RIGHT;
                    showProgressLayer(true, true);
                    return true;
                }

                if (e1x < mPovitX && e2x < mPovitX) {
                    if (e1e2x < mMinX) {//左侧亮度
                        showProgressLayer(false, false);
                        //音量立即消失
                        disappearVolumeLayer();
                        if (null != mBrightnessLayer) {
                            mBrightnessLayer.onScroll(event, mDisplayHeight);
                        }
                        return true;
                    }
                }
                if (e1x > mPovitX && e2x > mPovitX) {
                    if (e1e2x < mMinX && e1e2y > mMinY) {//右侧音量
                        showProgressLayer(false, false);
                        //亮度立即消失
                        disappearBrightnessLayer();
                        if (null != mVolumeLayer) {
                            mVolumeLayer.onScroll(event, mDisplayHeight);
                        }
                        return true;
                    }
             	}
            }
            break;
            case MotionEvent.ACTION_UP:
            	mBrightnessLayer.clearLastTouchEvent();
            	mVolumeLayer.clearLastTouchEvent();
                BFYLog.d(TAG, "MotionEvent.ACTION_UP");
                e1x = 0;
                e1y = 0;
                hideLayer(true);
                if (null != mProgressLayer && mProgressLayer.isShowing()) {
                	if (mPlayTaskType != PlayTaskType.LIVE) {
	                    if (moveDirection == MOVE_LEFT) {
	                        BFYLog.d(TAG, "MotionEvent.ACTION_UP,MOVE_LEFT,(null==mPlayerView)=" + (null == mPlayerView));
	                        if (null != mPlayerView)
	                            mPlayerView.fastBackward();
	                    } else if (moveDirection == MOVE_RIGHT) {
	                        BFYLog.d(TAG, "MotionEvent.ACTION_UP,MOVE_RIGHT,(null==mPlayerView)=" + (null == mPlayerView));
	                        if (null != mPlayerView)
	                            mPlayerView.fastForward();
	                    }
                	}
                    if (null != mProgressLayer) {
                        mProgressLayer.hide(true);
                    }
                }
                moveDirection = MOVE_NONE;
                break;
        }
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        BFYLog.d(TAG, "onAttachedToWindow");
        mContext = getContext();
    }

    @Override
    protected void onFinishInflate() {
        BFYLog.d(TAG, "onFinishInflate");
        super.onFinishInflate();
        setTouchParam();
    }

    private void hideLayer(boolean fadeOut) {
        if (null != mVolumeLayer) {
            mVolumeLayer.hide(fadeOut);
        }
        if (null != mBrightnessLayer) {
            mBrightnessLayer.hide(fadeOut);
        }
    }
    
    private void disappearVolumeLayer() {
        if (null != mVolumeLayer) {
            mVolumeLayer.hide(false);
        }
    }

    private void disappearBrightnessLayer() {
        if (null != mBrightnessLayer) {
        	mBrightnessLayer.hide(false);
        }
    }
    
    private void orientationChanged() {
        BFYLog.d(TAG, "orientationChanged,(null==mPlayerView)=" + (null == mPlayerView) + ",(null==mMediaController)=" + (null == mMediaController));
        if (null != mPlayerView)
            mPlayerView.orientationChanged();
        if (null != mMediaController)
            mMediaController.orientationChanged();
        if (null != mPlayCompleteHolder) {
        	mPlayCompleteHolder.orientationChanged();
        }
        if (null != mPlayErrorHolder) {
        	mPlayErrorHolder.orientationChanged();
        }
    }

    public void onStop() {
        BFYLog.d(TAG, "onStop");
        mActivityRunning = false;
        mIsPlayerControolerStop = true;
        if (mIsPlayComplete || mIsPlayWait) {
        	return;
        }
        if (null != mPlayerView) {
            mPlayerView.onStopWindow();
        }
    }
    
    public void onStart(){
    	mIsPlayerControolerStop = false;
    }
    
    public void onPause() {
        BFYLog.d(TAG, "onPause");
        mActivityRunning = false;
        if (!mIsDecodeLoaded || mIsPlayComplete || mIsPlayWait) {
        	return;
        }
        if (null != mPlayProxy) {
            mPlayProxy.onPause();
        }
        if (null != mPlayerView) {
            mPlayerView.onPauseWindow();
        }
    }

    public void onResume() {
        BFYLog.d(TAG, "onResume");
        mActivityRunning = true;
        if (!mIsDecodeLoaded || mIsPlayComplete || mIsPlayWait) {
        	return;
        }
        if (null != mPlayProxy) {
            mPlayProxy.onResume();
        }
        if (null != mPlayerView) {
            mPlayerView.onResumeWindow();
        }
        mContext = getContext();

        BFYLog.d(TAG, "onResume,adjustOriention");
        adjustOriention();
    }

    public void onRestart() {
        BFYLog.d(TAG, "onRestart");
        if (null != mPlayerView) {
            mPlayerView.onRestartWindow();
        }
        if (null != mPlayProxy) {
            mPlayProxy.playEventProceed(false);
        }
    }

    public void onDestroy() {
        BFYLog.d(TAG, "onDestroy");

        if (mInited) {
	        if (!mIsPlayComplete && mStarted) {
	        	reportPlayExperienceStatInfo();
	        }
	        
	        if (null != mPlayerView) {
	            mPlayerView.setQuit(true);
	            mPlayerView.onDestroyWindow();
	        }

	        if (mQuitting && null != mPlayProxy) {
	            BFYLog.d(TAG, "onDestroy,mPlayProxy.onDestory()");
	            mPlayProxy.onDestory();
	            mPlayProxy = null;
	        }
        }
        
        uninit();
    }

    public void onCreate() {
        BFYLog.d(TAG, "onCreate");
        
        mContext = getContext();
        init(mContext);

        if (!mInited) {
	        if (null != mPlayerView) {
	            mPlayerView.onCreateWindow();
	        }
        }
    }

    public void onLowMemory() {
        BFYLog.d(TAG, "onLowMemory");
        if (null != mPlayerView) {
            mPlayerView.onLowMemory();
        }
    }
    
    @Override
    public void onEvent(Object event) {
    	if (event instanceof PlayerEvent) {
    		processEvent((PlayerEvent)event);
    	}
    	if (event instanceof BFYNetworkStatusReportEvent) {
    		onEvent((BFYNetworkStatusReportEvent)event);
    	}
    }

    public void onEvent(BFYNetworkStatusReportEvent event) {
        BFYLog.d(TAG, "NetworkStatusReportEvent");
        if (null != mPlayerView) {
            BFYLog.d(TAG, "NetworkStatusReportEvent,mSeekWhenPrepared1=" + mPlayerView.getSeekPosition());
            if (null != mPlayerEvent) {
                mPlayerView.setFirstVideo(false);
            }
        }
        if (null == event) return;

        BFYNetworkStatusData status = (BFYNetworkStatusData) event.getData();
        if (null == status) return;

        int statusCode = status.getStatusCode();
        BFYLog.d(TAG, "doNetworkEvent,status=" + statusCode);
        if (statusCode == BFYNetworkStatusData.NETWORK_CONNECTION_NONE) {
            BFYLog.d(TAG, "doNetworkEvent,NETWORK_CONNECTION_NONE");
            if (null != mPlayerView) {
                mPlayerView.setSeekPosition(mPlayerView.getCurrentPosition());
            }
        }
        if (null != mPlayerView) {
            BFYLog.d(TAG, "NetworkStatusReportEvent,mSeekWhenPrepared2=" + mPlayerView.getSeekPosition());
        }
        /*if (null != mPlayProxy) {
            mPlayProxy.doNetworkEvent(event);
        }*/
        if (null != mPlayerEvent) {
        	doNetworkEvent(statusCode);
        } else {
        	if (mPlayErrorHolder != null) {
        		if (mPlayErrorHolder.isShowing()) {
        			doNetworkEvent(statusCode);
        		}
        	}
        }
    }

    private void doNetworkEvent(int netState) {     
        if (null == mPlayerView) return;
        if (mPlayerView.isPlayCompete() || 
        		(mPlayerView.getCurrentState() == MediaPlayerConstant.STATE_ERROR && mErrorCode != PlayErrorManager.NO_NETWORK)) {
        	return;
        }
        		  
        mPlayerView.hideStatusController();
    	if (netState == BFYNetworkStatusData.NETWORK_CONNECTION_NONE) {
            BFYLog.d(TAG, "doNetworkEvent,NO_NETWORK");                        
            //没有网络,提示"没有网络"
            if (mPlayerView.isInPlaybackState()) {
                //如果在播放中,先停止播放
            	mPlayerView.pause();
            	mPlayerView.showPlaceHolder();
            }
            if (null != mPlayProxy) {
            	mPlayProxy.sendErrorCommand(PlayErrorManager.NO_NETWORK);
            }
        } else if (netState == BFYNetworkStatusData.NETWORK_CONNECTION_WIFI) {
            BFYLog.d(TAG, "doNetworkEvent,NETWORK_WIFI");
            if (mPlayerView.isInPlaybackState()) {
                //如果在播放中,先停止播放
            	mPlayerView.pause();
            	mPlayerView.showPlaceHolder();
            }
            restartPlay();
        } else if (netState == BFYNetworkStatusData.NETWORK_CONNECTION_MOBILE) {
            BFYLog.d(TAG, "doNetworkEvent,NETWORK_MOBILE");
            if (mPlayerView.isInPlaybackState()) {
                //如果在播放中,先停止播放
            	mPlayerView.pause();
            	mPlayerView.showPlaceHolder();
            }
            restartPlay();            
        } 
    }
    
	private final String NO_NETWORK = "无网络可用";
	private final String MOBILE_NO_PLAY = "非WiFi环境下，继续播放将会产生流量费用";

    private int mErrorCode = PlayErrorManager.NO_ERROR;
    
    private String errorCodeToMsg(int code) {
    	mErrorCode = code;
    	switch (code) {
    	case PlayErrorManager.NO_NETWORK: {    		
    		return NO_NETWORK;    		
    		}
    	
    	case PlayErrorManager.MOBILE_NO_PLAY: {    		
    		return MOBILE_NO_PLAY;    		
    		}
    	
    	default:    		
    		if (null != mPlayErrorManager) {
    			return mPlayErrorManager.getErrorShowTips(mPlayTaskType);
    		} else {
    			return "";
    		}
    	}
   }
    
    private void processEvent(PlayerEvent event) {
        BFYLog.d(TAG, "processEvent,PlayerEvent");
        
        PlayerCommand playerCommand = (PlayerCommand) event.getData();
        int command = playerCommand.getCommand();
        
        switch (command) {
	        case PlayerCommand.START: {
	        	mStarted = true;
	            mIsPlayComplete = false;
	            mIsPlayWait = false;
	            mErrorCode = PlayErrorManager.NO_ERROR;
	            if (null != mPlayCompleteHolder) {
	            	mPlayCompleteHolder.hide();
	            }
	            if (null != mPlayErrorHolder) {
	            	mPlayErrorHolder.hide();
	            }

	            if (null != mPlayerView) {
	                mPlayerView.sendEmptyMessage(MediaPlayerConstant.UI_STATUS_CONTROLLER_SHOW);
	            }

	            mMediaController.reset();

	            if (null != mPlayerEvent) {
	                BFYLog.d(TAG, "onEvent,not first video");
	                if (null != mPlayerView) {
	                    mPlayerView.setFirstVideo(false);
	                    mPlayerView.setReceiveCompletion(false);
	                }
	            } else {
	                BFYLog.d(TAG, "onEvent,first video");
	                if (null != mPlayerView) {
	                    mPlayerView.setFirstVideo(true);
	                    mPlayerView.setReceiveCompletion(true);
	                    mPlayerView.setSeekPosition(-1);
	                }
	            }
	            mPlayerEvent = event;

	            if (null != mPlayProxy) {
	                if (null != mPlayerView) {
	                    BFYLog.d(TAG, "onEvent,recordHistory");
	                }
	                mPlayProxy.stopPlayback(outer);
	                mMediaController.enableDefinitionSwitch(true);
	                return;
	            }
	            playEventInitial();
	            mMediaController.enableDefinitionSwitch(true);
	            
	            break;
	        }
	        
	        case PlayerCommand.PAUSE: {
	            BFYLog.d(TAG, "onEvent,PAUSE");
	            mPlayerView.pause();
	        	break;
	        }
	        
	        case PlayerCommand.STOP: {
	            BFYLog.d(TAG, "onEvent,STOP");
	            mPlayerView.stop();
	        	break;
	        }

	        case PlayerCommand.NETWORK: {
                if (null != mPlayerView) {                                            
                    mPlayerView.showPlaceHolder();
                }
		    	BFYLog.d(TAG, "onMobileNetwork");
		    	if (mPlayErrorHolder != null) {
		    		mIsPlayWait = true;
            		mPlayErrorHolder.setMessage(PlayErrorManager.MOBILE_NO_PLAY, MOBILE_NO_PLAY);
            		mPlayErrorHolder.show();
		    	}
		    	break;
	        }
	        
	        case PlayerCommand.COMPLETE:
	        case PlayerCommand.ERROR: {
	            BFYLog.d(TAG, "onEvent: " + command);
	            mIsPlayComplete = true;
	            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
	            int rotation = windowManager.getDefaultDisplay().getRotation();
	            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
	                portrait();
	            }
            	if (command == PlayerCommand.ERROR) {
            		mIsPlayWait = true;
                    if (null != mPlayerView) {                        
                        mPlayerView.setCurrentState(MediaPlayerConstant.STATE_ERROR);
                        mPlayerView.setTargetState(MediaPlayerConstant.STATE_ERROR);
                        mPlayerView.showPlaceHolder();
                    }
                    
                    if (PlayErrorManager.NO_NETWORK == playerCommand.getErrorCode()) {
                    	mPlayErrorHolder.setMessage(PlayErrorManager.NO_NETWORK, errorCodeToMsg(PlayErrorManager.NO_NETWORK));
                    } else {
                        if (null != mPlayErrorManager) {
                        	mPlayErrorManager.setErrorCode(playerCommand.getErrorCode());
                        	mPlayErrorHolder.setMessage(mPlayErrorManager.getErrorCode(), errorCodeToMsg(mPlayErrorManager.getErrorCode()));
                        }
                    }
            		
            		mPlayErrorHolder.show();
            		if (mErrorCode == PlayErrorManager.MOBILE_NO_PLAY || mErrorCode == PlayErrorManager.NO_NETWORK) {
            			if (null != mPlayerEventListener) {
                			mPlayerEventListener.onNetworkError(mErrorCode);
                		}
            		}            		
            	} else {
            		if (null != mPlayCompleteHolder && mPlayTaskType == PlayTaskType.VOD) {
    	            	mPlayCompleteHolder.setMessage("");	
    	            	mPlayCompleteHolder.show();
    	            }    	        	
            	}
            	
            	reportPlayExperienceStatInfo();
	        	break;
	        }
	        
	        default:
	        	break;
        }
    }

    /**
     * 继续处理播放事件,初始化信息
     */
    private void playEventInitial() {
        BFYLog.d(TAG, "playEventInitial");
        if (null == mPlayerEvent) return;
        PlayerCommand playerCommand = (PlayerCommand) mPlayerEvent.getData();
        mVideoInfo = playerCommand.getVideoInfo();
        if (null == mVideoInfo) {
            BFYLog.d(TAG, "playEventInitial,invalid video info");
            return;
        }
        createPlayProxy();
        //根据视频类型,p2p、http、local初始化PlayProxy
        if (null != mPlayerView) {
            //记录当前播放历史
            mPlayerView.setCurrentState(MediaPlayerConstant.STATE_IDLE);
            mPlayerView.setTargetState(MediaPlayerConstant.STATE_IDLE);
            mPlayerView.setIsSeeking(false);
            mPlayerView.setVideoTitle(mTitle);
        }
        long historyPosition = playerCommand.getHistoryPosition();
        if (historyPosition >= 0) {
            if (null != mPlayerView) {
                mPlayerView.setSeekPosition((int) historyPosition);
            }
        }
        mPlayProxy.playEventProceed(true);
    }

    PlayProxy.CallbackOuter outer = new PlayProxy.CallbackOuter() {
        @Override
        public void playStopSuccess() {
            BFYLog.d(TAG, "CallbackOuter,playStopSuccess");
            playEventInitial();
        }

        @Override
        public void playStopFailure() {
            BFYLog.d(TAG, "CallbackOuter,playStopFailure");
        }
    };

    private void createPlayProxy() {
        BFYLog.d(TAG, "createPlayProxy,(null == mPlayProxy)=" + (null == mPlayProxy));
        
        mPlayProxy = P2pPlayProxy.getInstance(mPlayerViewRef);
        mPlayProxy.setP2pStreamServerPort(mP2pStreamPort, MediaCenter.StreamMode.STREAM_HLS_MODE);

        initPlayProxy();
    }

    private void initPlayProxy() {
        BFYLog.d(TAG, "initPlayProxy,(null == mPlayProxy)=" + (null == mPlayProxy));
        if (null != mPlayProxy) {
            mPlayProxy.init(mPlayerViewRef);
            mPlayProxy.setVideoInfo(mVideoInfo);
            mPlayProxy.setActivityRunning(mActivityRunning);
            mPlayProxy.setPlayEvent(mPlayerEvent);
        }
    }

    public void playPrepare(long pos) {
        BFYLog.d(TAG, "playPrepare,pos=" + pos);
        if (null == mPlayProxy) return;

        createPlayProxy();
        if (null != mPlayProxy) {
            mPlayProxy.executePlay(pos);
        }
    }

    /**
     * 竖屏
     */
    public void portrait() {
        BFYLog.d(TAG, "portrait");
        if (null == mContext) return;
        mLandScape = false;        
        Activity act = (Activity) mContext;
        act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mOrientationChangedListener.onOrientationChanged(false, mScreenWidth, mScreenHeight);
        
        resize(false);
        orientationChanged();
        setTouchParam();
        BFYLog.d(TAG, "portrait end");
    }
    
    /**
     * 横屏
     */
    public void landscape() {
        BFYLog.d(TAG, "landscape");
        if (null == mContext) return;        
        mLandScape = true;    
        int newOrientation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else {
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        Activity act = (Activity) mContext;
        act.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        act.setRequestedOrientation(newOrientation);
        
        mOrientationChangedListener.onOrientationChanged(true, mScreenWidth, mScreenHeight);
        
        resize(true);
        orientationChanged();
        setTouchParam();       
        BFYLog.d(TAG, "landscape end");
    }

    public void resize(boolean isFullScreen) {
        BFYLog.d(TAG, "resize,isFullScreen=" + isFullScreen);
        if (null == mContext) return;
        if (null == mPlayerViewRoot) return;

        LayoutParams params = (LayoutParams)mPlayerViewRoot.getLayoutParams();
        if (null != params) {
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            if (isFullScreen) {
        		params.width = mScreenHeight;
                params.height = mScreenWidth;
                mDisplayWidth = params.width;
                mDisplayHeight = params.height;
            } else {
                params.width = mScreenWidth;
                params.height = (int) (mScreenWidth * BFYConst.DEFAULT_VIDEO_VIEW_ASPECT_RATIO);
                mDisplayWidth = params.width;
                mDisplayHeight = params.height;
            }
            BFYLog.d(TAG, "resize,width=" + params.width + ",height=" + params.height);

            mPlayerViewRoot.setLayoutParams(params);
            mPlayerViewRoot.requestLayout();
        }
        BFYLog.d(TAG, "resize,end");
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    /**
     * 设置剧集切换是否可见
     * @param visible 剧集按钮是否可见
     */
    public void setMinfoButtonEnable(boolean visible) {
        mMinfoButtonEnable = visible;
        mMediaController.setMinfoButtonEnable(visible);
    }

    /**
     * 设置视频标题.
     *
     * @param title       视频标题
     */
    public void setVideoTitle(String title) {
        mTitle = title;
        if (null != mPlayerView) {
            mPlayerView.setVideoTitle(title);
        }
    }

    /**
     * 点击了播放器控制界面的返回按钮
     */
    public void backPressed(boolean immediateExit) {
        BFYLog.d(TAG, "backPressed start,mFullScreen=" + mFullScreen + ",immediateExit=" + immediateExit);
        if (null == mContext) return;
        if (immediateExit) {
            mQuitting = true;
            if (null != mPlayerView) {
                mPlayerView.setQuit(true);
            }
            return;
        }
        if (mFullScreen) {
            //全屏模式,点击返回退出播放
            BFYLog.d(TAG, "backPressed,mFullScreen,quit,(null==mPlayerView)=" + (null == mPlayerView));
            mQuitting = true;
            if (null != mPlayerView)
                mPlayerView.setQuit(true);
            ((Activity) mContext).finish();
        } else {
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int rotation = windowManager.getDefaultDisplay().getRotation();
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                BFYLog.d(TAG, "backPressed,portait,quit,(null==mPlayerView)=" + (null == mPlayerView));
                mQuitting = true;
                if (null != mPlayerView) {
                    //退出播放
                    mPlayerView.setQuit(true);
                }
                ((Activity) mContext).finish();
            } else {
                BFYLog.d(TAG, "backPressed,goto landscape");
                //当前横屏,转为竖屏
                portrait();
            }
        }
        BFYLog.d(TAG, "backPressed end");
    }

    public void setFullScreen(boolean fullScreen) {
        BFYLog.d(TAG, "setFullScreen,fullScreen=" + fullScreen);
        mFullScreen = fullScreen;
        if (!fullScreen) {
            registerSensor();
            portrait();
        } else {
            unregisterSensor();
            landscape();
        }
    }

    public void setAutoFullscreen(boolean autoFullscreen) {
    	mAutoFullscreen = autoFullscreen;
    }
    
    public boolean getAutoFullscreen() {
    	return mAutoFullscreen;
    }
    
    public boolean isFullScreen() {
        return mFullScreen;
    }

    public void adjustOriention() {
        BFYLog.d(TAG, "adjustOriention,isFullScreen=" + isFullScreen());
        if (null == mContext) return;
        if (!mAutoFullscreen) return;

        if (mCurrentOrigentation == ORIENTATION_LIE) {
            //平放
            BFYLog.d(TAG, "adjustOriention,mCurrentOrigentation == ORIENTATION_LIE,mLastOrientation=" + mLastOrientation);
            if (mLastOrientation == ORIENTATION_LEFT || mLastOrientation == ORIENTATION_RIGHT) {
                BFYLog.d(TAG, "adjustOriention,mLastOrientation == ORIENTATION_LEFT || mLastOrientation == ORIENTATION_RIGHT");
                landscape();
            } else {
//                WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
//                int rotation = windowManager.getDefaultDisplay().getRotation();
//                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
//                    BFYLog.d(TAG, "adjustOriention,Surface.ROTATION_90 || Surface.ROTATION_270");
//                    landscape();
//                } else {
//                    BFYLog.d(TAG, "adjustOriention,Surface.ROTATION_0 || Surface.ROTATION_180");
//                    portrait();
//                }
                Configuration configuration = mContext.getResources().getConfiguration();
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    BFYLog.d(TAG, "adjustOriention,Configuration.ORIENTATION_PORTRAIT");
                    if (!isFullScreen()) {
                        portrait();
                    }
                } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    BFYLog.d(TAG, "adjustOriention,Configuration.ORIENTATION_LANDSCAPE");
                    landscape();
                }
            }
        } else if (mCurrentOrigentation == ORIENTATION_TOP) {
            //顶部向上
            BFYLog.d(TAG, "adjustOriention,mCurrentOrigentation == ORIENTATION_TOP");
            if (!isFullScreen()) {
                portrait();
            }
        } else if (mCurrentOrigentation == ORIENTATION_LEFT) {
            //左边向上
            BFYLog.d(TAG, "adjustOriention,mCurrentOrigentation == ORIENTATION_LEFT");
            landscape();
        } else if (mCurrentOrigentation == ORIENTATION_BOTTOM) {
            //底部向上
            BFYLog.d(TAG, "adjustOriention,mCurrentOrigentation == ORIENTATION_BOTTOM");
            if (!isFullScreen()) {
                portrait();
            }
        } else if (mCurrentOrigentation == ORIENTATION_RIGHT) {
            //右边向上
            BFYLog.d(TAG, "adjustOriention,mCurrentOrigentation == ORIENTATION_RIGHT");
            landscape();
        }
    }

    public void configurationChanged(Configuration newConfig) {
        BFYLog.d(TAG, "configurationChanged,newConfig.orientation=" + newConfig.orientation + ",(mCurrentOrigentation == ORIENTATION_LIE)=" + (mCurrentOrigentation == ORIENTATION_LIE));
        if (mCurrentOrigentation == ORIENTATION_LIE) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                BFYLog.d(TAG, "Configuration.ORIENTATION_LANDSCAPE");
//                landscape();
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                BFYLog.d(TAG, "Configuration.ORIENTATION_PORTRAIT");
//                portrait();
            }
        }
    }
    
    public void setDisplayWidth(int width) {    
    	if (mLandScape) {
    		if (mDisplayWidth != width) {
    			mDisplayWidth = width;    		
        		if (null != mMediaController) {
        			if (mMediaController.isShowing()) {
        				mMediaController.resize();
        			}
        		}
    		}    		
    	}
    	 
    }
    
    public int getDisplayWidth() {    	
    	return mDisplayWidth; 
    }
    
    public int getDisplayHeight() { 
    	return mDisplayHeight; 
    }

    /**
     * 设置P2P 成功监听的Stream Server Port
     */
    public void setP2pStreamServerPort(int port) { mP2pStreamPort = port; }

    /**
     * 获取P2P 成功监听的Stream Server Port
     */
    public int getP2pStreamServerPort() { return mP2pStreamPort; }




    public interface PlayerViewControl extends MediaController.MediaPlayerControl, ScreenStateListener {
        public interface PlayerControllerListener {

            /**
             * 结束播放
             */
            void onCompletion();

            /**
             * 所有视频准备好了，只需要start就可以。
             */
            void onPrepare();


            /**
             * 视频大小发生变化
             */
            void onVideoSizeChanged();

            /**
             * 准备播放
             */
            void onReadytoPlay();
        }

    	void setMediaController(MediaController controller);

        void setPlayErrorListener(PlayErrorListener listener);

        void registerPlayerListener(PlayerControllerListener l);
        void unregisterPlayerListener(PlayerControllerListener l);



        void orientationChanged();

        void onCreateWindow();

        void onDestroyWindow();

        void onPauseWindow();

        void onResumeWindow();

        void onStopWindow();

        void onRestartWindow();
        
        void onLowMemory();

        void pause();

        void fastForward();

        void fastBackward();

        void setVideoTitle(String title);

        void setVideoSubTitle(String subTitle);
        
        void setStatusController(StatusController statusController);

        void setPlayerController(PlayerController playerController);

        void setPlaceHolder(BFYPlaceHolder holder);        
        
        void setParentLayoutParams(int width, int height);

        boolean isPlayCompete();

        Context getControlContext();

        void setQuit(boolean quit);

        void hideStatusController();

        void showPlaceHolder();

        boolean isSurfaceValid();

        int getSeekPosition();

        void setSeekPosition(int seekPos);

        void setCurrentState(int state);
        int getCurrentState();

        void setTargetState(int state);

        void setIsSeeking(boolean seeking);

        void setFirstVideo(boolean first);

        /**
         * 设置是否可以处理播放完成事件
         *
         * @param canReceive
         */
        void setReceiveCompletion(boolean canReceive);

        void sendEmptyMessage(int what);

        /**
         * 执行播放
         *
         * @param pos        播放视频的起始位置
         */
        void executePlay(long pos);

        /**
         * 设置播放代理
         *
         * @param playProxy
         */
        void setPlayProxy(PlayProxy playProxy);

        /**
         * 播放视频
         *
         * @param historyPosition 视频播放位置
         */
        void playPrepare(int historyPosition);
        
        void setVideoInfo(BFYVideoInfo videoInfo);

        void keycodeHome();
        
        StatInfo getStatInfo();
    }

    private int mPovitX;
    private int y = 20;
    private int x = 20;
    private float mMinY;
    private float mMinX;

    /**
     * 设置触摸时边界的值.
     */
    private void setTouchParam() {
        BFYLog.d(TAG, "setTouchParam");
        if (null == getContext()) return;

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        BFYLog.d(TAG, "setTouchParam,widthPixels=" + metrics.widthPixels + ",heightPixels=" + metrics.heightPixels);
        mPovitX = metrics.widthPixels / 2;
        mMinX = dip2px(getContext(), x);
        mMinY = dip2px(getContext(), y);
        BFYLog.d(TAG, "setTouchParam,mPovitX=" + mPovitX + ",mMinX=" + mMinX + ",mMinY=" + mMinY);
    }

    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    class PlayerControllerGestureListener extends GestureDetector.SimpleOnGestureListener {


        public PlayerControllerGestureListener() {
            super();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            float e1x = e1.getRawX();
//            float e2x = e2.getRawX();
//            //x坐标是否在允许范围内
//            if ((e1x > mLeftX && e1x < mRightOffset) || (e2x > mLeftX && e2x < mRightOffset)) {
//                hideLayer();
//                showProgressLayer(true);
//                return super.onScroll(e1, e2, distanceX, distanceY);
//            }
//            float e1y = e1.getRawY();
//            float e2y = e2.getRawY();
//            //y坐标是否符合
//            if (Math.abs(e1y - e2y) < mMinY) return super.onScroll(e1, e2, distanceX, distanceY);
//
//            int y = (int) Math.abs(e2.getRawY() - e1.getRawY());
//            if (y < mMinY) {
//                hideLayer();
//                showProgressLayer(true);
//                return super.onScroll(e1, e2, distanceX, distanceY);
//            }
//
//            if (e2x < mLeftX) {
//                showProgressLayer(false);
//                if (null != mBrightnessLayer) {
//                    mBrightnessLayer.onScroll(e2);
//                }
//                return true;
//            } else if (e2x > mRightOffset) {
//                showProgressLayer(false);
//                if (null != mVolumeLayer) {
//                    mVolumeLayer.onScroll(e2);
//                }
//                return true;
//            } else {
//                hideLayer();
//                showProgressLayer(true);
//            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            BFYLog.d(TAG, "onFling,e1.x=" + e1.getRawX() + ",e2.x=" + e2.getRawX() + ",velocityX=" + velocityX + ",velocityY=" + velocityY);
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            BFYLog.d(TAG, "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            BFYLog.d(TAG, "onDoubleTapEvent");
//            changeOrientation();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            toggleMediaControlsVisiblity();
            return true;
        }
    }

    private void showProgressLayer(boolean show, boolean fadeOut) {
        BFYLog.d(TAG, "showProgressLayer,show=" + show);
        if (null != mProgressLayer) {
            if (show) {
                mMediaController.hide();
                mProgressLayer.show();
            } else {
                mProgressLayer.hide(fadeOut);
            }
        }
    }

    /**
     * 设置解码模式
     */
    public void setDecodeMode(DecodeMode mode) {
    	BFYLog.d(TAG, "setDecodeMode, current=" + mCurrentDecodeMode + ", set=" + mode);
    	mCurrentDecodeMode = mode;
        loadDecodeView();
    }

    /**
     * 播放出错回调接口，调用方设置
     */
    public void setPlayErrorListener(PlayErrorListener listener) {
    	mPlayErrorListener = listener;
    	/*if (mPlayerView != null) {
    		mPlayerView.setPlayErrorListener(mPlayErrorListener);
    	}*/
    }
    
    public void setPlayerEventListener(PlayerEventListener listener) {
    	mPlayerEventListener = listener;
    	/*if (mPlayerView != null) {
    		mPlayerView.setPlayErrorListener(mPlayErrorListener);
    	}*/
    }

    
    public void changeOrientation() {
        BFYLog.d(TAG, "changeOrientation");
        if (mFullScreen || null == getContext()) return;

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            portrait();
        } else {
            landscape();
        }
    }
    
    private boolean canReportStatInfo() {
    	int errorCode = PlayErrorManager.NO_ERROR;
    	if (mPlayErrorManager != null) {
    		errorCode = mPlayErrorManager.getErrorCode();
    	}
    	
    	boolean result =
    			errorCode != PlayErrorManager.NO_NETWORK &&
    			errorCode != PlayErrorManager.MOBILE_NO_PLAY;
    	
    	return result;
    }
    
    private void prepareBaseStatInfo(StatInfo statInfo) {
		statInfo.gcid = BFYSysUtils.getFidFromVk(mVideoInfo.getUrl());
		statInfo.userId = BFYSysUtils.getUidFromVk(mVideoInfo.getUrl());
		statInfo.decodeMode = (mCurrentDecodeMode == DecodeMode.SOFT ? 0 : 1);
		statInfo.errorCode = mPlayErrorManager.getErrorCode();
    }

    /**
     * 上报播放体验数据统计
     */
    public void reportPlayExperienceStatInfo() {
    	if (!canReportStatInfo()) return;
    	
    	StatInfo statInfo = getStatInfo();
    	if (statInfo == null || mVideoInfo == null) return;

    	prepareBaseStatInfo(statInfo);
    	
    	if (isLivePlayer()) {
    		StatReporter.getInstance().report(statInfo.makeLiveExpUrl());
    	} else {
    		StatReporter.getInstance().report(statInfo.makeVodExpUrl());
    	}
    }
    
    /**
     * 上报播放过程数据统计
     */
    public void reportPlayProcessStatInfo() {
    	if (!canReportStatInfo()) return;

    	StatInfo statInfo = getStatInfo();
    	if (statInfo == null || mVideoInfo == null) return;

    	prepareBaseStatInfo(statInfo);
    	
    	if (isLivePlayer()) {
    		StatReporter.getInstance().report(statInfo.makeLiveProUrl());
    	} else {
    		StatReporter.getInstance().report(statInfo.makeVodProUrl());
    	}
    }

    public int getDuration() {
        if (null != mPlayerView) {
            return mPlayerView.getDuration();
        } else {
            return -1;
        }
    }
    
    public int getCurPosition() {    	
    	if (null != mPlayerView) {
    		if (mPlayTaskType == PlayTaskType.LIVE && mCurrentDecodeMode == DecodeMode.SOFT) {
    			if (mStartPlayTime == 0) {
    				mStartPlayTime = mPlayerView.getCurrentPosition();
    				return 0;
    			} else {
    				int curPlayTime = mPlayerView.getCurrentPosition();
    				if (curPlayTime < mStartPlayTime) {
    					mStartPlayTime = curPlayTime;
    					return 0;
    				} else {
    					return curPlayTime - mStartPlayTime; 
    				}
    			}
    		} else {
    			return mPlayerView.getCurrentPosition();
    		}
    	} else {    		
    		return -1;
    	}
    }

    public void setDefinitions(ArrayList<String> definitions) {
        if (null != mMediaController) {
            mMediaController.setDefinitions(definitions);
        }
    }

    public void setDefChangedListener(DefinitionController.OnDefinitionChangedListener l) {
        if (null != mMediaController) {
            mMediaController.setDefChangedListener(l);
        }
    }

    private final int ADJUST_ORIENTATION = 5;
    
    private static final int ORIENTATION_LIE = -1;//平放
    private static final int ORIENTATION_TOP = 0;//顶部向上
    private static final int ORIENTATION_LEFT = 1;//左侧向上
    private static final int ORIENTATION_BOTTOM = 2;//顶部向上
    private static final int ORIENTATION_RIGHT = 3;//右侧向上

    private OrientationEventListener mOrientationEventListener;
    private int mCurrentOrigentation = -1;//当前方向
    private int mLastOrientation = -1;//上一次方向

    class PlayerOrientationEventListener extends OrientationEventListener {

        public PlayerOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                BFYLog.d(TAG, "PlayerOrientationEventListener,onOrientationChanged,ORIENTATION_UNKNOWN");
                //平放
                mCurrentOrigentation = ORIENTATION_LIE;
            } else if ((orientation >= 0 && orientation < 10) || (orientation >= 350 && orientation < 360)) {
                //顶部向上
                mCurrentOrigentation = ORIENTATION_TOP;
            } else if (orientation >= 80 && orientation < 100) {
                //左边向上
                mCurrentOrigentation = ORIENTATION_LEFT;
            } else if (orientation >= 170 && orientation < 190) {
                //底部向上
                mCurrentOrigentation = ORIENTATION_BOTTOM;
            } else if (orientation >= 260 && orientation < 280) {
                //右边向上
                mCurrentOrigentation = ORIENTATION_RIGHT;
            }

            if (mLastOrientation != mCurrentOrigentation) {
                BFYLog.d(TAG, "PlayerOrientationEventListener,onOrientationChanged,mCurrentOrigentation=" + mCurrentOrigentation + ",mLastOrientation=" + mLastOrientation);
                mHandler.removeMessages(ADJUST_ORIENTATION);
                Message msg = mHandler.obtainMessage(ADJUST_ORIENTATION, mCurrentOrigentation, mLastOrientation);
                mHandler.sendMessageDelayed(msg, 100);
                mLastOrientation = mCurrentOrigentation;
            }
        }

    }

    private static class PlayerHandler extends BFYWeakReferenceHandler<PlayerController> {

        public PlayerHandler(PlayerController reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(PlayerController reference, Message msg) {
            if (msg.what == reference.ADJUST_ORIENTATION) {
//                int current = msg.arg1;
//                int last = msg.arg2;
            	
            	if (!reference.mAutoFullscreen)
            		return;
            	
                int current = reference.mCurrentOrigentation;
                int last = reference.mLastOrientation;
                BFYLog.d(reference.TAG, "PlayerHandler,handleMessage,current=" + current + ",last=" + last);
                if (current == ORIENTATION_LIE) {
                    //平放
                    if (last == ORIENTATION_LEFT || last == ORIENTATION_RIGHT) {
                        reference.landscape();
                    } else {
                        if (null == reference.mContext) return;
                        WindowManager windowManager = (WindowManager) reference.mContext.getSystemService(Context.WINDOW_SERVICE);
                        int rotation = windowManager.getDefaultDisplay().getRotation();
                        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                            BFYLog.d(reference.TAG, "adjustOriention,Surface.ROTATION_90 || Surface.ROTATION_270");
                            reference.landscape();
                        } else {
                            BFYLog.d(reference.TAG, "adjustOriention,Surface.ROTATION_0 || Surface.ROTATION_180");
                            reference.portrait();
                        }
                    }
                } else if (current == ORIENTATION_TOP) {
                    //顶部向上
                    reference.portrait();
                } else if (current == ORIENTATION_LEFT) {
                    //左边向上
                    reference.landscape();
                } else if (current == ORIENTATION_BOTTOM) {
                    //底部向上
                    reference.portrait();
                } else if (current == ORIENTATION_RIGHT) {
                    //右边向上
                    reference.landscape();
                }
            }
        }

    }

    class ScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BFYLog.d(TAG, "ScreenBroadcastReceiver,onReceive,action=" + action);
            if (Intent.ACTION_SCREEN_ON.equals(action)) { // 开屏
                if (null != mScreenStateListener) {
                    mScreenStateListener.onScreenOn();
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) { // 锁屏
                if (null != mScreenStateListener) {
                    mScreenStateListener.onScreenOff();
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) { // 解锁
                if (null != mScreenStateListener) {
                    mScreenStateListener.onUserPresent();
                }
            }
        }
    }

    public interface ScreenStateListener {

        public void onScreenOn();

        public void onScreenOff();

        public void onUserPresent();
    }

    public interface OrientationChangedListener {

        public void onOrientationChanged(boolean isFullScreen, int screenWidth, int screenHeight);

    }
}
