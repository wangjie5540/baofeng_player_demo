package bf.cloud.black_board_ui;

import java.util.Formatter;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.playutils.BasePlayer.PlayErrorListener;
import bf.cloud.android.playutils.BasePlayer.PlayEventListener;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;

/**
 * @author wang Note: You should change your project to UTF8
 */
public abstract class BFMediaPlayerControllerBase extends FrameLayout implements
		PlayErrorListener, PlayEventListener, View.OnClickListener,
		View.OnTouchListener {
	protected final String TAG = BFMediaPlayerControllerBase.class
			.getSimpleName();
	public final static int VIDEO_TYPE_VOD = 0;
	public final static int VIDEO_TYPE_LIVE = 1;
	protected final static int DIVISION = 4;
	protected int mVideoType = VIDEO_TYPE_VOD;
	private final static int DELAY_TIME_LONG = 5000; // ms
	private final static int DELAY_TIME_SHORT = 3000; // ms
	protected LayoutInflater mLayoutInflater = null;

	protected Context mContext = null;
	protected FrameLayout mPlaceHoler = null;
	protected RelativeLayout mErrorFrame = null;
	protected FrameLayout mStatusController = null;
	private ProgressBar mProgressBarBuffering = null;
	private ImageView mProgressBarIcon = null;
	private LinearLayout mBrightnessLayer = null;
	private TextView mBrightnessPercent = null;
	private LinearLayout mVolumeLayer = null;
	private TextView mVolumePercent = null;
	private EventHandler mEventHandler = new EventHandler();
	private ErrorHandler mErrorHandler = new ErrorHandler();
	protected PlayErrorManager mPlayErrorManager = null;
	protected FrameLayout mPlayerController = null;
	protected RelativeLayout mControllerHead = null;
	protected RelativeLayout mControllerBottom = null;
	protected ImageView mControllerBack = null;
	protected TextView mControllerVideoTitle = null;
	protected ImageButton mControllerPlayPause = null;
	protected TextView mControllerCurentPlayTime = null;
	protected TextView mControllerDuration = null;
	protected SeekBar mControllerProgressBar = null;
	protected Button mControllerDefinition = null;
	protected StringBuilder mFormatBuilder = null;
	protected Formatter mFormatter = null;
	// 切换屏幕
	protected Button mControllerChangeScreen = null;
	// 全屏标志
	protected boolean mIsFullScreen = false;
	// 自适应屏幕
	private boolean mIsAutoScreen = true;
	// 屏幕旋转观察者
	protected PlayerOrientationMessageListener mPlayerOrientationMessageListener = null;
	protected int mScreenWidth = -1;
	protected int mScreenHeight = -1;
	protected int mDisplayWidth = -1;
	protected int mDisplayHeight = -1;
	protected int mVideoFrameOrigenalWidth = -1;
	protected int mVideoFrameOrigenalHeight = -1;
	private BasePlayer mPlayer = null;

	protected static final int MSG_SHOW_CONTROLLER = 2000;
	protected static final int MSG_HIDE_CONTROLLER = 2001;
	protected static final int MSG_SHOW_BRIGHTNESS = 2002;
	protected static final int MSG_HIDE_BRIGHTNESS = 2003;
	protected static final int MSG_SHOW_VOLUME = 2004;
	protected static final int MSG_HIDE_VOLUME = 2005;
	protected static final int MSG_CHANGE_SCREEN_PORTRAIT = 2006;
	protected static final int MSG_CHANGE_SCREEN_LANDSCAPE = 2007;
	public static final int MSG_ADJUST_ORIENTATION = 5;

	protected Handler mMessageHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			int what = msg.what;
			int arg1 = msg.arg1;
			int arg2 = msg.arg2;
			switch (what) {
			case MSG_SHOW_CONTROLLER:
				mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
				mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
				// 5s后，自动隐藏
				mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLLER,
						DELAY_TIME_LONG);
				if (mIsFullScreen)
					showControllerHead(true);
				else
					showControllerHead(false);
				showControllerBottom(true);
				break;
			case MSG_HIDE_CONTROLLER:
				mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
				mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
				showControllerHead(false);
				showControllerBottom(false);
				break;
			case MSG_SHOW_BRIGHTNESS:
				mMessageHandler.removeMessages(MSG_HIDE_BRIGHTNESS);
				mMessageHandler.removeMessages(MSG_SHOW_BRIGHTNESS);
				showVolumeLayer(false);
				showBrightnessLayer(true);
				mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_BRIGHTNESS,
						DELAY_TIME_SHORT);
				break;
			case MSG_HIDE_BRIGHTNESS:
				mMessageHandler.removeMessages(MSG_HIDE_BRIGHTNESS);
				mMessageHandler.removeMessages(MSG_SHOW_BRIGHTNESS);
				showBrightnessLayer(false);
				break;
			case MSG_SHOW_VOLUME:
				mMessageHandler.removeMessages(MSG_SHOW_VOLUME);
				mMessageHandler.removeMessages(MSG_HIDE_VOLUME);
				showBrightnessLayer(false);
				showVolumeLayer(true);
				mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_VOLUME,
						DELAY_TIME_SHORT);
				break;
			case MSG_HIDE_VOLUME:
				mMessageHandler.removeMessages(MSG_SHOW_VOLUME);
				mMessageHandler.removeMessages(MSG_HIDE_VOLUME);
				showVolumeLayer(false);
				break;
			case MSG_ADJUST_ORIENTATION:
				mMessageHandler.removeMessages(MSG_ADJUST_ORIENTATION);
				int currentOrientation = mPlayerOrientationMessageListener
						.getCurrentOrigentation();

				if (currentOrientation == PlayerOrientationMessageListener.ORIENTATION_LEFT
						|| currentOrientation == PlayerOrientationMessageListener.ORIENTATION_RIGHT)
					changeToLandscape();
				else if (currentOrientation == PlayerOrientationMessageListener.ORIENTATION_BOTTOM
						|| currentOrientation == PlayerOrientationMessageListener.ORIENTATION_TOP)
					changeToPortrait();
				break;
			default:
				Log.d(TAG, "invailid msg");
				break;
			}
			return true;
		}
	});

	public BFMediaPlayerControllerBase(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	protected abstract void showErrorFrame(int errorCode);

	protected abstract void onClickPlayButton();

	protected abstract void initPlayerControllerFrame();

	protected abstract void doMoveLeft();

	protected abstract void doMoveRight();

	private void init() {
		setOnClickListener(this);
		setOnTouchListener(this);
		mPlayerOrientationMessageListener = new PlayerOrientationMessageListener(
				mContext, this);
		mPlayerOrientationMessageListener.start();
		getAllSize();

		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlayErrorManager = new PlayErrorManager();
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}

	private void getAllSize() {
		if (mContext == null) {
			throw new NullPointerException("you should get the Context first");
		}
		WindowManager windowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metrics);
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
		mCenterX = mScreenWidth / 2;
		Log.d(TAG, "mScreenWidth:" + mScreenWidth + "/mScreenHeight:"
				+ mScreenHeight);
		mMinX = Utils.dip2px(mContext, mMinMovementDipX);
		mMinY = Utils.dip2px(mContext, mMinMovementDipY);
		
		int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		measure(w, h);
		mVideoFrameOrigenalWidth = getWidth();
		mVideoFrameOrigenalHeight = getHeight();
		Log.d(TAG, "mVideoFrameOrigenalWidth:" + mVideoFrameOrigenalWidth);
	}

	/**
	 * 初始化common图层
	 */
	protected void initViews() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;

		// 图标层
		mStatusController = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_status_controller, this, false);
		mStatusController.setVisibility(View.VISIBLE);
		initStatusFrame();
		addView(mStatusController, layoutParams);
		// 遮挡层
		mPlaceHoler = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_place_holder, this, false);
		mPlaceHoler.setVisibility(View.INVISIBLE);
		addView(mPlaceHoler, layoutParams);

		RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		// 错误提示层
		mErrorFrame = (RelativeLayout) mLayoutInflater.inflate(
				R.layout.vp_error_frame, this, false);
		mErrorFrame.setVisibility(View.INVISIBLE);
		initErrorFrame();
		addView(mErrorFrame, layoutParams1);
		// 播放控制层
		mPlayerController = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_media_controller, this, false);
		mPlayerController.setVisibility(View.VISIBLE);
		initPlayerControllerFrame();
		addView(mPlayerController, layoutParams);
	}

	private void showControllerHead(boolean flag) {
		if (mControllerHead == null)
			return;
		if (flag)
			mControllerHead.setVisibility(View.VISIBLE);
		else
			mControllerHead.setVisibility(View.INVISIBLE);
	}

	private void showControllerBottom(boolean flag) {
		if (mControllerBottom == null)
			return;
		if (flag)
			mControllerBottom.setVisibility(View.VISIBLE);
		else
			mControllerBottom.setVisibility(View.INVISIBLE);
	}

	private void initStatusFrame() {
		mProgressBarBuffering = (ProgressBar) mStatusController
				.findViewById(R.id.progressBar);
		mProgressBarBuffering.setVisibility(View.INVISIBLE);
		mProgressBarIcon = (ImageView) mStatusController
				.findViewById(R.id.icon);
		mProgressBarIcon.setVisibility(View.INVISIBLE);
		mBrightnessLayer = (LinearLayout) mStatusController
				.findViewById(R.id.brightness_layout);
		mBrightnessPercent = (TextView) mStatusController
				.findViewById(R.id.brightness_percent);
		mBrightnessPercent.setText("");
		showBrightnessLayer(false);
		mVolumeLayer = (LinearLayout) mStatusController
				.findViewById(R.id.volume_layout);
		mVolumePercent = (TextView) mStatusController
				.findViewById(R.id.volume_percent);
		showVolumeLayer(false);
	}

	private void initErrorFrame() {
		ImageButton ibPlay = (ImageButton) mErrorFrame
				.findViewById(R.id.error_play_button);
		ibPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onClickPlayButton();
			}
		});
		ImageView ibBack = (ImageView) mErrorFrame
				.findViewById(R.id.error_backButton);
		ibBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				((Activity) mContext).finish();
			}
		});
	}

	protected void attachPlayer(BasePlayer bp) {
		if (bp == null) {
			Log.d(TAG, "mBasePlayer is null");
			throw new NullPointerException("mBasePlayer is null");
		}
		mPlayer = bp;
		// attach Listeners
		bp.registPlayEventListener(this);
		bp.registPlayErrorListener(this);
		// attach functions

	}
	
	private void restoreOrigenalVideoFrameSize(){
		if (mVideoFrameOrigenalWidth <= 0 || mVideoFrameOrigenalHeight <= 0){
			mVideoFrameOrigenalWidth = getWidth();
			mVideoFrameOrigenalHeight = getHeight();
		}
	}

	/**
	 * 竖屏
	 */
	public void changeToPortrait() {
		if (null == mContext)
			return;
		restoreOrigenalVideoFrameSize();
		Activity act = (Activity) mContext;
		act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = mVideoFrameOrigenalHeight;
		params.width = mVideoFrameOrigenalWidth;
		mIsFullScreen = false;
		mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
		Log.d(TAG, "portrait end");
	}

	/**
	 * 横屏
	 */
	public void changeToLandscape() {
		Log.d(TAG, "landscape");
		if (null == mContext)
			return;
		restoreOrigenalVideoFrameSize();
		int newOrientation;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		} else {
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		Activity act = (Activity) mContext;
		act.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		act.setRequestedOrientation(newOrientation);
		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = mScreenWidth;
		params.width = mScreenHeight;
		mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
		Log.d(TAG, "landscape end");
		mIsFullScreen = true;
	}

	private class EventHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			showBuffering(false);
			int what = msg.what;
			switch (what) {
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_ENDED:

				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_BUFFERING:
				showBuffering(true);
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_READY:

				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_PREPARING:

				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_START:
				hideErrorFrame();
				showPlaceHolder(false);
				showBuffering(false);
				showIcon(true);
				break;
			case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STARTED:
				showIcon(false);
				mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);

			default:
				break;
			}
		}
	}

	private class ErrorHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			showErrorFrame(msg.what);
		}
	}

	protected void showBuffering(boolean flag) {
		if (flag)
			mProgressBarBuffering.setVisibility(View.VISIBLE);
		else
			mProgressBarBuffering.setVisibility(View.INVISIBLE);
	}

	private void showIcon(boolean flag) {
		if (mProgressBarIcon == null)
			return;
		if (flag)
			mProgressBarIcon.setVisibility(View.VISIBLE);
		else
			mProgressBarIcon.setVisibility(View.INVISIBLE);
	}

	private void showBrightnessLayer(boolean flag) {
		if (mBrightnessLayer == null)
			return;
		if (flag)
			mBrightnessLayer.setVisibility(View.VISIBLE);
		else
			mBrightnessLayer.setVisibility(View.INVISIBLE);
	}

	private void showVolumeLayer(boolean flag) {
		if (mVolumeLayer == null)
			return;
		if (flag)
			mVolumeLayer.setVisibility(View.VISIBLE);
		else
			mVolumeLayer.setVisibility(View.INVISIBLE);
	}

	private void setBrightPercent(int percent) {
		if (mBrightnessPercent == null)
			return;
		mBrightnessPercent.setText(percent + "%");
		mMessageHandler.sendEmptyMessage(MSG_SHOW_BRIGHTNESS);
	}

	private void setVolumePercent(int percent) {
		if (mVolumePercent == null)
			return;
		mVolumePercent.setText(percent + "%");
		mMessageHandler.sendEmptyMessage(MSG_SHOW_VOLUME);
	}

	protected String stringForTime(long timeMs) {
		long totalSeconds = timeMs / 1000;

		long seconds = totalSeconds % 60;
		long minutes = (totalSeconds / 60) % 60;
		long hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	public void sendMessage(int msgType) {
		mMessageHandler.sendEmptyMessage(msgType);
	}

	public void sendMessageDelayed(Message msg, int ms) {
		mMessageHandler.sendMessageDelayed(msg, ms);
	}

	public void removeMessage(int msgType) {
		mMessageHandler.removeMessages(msgType);
	}

	protected void showPlaceHolder(boolean flag) {
		if (flag)
			mPlaceHoler.setVisibility(View.VISIBLE);
		else
			mPlaceHoler.setVisibility(View.INVISIBLE);
	}

	protected void hideErrorFrame() {
		mErrorFrame.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onError(int errorCode) {
		Log.d(TAG, "onError errorCode:" + errorCode);
		mErrorHandler.sendEmptyMessage(errorCode);
	}

	@Override
	public void onEvent(int eventCode) {
		Log.d(TAG, "onEvent eventCode:" + eventCode);
		mEventHandler.sendEmptyMessage(eventCode);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick");
		mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
	}

	private int mCenterX;
	private final int mMinMovementDipX = 20;
	private final int mMinMovementDipY = 20;
	private float mMinY;
	private float mMinX;
	protected static final int MOVE_NONE = -1;
	protected static final int MOVE_LEFT = 1;
	protected static final int MOVE_RIGHT = 2;
	protected static final int MOVE_UP = 3;
	protected static final int MOVE_DOWN = 4;

	protected float preMoveX = -1.0f;
	protected float preMoveY = -1.0f;
	protected MotionEvent motionEvent = null;

	protected int moveDirection = MOVE_NONE;
	protected float moveDistanceX = 0.0f;
	protected float moveDistanceY = 0.0f;
	protected MotionEvent mLastMotionEvent = null;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "MotionEvent.ACTION_DOWN");
			moveDirection = MOVE_NONE;
			preMoveX = event.getRawX();
			preMoveY = event.getRawY();
			break;
		case MotionEvent.ACTION_MOVE: {// 左侧滑动更改亮度,右侧滑动调节音量,其它符合要求的滑动调节播放进度
			Log.d(TAG, "MotionEvent.ACTION_MOVE");
			float afterMoveX = event.getRawX();
			float afterMoveY = event.getRawY();
			moveDistanceX = Math.abs(preMoveX - afterMoveX);
			moveDistanceY = Math.abs(preMoveY - afterMoveY);

			if (moveDistanceX < mMinX && moveDistanceY < mMinY) {// 移动距离太小,就忽略这个消息
				return false;
			} else if (moveDistanceX >= mMinX && moveDistanceY >= mMinY) {// 横向和纵向如果都超过预置距离,则整体忽略
				moveDirection = MOVE_NONE;
				return false;
			} else if (moveDistanceX > mMinX && moveDistanceY < mMinY) {// 横向滑动
				moveDirection = preMoveX > afterMoveX ? MOVE_LEFT : MOVE_RIGHT;
				return false;
			} else if (moveDistanceX < mMinX && moveDistanceY > mMinY) {// 纵向滑动
				moveDirection = preMoveY > afterMoveY ? MOVE_UP : MOVE_DOWN;
			}

			if (preMoveX < mCenterX) {// 靠左,调节屏幕亮度
				onPortraitMove(event, TYPE_BRIGHTNESS);
			} else if (preMoveX > mCenterX) { // 靠右,调节音量
				onPortraitMove(event, TYPE_VOLUME);
			} else { // 在中间,忽略
				return false;
			}
		}

			break;
		case MotionEvent.ACTION_UP:
			onMoveEventActionUp();
			break;

		default:
			break;
		}
		return false;
	}

	private void onPortraitMove(MotionEvent event, int type) {
		if (mContext == null || mPlayer == null)
			return;
		if (mLastMotionEvent == null) {
			mLastMotionEvent = MotionEvent.obtain(event);
			return;
		}

		if (type == TYPE_BRIGHTNESS) {
			float offset = event.getRawY() - mLastMotionEvent.getRawY();
			int value = Utils.getBrightness((Activity) mContext);
			Log.d(TAG, "onPortraitMove,current brightness=" + value);
			if (offset < 0) {
				value += Math
						.abs((int) (offset
								* (BFYConst.MAX_BRIGHTNESS - BFYConst.MIN_BRIGHTNESS) / mScreenHeight));
			} else if (offset > 0) {
				value -= Math
						.abs((int) (offset
								* (BFYConst.MAX_BRIGHTNESS - BFYConst.MIN_BRIGHTNESS) / mScreenHeight));
			}
			if (value < BFYConst.MIN_BRIGHTNESS) {
				value = (int) BFYConst.MIN_BRIGHTNESS;
			} else if (value > BFYConst.MAX_BRIGHTNESS) {
				value = (int) BFYConst.MAX_BRIGHTNESS;
			}
			Utils.effectBrightness((Activity) mContext, value);
			int percent = (int) (value * 100 / BFYConst.MAX_BRIGHTNESS);
			setBrightPercent(percent);
		} else if (type == TYPE_VOLUME) {
			float offset = event.getRawY() - mLastMotionEvent.getRawY();
			if (ignoreIt(Math.abs(offset), mScreenHeight)) {
				return;
			}
			if (offset < 0) {
				mPlayer.incVolume();
			} else if (offset > 0) {
				mPlayer.decVolume();
			}

			int percent = mPlayer.getCurrentVolume() * 100
					/ mPlayer.getMaxVolume();
			setVolumePercent(percent);
		}

		mLastMotionEvent.recycle();
		mLastMotionEvent = MotionEvent.obtain(event);
	}

	private boolean ignoreIt(float distance, int wholeDistance) {
		if (distance < wholeDistance / 10)
			return true;
		return false;
	}

	private void onMoveEventActionUp() {
		Log.d(TAG, "onMoveEventActionUp moveDirection:" + moveDirection);
		mLastMotionEvent = null;
		switch (moveDirection) {
		case MOVE_LEFT:
			doMoveLeft();
			break;
		case MOVE_RIGHT:
			doMoveRight();
			break;

		default:
			break;
		}
		moveDirection = MOVE_NONE;
	}
	
	protected void backToPortrait(){
		if (mIsFullScreen)
			changeToPortrait();
		else
			((Activity)mContext).finish();
	}

	protected final static int TYPE_VOLUME = 0;
	protected final static int TYPE_BRIGHTNESS = 1;
	protected int mType = TYPE_VOLUME;

	@Override
	public boolean performClick() {
		Log.d(TAG, "performClick");
		return super.performClick();
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown,keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            backToPortrait();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
