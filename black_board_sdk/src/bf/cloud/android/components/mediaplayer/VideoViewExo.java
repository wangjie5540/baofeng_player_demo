package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.components.mediaplayer.proxy.MediaplayerExo;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class VideoViewExo extends VideoViewBase{
	private final String TAG = VideoViewExo.class.getSimpleName();
	
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
	protected void openVideo() {
    	Log.d(TAG, "openVideo");
    	if (mPath == null || mSurfaceHolder == null) {
            return;
        }
    	// we shouldn't clear the target state, because somebody might have
        // called start() previously
    	release(false);
    	//now we can create the MediaPlayerProxy
    	mMediaPlayerProxy = new MediaplayerExo();
    	mMediaPlayerProxy.setDataSource(mPath);
    	mMediaPlayerProxy.setDisplay(mSurfaceHolder);
    	mCurrentState = STATE_PREPARING;
	}

}
