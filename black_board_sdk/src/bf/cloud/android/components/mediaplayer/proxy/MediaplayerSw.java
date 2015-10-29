package bf.cloud.android.components.mediaplayer.proxy;

import android.graphics.SurfaceTexture;

public class MediaplayerSw extends MediaPlayerProxy{
	
	static {
		System.loadLibrary("ffmpeg");
		System.loadLibrary("SDL2");
		System.loadLibrary("player");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDataSource(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSurfaceSize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void prepare() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCurrentState(int state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDisplay(SurfaceTexture st) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearDisplay() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void seekTo(int pos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCurrentPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRotationXY(float srcX, float srcY, float newX, float newY) {
		// TODO Auto-generated method stub
		
	}

	//native function below
	
}
