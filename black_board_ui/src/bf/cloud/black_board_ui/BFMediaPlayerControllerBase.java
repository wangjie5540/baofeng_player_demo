package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.VideoFrame;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public abstract class BFMediaPlayerControllerBase extends FrameLayout {
	protected final String TAG = BFMediaPlayerControllerBase.class.getSimpleName();
	public final static int VIDEO_TYPE_VOD = 0;
	public final static int VIDEO_TYPE_LIVE = 1;
	protected int mVideoType = VIDEO_TYPE_VOD;
	private LayoutInflater mLayoutInflater = null;

	private Context mContext = null;
	private VideoFrame mVideoFrame = null;
	private FrameLayout mPlaceHoler = null;
	private FrameLayout mErrorFrame = null;
	private FrameLayout mStatusController = null;

	public BFMediaPlayerControllerBase(Context context) {
		super(context);
		mContext = context;
		initViews();
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initViews();
		test();
	}

	public BFMediaPlayerControllerBase(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		initViews();
	}

	/**
	 * ≥ı ºªØcommonÕº≤„
	 */
	protected void initViews() {
		removeAllViews();
		setBackgroundColor(Color.WHITE);
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;

		//  ”∆µ≤„
		mVideoFrame = (VideoFrame) mLayoutInflater.inflate(
				R.layout.vp_video_frame, this, false);
		addView(mVideoFrame, layoutParams);
		// ª∫≥Â≤„
		mStatusController = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_status_controller, this, false);
		addView(mStatusController, layoutParams);
		// ¥ÌŒÛÃ· æ≤„
		mErrorFrame = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_error_frame, this, false);
		addView(mErrorFrame, layoutParams);
		// ’⁄µ≤≤„
		mPlaceHoler = (FrameLayout) mLayoutInflater.inflate(
				R.layout.vp_place_holder, this, false);
		addView(mPlaceHoler, layoutParams);
	}

	private void test() {
		mPlaceHoler.setVisibility(View.INVISIBLE);
		mErrorFrame.setVisibility(View.INVISIBLE);
//		mStatusController.setVisibility(View.INVISIBLE);
//		mVideoFrame.setVisibility(View.INVISIBLE);
	}
}
