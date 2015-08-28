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
    
}
