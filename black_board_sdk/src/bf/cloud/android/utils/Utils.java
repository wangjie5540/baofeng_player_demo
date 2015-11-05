package bf.cloud.android.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

public class Utils {

	public static String getAppDataPath(Context context) {
		return context.getFilesDir() + File.separator;
	}

	public static String getSdCardPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				+ File.separator;
	}

	public static boolean isSdCardAvailable() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	public static boolean fileExists(String pathName) {
		return (new File(pathName)).exists();
	}

	public static boolean deleteFile(String pathName) {
		return (new File(pathName)).delete();
	}

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	public static String pathWithSlash(String path) {
		final char SLASH = File.separatorChar;
		String result = path;

		if (result.isEmpty()) {
			result = String.valueOf(SLASH);
		} else {
			if (result.charAt(result.length() - 1) != SLASH) {
				result += SLASH;
			}
		}

		return result;
	}

	public static String pathWithoutSlash(String path) {
		final char SLASH = File.separatorChar;
		String result = path;

		if (!result.isEmpty()) {
			if (result.charAt(result.length() - 1) == SLASH) {
				result = result.substring(0, result.length() - 1);
			}
		}

		return result;
	}

	public static void copyAssetsToSdCard(Context context,
			String assetsSubPath, String sdCardPath) {
		try {
			assetsSubPath = pathWithoutSlash(assetsSubPath);
			sdCardPath = pathWithoutSlash(sdCardPath);

			String fileNames[] = context.getAssets().list(assetsSubPath);
			if (fileNames.length > 0) { // if is directory
				File file = new File(sdCardPath);
				file.mkdirs();
				for (String fileName : fileNames) {
					copyAssetsToSdCard(context, assetsSubPath + "/" + fileName,
							sdCardPath + "/" + fileName);
				}
			} else { // if is file
				InputStream is = context.getAssets().open(assetsSubPath);
				FileOutputStream os = new FileOutputStream(new File(sdCardPath));

				byte[] buffer = new byte[1024];
				int byteCount = 0;

				while ((byteCount = is.read(buffer)) != -1) {
					os.write(buffer, 0, byteCount);
				}

				os.flush();
				is.close();
				os.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取屏幕亮度
	 * 
	 * @return
	 */
	public static int getBrightness(Activity act) {
		try {
			return Settings.System.getInt(act.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (Settings.SettingNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	/**
     * 是否开启了自动亮度调节
     *
     * @param act
     * @return
     */
    public static boolean isAutoBrightness(Activity act) {
        boolean automicBrightness = false;
        ContentResolver aContentResolver = act.getContentResolver();
        try {
            automicBrightness = Settings.System.getInt(aContentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return automicBrightness;
    }
    /**
     * 调节屏幕亮度
     * @param act
     * @param value
     */
    public static void effectBrightness(Activity act, int value) {
        ContentResolver resolver = act.getContentResolver();
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value);
        //保存修改
        Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        resolver.notifyChange(uri, null);
//        Window localWindow = act.getWindow();
//        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
//        float f = value / mMaxBrightness;
//        localLayoutParams.screenBrightness = f;
//        localWindow.setAttributes(localLayoutParams);
    }
    
    /**
     * 停止自动亮度调节
     */
    public static void stopAutoBrightness(Activity act) {
    	ContentResolver contentResolver = act.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
    
    /**
     * 开启亮度自动调节
     */
    public static void startAutoBrightness(Activity act) {
        ContentResolver contentResolver = act.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        //保存修改
        Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        contentResolver.notifyChange(uri, null);
    }

}
