package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.StateChangedListener;
import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.playutils.VideoFrame;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class VideoViewBase extends TextureView implements
		TextureView.SurfaceTextureListener,
		MediaControllerBase.MediaPlayerControl, OnTouchListener {
	protected final String TAG = VideoViewBase.class.getSimpleName();
	// All the stuff we need for playing and showing a video
	protected SurfaceTexture mSurfaceTexture = null;
	protected MediaPlayerProxy mMediaPlayerProxy = null;
	protected StateChangedListener mMediaPlayerStateChangedListener = null;
	protected VideoFrame mVideoFrame = null;
	protected int mAudioSession;
	protected int mVideoWidth;
	protected int mVideoHeight;
	protected int mSurfaceWidth;
	protected int mSurfaceHeight;
	private MediaControllerBase mMediaController;
	// private OnCompletionListener mOnCompletionListener;
	// private MediaPlayer.OnPreparedListener mOnPreparedListener;
	protected int mCurrentBufferPercentage;
	// private OnErrorListener mOnErrorListener;
	// private OnInfoListener mOnInfoListener;
	protected int mSeekWhenPrepared; // recording the seek position while
										// preparing
	protected boolean mCanPause;
	protected boolean mCanSeekBack;
	protected boolean mCanSeekForward;
	protected long mDuration = 0;

	// all possible internal states
	protected static final int STATE_ERROR = -1;
	protected static final int STATE_IDLE = 0;
	protected static final int STATE_PREPARING = 1;
	protected static final int STATE_PREPARED = 2;
	protected static final int STATE_PLAYING = 3;
	protected static final int STATE_PAUSED = 4;
	protected static final int STATE_PLAYBACK_COMPLETED = 5;
	protected int mCurrentState = STATE_ERROR;
	protected int mTargetState = STATE_ERROR;
	protected String mPath = "";
	protected Context mContext = null;
	private boolean startWhenSurfaceReady = false;
	protected boolean mIsVr = false;
	private StatInfo mStatInfo = null;

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
		mVideoFrame = (VideoFrame) getParent();
		// getHolder().addCallback(this);
		setSurfaceTextureListener(this);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState = STATE_IDLE;
	}

	private boolean isInPlaybackState() {
		Log.d(TAG, "mMediaPlayerProxy:" + mMediaPlayerProxy + ",mCurrentState:"
				+ mCurrentState);
		return (mMediaPlayerProxy != null && mCurrentState != STATE_ERROR
				&& mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}

	/**
	 * Sets video data source.
	 * 
	 * @param path
	 *            the path of the video.
	 */
	public void setDataSource(String url) {
		Log.d(TAG, "setDataSource url:" + url);
		mPath = url;
		int ret = openVideo();
		if (ret < 0)
			startWhenSurfaceReady = true;
		requestLayout();
		invalidate();
	}

	protected abstract int openVideo();

	/*
	 * release the media player in any state
	 */
	protected void release(boolean cleartargetstate) {
		if (mMediaPlayerProxy != null) {
			// mMediaPlayerProxy.reset();
			// mMediaPlayerProxy.release();
			mMediaPlayerProxy.unregistStateChangedListener();
			mMediaPlayerProxy = null;
			mCurrentState = STATE_IDLE;
			if (cleartargetstate) {
				mTargetState = STATE_IDLE;
			}
			AudioManager am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);
			am.abandonAudioFocus(null);
		}
	}

	// @Override
	// public void surfaceChanged(SurfaceHolder holder, int format, int width,
	// int height) {
	// Log.d(TAG, "surfaceChanged mMediaPlayerProxy:" + mMediaPlayerProxy);
	// mSurfaceHolder = holder;
	// if (mMediaPlayerProxy != null){
	// mMediaPlayerProxy.setDisplay(mSurfaceHolder);
	// }
	// // mSurfaceWidth = width;
	// // mSurfaceHeight = height;
	// // boolean isValidState = (mTargetState == STATE_PLAYING);
	// // boolean hasValidSize = (mVideoWidth == width && mVideoHeight ==
	// height);
	// // if (mMediaPlayerProxy != null && isValidState && hasValidSize) {
	// // if (mSeekWhenPrepared != 0) {
	// // seekTo(mSeekWhenPrepared);
	// // }
	// // start();
	// // }
	// }
	// @Override
	// public void surfaceCreated(SurfaceHolder holder) {
	// Log.d(TAG, "surfaceCreated");
	// mSurfaceHolder = holder;
	// if (startWhenSurfaceReady){
	// start();
	// startWhenSurfaceReady = false;
	// }
	// }
	// @Override
	// public void surfaceDestroyed(SurfaceHolder holder) {
	// Log.d(TAG, "surfaceDestroyed");
	// // after we return from this we can't use the surface any more
	// if (mMediaPlayerProxy != null)
	// mMediaPlayerProxy.clearDisplay();
	// mSurfaceHolder = null;
	// }

	@Override
	public void start() {
		Log.d(TAG, "VideoView start");
		if (mMediaPlayerProxy != null) {
			mMediaPlayerProxy.stop();
			mMediaPlayerProxy.setDataSource(mPath);
			mMediaPlayerProxy.start();
			mCurrentState = STATE_PLAYING;
		}
	}

	@Override
	public void stop() {
		if (mMediaPlayerProxy != null)
			mMediaPlayerProxy.stop();
		release(false);
		mCurrentState = STATE_IDLE;
	}

	@Override
	public void pause() {
		if (mCurrentState == STATE_PLAYING) {
			mMediaPlayerProxy.pause();
			mCurrentState = STATE_PAUSED;
		}
	}

	@Override
	public void resume() {
		if (mCurrentState == STATE_PAUSED) {
			mMediaPlayerProxy.resume();
			mCurrentState = STATE_PLAYING;
		}
	}

	@Override
	public long getDuration() {
		if (mMediaPlayerProxy != null)
			return mMediaPlayerProxy.getDuration();
		else
			return 0;
	}

	@Override
	public long getCurrentPosition() {
		if (mMediaPlayerProxy != null)
			return mMediaPlayerProxy.getCurrentPosition();
		else
			return 0;
	}

	@Override
	public void seekTo(int pos) {
		if ((mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYING)
				&& mMediaPlayerProxy != null) {
			mMediaPlayerProxy.seekTo(pos);
		}
	}

	@Override
	public boolean isPlaying() {
		if (mCurrentState == STATE_PLAYING || mCurrentState == STATE_PAUSED)
			return true;
		else
			return false;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canPause() {
		if (mCurrentState == STATE_PLAYING)
			return true;
		else
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

	public void registMediaPlayerStateChangedListener(StateChangedListener scl) {
		mMediaPlayerStateChangedListener = scl;
		if (mMediaPlayerProxy != null)
			mMediaPlayerProxy.registStateChangedListener(scl);
	}

	public void unregistMediaPlayerStateChangedListener() {
		mMediaPlayerStateChangedListener = null;
	}

	public StatInfo getStatInfo() {
		return mStatInfo;
	}

	// Listener callback below
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
		Log.d(TAG, "onSurfaceTextureAvailable width:" + width + "/height:"
				+ height);
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		mSurfaceTexture = surface;
		if (mMediaPlayerProxy != null){
			mMediaPlayerProxy.setSurfaceSize(mSurfaceWidth, mSurfaceHeight);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.d(TAG, "onSurfaceTextureDestroyed");
		mSurfaceTexture = null;
		if (mMediaPlayerProxy != null)
			mMediaPlayerProxy.onSurfaceDestoryed();
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		Log.d(TAG, "onSurfaceTextureSizeChanged width:" + width + "/height:"
				+ height);
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		mSurfaceTexture = surface;
		if (mMediaPlayerProxy != null){
			mMediaPlayerProxy.setSurfaceSize(mSurfaceWidth, mSurfaceHeight);
		}
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//		Log.d(TAG, "onSurfaceTextureUpdated");
	}

	@Override
	public void setVrFlag(boolean flag) {
		mIsVr = flag;
		if (mIsVr)
			setOnTouchListener(this);
		else
			setOnTouchListener(null);
	}

	private boolean down = false;
	private float mx;
	private float my;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.d(TAG, "onTouch");
		if (mIsVr) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				down = true;
				mx = event.getX();
				my = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (down) {
					float xx = event.getX();
					float yy = event.getY();
					if (mMediaPlayerProxy != null)
						mMediaPlayerProxy.setRotationXY(mx, my, xx, yy);
					mx = xx;
					my = yy;
				}
				break;
			case MotionEvent.ACTION_UP:
				down = false;
				break;
			}
		}
		return true;
	}
}
