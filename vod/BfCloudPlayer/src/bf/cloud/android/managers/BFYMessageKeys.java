package bf.cloud.android.managers;

/**
 * 消息key管理类
 *
 * Created by gehuanfei on 2014/9/21.
 */
public class BFYMessageKeys {

    /**
     * 延时发送刷新图文详情页消息时间
     */
    public static final int MESSAGE_POST_DAILY_TIME = 300;
    /**
     * 显示图文详情页
     */
    public static final int MESSAGE_IMAGE_DETAIL_SHOW = 10001;
    /**
     * 隐藏图文详情页
     */
    public static final int MESSAGE_IMAGE_DETAIL_HIDE = 10002;
    /**
     * 资源收藏成功提示
     */
    public static final int MESSAGE_RESOUCE_LIKE_SUCCESS= 10004;
    /**
     *设置页检查更新成功提示
     */
    public static final int MESSAGE_CONFIG_UPDATE_SUCCESS= 10005;
    /**
     * 设置页 检查更新提示：无更新
     */
    public static final int MESSAGE_CONFIG_UPDATE_SUCCESS_NEWEST= 10006;
    /**
     * 设置页 检查更新提示：失败
     */
    public static final int MESSAGE_CONFIG_UPDATE_FAILURE= 10007;
    /**
     * 设置页 检查更无网络提示
     */
    public static final int MESSAGE_CONFIG_UPDATE_NO_NETNETORK= 10008;
    /**
     * 设置页 隐藏新版本提示
     */
    public static final int MESSAGE_CONFIG_UPDATE_HIDE_NEWVIEWSION_SIGN= 10009;
    /**
     * 主页 检查更新成功提示
     */
    public static final int MESSAGE_MAIN_UPDATE_SUCCESS = 10010;
    /**
     *安装apk
     */
    public static final int MESSAGE_UPDATE_NEW_VERSION_INSTALLATION= 10011;
    /**
     * 下载新版本apk失败
     */
    public static final int MESSAGE_DOWNLOAD_APK_FAILURE= 10012;
    /**
     * 显示新版本
     */
    public static final int MESSAGE_UPDATE_NEW_VERSION= 10013;
    /**
     * 侧滑菜单，查询数据成功后更新UI的消息类型
     */
    public static final int MESSAGE_MENU_FETCH_DATA_SUCCESS = 10031;
    public static final int MESSAGE_MENU_FETCH_DATA_FAILURE = 10032;
    /**
     * P2P Stream Start Success
     */
    public static final int MESSAGE_P2P_STREAM_START_SUCCESS = 10041;
    /**
     * P2P Stream Start Fail
     */
    public static final int MESSAGE_P2P_STREAM_START_FAILURE = 10042;
    public static final int MESSAGE_P2P_INIT_ERROR = 10043;
    public static final int MESSAGE_P2P_INIT_NO_NETWORK_ERROR = 10044;
    public static final int MESSAGE_P2P_INIT_MOBILE_NETWORK = 10045;    
    /**
     * Change decode mode 
     */
    public static final int MESSAGE_CHANGE_DECODE_MODE = 10051;
}
