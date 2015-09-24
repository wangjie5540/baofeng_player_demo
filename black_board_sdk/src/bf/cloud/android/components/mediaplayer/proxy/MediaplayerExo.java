package bf.cloud.android.components.mediaplayer.proxy;

import com.google.android.exoplayer.ExoPlayer;

import android.util.Log;
import android.view.SurfaceHolder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer;
import bf.cloud.android.modules.player.videoviewexo.HlsRendererBuilder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer.RendererBuilder;

public class MediaplayerExo extends MediaPlayerProxy implements ExoVideoPlayer.Listener{
	private ExoVideoPlayer mPlayer = null;

	public MediaplayerExo(String url) {
		Log.d(TAG, "new MediaplayerExo");
		mPath = url;
		if (mPlayer == null){
			mPlayer = new ExoVideoPlayer(getRendererBuilder());
			mPlayer.addListener(this);
			mPlayer.prepare();
			mPlayer.setPlayWhenReady(false);
			mPlayerInitilized  = true;
		}
	}
	
	private RendererBuilder getRendererBuilder() {
		String userAgent = "BfCloudPlayer";
		if (mPath == null){
			Log.d(TAG, "mPath is null");
			return null;
		}
		Log.d(TAG, "userAgent:" + userAgent + ",mPath:" + mPath);
		return new HlsRendererBuilder(userAgent, mPath);
	}

	@Override
	public void start() {
		Log.d(TAG, "MediaplayerExo start");
		if (mPath == null || mPath.length() == 0){
			Log.d(TAG, "dataSource is invailid");
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
		
	}

	@Override
	public void prepare() {
		mPlayer = new ExoVideoPlayer(getRendererBuilder());
	}

	@Override
	public void onStateChanged(boolean playWhenReady, int playbackState) {
		Log.d(TAG, "onStateChanged state:" + playbackState);
		switch (playbackState) {
		case ExoPlayer.STATE_PREPARING:
//			doStatePreparing();
			break;
		
		case ExoPlayer.STATE_BUFFERING:
//			doStateBuffering();
			break;

		case ExoPlayer.STATE_READY:
//			doStateReady();
			break;
		
		case ExoPlayer.STATE_ENDED:
//			doStateEnded();
			break;

		default:
			break;
		}
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
		mPlayer.setSurface(sh.getSurface());
	}
}
