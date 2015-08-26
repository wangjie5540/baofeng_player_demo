package bf.cloud.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import bf.cloud.android.base.BFYEventBus;
import bf.cloud.android.components.BFYNetworkStatusData;
import bf.cloud.android.events.BFYNetworkStatusReportEvent;
import bf.cloud.android.modules.log.BFYLog;

/**
 * Created by gehuanfei on 2015/5/15.
 */

public class BFYNetworkUtil extends BroadcastReceiver{
	
    private final static String TAG = "BFYNetworkUtil";
    private static int mCode = BFYNetworkStatusData.NETWORK_CONNECTION_NONE;
    private static BFYNetworkUtil mInstance = null;
    
    public static BFYNetworkUtil getInstance() {
        if (mInstance == null) {
        	mInstance = new BFYNetworkUtil();
        }
        return mInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //mCode = intent.getIntExtra("status", BFYNetworkStatusData.NETWORK_CONNECTION_NONE);
    	int code = BFYNetworkStatusData.NETWORK_CONNECTION_NONE;
    	if (isWifiEnabled(context)) {
    		code = BFYNetworkStatusData.NETWORK_CONNECTION_WIFI;
    	} else if (isMobileEnabled(context)) {
    		code = BFYNetworkStatusData.NETWORK_CONNECTION_MOBILE;
    	} 
    	
        BFYLog.d(TAG, "onReceive,network util old status is " + mCode + "new status is " + code);
        if (code != mCode) {
        	mCode = code;
        	sendEvent();
        }        
    }

    private void sendEvent() {
        BFYEventBus.getInstance().post(
        		new BFYNetworkStatusReportEvent(new BFYNetworkStatusData(mCode)));
    }
    
    public static boolean isWifiEnabled(Context context) {
        ConnectivityManager connectMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);        
        NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetInfo != null && wifiNetInfo.isConnected()) {
        	//mCode = BFYNetworkStatusData.NETWORK_CONNECTION_WIFI;
        	return true;
        } else {
        	return false;
        }
    }
    
    /**
     * 判断当前网络是否是移动数据或其他（非wifi）
     */
    public static boolean isMobileEnabled(Context context) {    	
        ConnectivityManager connectMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo mobNetInfo = connectMgr.getActiveNetworkInfo();
        NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        //boolean isMobEnabled = (mobNetInfo != null && mobNetInfo.isConnected() && mobNetInfo.getType() != ConnectivityManager.TYPE_WIFI);
        boolean isMobEnabled = (mobNetInfo != null && mobNetInfo.isConnected());
        if (isMobEnabled) {
        	//mCode = BFYNetworkStatusData.NETWORK_CONNECTION_MOBILE;
        	return true;
        } else {
        	return false;
        }        
    }
    
    /**
     * 判断有网无网络
     *
     * @param context
     * @return
     */
    public static boolean hasNetwork(Context context) {
    	BFYLog.d(TAG, "has network " + mCode);
        boolean mobEnabled = isMobileEnabled(context);
        boolean wifiEnabled = isWifiEnabled(context);
        if (mobEnabled || wifiEnabled) {
        	return true;
        } else {
        	mCode = BFYNetworkStatusData.NETWORK_CONNECTION_NONE;
        	return false;
        }        
    }
    
    public void setNetworkCode(int code) {
    	mCode = code;
    }
    
    public static int getNetworkCode() {
    	return mCode;
    }
}	