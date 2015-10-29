package bf.cloud.android.components.mediaplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class VideoViewSw extends VideoViewBase{
	public VideoViewSw(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public VideoViewSw(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public VideoViewSw(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int openVideo() {
		Log.d(TAG, "VideoViewSw openVideo mPath:" + mPath);
    	if (mPath == null || mPath.length() == 0 || mSurfaceTexture == null) {
            return -1;
        }
    	release(false);
		return 0;
	}

}
