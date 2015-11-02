package bf.cloud.android.components.mediaplayer.proxy;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.modules.player.exoplayer.ExoVideoPlayer;
import bf.cloud.black_board_sdk.R;
import bf.cloud.vr.Points;
import bf.cloud.vr.RawResourceReader;
import bf.cloud.vr.VideoTextureSurfaceRenderer;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

public final class MediaPlayerSw extends MediaPlayerProxy {
	static {
		try {
			System.loadLibrary("ffmpeg");
			System.loadLibrary("SDL2");
			System.loadLibrary("player");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}
	private static MediaPlayerSw sInstance = null;
	
	
	private static boolean mIsPaused = false;
    private static boolean mIsSurfaceReady = false;
    private static boolean mIsPlayerSizeInited = false;
    private static AudioTrack mAudioTrack;
    private static Thread mEventLoopThread;
	// messages
	private final int MSG_STATE_CHANGED = 1;
	private final int MSG_ERROR = 2;

	// play state
	private final int STATE_PREPARING = 0;
	private final int STATE_BUFFERING = 1;
	private final int STATE_READY = 2;
	private final int STATE_ENDED = 3;

	public MediaPlayerSw(Context context) {
		super(context);
		Log.d(TAG, "new MediaplayerSw");
		sInstance = this;
	}

	private Handler mHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			int messageType = msg.what;
			int message = msg.arg1;

			switch (messageType) {
			case MSG_STATE_CHANGED:
				onStateChanged(message);
				break;
			case MSG_ERROR:
				if (mMediaPlayerErrorListener != null)
					mMediaPlayerErrorListener.onError(message);
				break;
			}
			return false;
		}
	});
	public static Context getViewContext() {
    	if (sInstance != null) {
    		return sInstance.getContext();
    	} else {
    		return null;
    	}
    }

	private Context getContext() {
		if (mContext != null)
			return mContext;
		else
			return null;
	}
	
	protected static Surface getNativeSurface() {
    	if (sInstance != null) {
    		if (!sInstance.mIsVr){
    			sInstance.mSurface = new Surface(sInstance.mSurfaceTexture);
    		}
    		return sInstance.mSurface;
    	} else {
    		return null;
    	}
    }
	
	protected static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        if (mAudioTrack == null) {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
            
            if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e("SDL", "Failed during initialization of Audio Track");
                mAudioTrack = null;
                return -1;
            }
            
            mAudioTrack.play();
        }
        return 0;
    }
	
	protected static void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                return;
            }
        }
    }
	
	protected static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                return;
            }
        }
    }
	
	protected static void audioQuit() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

	@Override
	public void start() {
		stop();
		prepare();
		nativePlayerStart();
	}

	protected void onStateChanged(int message) {
		switch (message) {
		case STATE_PREPARING:
			if (mStateChangedListener != null)
				mStateChangedListener.onStatePreparing();
			doStatePreparing();
			break;
		case STATE_BUFFERING:
			if (mStateChangedListener != null)
				mStateChangedListener.onStateBuffering();
			doStateBuffering();
			break;
		case STATE_READY:
			if (mStateChangedListener != null)
				mStateChangedListener.onStateReady();
			doStateReady();
			break;
		case STATE_ENDED:
			if (mStateChangedListener != null)
				mStateChangedListener.onStateEnded();
			doStateEnded();
			break;

		default:
			break;
		}
	}

	@Override
	public void pause() {
		nativePlayerPause();
	}

	@Override
	public void resume() {
		nativePlayerStart();
	}

	@Override
	public void stop() {
		if (mPlayerInitilized) {
			nativePlayerStop();
			mPlayerInitilized = false;
		}
	}

	@Override
	public void release() {
		stop();
	}

	@Override
	public void setDataSource(String path) {
		mPath = path;
	}

	@Override
	public void setSurfaceSize(int width, int height) {
		mSurfaceHeight = height;
		mSurfaceWidth = width;
		int sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565 by default
		onNativeResize(mSurfaceWidth, mSurfaceHeight, sdlFormat);
        if (!mIsSurfaceReady) {
        	onNativeSurfaceChanged();
        }
		mIsSurfaceReady = true;
        mIsPlayerSizeInited = true;
	}

	@Override
	public void prepare() {
		if (!mPlayerInitilized) {
			if (mIsVr) {
				Points.ps = RawResourceReader.readPoints(mContext, R.raw.points);
				Points.index = RawResourceReader.readIndeces(mContext, R.raw.index);
				mVideoRenderer  = new VideoTextureSurfaceRenderer(mContext,
						sInstance.mSurfaceTexture, mSurfaceWidth,
						mSurfaceHeight, BFYConst.USUER_AGENT, null);
				mSurface = new Surface(mVideoRenderer.getSurfaceTexture());
			} 
			nativePlayerSetSource(mPath);
			nativeInit();
			nativePlayerInit();
			
			mPlayerInitilized = true;
		} else {
			Log.d(TAG, "PlayerInitilized has been inited");
		}
	}

	@Override
	public void setCurrentState(int state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearDisplay() {
		// TODO Auto-generated method stub

	}

	@Override
	public void seekTo(int pos) {
		nativePlayerSeekTo(pos);
	}

	@Override
	public long getDuration() {
		return nativePlayerGetDuration();
	}

	@Override
	public long getCurrentPosition() {
		return nativePlayerGetCurrentPosition();
	}

	@Override
	public void setRotationXY(float srcX, float srcY, float newX, float newY) {
		// TODO Auto-generated method stub

	}

	// 这里的消息来自jni
	protected static boolean sendMessage(int message, int param) {
		Log.d(TAG, "recv message: " + message + ", param:" + param);
		if (sInstance != null) {
			sInstance.mHandler.obtainMessage(message, param, 0).sendToTarget();
		}
		return true;
	}
	
	@Override
	public void onSurfaceDestoryed() {
		mIsSurfaceReady = false;
	}
	
	// native function below
	private static native boolean nativeInit();

	private static native boolean nativePlayerInit();

	private static native void nativeLowMemory();

	private static native void nativePause();

	private static native void nativeResume();

	private static native void onNativeResize(int x, int y, int format);

	private static native void onNativeSurfaceChanged();

	private static native void onNativeSurfaceDestroyed();

	private static native void nativePlayerSetSource(String url);

	private static native void nativePlayerStart();

	private static native void nativePlayerPause();

	private static native void nativePlayerStop();

	private static native int nativePlayerGetDuration();

	private static native int nativePlayerGetCurrentPosition();

	private static native void nativePlayerSeekTo(int ms);
}
