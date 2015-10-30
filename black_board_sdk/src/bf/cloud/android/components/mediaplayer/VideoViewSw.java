package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerSw;
import bf.cloud.android.playutils.VideoFrame;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class VideoViewSw extends VideoViewBase{
	public VideoViewSw(Context context) {
		super(context);
	}
	
	public VideoViewSw(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VideoViewSw(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected int openVideo() {
		Log.d(TAG, "VideoViewSw openVideo mPath:" + mPath);
    	if (mPath == null || mPath.length() == 0 || mSurfaceTexture == null) {
            return -1;
        }
    	release(false);
    	//now we can create the MediaPlayerProxy
    	mMediaPlayerProxy = new MediaPlayerSw(mContext);
    	mMediaPlayerProxy.setVrFlag(mIsVr);
    	mMediaPlayerProxy.setDataSource(mPath);
    	mMediaPlayerProxy.setSurfaceSize(mSurfaceWidth, mSurfaceHeight);
//    	((MediaplayerExo)mMediaPlayerProxy).registSizeChangedListener(new SizeChangedListener() {
//			
//			@Override
//			public void onSizeChanged(float ratio) {
//				Log.d(TAG, "onSizeChanged ratio:" + ratio);
//				mVideoAspectRatio = ratio;
//				mHandler.sendEmptyMessage(0);
//			}
//		});
    	if (mMediaPlayerStateChangedListener != null)
    		mMediaPlayerProxy.registStateChangedListener(mMediaPlayerStateChangedListener);
    	mMediaPlayerProxy.setDisplay(mSurfaceTexture);
    	mVideoFrame = (VideoFrame) getParent();
    	mVideoFrame.showPlaceHolder(false);
    	mCurrentState = STATE_PREPARED;
		return 0;
	}

}
