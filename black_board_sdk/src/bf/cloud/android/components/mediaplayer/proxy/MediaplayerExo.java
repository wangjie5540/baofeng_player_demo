package bf.cloud.android.components.mediaplayer.proxy;

import com.google.android.exoplayer.ExoPlayer;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer;
import bf.cloud.android.modules.player.videoviewexo.HlsRendererBuilder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer.RendererBuilder;

public class MediaplayerExo extends MediaPlayerProxy implements ExoVideoPlayer.Listener{
	private ExoVideoPlayer mPlayer = null;
	private Surface mSurface = null;

	public MediaplayerExo(String url) {
		Log.d(TAG, "new MediaplayerExo");
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
		stop();
		prepare();
		mPlayer.getPlayerControl().start();
	}

	@Override
	public void pause() {
		mPlayer.getPlayerControl().pause();
	}

	@Override
	public void resume() {
		mPlayer.getPlayerControl().start();
	}
	
	@Override
	public void seekTo(int pos) {
		mPlayer.getPlayerControl().seekTo(pos);
	}
	
	@Override
	public void stop() {
		if (mPlayerInitilized){
			mPlayer.blockingClearSurface();
			mPlayer.release();
			mPlayer = null;
			mPlayerInitilized = false;
		}
	}

	@Override
	public void setDataSource(String path) {
		mPath = path;
	}

	@Override
	public void prepare() {
		if (!mPlayerInitilized){
			mPlayer = new ExoVideoPlayer(getRendererBuilder());
			mPlayer.addListener(this);
			mPlayer.prepare();
			mPlayer.setPlayWhenReady(false);
			mPlayer.setSurface(mSurface);
			mPlayerInitilized  = true;
		} else {
			Log.d(TAG, "PlayerInitilized has been inited");
		}
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
		mSurface  = sh.getSurface();
	}

}
