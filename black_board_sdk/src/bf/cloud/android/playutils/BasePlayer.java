package bf.cloud.android.playutils;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.components.mediaplayer.proxy.BFVolumeManager;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.MediaPlayerErrorListener;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.StateChangedListener;
import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.BFStream.BFP2PListener;
import bf.cloud.android.modules.p2p.BFStream.BFStreamMessageListener;
import bf.cloud.android.modules.p2p.MediaCenter.NetState;
import bf.cloud.android.utils.BFYNetworkUtil;

/**
 * Created by wangtonggui
 */
public abstract class BasePlayer implements BFStreamMessageListener,
		BFP2PListener, MediaPlayerErrorListener, StateChangedListener{
	private final String TAG = BasePlayer.class.getSimpleName();
	
	private Context mContext = null;
	private VideoViewBase mVideoView = null;
	protected String mToken = "";
	// 这里的变化必须要对应一个变化mode的消息
	protected DecodeMode mDecodeMode = BFYConst.DEFAULT_DECODE_MODE;
	protected VideoFrame mVideoFrame = null;
	private BFStream mBfStream = null;
	private String mDataSource = null;
	private PlayerHandlerThread mPlayerHandlerThread = null;
	private STATE mState = STATE.IDLE;
	private static boolean isMediaCenterInited = false;
	private BFVolumeManager mBFVolumeManager = null;
	private String mVideoName = null;
	private PlayErrorListener mPlayErrorListener = null;
	private PlayEventListener mPlayEventListener = null;
	private boolean mLowLatencyFlag = false;
	private boolean mForceStartFlag = false;
	private VideoDefinition mVideoDefinition = VideoDefinition.UNKNOWN;

	private static final int MSG_STREAM_CREATE = 10000;
	private static final int MSG_STREAM_START = 10001;
	private static final int MSG_STREAM_STOP = 10002;
	private static final int MSG_STREAM_DESTORY = 10003;

	private static final int MSG_P2P_INIT = 10004;
	private static final int MSG_P2P_UNINIT = 10005;

	private static final int MSG_UI_ = 20000;

	private enum STATE {
		IDLE(0), PREPARING(1), PREPARED(2), PLAYING(3), PAUSED(4), COMPLETED(5), ERROR(
				-1);
		int state = 0;

		STATE(int state) {
			this.state = state;
		}
	}

	private Handler mUIHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			Log.d(TAG, "mUIHandler msg");
			mState = STATE.PREPARED;
			mVideoView.setDataSource(mBfStream.getStreamUrl());
			mVideoView.start();
			mState = STATE.PLAYING;
			return false;
		}
	});

	protected BasePlayer(VideoFrame vf, String settingDataPath) {
		mPlayerHandlerThread = new PlayerHandlerThread(this.toString(),
				Process.THREAD_PRIORITY_FOREGROUND);
		mPlayerHandlerThread.start();
		if (vf == null) {
			throw new NullPointerException("VideoFrame is null");
		}
		if (settingDataPath == null || settingDataPath.length() == 0) {
			throw new NullPointerException("settingDataPath is invailid");
		}
		mVideoFrame = vf;
		mContext = mVideoFrame.getContext();
		mVideoView = mVideoFrame.getVideoView();
		mVideoFrame.registMediaPlayerStateChangedListener(this);
		mBfStream = new BFStream("/sdcard");
		mBfStream.registerStreamListener(this);
		mBfStream.registerP2PListener(this);
		mBFVolumeManager = BFVolumeManager.getInstance(mContext);
	}

	/**
	 * 设置播放数据源
	 * 
	 * @param url
	 *            播放数据源 (如：
	 *            "servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0"
	 *            )
	 */
	public void setDataSource(String url) {
		mDataSource = url;
	}

	/**
	 * 设置播放数据源
	 * 
	 * @param url
	 *            播放数据源 (如：
	 *            "servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0"
	 *            )
	 * @param playToken
	 *            播放token
	 */
	public void setDataSource(String url, String playToken) {
		mDataSource = url;
		mToken = playToken;
	}

	/**
	 * 设置用于存储信息的本地目录(此接口已废弃)
	 */
	@Deprecated
	public void setDataPath(String dataPath) {

	}

	/**
	 * 设置视频解码方式
	 */
	public void setDecodeMode(DecodeMode decodeMode) {
		if (decodeMode == null)
			decodeMode = DecodeMode.AUTO;
		mVideoFrame.setDecodeMode(decodeMode);
		mVideoView = mVideoFrame.getVideoView();
	}

	/**
	 * 开始播放
	 */
	public void start() {
		Log.d(TAG, "start isMediaCenterInited:" + isMediaCenterInited
				+ "/mState:" + mState);
		synchronized (BasePlayer.class) {
			int type = detectNetwork(mContext, mForceStartFlag);
			if (type == NetState.NET_NOT_REACHABLE){
				Log.d(TAG, "network is not usable");
				if (mPlayErrorListener != null)
					mPlayErrorListener.onError(BFYConst.NO_NETWORK);
				return;
			}
		}
		mVideoView = mVideoFrame.getVideoView();
		if (!isMediaCenterInited && mState == STATE.IDLE) {
			mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_P2P_INIT);
		} else if (isMediaCenterInited) {
			Log.d(TAG, "start isMediaCenterInited:" + isMediaCenterInited);
			mPlayerHandlerThread.playerHandler
					.sendEmptyMessage(MSG_STREAM_DESTORY);
			mPlayerHandlerThread.playerHandler
					.sendEmptyMessage(MSG_STREAM_CREATE);
		}
		mState = STATE.PREPARING;
	}

	private int detectNetwork(Context c, boolean forceFlag) {
		if (!BFYNetworkUtil.hasNetwork(c)){
			BFStream.setNetState(NetState.NET_NOT_REACHABLE);
			return BFYNetworkUtil.NETWORK_CONNECTION_NONE;
		}
		
		if (BFYNetworkUtil.isWifiEnabled(c)){
			BFStream.setNetState(NetState.NET_WIFI_REACHABLE);
			return BFYNetworkUtil.NETWORK_CONNECTION_WIFI;
		}else if (BFYNetworkUtil.isEthernetEnabled(c)){
			BFStream.setNetState(NetState.NET_WWAN_REACHABLE);
			return BFYNetworkUtil.NETWORK_CONNECTION_ETHERNET;
		} else if (BFYNetworkUtil.isMobileEnabled(c)){
			if (forceFlag)
				BFStream.setNetState(NetState.NET_WWAN_REACHABLE);
			else{
				if (mPlayErrorListener != null)
					mPlayErrorListener.onError(BFYConst.MOBILE_NO_PLAY);
			}
				
			return BFYNetworkUtil.NETWORK_CONNECTION_MOBILE;
		}
		return BFYNetworkUtil.NETWORK_CONNECTION_NONE;
	}

	/**
	 * 停止播放
	 */
	public void stop() {
		Log.d(TAG, "stop");
		mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_STREAM_DESTORY);
		mVideoView.stop();
		mVideoFrame.updateViews();
		mState = STATE.IDLE;
	}

	/**
	 * 暂停播放
	 */
	protected void pause() {
		Log.d(TAG, "pause");
		if (mState == STATE.PLAYING) {
			mVideoView.pause();
			mState = STATE.PAUSED;
		} else {
			Log.d(TAG, "Player state is not PLAYING");
		}
	}

	/**
	 * 继续播放
	 */
	protected void resume() {
		Log.d(TAG, "resume");
		if (mState == STATE.PAUSED) {
			mVideoView.resume();
			mState = STATE.PLAYING;
		} else {
			Log.d(TAG, "Player state is not PAUSED");
		}
	}

	/**
	 * 拖动到指定播放点
	 */
	protected void seekTo(int ms) {
		Log.d(TAG, "seekTo ms:" + ms);
		if (mState == STATE.PAUSED || mState == STATE.PLAYING) {
			mVideoView.seekTo(ms);
		} else {
			Log.d(TAG, "Player state is not PAUSED or PLAYING");
		}
	}

	/**
	 * 增加音量
	 */
	public void incVolume() {
		mBFVolumeManager.incVolume();
	}

	/**
	 * 减小音量
	 */
	public void decVolume() {
		mBFVolumeManager.decVolume();
	}

	/**
	 * 设置音量
	 */
	public void setVolume(int value) {
		mBFVolumeManager.setVolume(value);
	}

	@Override
	public void onMessage(int type, int data, int error) {
		Log.d(TAG, "onMessage type:" + type + ",data:" + data + ",error:"
				+ error);
		if (data == BFStreamMessageListener.MSG_TYPE_ERROR) {
			mState = STATE.ERROR;
			if (mPlayErrorListener != null)
				mPlayErrorListener.onError(error);
		}
	}

	@Override
	public void onStreamReady() {
		Log.d(TAG, "onStreamReady");
		mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_STREAM_START);
	}
	
	@Override
	public void onMediaInfoNotFound() {
		mState = STATE.ERROR;
		if (mPlayErrorListener != null)
			mPlayErrorListener.onError(BFYConst.GET_MOVIE_INFO_FAILED);
	}

	@Override
	public void onMediaCenterInitSuccess() {
		isMediaCenterInited = true;
		mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_STREAM_DESTORY);
		mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_STREAM_CREATE);
	}

	@Override
	public void onMediaCenterInitFailed(int error) {
		isMediaCenterInited = false;
		mState = STATE.ERROR;
		if (mPlayErrorListener != null){
			switch (error) {
			case BFStream.UNKNOWN_ERROR:
				mPlayErrorListener.onError(BFYConst.MEDIA_CENTER_INIT_FAILED);
				break;
			case BFStream.INVALID_PARAM:
				mPlayErrorListener.onError(BFYConst.MEDIA_CENTER_INVALID_PARAM);
				break;
			case BFStream.INVALID_HANDLE:
				mPlayErrorListener.onError(BFYConst.MEDIA_CENTER_INVALID_HANDLE);
				break;
			case BFStream.INIT_ERROR:
				mPlayErrorListener.onError(BFYConst.MEDIA_CENTER_INIT_ERROR);
				break;
			case BFStream.PORT_BIND_FAILED:
				mPlayErrorListener.onError(BFYConst.PORT_BIND_FAILED);
				break;
			case BFStream.INVALID_STREAM_ID:
				mPlayErrorListener.onError(BFYConst.INVALID_STREAM_ID);
				break;
			case BFStream.GENERATE_URL_FAILED:
				mPlayErrorListener.onError(BFYConst.GENERATE_URL_FAILED);
				break;
			case BFStream.INVALID_URL:
				mPlayErrorListener.onError(BFYConst.INVALID_URL);
				break;
			case BFStream.NOT_ENOUGH_SPACE:
				mPlayErrorListener.onError(BFYConst.NOT_ENOUGH_SPACE);
				break;
			case BFStream.FILE_IO_ERROR:
				mPlayErrorListener.onError(BFYConst.FILE_IO_ERROR);
				break;
			case BFStream.ALLOC_MEMORY_FAILED:
				mPlayErrorListener.onError(BFYConst.ALLOC_MEMORY_FAILED);
				break;
				
			default:
				break;
			}
		}
	}

	/**
	 * 取得当前音量
	 */
	public int getCurrentVolume() {
		return mBFVolumeManager.getCurrentVolume();
	}

	/**
	 * 取得最大音量
	 */
	public int getMaxVolume() {
		return mBFVolumeManager.getMaxVolume();
	}

	/**
	 * 取得当前的解码模式
	 */
	public DecodeMode getDecodeMode() {
		return mDecodeMode;
	}

	/**
	 * 取得当前播放位置 (毫秒)
	 */
	protected long getCurrentPosition() {
		return mVideoView.getCurrentPosition();
	}

	/**
	 * 
	 * @return String VideoName
	 */
	@Nullable
	protected String getVideoName() {
		return mVideoName;
	}

	/**
	 * 设置播放错误监听器
	 * 
	 * @param listener
	 */
	public void registPlayErrorListener(PlayErrorListener listener) {
		mPlayErrorListener = listener;
	}
	
	/**
	 * 取消播放错误监听器
	 * 
	 * @param listener
	 */
	public void unregistPlayErrorListener() {
		mPlayErrorListener = null;
	}

	/**
	 * 注册事件监听器
	 */
	public void registPlayEventListener(PlayEventListener listener) {
		mPlayEventListener = listener;
	}

	/**
	 * 注销事件监听器
	 */
	public void unregistPlayEventListener(){
		mPlayEventListener = null;
	}
	
	/**
	 * 设置视频播放清晰度
	 */
	protected void setDefinition(VideoDefinition definition) {
		mVideoDefinition = definition;
		if (mBfStream != null)
			mBfStream.changeDefinition(mVideoDefinition);
	}
	
	/**
	 * 取得当前视频清晰度
	 */
	protected VideoDefinition getCurrentDefinition() {
		return mVideoDefinition;
	}
	
	/**
	 * 取得片长 (毫秒)
	 */
	protected long getDuration() {
		return mVideoView.getDuration();
	}
	
	/**
	 * 设置直播低延时
	 */
	protected void setLowLatency(boolean lowLatency) {
		mLowLatencyFlag = lowLatency;
		if (mLowLatencyFlag && !mDataSource.contains("&livelowlatency=1")){
			mDataSource += "&livelowlatency=1";
		}
	}
	
	/**
	 * 获取直播是否低延时
	 */
	protected boolean getLowLatency() {
		return mLowLatencyFlag;
	}

	private class PlayerHandlerThread extends HandlerThread {
		private Handler playerHandler = null;
		private Handler.Callback callback = new Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				Log.d(TAG, "PlayerHandlerThread msg.what = " + msg.what);
				switch (msg.what) {
				case MSG_P2P_INIT: {
					BFStream.startP2p();
					break;
				}
				case MSG_P2P_UNINIT: {
					BFStream.stopP2P();
					break;
				}
				case MSG_STREAM_START: {
					int ret = mBfStream.startStream();
					if (ret < 0)
						Log.d(TAG, "startStream error");
					mUIHandler.sendEmptyMessage(0);
					break;
				}
				case MSG_STREAM_CREATE: {
					// 这个时候要保证p2p已经启动，创建stream
					int ret = mBfStream.createStream(mDataSource, mToken, 0);
					if (ret < 0){
						Log.d(TAG, "createStream error");
						mPlayErrorListener.onError(BFYConst.MEDIA_CENTER_INVALID_PARAM);
					}
					break;
				}
				case MSG_STREAM_STOP: {
					int ret = mBfStream.closeStream();
					if (ret < 0)
						Log.d(TAG, "closeStream error");
					break;
				}
				case MSG_STREAM_DESTORY: {
					int ret = mBfStream.destoryStream();
					if (ret < 0)
						Log.d(TAG, "destoryStream error");
					break;
				}
				default:
					break;
				}
				return false;
			}
		};

		public PlayerHandlerThread(String name, int priority) {
			super(name, priority);
			Log.d(TAG, "new PlayerHandlerThread name:" + name);
		}

		@Override
		protected void onLooperPrepared() {
			Log.d(TAG, "thread " + getName() + " onLooperPrepared");
			playerHandler = new Handler(getLooper(), callback);
			super.onLooperPrepared();
		}

	}
	
	public interface PlayErrorListener{
		void onError(int errorCode);
	}
	
	public static int EVENT_TYPE_MEDIAPLAYER_ENDED = 3000;
	public static int EVENT_TYPE_MEDIAPLAYER_BUFFERING = 3001;
	public static int EVENT_TYPE_MEDIAPLAYER_READY = 3002;
	public static int EVENT_TYPE_MEDIAPLAYER_PREPARING = 3003;
	public interface PlayEventListener{
		void onEvent(int eventCode);
	}
	
	@Override
	/**
	 * 软解播放器出现异常，返回错误码
	 */
	public void onError(int errorCode) {
		if (mDecodeMode != DecodeMode.SOFT){
			Log.d(TAG, "onError, mode is not SOFT");
			return;
		}
		
		if (mPlayErrorListener != null)
			mPlayErrorListener.onError(BFYConst.SOFT_DECODE_FAILED);
	}
	
	@Override
	/**
	 * AUTO播放器出现异常，返回错误信息
	 */
	public void onError(String errorMsg) {
		if (mDecodeMode != DecodeMode.AUTO){
			Log.d(TAG, "onError, mode is not AUTO");
			return;
		}
		
		if (mPlayErrorListener != null)
			mPlayErrorListener.onError(BFYConst.EXOPLAYER_DECODE_FAILED);
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	public void setForceStartFlag(boolean flag){
		mForceStartFlag = flag;
	}
	
	@Override
	public void onStateBuffering() {
		Log.d(TAG, "MediaPlyaerProxy onStateBuffering");
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_BUFFERING);
	}
	
	@Override
	public void onStateEnded() {
		Log.d(TAG, "MediaPlyaerProxy onStateEnded");
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_ENDED);
	}
	
	@Override
	public void onStatePreparing() {
		Log.d(TAG, "MediaPlyaerProxy onStatePreparing");
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_PREPARING);
	}
	
	@Override
	public void onStateReady() {
		Log.d(TAG, "MediaPlyaerProxy onStateReady");
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_READY);
	}
}
