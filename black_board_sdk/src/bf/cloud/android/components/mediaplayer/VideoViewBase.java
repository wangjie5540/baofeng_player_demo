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
	private final String TAG = VideoViewBase.class.getSimpleName();
	// All the stuff we need for playing and showing a video
    protected SurfaceHolder mSurfaceHolder = null;
    protected MediaPlayerProxy mMediaPlayerProxy = null;
    private int         mAudioSession;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSurfaceWidth;
    private int         mSurfaceHeight;
    private MediaControllerBase mMediaController;
//    private OnCompletionListener mOnCompletionListener;
//    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private int         mCurrentBufferPercentage;
//    private OnErrorListener mOnErrorListener;
//    private OnInfoListener  mOnInfoListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;
    
    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
	private int mCurrentState = STATE_ERROR;
	private int mTargetState = STATE_ERROR;
	protected String mPath = null;
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
        return (mMediaPlayerProxy != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }
	
	/**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
    	mMediaPlayerProxy.setDataSource(path);
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
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public void start(){
		if (isInPlaybackState()){
			mMediaPlayerProxy.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}
	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
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
