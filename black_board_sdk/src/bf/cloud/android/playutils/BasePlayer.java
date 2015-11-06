package bf.cloud.android.playutils;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.proxy.BFVolumeManager;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.MediaPlayerErrorListener;
import bf.cloud.android.components.mediaplayer.proxy.MediaPlayerProxy.StateChangedListener;
import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.BFStream.BFP2PListener;
import bf.cloud.android.modules.p2p.BFStream.BFStreamMessageListener;
import bf.cloud.android.modules.p2p.MediaCenter.NetState;
import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.utils.BFYNetworkUtil;
import bf.cloud.android.utils.BFYSysUtils;

/**
 * Created by wangtonggui
 */
public abstract class BasePlayer extends VideoFrame implements BFStreamMessageListener,
		BFP2PListener, MediaPlayerErrorListener, StateChangedListener{
	private final String TAG = BasePlayer.class.getSimpleName();
	
	private Context mContext = null;
	protected BFYVideoInfo mVideoInfo = null;
	protected String mToken = "";
	// 这里的变化必须要对应一个变化mode的消息
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
	private boolean mIsVr = false;
	private VideoDefinition mVideoDefinition = VideoDefinition.UNKNOWN;
	private String mSettingDataPath = BFYConst.LOG_PATH;

	//error codes below
    public static final int ERROR_NO_ERROR = 0;     
	public static final int ERROR_MEDIA_CENTER_INIT_FAILED = 1000;     // 初始化media center失败
	public static final int ERROR_MEDIA_CENTER_INVALID_PARAM = 1001;   // 无效参数
	public static final int ERROR_MEDIA_CENTER_INVALID_HANDLE = 1002;  // 无效句柄
	public static final int ERROR_MEDIA_CENTER_INIT_ERROR = 1003;      // 初始化错误
	public static final int ERROR_PORT_BIND_FAILED = 1004;		     // 端口绑定失败
	public static final int ERROR_INVALID_STREAM_ID = 1005;		     // 无效的流ID
	public static final int ERROR_GENERATE_URL_FAILED = 1006;		     // 生成URL失败
	public static final int ERROR_INVALID_URL = 1007;		     		 // 创建任务时解析URL失败
	public static final int ERROR_NOT_ENOUGH_SPACE = 1008;		     // 创建任务时磁盘空间不足
	public static final int ERROR_FILE_IO_ERROR = 1009;		    	 // 创建任务时文件IO错误
	public static final int ERROR_ALLOC_MEMORY_FAILED = 1010;		     // 创建任务时内存分配错误
	public static final int ERROR_GET_MOVIE_INFO_FAILED = 1011;        // 从media center获取媒体信息失败
	public static final int ERROR_EXOPLAYER_DECODE_FAILED = 1012;      // 调用 ExoPlayer 播放时解码失败
	public static final int ERROR_SOFT_DECODE_FAILED = 1013;           // 软解失败
	public static final int ERROR_NO_NETWORK = 1014;      			 // 没有网络
	public static final int ERROR_MOBILE_NO_PLAY = 1015;      	     // 移动网络默认先停止播放
	public static final int ERROR_MEDIA_MOVIE_INFO_NOT_FOUND = 2005;	 // 查询媒体信息失败：找不到媒体信息
	public static final int ERROR_MEDIA_MOVIE_INFO_LIVE_ENDED = 2006;	 // 直播停止
	public static final int ERROR_MEDIA_MOVIE_INFO_FORBIDDEN = 2008;	 // 查询媒体信息失败：无权限
	public static final int ERROR_MEDIA_MOVIE_INFO_UNAUTHORIZED = 2009;// 查询媒体信息失败：未授权
	public static final int ERROR_P2P_NO_DATA_SOURCE = 3006;			 // 数据源异常
	public static final int ERROR_P2P_LIVE_ENDED = 3009;			 	 // 直播结束
	public static final int ERROR_P2P_LIVE_NOT_BEGIN = 3010;			 // 直播还没开始
	
	//error code below
	public static final int ERROR_PLAYER_ERROR_MIN = 1000;
	public static final int ERROR_PLAYER_ERROR_MAX = 1999;
	public static final int ERROR_MEDIA_INFO_ERROR_MIN = 2000;
	public static final int ERROR_MEDIA_INFO_ERROR_MAX = 2999;
	public static final int ERROR_P2P_ERROR_MIN = 3000;
	public static final int ERROR_P2P_ERROR_MAX = 3999;
	
	private static final int MSG_STREAM_CREATE = 5000;
	private static final int MSG_STREAM_START = 5001;
	private static final int MSG_STREAM_STOP = 5002;
	private static final int MSG_STREAM_DESTORY = 5003;

	private static final int MSG_P2P_INIT = 10004;
	private static final int MSG_P2P_UNINIT = 10005;

	private static final int MSG_UI_ = 20000;
	
	public static final int EVENT_TYPE_MEDIAPLAYER_ENDED = 4000;
	public static final int EVENT_TYPE_MEDIAPLAYER_BUFFERING = 4001;
	public static final int EVENT_TYPE_MEDIAPLAYER_READY = 4002;
	public static final int EVENT_TYPE_MEDIAPLAYER_PREPARING = 4003;
	public static final int EVENT_TYPE_MEDIAPLAYER_START = 4004;
	public static final int EVENT_TYPE_MEDIAPLAYER_STARTING = 4005;
	public static final int EVENT_TYPE_MEDIAPLAYER_STARTED = 4006;
	public static final int EVENT_TYPE_MEDIAPLAYER_STOP = 4007;
	public static final int EVENT_TYPE_MEDIAPLAYER_SEEKTO = 4008;
	public static final int EVENT_TYPE_MEDIAPLAYER_PAUSE = 4009;
	public static final int EVENT_TYPE_MEDIAPLAYER_RESUME = 4010;
	

	public enum STATE {
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
			mVideoView.setVrFlag(mIsVr);
			mVideoView.setDataSource(mBfStream.getStreamUrl());
			mVideoView.start();
			mState = STATE.PLAYING;
			if (mPlayEventListener != null)
				mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_STARTED);
			return false;
		}
	});

	protected BasePlayer(Context c) {
		super(c);
		mContext = c;
		init();
	}
	
	protected BasePlayer(Context c, AttributeSet attrs){
		super(c, attrs);
		mContext = c;
		init();
	}
	
	protected BasePlayer(Context c, AttributeSet attrs, int defStyleAttr){
		super(c, attrs, defStyleAttr);
		mContext = c;
		init();
	}
	
	private void init(){
		mPlayerHandlerThread = new PlayerHandlerThread(this.toString(),
				Process.THREAD_PRIORITY_FOREGROUND);
		mPlayerHandlerThread.start();
		if (mSettingDataPath == null || mSettingDataPath.length() == 0) {
			throw new NullPointerException("settingDataPath is invailid");
		}
		registMediaPlayerStateChangedListener(this);
		mBfStream = new BFStream(mSettingDataPath);
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
	 * 设置用于存储信息的本地目录
	 */
	public void setDataPath(String dataPath) {
		mSettingDataPath = dataPath;
	}

	/**
	 * 设置视频解码方式
	 */
	public void setDecodeMode(DecodeMode decodeMode) {
		if (decodeMode == null)
			decodeMode = DecodeMode.AUTO;
		super.setDecodeMode(decodeMode);
	}

	/**
	 * 开始播放
	 */
	public void start() {
		Log.d(TAG, "start isMediaCenterInited:" + isMediaCenterInited
				+ "/mState:" + mState);
		synchronized (BasePlayer.class) {
			int type = detectNetwork(mContext, mForceStartFlag);
			Log.d(TAG, "network type:" + type);
			if (type == BFYNetworkUtil.NETWORK_CONNECTION_NONE){
				Log.d(TAG, "network is unusable/mPlayErrorListener:" + mPlayErrorListener);
				if (mPlayErrorListener != null)
					mPlayErrorListener.onError(ERROR_NO_NETWORK);
				return;
			} else if (type == BFYNetworkUtil.NETWORK_CONNECTION_MOBILE){
				if (mForceStartFlag){
					mForceStartFlag = false;
				} else{
					if (mPlayErrorListener != null)
						mPlayErrorListener.onError(ERROR_MOBILE_NO_PLAY);
					Log.d(TAG, "network is mobile, you must set setForceStartFlag(true)");
					return;
				}
			}
		}
		mForceStartFlag = false;
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_START);
		if (!isMediaCenterInited && mState == STATE.IDLE) {
			mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_P2P_INIT);
		} else if (isMediaCenterInited) {
			Log.d(TAG, "start isMediaCenterInited:" + isMediaCenterInited);
			mPlayerHandlerThread.playerHandler
					.sendEmptyMessage(MSG_STREAM_DESTORY);
			mPlayerHandlerThread.playerHandler
					.sendEmptyMessage(MSG_STREAM_CREATE);
		}
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_STARTING);
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
			else
				BFStream.setNetState(NetState.NET_NOT_REACHABLE);
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
		updateViews();
		if (mPlayEventListener != null)
			mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_STOP);
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
			if (mPlayEventListener != null)
				mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_PAUSE);
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
			if (mPlayEventListener != null)
				mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_RESUME);
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
			if (mPlayEventListener != null)
				mPlayEventListener.onEvent(EVENT_TYPE_MEDIAPLAYER_SEEKTO);
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
		if (mBfStream != null)
			mVideoName = mBfStream.getVideoName();
		mPlayerHandlerThread.playerHandler.sendEmptyMessage(MSG_STREAM_START);
	}
	
	@Override
	public void onMediaInfoNotFound() {
		mState = STATE.ERROR;
		if (mPlayErrorListener != null)
			mPlayErrorListener.onError(ERROR_GET_MOVIE_INFO_FAILED);
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
				mPlayErrorListener.onError(ERROR_MEDIA_CENTER_INIT_FAILED);
				break;
			case BFStream.INVALID_PARAM:
				mPlayErrorListener.onError(ERROR_MEDIA_CENTER_INVALID_PARAM);
				break;
			case BFStream.INVALID_HANDLE:
				mPlayErrorListener.onError(ERROR_MEDIA_CENTER_INVALID_HANDLE);
				break;
			case BFStream.INIT_ERROR:
				mPlayErrorListener.onError(ERROR_MEDIA_CENTER_INIT_ERROR);
				break;
			case BFStream.PORT_BIND_FAILED:
				mPlayErrorListener.onError(ERROR_PORT_BIND_FAILED);
				break;
			case BFStream.INVALID_STREAM_ID:
				mPlayErrorListener.onError(ERROR_INVALID_STREAM_ID);
				break;
			case BFStream.GENERATE_URL_FAILED:
				mPlayErrorListener.onError(ERROR_GENERATE_URL_FAILED);
				break;
			case BFStream.INVALID_URL:
				mPlayErrorListener.onError(ERROR_INVALID_URL);
				break;
			case BFStream.NOT_ENOUGH_SPACE:
				mPlayErrorListener.onError(ERROR_NOT_ENOUGH_SPACE);
				break;
			case BFStream.FILE_IO_ERROR:
				mPlayErrorListener.onError(ERROR_FILE_IO_ERROR);
				break;
			case BFStream.ALLOC_MEMORY_FAILED:
				mPlayErrorListener.onError(ERROR_ALLOC_MEMORY_FAILED);
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
						if (mPlayErrorListener != null)
							mPlayErrorListener.onError(ERROR_MEDIA_CENTER_INVALID_PARAM);
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
			mPlayErrorListener.onError(ERROR_SOFT_DECODE_FAILED);
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
			mPlayErrorListener.onError(ERROR_EXOPLAYER_DECODE_FAILED);
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	/**
	 * 忽略网络类型，强制播放
	 * @param true:无论什么网络类型都可开始播放
	 * 		  false:在使用流量时，禁止播放，并上报事件
	 */
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
		mState = STATE.COMPLETED;
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
	
	abstract protected void reportPlayExperienceStatInfo();
	abstract protected void reportPlayProcessStatInfo();
	
	protected boolean canReportStatInfo(){
		int errorCode = ERROR_NO_ERROR;
    	
    	boolean result =
    			errorCode != ERROR_NO_ERROR &&
    			errorCode != ERROR_NO_NETWORK &&
    			errorCode != ERROR_MOBILE_NO_PLAY &&
    			errorCode != ERROR_MEDIA_MOVIE_INFO_NOT_FOUND && 
    			errorCode != ERROR_MEDIA_MOVIE_INFO_LIVE_ENDED && 
    			errorCode != ERROR_P2P_LIVE_ENDED &&
    			errorCode != ERROR_P2P_LIVE_NOT_BEGIN;
    	
    	return result;
	}
	
	protected void prepareBaseStatInfo(StatInfo statInfo) {
		statInfo.gcid = BFYSysUtils.getFidFromVk(mVideoInfo.getUrl());
		statInfo.userId = BFYSysUtils.getUidFromVk(mVideoInfo.getUrl());
		statInfo.decodeMode = (mDecodeMode == DecodeMode.SOFT ? 0 : 1);
//		statInfo.errorCode = mPlayErrorManager.getErrorCode();
    }
	
	public void setVrFlag(boolean flag){
		mIsVr = flag;
	}
	
	public STATE getState(){
		return mState;
	}
}
