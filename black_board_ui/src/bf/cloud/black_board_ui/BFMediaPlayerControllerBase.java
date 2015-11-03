package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.BasePlayer.PlayErrorListener;
import bf.cloud.android.playutils.BasePlayer.PlayEventListener;
import bf.cloud.android.playutils.BasePlayer;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public abstract class BFMediaPlayerControllerBase extends FrameLayout implements
		PlayErrorListener, PlayEventListener {
	protected final String TAG = BFMediaPlayerControllerBase.class
			.getSimpleName();
	public final static int VIDEO_TYPE_VOD = 0;
	public final static int VIDEO_TYPE_LIVE = 1;
	protected int mVideoType = VIDEO_TYPE_VOD;
	protected LayoutInflater mLayoutInflater = null;

	protected Context mContext = null;
	protected FrameLayout mPlaceHoler = null;
	protected RelativeLayout mErrorFrame = null;
	protected FrameLayout mStatusController = null;
	private ProgressBar mProgressBarBuffering = null;
	private ImageView mProgressBarIcon = null;
	private EventHandler mEventHandler = new EventHandler();
	private ErrorHandler mErrorHandler = new ErrorHandler();
	protected PlayErrorManager mPlayErrorManager = null;

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
	
	private void init(){
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlayErrorManager = new PlayErrorManager();
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
	}

	private void initStatusFrame() {
		mProgressBarBuffering = (ProgressBar) mStatusController.findViewById(R.id.progressBar);
		mProgressBarBuffering.setVisibility(View.INVISIBLE);
		mProgressBarIcon = (ImageView) mStatusController.findViewById(R.id.icon);
		mProgressBarIcon.setVisibility(View.INVISIBLE);
	}

	private void initErrorFrame() {
		ImageButton ibPlay = (ImageButton) mErrorFrame.findViewById(R.id.error_play_button);
		ibPlay.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onClickPlayButton();
			}
		});
		ImageView ibBack = (ImageView) mErrorFrame.findViewById(R.id.error_backButton);
		ibBack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((Activity)mContext).finish();
			}
		});
	}

	protected abstract void onClickPlayButton();

	protected void attachPlayer(BasePlayer bp) {
		if (bp == null) {
			Log.d(TAG, "mBasePlayer is null");
			throw new NullPointerException("mBasePlayer is null");
		}
		// attach Listeners
		bp.registPlayEventListener(this);
		bp.registPlayErrorListener(this);
		// attach functions

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

			default:
				break;
			}
		}
	}
	
	private class ErrorHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			showErrorFrame(msg.what);
		}
	}
	
	protected void showBuffering(boolean flag){
		if (flag)
			mProgressBarBuffering.setVisibility(View.VISIBLE);
		else
			mProgressBarBuffering.setVisibility(View.INVISIBLE);
	}
	
	public void showIcon(boolean flag) {
		if (mProgressBarIcon == null)
			return;
		if (flag)
			mProgressBarIcon.setVisibility(View.VISIBLE);
		else
			mProgressBarIcon.setVisibility(View.INVISIBLE);
	}

	protected void showPlaceHolder(boolean flag){
		if (flag)
			mPlaceHoler.setVisibility(View.VISIBLE);
		else
			mPlaceHoler.setVisibility(View.INVISIBLE);
	}
	
	protected abstract void showErrorFrame(int errorCode);
	
	protected void hideErrorFrame(){
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
}
