package bf.cloud.android.components.mediaplayer;


import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy;
import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class VideoViewBase extends SurfaceView implements
		SurfaceHolder.Callback, MediaControllerBase.MediaPlayerControl{
	protected final String TAG = VideoViewBase.class.getSimpleName();
	// All the stuff we need for playing and showing a video
    protected SurfaceHolder mSurfaceHolder = null;
    protected MediaPlayerProxy mMediaPlayerProxy = null;
    protected int         mAudioSession;
    protected int         mVideoWidth;
    protected int         mVideoHeight;
    protected int         mSurfaceWidth;
    protected int         mSurfaceHeight;
    private MediaControllerBase mMediaController;
//    private OnCompletionListener mOnCompletionListener;
//    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    protected int         mCurrentBufferPercentage;
//    private OnErrorListener mOnErrorListener;
//    private OnInfoListener  mOnInfoListener;
    protected int         mSeekWhenPrepared;  // recording the seek position while preparing
    protected boolean     mCanPause;
    protected boolean     mCanSeekBack;
    protected boolean     mCanSeekForward;
    
    // all possible internal states
    protected static final int STATE_ERROR              = -1;
    protected static final int STATE_IDLE               = 0;
    protected static final int STATE_PREPARING          = 1;
    protected static final int STATE_PREPARED           = 2;
    protected static final int STATE_PLAYING            = 3;
    protected static final int STATE_PAUSED             = 4;
    protected static final int STATE_PLAYBACK_COMPLETED = 5;
    protected int mCurrentState = STATE_ERROR;
    protected int mTargetState = STATE_ERROR;
	protected String mPath = "";
	private Context mContext = null;;
    
	public VideoViewBase(Context context) {
		super(context);
		mContext = context;
		initVideoView();
	}
	public VideoViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initVideoView();
	}
	public VideoViewBase(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		initVideoView();
	}
	
	
	private void initVideoView() {
		getHolder().addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState   = STATE_IDLE;
	}
	
	private boolean isInPlaybackState() {
		Log.d(TAG, "mMediaPlayerProxy:" + mMediaPlayerProxy 
						+ ",mCurrentState:" + mCurrentState);
        return (mMediaPlayerProxy != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }
	
	/**
     * Sets video data source.
     *
     * @param path the path of the video.
     */
    public void setDataSource(String url) {
    	Log.d(TAG, "setDataSource url:" + url);
    	mPath = url;
    	openVideo();
    	requestLayout();
        invalidate();
    }
    
    protected abstract void openVideo();
    
    /*
     * release the media player in any state
     */
    protected void release(boolean cleartargetstate) {
        if (mMediaPlayerProxy != null) {
//        	mMediaPlayerProxy.reset();
//        	mMediaPlayerProxy.release();
        	mMediaPlayerProxy = null;
//            mPendingSubtitleTracks.clear();
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mContext .getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }
	
	
	
	
	
	
	
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");
		mSurfaceWidth = width;
        mSurfaceHeight = height;
        boolean isValidState =  (mTargetState == STATE_PLAYING);
        boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
        if (mMediaPlayerProxy != null && isValidState && hasValidSize) {
            if (mSeekWhenPrepared != 0) {
                seekTo(mSeekWhenPrepared);
            }
            start();
        }
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		mSurfaceHolder = holder;
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		// after we return from this we can't use the surface any more
        mSurfaceHolder = null;
//        if (mMediaController != null) mMediaController.hide();
        release(true);
	}
	
	
	@Override
	public void start(){
		Log.d(TAG, "VideoView start");
		mMediaPlayerProxy.start(mPath);
		mCurrentState = STATE_PLAYING;
	}
	
	@Override
	public void stop() {
		if (mMediaPlayerProxy != null)
			mMediaPlayerProxy.stop();
		mCurrentState = STATE_IDLE;
	}
	
	@Override
	public void pause() {
		if (mCurrentState == STATE_PLAYING){
			mMediaPlayerProxy.pause();
			mCurrentState = STATE_PAUSED;
		}
	}
	
	@Override
	public void resume() {
		if (mCurrentState == STATE_PAUSED){
			mMediaPlayerProxy.resume();
			mCurrentState = STATE_PLAYING;
		}
	}
	
	@Override
	public int getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getCurrentPosition() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void seekTo(int pos) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean canPause() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean canSeekBackward() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean canSeekForward() {
		// TODO Auto-generated method stub
		return false;
	}
}
