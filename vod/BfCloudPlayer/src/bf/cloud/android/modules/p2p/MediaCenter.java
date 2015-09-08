package bf.cloud.android.modules.p2p;

/**
 * Created by zhangsong on 14-9-23.
 */
public final class MediaCenter {
    private static MediaCenter instance = null;

    public static final int INVALID_MEDIA_HANDLE = 0;
    public static final int INVALID_STREAM_ID = -1;

    static final String TAG = MediaCenter.class.getSimpleName();

    public static class MediaHandleState {
        public final static int MEDIA_HANDLE_IDLE = 0;
        public final static int MEDIA_HANDLE_RUNNABLE = 1;
        public final static int MEDIA_HANDLE_RUNNING = 2;
        public final static int MEDIA_HANDLE_ACCOMPLISH = 3;
        public final static int MEDIA_HANDLE_ERROR = 4;
    }

    public static class NetState {
        public final static int NET_NOT_REACHABLE = 0;
        public final static int NET_WWAN_REACHABLE = 1;
        public final static int NET_WIFI_REACHABLE = 2;
    }

    public static class StreamMode{
        public final static int STREAM_MP4_MODE = 0;
        public final static int STREAM_HLS_MODE = 1;
		public final static int STREAM_RAW_MODE = 2;
    }

    public static class NativeReturnType{
    	public final static int NO_ERROR = 0;				// 无错误
    	public final static int UNKNOWN_ERROR = -1;			// 未知错误
		public final static int INVALID_PARAM = -2;			// 无效的参数
		public final static int INVALID_HANDLE = -3;		// 无效句柄
		public final static int INIT_ERROR = -4;			// 初始化错误
		public final static int PORT_BIND_FAILED = -5;		// 端口绑定失败
		public final static int INVALID_STREAM_ID = -6;		// 无效的流ID
		public final static int GENERATE_URL_FAILED = -8;	// 生成URL失败
		public final static int INVALID_URL = -10;			// 无效的URL
		public final static int NOT_ENOUGH_SPACE = -11;		// 存储空间不足
		public final static int FILE_IO_ERROR = -12;		// 文件IO错误
		public final static int ALLOC_MEMORY_FAILED = -13;	// 分配内存失败
    }
    
    public class StreamInfo {
        public int streamId = MediaCenter.INVALID_STREAM_ID;
        public String streamName = null;
        public boolean defaultStream = false;
        public int fileSize = 0;
        public int duration = 0;
    }
    
    public class MediaInfo {
        public String mediaName = null;
        public int mediaStreamCount = 0;
    }

    
    private MediaCenter() {
    }

    public static MediaCenter getInstance() {
        if (instance == null) {
            synchronized (MediaCenter.class) {
                if (instance == null) {
                    instance = new MediaCenter();
                }
            }
        }
        return instance;
    }

    static {
        try {
            System.loadLibrary("mediacenter");
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public interface HandleStateCallback {
        public void OnStateChanged(int handle, int state, int error);
    }
    
    private static HandleStateCallback mCallBackObject = null;
    
    public static HandleStateCallback getCallBackObj() {
        return mCallBackObject;
    }
    
    public void setCallback(HandleStateCallback callbackObject) {
        mCallBackObject = callbackObject;
    }

    // native routines
    public native int InitMediaCenter(String data_path, int net_state);
    public native int MediaCenterCleanup();
    public native int SetNetState(int state);
    public native int CreateMediaHandle(String url, String token);  // return handle, or INVALID_MEDIA_HANDLE if failed
    public native int DestroyMediaHandle(int handle);
    public native MediaInfo GetMediaInfo(int handle);
    public native StreamInfo[] GetStreamInfo(int handle, int count);    // if count is not suitable, return null
    public native int StartStreamService(int handle,int stream_id, int stream_mode, int service_port);
    public native int StopStreamService(int handle);
    public native int GetHandleState(int handle);
    public native int GetDownloadSpeed(int handle);
    public native int GetDownloadPercent( int handle);
    public native int CalcCanPlayTime(int handle, int time);
    public native int SetCurPlayTime(int handle, int time);
    public native String GetErrorInfo(int error);
}
