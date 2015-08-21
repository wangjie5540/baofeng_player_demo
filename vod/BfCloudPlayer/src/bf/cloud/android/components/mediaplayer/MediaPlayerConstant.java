package bf.cloud.android.components.mediaplayer;

/**
 * Created by gehuanfei on 2014/9/18.
 */
public class MediaPlayerConstant {
    public static final String P2PSERVER = "http://127.0.0.1";

    //没有网络
    public static final int NO_NETWORK = 100;
    //网络为2g/3g
    public static final int NETWORK_MOBILE = 101;
    //网络为wifi
    public static final int NETWORK_WIFI = 102;
    //取消弹窗
    public static final int POPUP_DISMISS = 103;

    //destory p2p多个任务
    public static final int P2P_DESTORY_TASK_MULTIPLE = 201;
    //destory p2p单个任务
    public static final int P2P_DESTORY_TASK_SINGLE = 202;
    //停止p2p播放服务
    public static final int P2P_STOP_PLAYBACK = 203;
    //启动p2p播放服务
    public static final int P2P_START_PLAYBACK = 204;
    //release p2p服务
    public static final int P2P_RELEASE = 205;
    //记录播放历史
    public static final int P2P_RECORD_HISTORY = 206;
    //destory p2p服务,带有callback
    public static final int P2P_DESTORY_TASK_CALLBACK = 207;
    public static final int P2P_START_PLAY_SUCCESS = 208;
    public static final int P2P_START_PLAY_FAILURE = 209;
    //继续p2p播放
    public static final int P2P_CONTINUE_PLAYBACK = 210;
    //播放器seek
    public static final int PROXY_SEEK = 300;
    //打开播放器
    public static final int PROXY_OPEN_VIDEO = 301;
    //切换剧集打开播放器
    public static final int PROXY_OPEN_VIDEO_WITH_SURFACE = 302;
    public static final int PROXY_CREATE_MEDIA_PLAYER_0 = 303;
    public static final int PROXY_CREATE_MEDIA_PLAYER_1 = 304;
    //pause状态下退出播放
    public static final int PROXY_PAUSE_QUIT = 305;
    //destory状态下退出播放
    public static final int PROXY_DESTORY_QUIT = 306;
    //开始播放
    public static final int PROXY_PLAYBACK = 307;
    //暂停播放
    public static final int PROXY_PLAY_PAUSE = 308;

    //隐藏视频未播放时的占位布局
    public static final int UI_PLACEHOLDER_HIDE = 400;
    //显示视频未播放时的占位布局
    public static final int UI_PLACEHOLDER_SHOW = 401;
    //显示媒体控制器
    public static final int UI_MEDIA_CONTROLLER_SHOW = 402;
    //隐藏媒体控制器
    public static final int UI_MEDIA_CONTROLLER_HIDE = 403;
    //显示加载状态
    public static final int UI_STATUS_CONTROLLER_SHOW = 404;
    //隐藏加载状态
    public static final int UI_STATUS_CONTROLLER_HIDE = 405;
    public static final int UI_SURFACE_FIXED_SIZE = 406;
    //seek完毕
    public static final int UI_SEEK_COMPLETE = 407;
    //释放掉surface
    public static final int UI_SURFACE_GONE = 408;
    //显示surface
    public static final int UI_SURFACE_VISIBLE = 409;
    //隐藏并显示(重建)surface
    public static final int UI_SURFACE_GONE_VISIBLE = 410;
    public static final int UI_STOP_PROGRESS = 411;
    public static final int UI_START_PROGRESS = 412;
    public static final int UI_SURFACE_INVISIBLE_VISIBLE = 413;
    public static final int START_FAILURE = 414;
    public static final int UI_P2P_START_FAILURE = 415;
    public static final int UI_P2P_INIT_FAILURE = 416;
    public static final int UI_MEDIA_START_COMPLETE = 417;
    public static final int UI_MEDIA_PAUSE_COMPLETE = 418;
    public static final int UI_SET_TITLE = 419;

    //视频段播放模式,自动、手动
    public static final int COMPLETION_AUTO = 500;
    public static final int COMPLETION_MANUAL = 501;
    
    //播放器所有内部可能的状态
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;
    public static final int STATE_RELEASE = 6;

    public static final int MP_ON_PREPARED = 600;


    //p2p初始化成功或之前已经初始化成功
    public static final int P2P_INIT_SUCCESS = 0;
    //下载失败
    public static final int P2P_INIT_ERROR_DOWNLOAD_FAILED = 0x00000002;
    //任务未启动
    public static final int P2P_INIT_ERROR_TASK_NOTSTARTED = 0x00000003;
    //非法参数
    public static final int P2P_INIT_ERROR_PARAM_INVALID = 0x00000004;
    //P2P任务不存在
    public static final int P2P_INIT_TASK_NOT_EXIST = 0x00000005;
    //P2P任务达到上限
    public static final int P2P_INIT_ERROR_TASKBUFFER_FULL = 0x00000006;
    //P2P任务已存在
    public static final int P2P_INIT_ERROR_TASK_EXIST = 0x00000007;
    //P2P任务已存在
    public static final int P2P_INIT_ERROR_TASK_CREATE = 0x80000000;
    //播放失败
    public static final int P2P_INIT_PLAY_TASK_FAILED = 0x00000009;
    //存储路径错误
    public static final int P2P_INIT_ERROR_INVALID_PATH = 0x0000000A;
    //已经初始化
    public static final int P2P_INIT_ERROR_INITED = 0x0000000B;


    public static final int TIMEOUT_PLAYBACK_VALUE = 60 * 1000;//60秒超时,切换到软解
    public static final int TIMEOUT_PLAYBACK_WHAT = 700;
}
