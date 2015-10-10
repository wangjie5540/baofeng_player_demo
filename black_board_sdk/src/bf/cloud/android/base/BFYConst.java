package bf.cloud.android.base;

import bf.cloud.android.playutils.DecodeMode;

public class BFYConst {
	
    public static final String PACKAGE_NAME = "bf.cloud.android";
    public static final String SDK_VERSION = "1.2.4";
    // 默认解码方式
    public static final DecodeMode DEFAULT_DECODE_MODE = DecodeMode.AUTO;
    // 播放画面区域默认长宽比 (height / width) 
    public static final double DEFAULT_VIDEO_VIEW_ASPECT_RATIO = ((double)9 / 16);
    //p2p默认端口
    public static final int DEFAULT_P2P_PORT = 8080;
    public static final String P2PSERVER = "http://127.0.0.1";
    
    //error codes below
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
	
	//error tips below
	public static final String PLAYER_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	public static final String MEDIA_INFO_ERROR_SHOW_TIPS = "网络好慢啊，我都受不了了 :(";
	public static final String P2P_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	public static final String MEDIA_INFO_ERROR_TIPS_VOD_2005 = "亲，你确定这个视频还在吗？";
	public static final String MEDIA_INFO_ERROR_TIPS_LIVE_2005 = "直播还没有开始，再等一会吧！";
	public static final String MEDIA_INFO_ERROR_TIPS_2008 = "没有权限看，赶紧去要一个授权吧！";
	public static final String MEDIA_INFO_ERROR_TIPS_2009 = "授权失效了，重新要一个吧！";
	public static final String P2P_ERROR_TIPS_LIVE_3006 = "直播已经结束了,再见:)";
	
	//error code below
	public static final int PLAYER_ERROR_MIN = 1000;
	public static final int PLAYER_ERROR_MAX = 1999;
	public static final int MEDIA_INFO_ERROR_MIN = 2000;
	public static final int MEDIA_INFO_ERROR_MAX = 2999;
	public static final int P2P_ERROR_MIN = 3000;
	public static final int P2P_ERROR_MAX = 3999;
}
