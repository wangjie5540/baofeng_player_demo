package bf.cloud.android.components.mediaplayer.proxy;

import android.view.SurfaceHolder;

/**
 * 
 * @author wangtonggui
 *
 */
public abstract class MediaPlayerProxy {
	protected final String TAG = MediaPlayerProxy.class.getSimpleName();
	protected String mPath = null;
	protected boolean mPlayerInitilized = false;
	protected StateChangedListener mStateChangedListener = null;
	protected MediaPlayerErrorListener mMediaPlayerErrorListener = null;
	//Todo:播放器状态
	
	public abstract void start();
	public abstract void pause();
	public abstract void resume();
	public abstract void stop();
	public abstract void release();
	public abstract void setDataSource(String path);
	public abstract void prepare();
	public abstract void setCurrentState(int state);
	public abstract void setDisplay(SurfaceHolder sh);
	public abstract void clearDisplay();
	public abstract void seekTo(int pos);
	
	public interface StateChangedListener{
		void onStatePreparing();
		void onStateBuffering();
		void onStateReady();
		void onStateEnded();
	}
	
	public void registStateChangedListener(StateChangedListener scl){
		mStateChangedListener = scl;
	}
	
	public void unregistStateChangedListener(){
		mStateChangedListener = null;
	}
	
	public interface MediaPlayerErrorListener{
		void onError(String errorMsg);
		void onError(int errorCode);
	}
}
