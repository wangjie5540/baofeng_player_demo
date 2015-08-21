package bf.cloud.android.components.mediaplayer;

import android.content.Context;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import bf.cloud.android.components.mediaplayer.definition.DefinitionController;
import bf.cloud.android.components.mediaplayer.playprogress.PlayProgressController;
import bf.cloud.android.components.mediaplayer.widget.PlayPause;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.playutils.PlayTaskType;


/**
 * 控制器,包含控制MediaPlayer的各种控制器.
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class MediaController extends Controller {
    private final String TAG = MediaController.class.getSimpleName();

    //控制器默认显示时间,5秒
    private static final int DEFAULT_TIMEOUT = 5000;
    //控制器隐藏的handle的message标示
    private static final int FADE_OUT = 1;

    private Context mContext;
    private PlayerController mPlayerController;
    //视频播放器
    private MediaPlayerControl mPlayer;
    private View mAnchor;
    private View mRoot;
    //播放控制器是否可见
    private boolean mShowing;
    //播放控制器顶部
    private View mRootHeader;
    //暂停、播放按钮
    private PlayPause mPausePlay;
    //private ImageButton mPausePlayButton;
    //播放进度控制器
    private PlayProgressController mPlayProgressController;
    //返回按钮
    private ImageView mBackButton;
    //选集按钮
    private Button mSectionButton;
    private boolean mMinfoButtonEnable;
    private TextView mVideoTitleTxt;
    //剧集的标题
    private String mVideoTitle;
    //清晰度控制器
    private DefinitionController mDefinationController;
	private Button mFullScreenButton;

    private int mLandspaceOffset = 250;
    private int mPortaitOffset = 150;

    private MediaHandler mHandler;

    public MediaController(Context context) {
        super(context);
        init(context);
    }

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Context context) {
        BFYLog.d(TAG, "init");
        mContext = context;
        mHandler = new MediaHandler(this);
        initFloatingWindow();
    }

    private void initFloatingWindow() {
        BFYLog.d(TAG, "initFloatingWindow");
//        setFocusable(true);
//        setFocusableInTouchMode(true);
//        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
//        requestFocus();
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        BFYLog.d(TAG, "setMediaPlayer");
        mPlayer = player;
    }

    /**
     * 设置视频控制器view锚点view,可以是一个VideoViewHw,或者Activity的主view
     *
     * @param view 控制器可见时锚点view.
     */
    public void setAnchorView(View view) {
        BFYLog.d(TAG, "setAnchorView");
        if (null != mAnchor && mAnchor == view) return;

        mAnchor = view;

        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);

        initControllerView(mRoot);
    }

    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_media_controller"), null);
        return mRoot;
    }

    private void initControllerView(View v) {
        BFYLog.d(TAG, "initControllerView");
        mPausePlay = (PlayPause) v.findViewById(BFYResUtil.getId(getContext(), "pausePlay"));
        if (null != mPausePlay) {
            mPausePlay.setMediaController(this);
        }
        mRootHeader = v.findViewById(BFYResUtil.getId(getContext(), "head"));
        if (null != mPausePlay) {
            mPausePlay.setMediaController(this);
        }
        mVideoTitleTxt = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "videoTitle"));
        if (null != mVideoTitleTxt) {
            mVideoTitleTxt.setText(mVideoTitle);
        }
        mBackButton = (ImageView) v.findViewById(BFYResUtil.getId(getContext(), "backButton"));
        if (null != mBackButton) {
            mBackButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (null != mPlayerController) {
                        mPlayerController.backPressed(false);
                    }
                }
            });
        }

        mPausePlay = (PlayPause) v.findViewById(BFYResUtil.getId(getContext(), "pausePlay"));
        if (null != mPausePlay) {
            mPausePlay.setMediaController(this);
        }

        //初始化音量控制器
        /*mVolumeController = (VolumeController) v.findViewById(ResUtil.getId(getContext(), "volumeController"));
        if (null != mVolumeController) {
            mVolumeController.setMediaController(this);
        }*/
        //初始化播放进度控制器
        mPlayProgressController = (PlayProgressController) v.findViewById(BFYResUtil.getId(getContext(), "playProgressController"));
        if (null != mPlayProgressController) {
            mPlayProgressController.setMediaController(this);
        }

        mDefinationController = (DefinitionController) v.findViewById(BFYResUtil.getId(getContext(), "definationController"));
        if (null != mDefinationController) {
            mDefinationController.setMediaController(this);
            enableDefinitionSwitch(false);
        }
        mFullScreenButton = (Button) v.findViewById(BFYResUtil.getId(getContext(), "fullScreen"));
        if (null != mFullScreenButton) {
            mFullScreenButton.setOnClickListener(clickListener);
        }
    }
    
    private void updateViews() {
        boolean inPlaybackState = mPlayer.isInPlaybackState() || !mPlayerController.isPlayWaitState();
        setEnabled(inPlaybackState);
        if (isLivePlayer()) {
        	mPlayProgressController.hide();
        }
    }

    /**
     * 在屏幕上显示视频控制器.不活动状态下,sDefaultTimeout秒后自动消失
     */
    public void show() {
    	updateViews();
        show(DEFAULT_TIMEOUT);
    }

    /**
     * 在屏幕上显示视频控制器.不活动状态下,'timeout'毫秒后自动消失.
     *
     * @param timeout 毫秒为单位的时间延迟.用0显示控制器直到hide()方法被调用.
     */
    public void show(int timeout) {
        BFYLog.d(TAG, "show,timeout=" + timeout);
        BFYLog.d(TAG, "show,mShowing=" + mShowing + "--mAnchor == null  " + (mAnchor == null));
        if (!mShowing && mAnchor != null) {
            //int[] anchorpos = new int[2];
            //mAnchor.getLocationOnScreen(anchorpos);

            //BFYLog.d(TAG, "show,anchor width=" + mAnchor.getWidth() + ",height=" + mAnchor.getHeight());
            //LayoutParams lp = new LayoutParams(mAnchor.getWidth(), mAnchor.getHeight());
        	BFYLog.d(TAG, "show,anchor width=" + mPlayerController.getDisplayWidth() + ",height=" + mPlayerController.getDisplayHeight());
        	LayoutParams lp = new LayoutParams(mPlayerController.getDisplayWidth(), mPlayerController.getDisplayHeight());
            setLayoutParams(lp);
            setVisibility(View.VISIBLE);
            mShowing = true;
        }
        updatePausePlay();

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            BFYLog.d(TAG, "show,ORIENTATION_LANDSCAPE");
            if (null != mSectionButton) {
                //横屏根据mMinfoButtonEnable显示或隐藏剧集按钮
                mSectionButton.setVisibility(mMinfoButtonEnable ? View.VISIBLE : View.GONE);
            }
            //显示顶部控制栏
            if (null != mRootHeader) {
            	mRootHeader.setVisibility(View.VISIBLE);
            }
            if (null != mBackButton) {
                mBackButton.setVisibility(View.VISIBLE);
            }
            if (null != mDefinationController) {
                //全屏显示清晰度按钮
                mDefinationController.setVisibility(View.VISIBLE);
            }
            if (null != mFullScreenButton) {
                //全屏时隐藏全屏按钮
                mFullScreenButton.setVisibility(View.GONE);
            }
        } else {//竖屏时隐藏选集、音量控制器、清晰度按钮,显示全屏按钮
            BFYLog.d(TAG, "show,ORIENTATION_PORTRAIT");
            //隐藏顶部控制栏
            if (null != mRootHeader) {
            	mRootHeader.setVisibility(View.GONE);
            }
            if (null != mSectionButton) {
                //竖屏不显示剧集按钮
                mSectionButton.setVisibility(View.GONE);
            }
            if (null != mBackButton) {
                mBackButton.setVisibility(View.GONE);
            }
            if (null != mDefinationController) {
                //竖屏时不显示清晰度按钮
                mDefinationController.setVisibility(View.GONE);
            }
            if (null != mFullScreenButton) {
                //竖屏时显示全屏按钮
                mFullScreenButton.setVisibility(View.VISIBLE);
            }
        }

        // 促使progress bar 更新,即使mShowing属性已经为true
        if (null != mPlayProgressController && !isLivePlayer()) {
            mPlayProgressController.show();
        }
        setTitleLayout();
        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    private void setTitleLayout() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        ViewGroup.LayoutParams layoutParams = mVideoTitleTxt.getLayoutParams();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            layoutParams.width = metrics.widthPixels - dip2px(mContext, mLandspaceOffset);
            BFYLog.d(TAG, "setTitleLayout,landspace," + layoutParams.width);
        } else {
            layoutParams.width = metrics.widthPixels - dip2px(mContext, mPortaitOffset);
            BFYLog.d(TAG, "setTitleLayout,portait," + layoutParams.width);
        }
        mVideoTitleTxt.setLayoutParams(layoutParams);
    }

    private boolean isLivePlayer() {
    	if (mPlayerController != null) {
    		return mPlayerController.getPlayTaskType() == PlayTaskType.LIVE;
    	} else {
    		return false;
    	}
    }

    public int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public boolean isShowing() {
        return mShowing;
    }

    /**
     * 从屏幕上移除视频控制器
     */
    public void hide() {
        BFYLog.d(TAG, "hide");
        if (mAnchor == null)
            return;

        if (mShowing) {
            if (null != mPlayProgressController)
                mPlayProgressController.hide();
            if (null != mDefinationController) {
                mDefinationController.hide();
            }
        }
        mShowing = false;
        setVisibility(View.GONE);
    }

    @Override
    public void reset() {
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    private static class MediaHandler extends BFYWeakReferenceHandler<MediaController> {

        public MediaHandler(MediaController reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(MediaController reference, Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    reference.hide();
                    break;
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        return super.onTrackballEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        BFYLog.d(TAG, "dispatchKeyEvent,keyCode=" + keyCode);
        if (event.getRepeatCount() == 0 && (
                keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_SPACE)) {
            doPauseResume();
            show();
            if (mPausePlay != null) {
                mPausePlay.requestFocus();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            //音量键,显示控制器
            show();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();
            return true;
        } else {
            show();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        BFYLog.d(TAG, "onInterceptTouchEvent");
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        BFYLog.d(TAG, "onTouchEvent");
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            BFYLog.d(TAG, "onInterceptTouchEvent,ACTION_UP");
            show();
        } else {
            show(3600000);
        }
        return super.onTouchEvent(event);
    }

    private OnClickListener clickListener = new OnClickListener() {
        public void onClick(View v) {
            int vid = v.getId();
            if (vid == BFYResUtil.getId(getContext(), "fullScreen")) {//全屏按钮
                mFullScreenButton.setVisibility(View.GONE);
                mPlayerController.changeOrientation();
            }
        }
    };

    /**
     * 根据播放状态更改播放、暂停按钮
     */
    public void updatePausePlay() {
        BFYLog.d(TAG, "updatePausePlay");
        if (mRoot == null || mPausePlay == null)
            return;

        BFYLog.d(TAG, "updatePausePlay,mPlayer.isPlaying()=" + mPlayer.isPlaying());
        mPausePlay.updatePausePlay(mPlayer.isPlaying());
    }

    /**
     * 做一次视频暂停或播放的切换.
     */
    public void doPauseResume() {
        boolean isPlaying = mPlayer.isPlaying();
        BFYLog.d(TAG, "doPauseResume,isPlaying=" + isPlaying);
        if (isPlaying) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        show();
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        BFYLog.d(TAG, "setEnabled,enabled=" + enabled);

        if (mPausePlay != null) {
            mPausePlay.setEnabled(enabled);
        }
        if (mPlayProgressController != null) {
            mPlayProgressController.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BFYLog.d(TAG, "onKeyDown,keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            show();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 设置视频标题.
     *
     * @param title 视频标题
     */
    public void setTitle(String title) {
        BFYLog.d(TAG, "setTitle,title=" + title);
        mVideoTitle = title;
        if (null != mVideoTitleTxt) {
            mVideoTitleTxt.setText(title);
        }
    }

    /**
     * 设置剧集切换是否可见
     *
     * @param visible 可见性
     */
    public void setMinfoButtonEnable(boolean visible) {
        BFYLog.d(TAG, "setMinfoButtonEnable,visible=" + visible);
        mMinfoButtonEnable = visible;
        if (null == mSectionButton) return;
        if (visible) {
            mSectionButton.setVisibility(View.VISIBLE);
        } else {
            mSectionButton.setVisibility(View.GONE);
        }
    }

    /**
     * 收到屏幕旋转的通知,隐藏视频控制视图.
     */
    public void orientationChanged() {
        BFYLog.d(TAG, "orientationChanged");
        LayoutParams params = (LayoutParams) getLayoutParams();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        setLayoutParams(params);

        hide();
    }

    public MediaPlayerControl getMediaPlayer() {
        return mPlayer;
    }

    public void setPlayerController(PlayerController mPlayerController) {
        this.mPlayerController = mPlayerController;
    }

    /**
     * 停止播放进度.
     */
    public void stopProgress() {
        mPlayProgressController.stopProgress();
    }

    /**
     * 启动播放进度.
     */
    public void startProgress() {
        if (isShowing()) {
            mPlayProgressController.startProgress();
        }
    }

    public void updateProgress(int current, int duration) {
        mPlayProgressController.updateProgress(current, duration);
    }

    public void switchDecode() {
        BFYLog.d(TAG, "switchDecode");
        if (null != mPlayerController) {
            //mPlayerController.changeDecode();
        }
    }

    public void enableDefinitionSwitch(boolean enabled) {
        BFYLog.d(TAG, "enableDefinitionSwitch:" + enabled);
        if (null != mDefinationController) {
            mDefinationController.setDefinitonEnabled(enabled);
        }
    }

    public void setDefinitions(ArrayList<String> definitions) {
        if (null != mDefinationController) {
            mDefinationController.setDefinitions(definitions);
        }
    }

    public void setDefChangedListener(DefinitionController.OnDefinitionChangedListener l) {
        if (null != mDefinationController) {
            mDefinationController.setOnDefChangedListener(l);
        }
    }
    
    public void resize() {
    	//LayoutParams lp = new LayoutParams(mPlayerController.getDisplayWidth(), mPlayerController.getDisplayHeight());
        //setLayoutParams(lp);
    	hide();
    	show();
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        void stop();

        /**
         * 获取当前播放视频的总长度
         *
         * @return 当前视频总长度, 单位毫秒
         */
        int getDuration();

        /**
         * 获取视频的当前播放位置
         *
         * @return 当前播放位置, 单位毫秒
         */
        int getCurrentPosition();

        /**
         * 跳到pos处播放
         *
         * @param pos 待播放位置(毫秒)
         */
        void seekTo(int pos);

        /**
         * 是否正在播放
         *
         * @return
         */
        boolean isPlaying();

        //是否在播放状态
        boolean isInPlaybackState();

        //获取视频缓冲的百分比
        int getBufferPercentage();

        void setVolume(float leftVolume, float rightVolume);

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        void setAnchorView(View anchorView);
    }

}
