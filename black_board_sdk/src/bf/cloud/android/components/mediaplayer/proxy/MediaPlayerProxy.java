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
	//Todo:播放器状态
	
	public abstract void start();
	public abstract void pause();
	public abstract void stop();
	public abstract void setDataSource(String path);
	public abstract void prepare();
	public abstract void setCurrentState(int state);
	public abstract void setDisplay(SurfaceHolder sh);
}
