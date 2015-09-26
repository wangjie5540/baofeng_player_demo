package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.components.mediaplayer.proxy.MediaplayerExo;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class VideoViewExo extends VideoViewBase{
	
	public VideoViewExo(Context context) {
		super(context);
	}
	
	public VideoViewExo(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VideoViewExo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	
	@Override
	protected int openVideo() {
    	Log.d(TAG, "VideoViewExo openVideo mPath:" + mPath);
    	if (mPath == null || mPath.length() == 0 || mSurfaceHolder == null) {
            return -1;
        }
    	// we shouldn't clear the target state, because somebody might have
        // called start() previously
    	release(false);
    	//now we can create the MediaPlayerProxy
    	mMediaPlayerProxy = new MediaplayerExo(mPath);
    	mMediaPlayerProxy.setDisplay(mSurfaceHolder);
    	mMediaPlayerProxy.prepare();
    	mCurrentState = STATE_PREPARED;
    	return 0;
	}

}
