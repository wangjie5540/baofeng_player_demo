package bf.cloud.android.modules.p2p;

import java.util.ArrayList;

import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.modules.log.BFYLog;
/**
 * Created by zhangsong on 2014/10/9.
 */
public class BFStream {

    static final String TAG = BFStream.class.getSimpleName();

    private MediaCenter mMediaCenter = MediaCenter.getInstance();
    private String mCurrentUrl = null;
    private int mMediaHandle = MediaCenter.INVALID_MEDIA_HANDLE;
    private int mStreamId = MediaCenter.INVALID_STREAM_ID;
    private MediaCenter.MediaInfo mMediaInfo = null;
    private ArrayList<MediaCenter.StreamInfo> mStreamInfoList = new ArrayList<MediaCenter.StreamInfo>();
    private StateCallBackHandler mCallBackHandler = null;
    private static BFStreamMessageListener mListener = null;
    private boolean mIsWaitingToPlay = false;
    private int mStreamWaitToPlay =  MediaCenter.INVALID_STREAM_ID;
    private int mPort = 8080;
    private int mStreamMode;

    public BFStream() {
        mCallBackHandler = new StateCallBackHandler();
        mMediaCenter.setCallback(mCallBackHandler);
    }

    public interface BFStreamMessageListener {
        public final static int MSG_TYPE_ERROR = 0;
        public final static int MSG_TYPE_CREATE_STREAM_SUCCESS = 1;
        public void onMessage(int type, int data);
    }
    
    public static BFStreamMessageListener getListener() {
        return mListener;
    }
    
    public void registerListener(BFStreamMessageListener listener) {
        mListener = listener;
    }
    
    public void unregisterListener() {
        mListener = null;
    }

    private void sendMsg(int type, int data) {
        if (mListener != null) {
            mListener.onMessage(type, data);
        }
    }

    class StateCallBackHandler implements MediaCenter.HandleStateCallback {
        protected static final String LOG_TAG = "BFStream_StateCallBack";
        
        public void OnStateChanged(int handle, int state, int error) {
            if (handle != mMediaHandle) {
               	return;
            }
            
            BFYLog.d(LOG_TAG, "Handle State Changed to [" + state +"] （ 0.IDLE 1.RUNNABLE 2.RUNNING 3.ACCOMPLISH 4.ERROR）");
            
            switch(state) {
                case MediaCenter.MediaHandleState.MEDIA_HANDLE_IDLE: {
                    break;
                }
                case MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNABLE: {
                    //update Media Information
                    mMediaInfo = mMediaCenter.GetMediaInfo(mMediaHandle);
                    if (mMediaInfo == null || mMediaInfo.mediaStreamCount <= 0) {
                        handleError(PlayErrorManager.GET_MOVIE_INFO_FAILED);
                        return;
                    }
                    MediaCenter.StreamInfo streamInfoList[] = new MediaCenter.StreamInfo[mMediaInfo.mediaStreamCount];
                    streamInfoList = mMediaCenter.GetStreamInfo(mMediaHandle, mMediaInfo.mediaStreamCount);
                    mStreamInfoList.clear();
                    for (int i = 0; i < mMediaInfo.mediaStreamCount; i++) {
                        mStreamInfoList.add(i, streamInfoList[i]);
                    }
                    //check if it is waiting to start a new stream
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
                case MediaCenter.MediaHandleState.MEDIA_HANDLE_ERROR: {
                    handleError(error);
                    break;
                }
                default:
                    break;
            }
        }
    }

    public void destroyCurMediaHandle() {
    	BFYLog.d(TAG,"DestroyCurrHandle:" + mMediaHandle);
        if (mMediaHandle != MediaCenter.INVALID_MEDIA_HANDLE) {
            destroyCurStream();
            mMediaCenter.DestroyMediaHandle(mMediaHandle);
            mMediaHandle = MediaCenter.INVALID_MEDIA_HANDLE;
            mCurrentUrl = null;
            mMediaInfo = null;
            mStreamInfoList.clear();
            mStreamWaitToPlay =  MediaCenter.INVALID_STREAM_ID;
        }
    }

    private void destroyCurStream() {
    	BFYLog.d(TAG,"DestroyCurrStream:" + mStreamId);
    	mIsWaitingToPlay = false;
        if (mStreamId != MediaCenter.INVALID_STREAM_ID) {
            mMediaCenter.StopStreamService(mMediaHandle);
            mStreamId = MediaCenter.INVALID_STREAM_ID;
        }
    }

    private void handleError(int error) {
        sendMsg(BFStreamMessageListener.MSG_TYPE_ERROR, error);
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

    private void startStream(int streamId) {
        BFYLog.d(TAG, "try to create stream[" + streamId + "]...");
        mStreamId = streamId;
        int result = MediaCenter.NativeReturnType.NO_ERROR;
        for (int i = 0; i < 50; i++) {
            if (mPort > 65400) {
            	mPort = 8080 + i; 
            }
            mPort += i;
            result = mMediaCenter.StartStreamService(mMediaHandle, streamId, mStreamMode, mPort);
            switch (result) {
                case MediaCenter.NativeReturnType.NO_ERROR: {                    
                    BFYLog.d(TAG, "Start Stream Bind Port[" + mPort + "] Success");
                    sendMsg(BFStreamMessageListener.MSG_TYPE_CREATE_STREAM_SUCCESS, mPort);
                    return;
                }
                case MediaCenter.NativeReturnType.PORT_BIND_FAILED:
                    continue;
                default: {
                    handleError(result);
                    return;
                }
            }
        }
        //handle PORT_BIND_FAILED error
        BFYLog.d(TAG, "Bind Port fail, try to [" + mPort +"]");
        handleError(result);
    }

////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * init Media Center
     * @param dataPath : location to save data
     * @param netState : current net state
     * @return error code
     */
    public int init(String dataPath,int netState) {
        BFYLog.d(TAG, "Init Media Center. data_path:[" + dataPath + "]");
        int ret = mMediaCenter.InitMediaCenter(dataPath, netState);
        BFYLog.d(TAG, "Init Media Center: " + ret);
        return ret;
    }

    /**
     * uninit Media Center
     * @return error code
     */
    public int uninit() {
        BFYLog.d(TAG, "Uninit Media Center");
        destroyCurMediaHandle();
        return mMediaCenter.MediaCenterCleanup();
    }

    /**
     * play a default stream(definition)
     * @param url : media url
     * @param token : user's token
     * @param streamId : if play default, use "MediaCenter.IVNALID_STREAM_ID"
     */
    public void play(String url, String token, int streamId, int streamMode) {
        BFYLog.d(TAG, "Play a new stream： url[" + url +"] token["+token+"] streamId["+streamId+"]");
        destroyCurStream();
        mStreamMode = streamMode;
        if (url == mCurrentUrl && getHandleState() == MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNABLE) {
            BFYLog.d(TAG,"Same Media, and the handle is runnable, just restart a new stream");
            if (streamId != MediaCenter.INVALID_STREAM_ID) {
                startStream(streamId);
            } else {
                startStream(getDefaultStreamId());
            }
        } else {
            BFYLog.d(TAG,"create new handle");
            destroyCurMediaHandle();
            //记录要播放的stream，等待状态返回RUNNABLE开始播放
            mIsWaitingToPlay = true;
            mStreamWaitToPlay = streamId;
            mMediaHandle = mMediaCenter.CreateMediaHandle(url, token);
            if (mMediaHandle == MediaCenter.INVALID_MEDIA_HANDLE) {
            	BFYLog.d(TAG,"Play:" + mMediaHandle);
                handleError(PlayErrorManager.MEDIA_CENTER_INVALID_PARAM);
                mIsWaitingToPlay = false;
                mStreamWaitToPlay = MediaCenter.INVALID_STREAM_ID;
                return;
            }
            //create handle success
            BFYLog.d(TAG, "Create Handle SUCCESS!! Waite to create Stream..." + mMediaHandle);
            mCurrentUrl = url;
        }
    }

    /**
     * stop the curr stream
     */
    public void stop() {
        BFYLog.d(TAG,"Stop, just stop current stream");
        destroyCurStream();
    }

    public int setNetState(int state) {
        BFYLog.d(TAG,"SetNetState ["+state+"]");
        return mMediaCenter.SetNetState(state);
    }

    public MediaCenter.MediaInfo getMediaInfo() { 
    	return mMediaInfo; 
    }

    public ArrayList<MediaCenter.StreamInfo> getStreamInfoList() { 
    	return mStreamInfoList; 
    }

    public int getHandleState() {
        return mMediaCenter.GetHandleState(mMediaHandle);
    }

    public int getDownloadSpeed() {
        return mMediaCenter.GetDownloadSpeed(mMediaHandle);
    }

    public int getDownloadPercent() {
        return mMediaCenter.GetDownloadPercent(mMediaHandle);
    }

    public int calcCanPlayTime(int time)
    {
        return mMediaCenter.CalcCanPlayTime(mMediaHandle, time);
    }

    public int setCurPlayTime(int time) {
        return mMediaCenter.SetCurPlayTime(mMediaHandle, time);
    }

    public String getErrorInfo(int error) {
        return mMediaCenter.GetErrorInfo(error);
    }

}
