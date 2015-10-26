package bf.cloud.android.components.mediaplayer.proxy;

import com.google.android.exoplayer.ExoPlayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer;
import bf.cloud.android.modules.player.videoviewexo.HlsRendererBuilder;
import bf.cloud.android.modules.player.videoviewexo.ExoVideoPlayer.RendererBuilder;
import bf.cloud.black_board_sdk.R;
import bf.cloud.vr.Points;
import bf.cloud.vr.RawResourceReader;
import bf.cloud.vr.VideoTextureSurfaceRenderer;

public class MediaplayerExo extends MediaPlayerProxy implements ExoVideoPlayer.Listener{
	private ExoVideoPlayer mPlayer = null;
	private Surface mSurface = null;
	private SizeChangedListener mSizeChangedListener = null;
	private SurfaceTexture mSurfaceTexture = null;

	public MediaplayerExo(Context context) {
		Log.d(TAG, "new MediaplayerExo");
		mContext = context;
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
			if (mIsVr){
				Log.d(TAG, "wangtonggui prepare");
				Points.ps = RawResourceReader.readPoints(mContext, R.raw.points);
				Points.index = RawResourceReader.readIndeces(mContext, R.raw.index);
				VideoTextureSurfaceRenderer videoRenderer = new VideoTextureSurfaceRenderer(mContext,
						mSurfaceTexture, mSurfaceWidth,
						mSurfaceHeight, "BfCloudPlayer", mPath);
				mSurface = new Surface(videoRenderer.getSurfaceTexture());
				mPlayer = new ExoVideoPlayer(videoRenderer);
				mPlayer.setSurface(mSurface);
			}else{
				mSurface = new Surface(mSurfaceTexture);
				mPlayer = new ExoVideoPlayer(new HlsRendererBuilder(BFYConst.USUER_AGENT, mPath));
				mPlayer.setSurface(mSurface);
			}
			mPlayer.addListener(this);
			mPlayer.prepare();
			mPlayer.setPlayWhenReady(false);
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
			if (mStateChangedListener != null)
				mStateChangedListener.onStatePreparing();
			break;
		
		case ExoPlayer.STATE_BUFFERING:
//			doStateBuffering();
			if (mStateChangedListener != null)
				mStateChangedListener.onStateBuffering();
			break;

		case ExoPlayer.STATE_READY:
			if (mStateChangedListener != null)
				mStateChangedListener.onStateReady();
			break;
		
		case ExoPlayer.STATE_ENDED:
//			doStateEnded();
			if (mStateChangedListener != null)
				mStateChangedListener.onStateEnded();
			break;

		default:
			break;
		}
	}

	@Override
	public void onError(Exception e) {
		if (mMediaPlayerErrorListener != null)
			mMediaPlayerErrorListener.onError(e.getMessage());
	}

	@Override
	public void onVideoSizeChanged(int width, int height,
			float pixelWidthHeightRatio) {
		Log.d(TAG, "onVideoSizeChanged width:" + width 
											   + "/height:" + height 
											   + "/ratio:" + pixelWidthHeightRatio);
		
		if (mSizeChangedListener != null){
			mSizeChangedListener.onSizeChanged(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
		}
	}

	@Override
	public void setCurrentState(int state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDisplay(SurfaceTexture st) {
		Log.d(TAG, "setDisplay mPlayer:" + mPlayer);
		mSurfaceTexture  = st;
	}
	
	public interface SizeChangedListener{
		void onSizeChanged(float ratio);
	}
	
	public void registSizeChangedListener(SizeChangedListener scl){
		mSizeChangedListener = scl;
	}
	
	public void unregistSizeChangedListener(){
		mSizeChangedListener = null;
	}

	@Override
	public void clearDisplay() {
		if (mPlayer != null)
			mPlayer.blockingClearSurface();
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDuration() {
		return mPlayer.getDuration();
	}

	@Override
	public long getCurrentPosition() {
		return mPlayer.getCurrentPosition();
	}

	@Override
	public void setSurfaceSize(int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;
	}

}
