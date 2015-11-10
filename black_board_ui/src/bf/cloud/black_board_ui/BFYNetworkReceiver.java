package bf.cloud.black_board_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author wang
 *
 */
public final class BFYNetworkReceiver extends BroadcastReceiver{
    private final static String TAG = BFYNetworkReceiver.class.getSimpleName();
    public final static int NET_STATE_CONNECTION_NONE = 0;
    public final static int NET_STATE_CONNECTION_WIFI = 1;
    public final static int NET_STATE_CONNECTION_MOBILE = 2;
    public final static int NET_STATE_CONNECTION_ETHERNET = 3;
    private static int mCurrentNetState = NET_STATE_CONNECTION_NONE;
    private static int mLastNetState = NET_STATE_CONNECTION_NONE;
    private static BFYNetworkReceiver mInstance = null;
    private static Context mContext = null;
    private NetStateChangedListener mListener = null;
    
    public static BFYNetworkReceiver getInstance(Context context) {
    	if (context == null){
    		throw new NullPointerException("context is invailid");
    	}
    	mContext = context;
        if (mInstance == null) {
        	mInstance = new BFYNetworkReceiver();
        }
        updateNetState();
		return mInstance;
    }
    
    private static void updateNetState(){
    	if (isWifiEnabled(mContext)){
        	mCurrentNetState = NET_STATE_CONNECTION_WIFI;
        } else if (isMobileEnabled(mContext)){
        	mCurrentNetState = NET_STATE_CONNECTION_MOBILE;
        } else if (isEthernetEnabled(mContext)){
        	mCurrentNetState = NET_STATE_CONNECTION_ETHERNET;
        } else {
        	mCurrentNetState = NET_STATE_CONNECTION_NONE;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.d(TAG, "onReceive");
    	String action = intent.getAction();
    	if (!action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION))
    		return;
		updateNetState();
		if (mLastNetState != mCurrentNetState)
    		mLastNetState = mCurrentNetState;
		if (mListener != null)
			mListener.onChanged(mLastNetState, mCurrentNetState);
    }

    public static boolean isWifiEnabled(Context context) {
        ConnectivityManager connectMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);        
        NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetInfo != null && wifiNetInfo.isConnected()) {
        	return true;
        } else {
        	return false;
        }
    }
    
    public static boolean isEthernetEnabled(Context context) {
        ConnectivityManager connectMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);        
        NetworkInfo ethernetNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (ethernetNetInfo != null && ethernetNetInfo.isConnected()) {
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
    
    public int getCurrentNetworkState(){
    	return mCurrentNetState;
    }
    
    public interface NetStateChangedListener{
    	void onChanged(int lastNetState, int CurrentNetState);
    }
    
    public void registNetStateChangedListener(NetStateChangedListener listener){
    	mListener = listener;
    }
}	