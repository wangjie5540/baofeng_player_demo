package bf.cloud.android.components.mediaplayer.proxy;

/**
 * 
 * @author wangtonggui
 *
 */
public abstract class MediaPlayerProxy {
	//Todo:播放器状态
	
	public abstract void start();
	public abstract void pause();
	public abstract void stop();
	public abstract void setDataSource(String path);
	public abstract void prepare();
	public abstract void setCurrentState(int state);
}
