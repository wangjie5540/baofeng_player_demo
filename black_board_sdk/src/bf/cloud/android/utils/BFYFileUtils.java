package bf.cloud.android.utils;

import android.os.Environment;

/**
 * 文件操作工具类
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class BFYFileUtils {

    /**
     * 检测SD卡是否存在
     *
     * @return
     */
    public static boolean isSDCardExist() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }
}