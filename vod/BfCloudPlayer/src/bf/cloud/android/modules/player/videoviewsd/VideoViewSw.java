package bf.cloud.android.modules.player.videoviewsd;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.mediaplayer.StatusController;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.modules.log.BFYLog;

public class VideoViewSw extends VideoViewBase {
	
    private static final String TAG = VideoViewSw.class.getSimpleName();

    private static VideoViewSw sInstance = null;

    private static boolean mIsPaused = false;
    private static boolean mIsSurfaceReady = false;
    private static boolean mIsPlayerSizeInited = false;

    private static AudioTrack mAudioTrack;
    private static EventListener mEventListener;
    private static Thread mEventLoopThread;
    
    // messages
    protected final int MSG_STATE_CHANGED = 1;
    protected final int MSG_ERROR = 2;
    
    // play state
    protected final int STATE_PREPARING = 0;
    protected final int STATE_BUFFERING = 1;
    protected final int STATE_READY = 2;
    protected final int STATE_ENDED = 3;
    
    static {
        try {
            System.loadLibrary("ffmpeg");
            System.loadLibrary("SDL2");
            System.loadLibrary("player");
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

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

    
	public interface EventListener {
		void onStateChanged(int state);
		void onError(int errorCode);
	    void onVideoSizeChanged();
	}
    
    public VideoViewSw(Context context, AttributeSet attrs) {
        super(context, attrs);
        sInstance = this;
        initVideoView();
    }
    
    private void initVideoView() {
    	mIsPaused = false;
    	mIsSurfaceReady = false;
    	mIsPlayerSizeInited = false;
    	mAudioTrack = null;
    	mEventListener = null;
    	mEventLoopThread = null;
    }

    public static Context getViewContext() {
    	if (sInstance != null) {
    		return sInstance.getContext();
    	} else {
    		return null;
    	}
    }
    
    protected static Surface getNativeSurface() {
    	if (sInstance != null) {
    		return sInstance.getHolder().getSurface();
    	} else {
    		return null;
    	}
    }
    
    protected static boolean sendMessage(int message, int param) {
		Log.d(TAG, "recv message: " + message + ", param:" + param);
		if (sInstance != null) {
			sInstance.mMsgHandler.obtainMessage(message, param, 0).sendToTarget();
		}
        return true;
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

    private Handler mMsgHandler = new Handler() {
        public void handleMessage(Message msg){
        	int message = msg.what;
        	int param = msg.arg1;
        	
            switch (message) {
                case MSG_STATE_CHANGED: {
                	mEventListener.onStateChanged(param);
                    break;
                }
                case MSG_ERROR: {
                	mEventListener.onError(param);
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    protected void playerRelease() {
        BFYLog.d(TAG, "playerRelease start");
        nativePlayerStop();
        mIsPlayerSizeInited = false;
        BFYLog.d(TAG, "playerRelease end");
    }

    protected void playerSetDataSource(String url) {
        nativePlayerSetSource(url);
    }

    protected int playerGetTotalLength() {
        return nativePlayerGetDuration() / 1000;
    }

    protected int playerGetPosition() {
        return nativePlayerGetCurrentPosition() / 1000;
    }

    protected int playerStart() {
        BFYLog.d(TAG, "playerStart");
        nativePlayerStart();
        return 0;
    }

    protected int playerPause() {
        nativePlayerPause();
        return 0;
    }

    protected int playerStop() {
        BFYLog.d(TAG, "playerStop");
        return 0;
    }

    protected int playerSeekTo(int ms) {
        BFYLog.d(TAG, "playerSeekTo, ms=" + ms);
        nativePlayerSeekTo(ms);
        return 0;
    }

    protected void initPlayer() {
        BFYLog.d(TAG, "initPlayer");
        nativeInit();
        nativePlayerInit();
    }

    protected void playerSetVolume(float LeftVolume, float RightVolume) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(LeftVolume, RightVolume);
        }
    }

    protected void setVideoEventListener(EventListener listener) {
        mEventListener = listener;
    }
    
    protected boolean isPlayerSizeInited() {
    	return mIsPlayerSizeInited;
    }
    
    @Override
    public void onScreenOn() {
    }

    @Override
    public void onScreenOff() {
    }

    @Override
    public void onUserPresent() {
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        BFYLog.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        BFYLog.d(TAG, "surfaceDestroyed");

        handlePause();
        mIsSurfaceReady = false;
        onNativeSurfaceDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        BFYLog.d(TAG, "surfaceChanged,format=" + format + ",w=" + width + ",h=" + height);

        int sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565 by default
        switch (format) {
        case PixelFormat.RGBA_8888:
            Log.v("SDL", "pixel format RGBA_8888");
            sdlFormat = 0x16462004; // SDL_PIXELFORMAT_RGBA8888
            break;
        case PixelFormat.RGBX_8888:
            Log.v("SDL", "pixel format RGBX_8888");
            sdlFormat = 0x16261804; // SDL_PIXELFORMAT_RGBX8888
            break;
        case PixelFormat.RGB_565:
            Log.v("SDL", "pixel format RGB_565");
            sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565
            break;
        case PixelFormat.RGB_888:
            Log.v("SDL", "pixel format RGB_888");
            // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
            sdlFormat = 0x16161804; // SDL_PIXELFORMAT_RGB888
            break;
        default:
            Log.v("SDL", "pixel format unknown " + format);
            break;
        }
        
        onNativeResize(width, height, sdlFormat);
        
        boolean isSurfaceRecreated = !mIsSurfaceReady;
        if (isSurfaceRecreated) {
        	onNativeSurfaceChanged();
        }

        mIsSurfaceReady = true;
        mIsPlayerSizeInited = true;
        
        mEventListener.onVideoSizeChanged();
    }

    @Override
    public void orientationChanged() {
    }

    @Override
    public void onCreateWindow() {
    }

    @Override
    public void onDestroyWindow() {
    }

    @Override
    public void onPauseWindow() {
    	handlePause();
    }

    @Override
    public void onResumeWindow() {
    	handleResume();
    }
    
    @Override
    public void onRestartWindow() {
    }

    @Override
    public void onLowMemory() {
    	nativeLowMemory();
    }

    @Override
    public void start() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void stop() {
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public boolean isInPlaybackState() {
        return false;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    @Override
    public void fastForward() {
    }

    @Override
    public void fastBackward() {
    }

    @Override
    public void setStatusController(StatusController mStatusController) {
    }

    @Override
    public void hideStatusController() {
    }

    @Override
    public void showPlaceHolder() {
    }

    @Override
    public void sendEmptyMessage(int what) {
    }

    @Override
    public void executePlay(long pos) {
    }

    @Override
    public void playPrepare(int historyPosition) {
    }

    @Override
    public void keycodeHome() {
    }
    
    private void handlePause() {
    	if (!mIsPaused && mIsSurfaceReady) {
    		mIsPaused = true;
    		nativePause();
    	}
    }
    
    private void handleResume() {
    	if (mIsPaused && mIsSurfaceReady) {
    		mIsPaused = false;
    		nativeResume();
    	}
    }

}