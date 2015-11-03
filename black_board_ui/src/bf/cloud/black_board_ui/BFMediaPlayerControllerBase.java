package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.BasePlayer.PlayErrorListener;
import bf.cloud.android.playutils.BasePlayer.PlayEventListener;
import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.VideoFrame;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public abstract class BFMediaPlayerControllerBase extends FrameLayout implements
		PlayErrorListener, PlayEventListener {
	protected final String TAG = BFMediaPlayerControllerBase.class
			.getSimpleName();
	public final static int VIDEO_TYPE_VOD = 0;
	public final static int VIDEO_TYPE_LIVE = 1;
	protected int mVideoType = VIDEO_TYPE_VOD;
	protected LayoutInflater mLayoutInflater = null;

	private Context mContext = null;
	private FrameLayout mPlaceHoler = null;
	private FrameLayout mErrorFrame = null;
	private FrameLayout mStatusController = null;

	public BFMediaPlayerControllerBase(Context context) {
		super(context);
		mContext = context;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/**
	 * ≥ı ºªØcommonÕº≤„
	 */
	protected void initViews() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;

		// ª∫≥Â≤„
		mStatusController = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_status_controller, this, false);
		mStatusController.setVisibility(View.INVISIBLE);
		addView(mStatusController, layoutParams);
		// ¥ÌŒÛÃ· æ≤„
		mErrorFrame = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_error_frame, this, false);
		mErrorFrame.setVisibility(View.INVISIBLE);
		addView(mErrorFrame, layoutParams);
		// ’⁄µ≤≤„
		mPlaceHoler = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_place_holder, this, false);
		mPlaceHoler.setVisibility(View.INVISIBLE);
		addView(mPlaceHoler, layoutParams);
		
//		test();
	}

	private void test() {
		mPlaceHoler.setVisibility(View.INVISIBLE);
		mErrorFrame.setVisibility(View.INVISIBLE);
		// mStatusController.setVisibility(View.INVISIBLE);
		// mVideoFrame.setVisibility(View.INVISIBLE);
	}
	
	protected void attachPlayer(BasePlayer bp){
		if (bp == null){
			Log.d(TAG, "mBasePlayer is null");
			throw new NullPointerException("mBasePlayer is null");
		}
		//attach Listeners
		bp.registPlayEventListener(this);
		bp.registPlayErrorListener(this);
		//attach functions
		
	}
	
	@Override
	public void onError(int errorCode) {
		Log.d(TAG, "onError errorCode:" + errorCode);
		
	}
	
	@Override
	public void onEvent(int eventCode) {
		Log.d(TAG, "onEvent eventCode:" + eventCode);
		
	}
}
