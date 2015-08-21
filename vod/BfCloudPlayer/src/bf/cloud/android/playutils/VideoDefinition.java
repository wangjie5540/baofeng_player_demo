package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public enum VideoDefinition {
	UNKNOWN(-1),  // 未知
	FLUENT(0),	  // 流畅
	STANDARD(1),  // 标清
	HIGH(2),      // 高清
	P1080(3),     //1080P
	K2(4);        //2K
	
	private int mValue;
	
	private VideoDefinition(int value) {
		mValue = value;
	}
	
	public static VideoDefinition valueOf(int value) {
		switch (value) {
			case -1: return UNKNOWN;
			case 0:  return FLUENT;
			case 1:  return STANDARD;
			case 2:  return HIGH;
			case 3:  return P1080;
			case 4:  return K2;
			default: return null;
		}
	}
	
	public static VideoDefinition fromString(String name) {
		for (int i = -1; i <= 2; i++) {
			if (name.equalsIgnoreCase(VideoDefinition.valueOf(i).toString())) {
				return VideoDefinition.valueOf(i);
			}
		}
		return null;
	}
	
	public int value() {
		return mValue;
	}
	
	public String toString() {
		switch (valueOf(mValue)) {
			case UNKNOWN: return "";
			case FLUENT: return "流畅";
			case STANDARD: return "标清";
			case HIGH: return "高清";
			case P1080: return "1080P";
			case K2: return "2K";
			default: return "";
		}
	}
}
