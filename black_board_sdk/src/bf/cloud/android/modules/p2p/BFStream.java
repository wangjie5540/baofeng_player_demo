package bf.cloud.android.modules.p2p;

import java.util.ArrayList;

import bf.cloud.android.modules.log.BFYLog;

/**
 * Created by wangtonggui
 */
public class BFStream {

	static final String TAG = BFStream.class.getSimpleName();

	private static MediaCenter mMediaCenter = MediaCenter.getInstance();
	private String mCurrentUrl = null;
	private int mMediaHandle = MediaCenter.INVALID_MEDIA_HANDLE;
	private int mStreamId = MediaCenter.INVALID_STREAM_ID;
	private MediaCenter.MediaInfo mMediaInfo = null;
	private ArrayList<MediaCenter.StreamInfo> mStreamInfoList = new ArrayList<MediaCenter.StreamInfo>();
	private StateCallBackHandler mCallBackHandler = null;
	private BFStreamMessageListener mListener = null;
	private boolean mIsWaitingToPlay = false;
	private int mStreamWaitToPlay = MediaCenter.INVALID_STREAM_ID;
	private int mPort = 8080;
	private int mStreamMode;

	public BFStream() {
		mCallBackHandler = new StateCallBackHandler();
		mMediaCenter.setCallback(mCallBackHandler);
	}

	public interface BFStreamMessageListener {
		public final static int MSG_TYPE_ERROR = 0;
		public final static int MSG_TYPE_NORMAL = 1;

		public void onMessage(int type, int data, int error);
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

			BFYLog.d(LOG_TAG, "Handle State Changed to [" + state
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
				// check if it is waiting to start a new stream
				if (mIsWaitingToPlay) {
					mIsWaitingToPlay = false;
					if (mStreamWaitToPlay != MediaCenter.INVALID_STREAM_ID) {
						startStream(mStreamWaitToPlay);
					} else {
						startStream(getDefaultStreamId());
					}
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

	/**
	 * 开启流
	 * 
	 * @param streamId
	 */
	public int startStream() {
		int result = MediaCenter.NativeReturnType.NO_ERROR;
		for (int i = 0; i < 50; i++) {
			if (mPort > 65400) {
				mPort = 8080 + i;
			}
			mPort += i;
			result = mMediaCenter.StartStreamService(mMediaHandle, mStreamId,
					mStreamMode, mPort);
			switch (result) {
			case MediaCenter.NativeReturnType.NO_ERROR: {
				BFYLog.d(TAG, "Start Stream Bind Port[" + mPort + "] Success");
			}
			case MediaCenter.NativeReturnType.PORT_BIND_FAILED:
				continue;
			default:
				return -1;
			}
		}
		BFYLog.d(TAG, "Bind Port fail, try to [" + mPort + "]");
		return 0;
	}

	/**
	 * 关闭流
	 */
	public int closeStream() {

		return 0;
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * init Media Center
	 * 
	 * @param dataPath
	 *            : location to save data
	 * @param netState
	 *            : current net state
	 * @return error code
	 */
	public int init(String dataPath, int netState) {
		BFYLog.d(TAG, "Init Media Center. data_path:[" + dataPath + "]");
		int ret = mMediaCenter.InitMediaCenter(dataPath, netState);
		BFYLog.d(TAG, "Init Media Center: " + ret);
		return ret;
	}

	/**
	 * uninit Media Center
	 * 
	 * @return error code
	 */
	public int uninit() {
		BFYLog.d(TAG, "Uninit Media Center");
		closeStream();
		return mMediaCenter.MediaCenterCleanup();
	}

	/**
	 * 设置网络类型，静态方法
	 * 
	 * @param state
	 * @return
	 */
	public static int setNetState(int state) {
		BFYLog.d(TAG, "SetNetState [" + state + "]");
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

	private String getErrorInfo(int error) {
		return mMediaCenter.GetErrorInfo(error);
	}

}
