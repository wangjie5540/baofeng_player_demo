package bf.cloud.android.playutils;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.components.mediaplayer.VideoViewExo;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class VideoFrame extends FrameLayout {
	private final String TAG = VideoFrame.class.getSimpleName();
	private DecodeMode mDecodeMode = BFYConst.DEFAULT_DECODE_MODE;
	private VideoViewBase mVideoViewBase = null;
	private Context mContext = null;

	public VideoFrame(Context context) {
		super(context);
		mContext = context;
		updateViews();
	}

	public VideoFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		updateViews();
	}

	public VideoFrame(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		updateViews();
	}

	private void updateViews() {
		// make view
		if (mDecodeMode == DecodeMode.AUTO) {
			mVideoViewBase = new VideoViewExo(mContext);
		} else {
			// mVideoViewBase = new videoview
		}
		// init view

		// add view
		removeAllViews();
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		addView(mVideoViewBase, params);
		// update
		invalidate();
	}

	public void setDecodeMode(DecodeMode mode) {
		mDecodeMode = mode;
		updateViews();
	}
	
	public VideoViewBase getVideoView(){
		return mVideoViewBase;
	}

}
