package bf.cloud.android.modules.p2p;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.widget.MediaController.MediaPlayerControl;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.modules.p2p.MediaCenter.NetState;

/**
 * Created by wangtonggui
 */
public final class BFStream {

	static final String TAG = BFStream.class.getSimpleName();

	private static MediaCenter mMediaCenter = MediaCenter.getInstance();
	private int mMediaHandle = MediaCenter.INVALID_MEDIA_HANDLE;
	private int mStreamId = MediaCenter.INVALID_STREAM_ID;
	private MediaCenter.MediaInfo mMediaInfo = null;
	private ArrayList<MediaCenter.StreamInfo> mStreamInfoList = new ArrayList<MediaCenter.StreamInfo>();
	private StateCallBackHandler mCallBackHandler = null;
	private BFStreamMessageListener mListener = null;
	private boolean mIsReadyToStartStream = false;
	private int mStreamWaitToPlay = MediaCenter.INVALID_STREAM_ID;
	private int mPort = BFYConst.DEFAULT_P2P_PORT;
	private int mStreamMode;
	private static P2pHandlerThread mP2pHandlerThread = null;
	private static String mSettingDataPath = null;
	private static int mNetState = NetState.NET_NOT_REACHABLE;

	public BFStream(String settingDataPath, int netState) {
		if (settingDataPath == null || netState == NetState.NET_NOT_REACHABLE) {
			throw new NullPointerException(
					"dataPath is null, or netState is NET_NOT_REACHABLE");
		}
		mSettingDataPath = settingDataPath;
		mNetState = netState;
		if (mP2pHandlerThread == null) {
			mP2pHandlerThread = new P2pHandlerThread(TAG,
					Process.THREAD_PRIORITY_FOREGROUND);
			mP2pHandlerThread.start();
		}
		mCallBackHandler = new StateCallBackHandler();
		mMediaCenter.setCallback(mCallBackHandler);
	}

	public interface BFStreamMessageListener {
		public final static int MSG_TYPE_ERROR = 0;
		public final static int MSG_TYPE_NORMAL = 1;

		/**
		 * 从MediaCenter里面回调的消息，分为MSG_TYPE_ERROR和MSG_TYPE_NORMAL两种
		 * @param type MSG_TYPE_ERROR或MSG_TYPE_NORMAL
		 * @param data
		 * @param error 错误码
		 */
		public void onMessage(int type, int data, int error);

		/**
		 * 流创建就绪
		 */
		public void onStreamReady();
		
		/**
		 * MediaCenter初始化成功
		 */
		public void onMediaCenterInitSuccess();
		
		/**
		 * MediaCenter初始化失败
		 */
		public void onMediaCenterInitFailed(int error);

	}

	public BFStreamMessageListener getListener() {
		return mListener;
	}

	public void registerListener(BFStreamMessageListener listener) {
		mListener = listener;
	}

	public void unregisterListener() {
		mListener = null;
	}

	class StateCallBackHandler implements MediaCenter.HandleStateCallback {
		protected static final String LOG_TAG = "BFStream_StateCallBack";

		public void OnStateChanged(int handle, int state, int error) {
			if (handle != mMediaHandle) {
				return;
			}

			Log.d(LOG_TAG, "Handle State Changed to [" + state
					+ "] (0.IDLE 1.RUNNABLE 2.RUNNING 3.ACCOMPLISH 4.ERROR)");
			if (mListener != null) {
				if (MediaCenter.MediaHandleState.MEDIA_HANDLE_ERROR == state) {
					mListener.onMessage(BFStreamMessageListener.MSG_TYPE_ERROR,
							state, error);
				} else {
					mListener.onMessage(
							BFStreamMessageListener.MSG_TYPE_NORMAL, state,
							error);
				}
			}
			switch (state) {
			case MediaCenter.MediaHandleState.MEDIA_HANDLE_IDLE: {
				break;
			}
			case MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNABLE: {
				// update Media Information
				mMediaInfo = mMediaCenter.GetMediaInfo(mMediaHandle);
				if (mMediaInfo == null || mMediaInfo.mediaStreamCount <= 0) {
					return;
				}
				MediaCenter.StreamInfo streamInfoList[] = new MediaCenter.StreamInfo[mMediaInfo.mediaStreamCount];
				streamInfoList = mMediaCenter.GetStreamInfo(mMediaHandle,
						mMediaInfo.mediaStreamCount);
				mStreamInfoList.clear();
				for (int i = 0; i < mMediaInfo.mediaStreamCount; i++) {
					mStreamInfoList.add(i, streamInfoList[i]);
				}
				if (!mIsReadyToStartStream) {
					mIsReadyToStartStream = true;
					if (mListener != null)
						mListener.onStreamReady();
				}
				break;
			}
			case MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNING:
				break;
			case MediaCenter.MediaHandleState.MEDIA_HANDLE_ACCOMPLISH:
				break;
			case MediaCenter.MediaHandleState.MEDIA_HANDLE_ERROR:
				break;
			default:
				break;
			}
		}
	}

	private int getDefaultStreamId() {
		int result = MediaCenter.INVALID_STREAM_ID;

		int streamCount = mStreamInfoList.size();
		if (streamCount > 0) {
			for (int i = 0; i < streamCount; i++) {
				MediaCenter.StreamInfo streamInfo = mStreamInfoList.get(i);
				if (streamInfo.defaultStream) {
					result = streamInfo.streamId;
					break;
				}
			}

			if (result == MediaCenter.INVALID_STREAM_ID) {
				result = mStreamInfoList.get(streamCount / 2).streamId;
			}
		}

		return result;
	}
	
	private void initMediaCenter(){
		mP2pHandlerThread.p2pHandler
		.sendEmptyMessage(P2pHandlerThread.INIT_MEDIA_CENTER);
	}

	/**
	 * 创建流
	 * 
	 * @return 正常返回0，异常返回-1
	 */
	public int createStream(String url, String token, int mode) {
		mMediaHandle = mMediaCenter.CreateMediaHandle(url, token);
		Log.d(TAG, "mMediaHandle = " + mMediaHandle);
		if (mMediaHandle == MediaCenter.INVALID_MEDIA_HANDLE) {
			return -1;
		}
		return 0;
	}

	/**
	 * 开启流(可以在BFStreamMessageListener的onStreamReady中调用)
	 * 流地址：http://127.0.0.1:mPort/live.m3u8
	 * 
	 * @param
	 */
	public int startStream() {
		int result = MediaCenter.NativeReturnType.NO_ERROR;
		if (mStreamWaitToPlay == MediaCenter.INVALID_STREAM_ID)
			mStreamId = getDefaultStreamId();
		else
			mStreamId = mStreamWaitToPlay;
		if (mIsReadyToStartStream) {
			for (int i = 0; i < 50; i++) {
				if (mPort > 65400) {
					mPort = BFYConst.DEFAULT_P2P_PORT + i;
				}
				mPort += i;
				// 设置流ID
				result = mMediaCenter.StartStreamService(mMediaHandle,
						mStreamId, mStreamMode, mPort);
				switch (result) {
				case MediaCenter.NativeReturnType.NO_ERROR: {
					Log.d(TAG, "Start Stream Bind Port[" + mPort + "] Success");
				}
				case MediaCenter.NativeReturnType.PORT_BIND_FAILED:
					continue;
				default:
					return -1;
				}
			}
			Log.d(TAG, "Bind Port fail, try to [" + mPort + "]");
		} else {
			Log.d(TAG, "Stream not ready or not want to start");
			return -1;
		}
		return result;
	}

	/**
	 * 关闭流
	 */
	public int closeStream() {
		int ret = -1;
		if (MediaCenter.INVALID_MEDIA_HANDLE != mMediaHandle) {
			ret = mMediaCenter.StopStreamService(mMediaHandle);
			mStreamId = -1;
		}
		return ret;
	}

	/**
	 * 销毁流
	 */
	public int destoryStream() {
		int ret = -1;
		ret = closeStream();
		if (ret < 0)
			return ret;
		ret = mMediaCenter.DestroyMediaHandle(mMediaHandle);
		mMediaHandle = MediaCenter.INVALID_MEDIA_HANDLE;
		return ret;
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 设置网络类型，静态方法
	 * 
	 * @param state
	 * @return
	 */
	public static int setNetState(int state) {
		Log.d(TAG, "SetNetState [" + state + "]");
		return mMediaCenter.SetNetState(state);
	}

	public MediaCenter.MediaInfo getMediaInfo() {
		return mMediaInfo;
	}

	public ArrayList<MediaCenter.StreamInfo> getStreamInfoList() {
		return mStreamInfoList;
	}

	public int getState() {
		return mMediaCenter.GetHandleState(mMediaHandle);
	}

	public int getDownloadSpeed() {
		return mMediaCenter.GetDownloadSpeed(mMediaHandle);
	}

	public int getDownloadPercent() {
		return mMediaCenter.GetDownloadPercent(mMediaHandle);
	}

	public int calcCanPlayTime(int time) {
		return mMediaCenter.CalcCanPlayTime(mMediaHandle, time);
	}

	public int setCurPlayTime(int time) {
		return mMediaCenter.SetCurPlayTime(mMediaHandle, time);
	}

	public int getStreamId() {
		return mStreamId;
	}

	public String getStreamUrl() {
		String url = null;
		switch (mStreamMode) {
		case MediaCenter.StreamMode.STREAM_MP4_MODE:
			url = BFYConst.P2PSERVER + ":" + mPort;
			break;
		case MediaCenter.StreamMode.STREAM_HLS_MODE:
			url = BFYConst.P2PSERVER + ":" + mPort + "/bfcloud.m3u8";
			break;
		default:
			url = BFYConst.P2PSERVER + ":" + mPort;
			break;
		}
		return url;
	}

	private String getErrorInfo(int error) {
		return mMediaCenter.GetErrorInfo(error);
	}

	private enum P2pState {
		NOT_INIT(-1), INITING(0), INITED(1);
		private P2pState(int state) {
			this.state = state;
		}

		private int state = -1;

		private int value() {
			return state;
		}

	}

	private class P2pHandlerThread extends HandlerThread {
		private final static int INIT_MEDIA_CENTER = 1;
		private final static int UNINIT_MEDIA_CENTER = 2;

		private Handler p2pHandler = null;
		private P2pState state = P2pState.NOT_INIT;
		private Callback callback = new Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case INIT_MEDIA_CENTER:
					if (state == P2pState.NOT_INIT) {
						Log.d(TAG, "Init Media Center. data_path:["
								+ mSettingDataPath + "]");
						int ret = -1;
						state = P2pState.INITING;
						ret = mMediaCenter.InitMediaCenter(mSettingDataPath,
								mNetState);
						if (ret < 0) {
							state = P2pState.NOT_INIT;
						} else {
							Log.d(TAG, "Init Media Center success");
							state = P2pState.INITED;
						}
					}
					break;
				case UNINIT_MEDIA_CENTER:
					//这里暂时对MediaCenter不进行卸载操作，思路是：一旦加载，就不卸载了
				default:
					break;
				}
				return false;
			}
		};

		public P2pHandlerThread(String name, int priority) {
			super(name, priority);
			Log.d(TAG, "new P2pHandlerThread " + name);
		}

		@Override
		protected void onLooperPrepared() {
			Log.d(TAG, "thread " + getName() + " onLooperPrepared");
			p2pHandler = new Handler(getLooper(), callback);
			initMediaCenter();
			super.onLooperPrepared();
		}
		
		@Override
		public void run() {
			super.run();
		}
	}
}
