package bf.cloud.android.components.mediaplayer.proxy;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * 
 * @author wangtonggui
 * 
 */
public abstract class MediaPlayerProxy {
	protected final static String TAG = MediaPlayerProxy.class.getSimpleName();
	protected String mPath = null;
	protected boolean mPlayerInitilized = false;
	protected StateChangedListener mStateChangedListener = null;
	protected MediaPlayerErrorListener mMediaPlayerErrorListener = null;
	protected boolean mIsVr = false;
	protected int mSurfaceWidth = 0;
	protected int mSurfaceHeight = 0;
	protected Context mContext = null;
	protected Surface mSurface = null;
	protected SurfaceTexture mSurfaceTexture = null;

	// Todo:播放器状态

	public abstract void start();

	public abstract void pause();

	public abstract void resume();

	public abstract void stop();

	public abstract void release();

	public abstract void setDataSource(String path);

	public abstract void setSurfaceSize(int width, int height);
	
	public abstract void onSurfaceDestoryed();

	public abstract void prepare();

	public abstract void setCurrentState(int state);

	public abstract void clearDisplay();

	public abstract void seekTo(int pos);

	public abstract long getDuration();

	public abstract long getCurrentPosition();

	public abstract void setRotationXY(float srcX, float srcY, float newX,
			float newY);

	protected MediaPlayerProxy(Context context) {
		mContext = context;
	}

	public interface StateChangedListener {
		void onStatePreparing();

		void onStateBuffering();

		void onStateReady();

		void onStateEnded();
	}

	public void registStateChangedListener(StateChangedListener scl) {
		mStateChangedListener = scl;
	}

	public void unregistStateChangedListener() {
		mStateChangedListener = null;
	}

	public interface MediaPlayerErrorListener {
		void onError(String errorMsg);

		void onError(int errorCode);
	}

	public void setVrFlag(boolean flag) {
		mIsVr = flag;
	}

	protected void doStatePreparing() {

	}

	protected void doStateBuffering() {

	}

	protected void doStateReady() {

	}

	protected void doStateEnded() {

	}
	
	public void setDisplay(SurfaceTexture st) {
		Log.d(TAG, "setDisplay");
		mSurfaceTexture  = st;
	}
}
