package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public enum PlayTaskType {
	VOD(1),   // 点播
	LIVE(2);  // 直播
	
	private int mValue;
	
	private PlayTaskType(int value) {
		mValue = value;
	}
	
	public static PlayTaskType valueOf(int value) {
		switch (value) {
			case 0: return VOD;
			case 1: return LIVE;
			default: return null;
		}
	}
	
	public int value() {
		return mValue;
	}
	
	public String toString() {
		switch (valueOf(mValue)) {
			case VOD: return "VOD";
			case LIVE: return "LIVE";
			default: return "unknown";
		}
	}
}
