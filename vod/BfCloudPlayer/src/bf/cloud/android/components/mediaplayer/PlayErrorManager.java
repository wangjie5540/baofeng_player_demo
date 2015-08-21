package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.modules.p2p.MediaCenter;
import bf.cloud.android.playutils.PlayTaskType;

public class PlayErrorManager {
	
	public static final int NO_ERROR = 0;     
	public static final int MEDIA_CENTER_INIT_FAILED = 1000;     // 初始化media center失败
	public static final int MEDIA_CENTER_INVALID_PARAM = 1001;   // 无效参数
	public static final int MEDIA_CENTER_INVALID_HANDLE = 1002;  // 无效句柄
	public static final int MEDIA_CENTER_INIT_ERROR = 1003;      // 初始化错误
	public static final int PORT_BIND_FAILED = 1004;		     // 端口绑定失败
	public static final int INVALID_STREAM_ID = 1005;		     // 无效的流ID
	public static final int GENERATE_URL_FAILED = 1006;		     // 生成URL失败
	public static final int INVALID_URL = 1007;		     		 // 创建任务时解析URL失败
	public static final int NOT_ENOUGH_SPACE = 1008;		     // 创建任务时磁盘空间不足
	public static final int FILE_IO_ERROR = 1009;		    	 // 创建任务时文件IO错误
	public static final int ALLOC_MEMORY_FAILED = 1010;		     // 创建任务时内存分配错误
	public static final int GET_MOVIE_INFO_FAILED = 1011;        // 从media center获取媒体信息失败
	public static final int EXOPLAYER_DECODE_FAILED = 1012;      // 调用 ExoPlayer 播放时解码失败
	public static final int SOFT_DECODE_FAILED = 1013;           // 软解失败
	public static final int NO_NETWORK = 1014;      			 // 没有网络
	public static final int MOBILE_NO_PLAY = 1015;      	     // 移动网络默认先停止播放
	public static final int MEDIA_MOVIE_INFO_NOT_FOUND = 2005;	 // 查询媒体信息失败：找不到媒体信息
	public static final int MEDIA_MOVIE_INFO_FORBIDDEN = 2008;	 // 查询媒体信息失败：无权限
	public static final int MEDIA_MOVIE_INFO_UNAUTHORIZED = 2009;// 查询媒体信息失败：未授权
	public static final int P2P_NO_DATA_SOURCE = 3006;			 // 数据源异常
	
	private final String PLAYER_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	private final String MEDIA_INFO_ERROR_SHOW_TIPS = "网络好慢啊，我都受不了了 :(";
	private final String P2P_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	private final String MEDIA_INFO_ERROR_TIPS_VOD_2005 = "亲，你确定这个视频还在吗？";
	private final String MEDIA_INFO_ERROR_TIPS_LIVE_2005 = "直播还没有开始，再等一会吧！";
	private final String MEDIA_INFO_ERROR_TIPS_2008 = "没有权限看，赶紧去要一个授权吧！";
	private final String MEDIA_INFO_ERROR_TIPS_2009 = "授权失效了，重新要一个吧！";
	private final String P2P_ERROR_TIPS_LIVE_3006 = "直播已经结束了,再见:)";
	
	private final int PLAYER_ERROR_MIN = 1000;
	private final int PLAYER_ERROR_MAX = 1999;
	private final int MEDIA_INFO_ERROR_MIN = 2000;
	private final int MEDIA_INFO_ERROR_MAX = 2999;
	private final int P2P_ERROR_MIN = 3000;
	private final int P2P_ERROR_MAX = 3999;
	
	private int mErrorCode = 0;
	
	public PlayErrorManager() {
	}
	
	public String getErrorShowTips(PlayTaskType type) {
		String tips = "";		
		if (mErrorCode >= PLAYER_ERROR_MIN && mErrorCode <= PLAYER_ERROR_MAX) {
			tips = PLAYER_ERROR_SHOW_TIPS;
		} else if (mErrorCode >= MEDIA_INFO_ERROR_MIN && mErrorCode <= MEDIA_INFO_ERROR_MAX) {
			switch (mErrorCode) {
			case MEDIA_MOVIE_INFO_NOT_FOUND: {
					if (type == PlayTaskType.VOD) {
						tips = MEDIA_INFO_ERROR_TIPS_VOD_2005;
					} else if (type == PlayTaskType.LIVE) {
						tips = MEDIA_INFO_ERROR_TIPS_LIVE_2005;
					}
					break;
				}
			case MEDIA_MOVIE_INFO_FORBIDDEN: {
					tips = MEDIA_INFO_ERROR_TIPS_2008;
					break;
				}
			case MEDIA_MOVIE_INFO_UNAUTHORIZED: {
					tips = MEDIA_INFO_ERROR_TIPS_2009;
					break;
				}
			default: {
					tips = MEDIA_INFO_ERROR_SHOW_TIPS;
					break;
				}
			}
		} else if (mErrorCode >= P2P_ERROR_MIN && mErrorCode <= P2P_ERROR_MAX) {
			switch (mErrorCode) {
			case P2P_NO_DATA_SOURCE: {
					if (type == PlayTaskType.LIVE) {
						tips = P2P_ERROR_TIPS_LIVE_3006;
					}
					break;
				}
			default: {
					tips = P2P_ERROR_SHOW_TIPS;;
					break;
				}
			}
		}
		
		return tips;
	}
	
	public void setErrorCode(int errorCode) {
		switch (errorCode) {
		case MediaCenter.NativeReturnType.UNKNOWN_ERROR:
			mErrorCode = MEDIA_CENTER_INIT_FAILED;
			break;
		case MediaCenter.NativeReturnType.INVALID_PARAM:
			mErrorCode = MEDIA_CENTER_INVALID_PARAM;
			break;
		case  MediaCenter.NativeReturnType.INVALID_HANDLE:
			mErrorCode = MEDIA_CENTER_INVALID_HANDLE;
			break;
		case  MediaCenter.NativeReturnType.INIT_ERROR:
			mErrorCode = MEDIA_CENTER_INIT_ERROR;
			break;
		case  MediaCenter.NativeReturnType.PORT_BIND_FAILED:
			mErrorCode = PORT_BIND_FAILED;
			break;
		case  MediaCenter.NativeReturnType.INVALID_STREAM_ID:
			mErrorCode = INVALID_STREAM_ID;
			break;
		case  MediaCenter.NativeReturnType.GENERATE_URL_FAILED:
			mErrorCode = GENERATE_URL_FAILED;
			break;
		case  MediaCenter.NativeReturnType.INVALID_URL:
			mErrorCode = INVALID_URL;
			break;
		case  MediaCenter.NativeReturnType.NOT_ENOUGH_SPACE:
			mErrorCode = NOT_ENOUGH_SPACE;
			break;
		case  MediaCenter.NativeReturnType.FILE_IO_ERROR:
			mErrorCode = FILE_IO_ERROR;
			break;
		case  MediaCenter.NativeReturnType.ALLOC_MEMORY_FAILED:
			mErrorCode = ALLOC_MEMORY_FAILED;
			break;
		default:
			mErrorCode = errorCode;
			break;
		}		
	}
	
	public int getErrorCode() {
		return mErrorCode;
	}
}
