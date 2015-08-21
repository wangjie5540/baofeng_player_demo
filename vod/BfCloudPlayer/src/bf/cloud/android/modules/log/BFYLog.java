package bf.cloud.android.modules.log;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.utils.BFYFileUtils;

/**
 * 日志类
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class BFYLog {
	// 日志标签
    private final static String LOG_TAG = "BfCloudPlayer";
    // 日志输出文件名
    private final static String LOG_FILE_NAME = "BfCloudPlayer.log";
    // 日志格式，形如：[2010-01-22 13:39:1][D][com.a.c]error occured *
    private final static String LOG_ENTRY_FORMAT = "[%tF %tT][%s][%s]%s";
    
    // 日志开关
    private static boolean mDebugMode = false;

    private static PrintStream mLogStream;
    private static boolean mInitialized = false;

    /*
     * 设置是否输出日志
     */
    public static void setDebugMode(boolean debugMode) {
    	mDebugMode = debugMode;
    }
    
    public static boolean isDebugMode() {
    	return mDebugMode;
    }
    
    public static void d(String tag, String msg) {
        if (mDebugMode) {
	        tag = Thread.currentThread().getName() + ":" + tag;
	        Log.d(LOG_TAG, tag + " : " + msg);
	        write("D", tag, msg, null);
        }
    }

    public static void d(String tag, String msg, Throwable error) {
        if (mDebugMode) {
	        tag = Thread.currentThread().getName() + ":" + tag;
	        Log.d(LOG_TAG, tag + " : " + msg, error);
	        write("D", tag, msg, error);
        }
    }

    private static void write(String level, String tag, String msg, Throwable error) {
        if (!mInitialized)
            init();
        if (mLogStream == null || mLogStream.checkError()) {
            mInitialized = false;
            return;
        }
        Date now = new Date();
        try {
            mLogStream.printf(LOG_ENTRY_FORMAT, now, now, level, tag, " : " + msg);
            mLogStream.println();
            if (error != null) {
                error.printStackTrace(mLogStream);
                mLogStream.println();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static synchronized void init() {
        if (mInitialized)
            return;
        try {
            File cacheDir = getAppCacheDir();
            if (cacheDir != null) {
                File logFile = new File(cacheDir, LOG_FILE_NAME);
                logFile.createNewFile();
                if (mLogStream != null) {
                    mLogStream.close();
                }
                mLogStream = new PrintStream(new FileOutputStream(logFile, true), true);
                mInitialized = true;
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    private static File getAppCacheDir() {
        if (BFYFileUtils.isSDCardExist()) {
            File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
            File appCacheDir = new File(new File(dataDir, BFYConst.PACKAGE_NAME), "cache");
            if (!appCacheDir.exists()) {
                if (!appCacheDir.mkdirs()) {
                    return null;
                }
            }
            return appCacheDir;
        } else {
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mLogStream != null)
            mLogStream.close();
    }

}