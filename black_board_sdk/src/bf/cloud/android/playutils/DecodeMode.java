package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public enum DecodeMode {
	AUTO(0),  // 根据系统版本自动选择 ExoPlayer 硬解或 ffmpeg 软解
	SOFT(1);  // 用 ffmpeg 进行软解
	
	private int mValue;
	
	private DecodeMode(int value) {
		mValue = value;
	}
	
	public static DecodeMode valueOf(int value) {
		switch (value) {
			case 0: return AUTO;
			case 1: return SOFT;
			default: return null;
		}
	}
	
	public int value() {
		return mValue;
	}
	
	public String toString() {
		switch (valueOf(mValue)) {
			case AUTO: return "AUTO";
			case SOFT: return "SOFT";
			default: return "unknown";
		}
	}
}
