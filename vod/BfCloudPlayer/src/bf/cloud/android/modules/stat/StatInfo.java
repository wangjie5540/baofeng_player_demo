package bf.cloud.android.modules.stat;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.utils.BFYSysUtils;

public class StatInfo {
	
	private final static String REPORT_DOMAIN = "http://nclog.baofengcloud.com";

	// 基本信息
	public int platform;                // 平台 (为1)
	public int os;                      // 操作系统 (0:未知, 1:Windows, 10:Android, 11:iOS)
	public String sdkVersion;           // SDK版本号
	public String deviceId;             // 设备唯一ID
	public String gcid;                 // 影片文件标识
	public String userId;               // 用户ID
	public int decodeMode;              // 解码方式 (0:软解, 1:硬解)
	public int streamMode;              // 输出流类型 (0:MP4, 1:HLS)
	
	// 统计信息
	public int firstBufferTime;         // 初次缓冲时长 (ms)
	public boolean firstBufferSuccess;  // 首缓冲后是否成功播放
	public int breakCount;              // 卡断次数
	public int errorCode;               // 错误码

	public StatInfo() {
		init();
	}
	
	public void init() {
		platform = 1;
		os = 10;
		sdkVersion = BFYConst.SDK_VERSION;
		deviceId = BFYSysUtils.getDeviceId();
		gcid = "";
		userId = "";
		decodeMode = 1;
		streamMode = 1;
		
		firstBufferTime = 0;
		firstBufferSuccess = false;
		breakCount = 0;
		errorCode = 0;
	}

	// 点播播放体验上报地址
	public String makeVodExpUrl() {
		return REPORT_DOMAIN + "/xplayfeel.php?" + getBaseUrl() +
			String.format("&fbuftm=%d&breaktms=%d&errcode=%d",
			firstBufferTime, breakCount, errorCode);
	}
	
	// 点播播放过程上报地址
	public String makeVodProUrl() {
		return REPORT_DOMAIN + "/xplay.php?" + getBaseUrl() +
			String.format("&plysflg=%d&plystm=%d",
			(firstBufferSuccess?1:0), firstBufferTime);
	}
	
	// 直播播放体验上报地址
	public String makeLiveExpUrl() {
		return REPORT_DOMAIN + "/xliveplayfeel.php?" + getBaseUrl() +
			String.format("&fbuftm=%d&breaktms=%d&errcode=%d",
			firstBufferTime, breakCount, errorCode);
	}
	
	// 直播播放过程上报地址
	public String makeLiveProUrl() {
		return REPORT_DOMAIN + "/xliveplay.php?" + getBaseUrl() +
			String.format("&plysflg=%d&plystm=%d",
			(firstBufferSuccess?1:0), firstBufferTime);
	}
	
	private String getBaseUrl() {
		return String.format("pf=%d&os=%d&ver=%s&uid=%s&gcid=%s&user=%s&dcode=%d&pmode=%d",
			platform, os, sdkVersion, deviceId, gcid, userId, decodeMode, streamMode);
	}
	
}
