package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.VodPlayer;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class BFMediaPlayerControllerVod extends BFMediaPlayerControllerBase {
	private VodPlayer mVodPlayer = null;
	private RelativeLayout mPlayCompleteFrame = null;

	public BFMediaPlayerControllerVod(Context context) {
		super(context);
		initViews();
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs) {
		super(context, attrs);
		initViews();
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initViews();
	}

	@Override
	public void attachPlayer(BasePlayer bp) {
		mVodPlayer = (VodPlayer) bp;
		super.attachPlayer(bp);
	}

	@Override
	public void onError(int errorCode) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onEvent(int eventCode) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void initViews() {
		super.initViews();
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		// ²¥·Å½áÊø²ã
		mPlayCompleteFrame = (RelativeLayout) mLayoutInflater.inflate(
				R.layout.vp_play_complete, this, false);
		addView(mPlayCompleteFrame, layoutParams);
	}
}
