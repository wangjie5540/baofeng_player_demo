package bf.cloud.android.components.mediaplayer.proxy;

import android.util.Log;
import android.view.SurfaceHolder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer;
import bf.cloud.android.modules.player.videoviewexo.HlsRendererBuilder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer.RendererBuilder;

public class MediaplayerExo extends MediaPlayerProxy implements ExoVideoPlayer.Listener{
	private final String TAG = MediaplayerExo.class.getSimpleName();
	private ExoVideoPlayer mPlayer = null;

	public MediaplayerExo() {
		Log.d(TAG, "new MediaplayerExo");
	}
	
	private RendererBuilder getRendererBuilder() {
		String userAgent = "BfCloudPlayer";
		if (mPath == null){
			Log.d(TAG, "mPath is null");
			return null;
		}
		return new HlsRendererBuilder(userAgent, mPath);
	}

	@Override
	public void start() {
		Log.d(TAG, "MediaplayerExo start");
		if (mPath == null || mPath.length() == 0){
			Log.d(TAG, "dataSource is invailid");
		}
		if (mPlayer == null){
			mPlayer = new ExoVideoPlayer(getRendererBuilder());
			mPlayer.addListener(this);
			mPlayer.prepare();
//			mPlayer.setSurface()
			mPlayer.setPlayWhenReady(false);
			mPlayerInitilized  = true;
		}
		mPlayer.getPlayerControl().start();
	}

	@Override
	public void pause() {
		mPlayer.getPlayerControl().pause();
	}

	@Override
	public void stop() {
	}

	@Override
	public void setDataSource(String path) {
		if (path == null || path.length() == 0){
			Log.d(TAG, "dataSource is invailid");
		}
		mPath = path;
	}

	@Override
	public void prepare() {
		mPlayer = new ExoVideoPlayer(getRendererBuilder());
	}

	@Override
	public void onStateChanged(boolean playWhenReady, int playbackState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVideoSizeChanged(int width, int height,
			float pixelWidthHeightRatio) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCurrentState(int state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDisplay(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		
	}
}
