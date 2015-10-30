package bf.cloud.android.playutils;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.components.mediaplayer.VideoViewExo;
import bf.cloud.android.components.mediaplayer.VideoViewSw;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.StateChangedListener;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

public class VideoFrame extends FrameLayout {
	private final String TAG = VideoFrame.class.getSimpleName();
	private DecodeMode mDecodeMode = DecodeMode.SOFT;
	private VideoViewBase mVideoViewBase = null;
	private FrameLayout mPlaceHolder = null;
	private Context mContext = null;
	private StateChangedListener mStateChangedListener = null;

	public VideoFrame(Context context) {
		super(context);
		mContext = context;
		setBackgroundColor(Color.BLACK);
		updateViews();
	}

	public VideoFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		setBackgroundColor(Color.BLACK);
		mContext = context;
		updateViews();
	}

	public VideoFrame(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setBackgroundColor(Color.BLACK);
		mContext = context;
		updateViews();
	}

	/**
	 * 每次updateViews，videoView会重新创建
	 */
	public void updateViews() {
		// make view
		if (mDecodeMode == DecodeMode.AUTO) {
			mVideoViewBase = new VideoViewExo(mContext);
		} else {
			mVideoViewBase = new VideoViewSw(mContext);
		}
		// init view
		mVideoViewBase.registMediaPlayerStateChangedListener(mStateChangedListener);
		// add view
		removeAllViews();
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.CENTER;
		addView(mVideoViewBase, params);
		
		mPlaceHolder = new FrameLayout(mContext);
		params = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mPlaceHolder.setBackgroundColor(Color.BLACK);
		addView(mPlaceHolder, params);
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
	
	public void showPlaceHolder(boolean flag){
		Log.d(TAG, "showPlaceHolder flag:" + flag);
		if (mPlaceHolder == null){
			Log.d(TAG, "there is none place holder now");
			return;
		}
			
		if (flag)
			mPlaceHolder.setVisibility(View.VISIBLE);
		else
			mPlaceHolder.setVisibility(View.INVISIBLE);
	}
	
	public void registMediaPlayerStateChangedListener(StateChangedListener scl){
		Log.d(TAG, "registMediaPlayerStateChangedListener scl:" + scl);
		mStateChangedListener = scl;
	}
	
	public void unregistMediaPlayerStateChangedListener(){
		mStateChangedListener = null;
	}

}
