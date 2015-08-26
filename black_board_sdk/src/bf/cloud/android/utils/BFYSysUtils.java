package bf.cloud.android.utils;

import android.os.Build;
import android.os.Environment;

/**
 * Created by gehuanfei on 2014-9-21.
 */
public class BFYSysUtils {

    public static boolean isExternalSDExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 判断当前系统版本是否支持 ExoPlayer
     */
    public static boolean isExoPlayerUsable() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);  // >= android 4.1
    }

    /**
     * TODO 
     */
    public static String getDeviceId() {
    	return "";
    }
    
    /**
     * 从VK中取得指定的字段值 
     */
    public static String getValueFromVk(String vk, String key) {
    	String result = "";
    	String[] items = vk.split("&");
    	for (String item: items) {
    		String[] parts = item.split("=");
    		if (parts.length == 2) {
    			if (parts[0].equalsIgnoreCase(key)) {
    				result = parts[1];
    				break;
    			}
    		}
    	}
    	return result;
    }
    
    /**
     * 从VK中取得uid 
     */
    public static String getUidFromVk(String vk) {
    	return getValueFromVk(vk, "uid");
    }

    /**
     * 从VK中取得fid 
     */
    public static String getFidFromVk(String vk) {
    	return getValueFromVk(vk, "fid");
    }

}
