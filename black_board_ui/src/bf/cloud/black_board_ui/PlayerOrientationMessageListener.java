package bf.cloud.black_board_ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

public class PlayerOrientationMessageListener {
	private final static String TAG = PlayerOrientationMessageListener.class
			.getSimpleName();
	public static final int ORIENTATION_LIE = -1;// 平放
	public static final int ORIENTATION_TOP = 0;// 顶部向上
	public static final int ORIENTATION_LEFT = 1;// 左侧向上
	public static final int ORIENTATION_BOTTOM = 2;// 顶部向上
	public static final int ORIENTATION_RIGHT = 3;// 右侧向上

	private int mCurrentOrigentation = -1;// 当前方向
	private int mLastOrientation = -1;// 上一次方向
	private Context mContext = null;
	private BFMediaPlayerControllerBase mController = null;
	private OrientationEventListener mOrientationEventListener = null;

	public PlayerOrientationMessageListener(Context context,
			BFMediaPlayerControllerBase controller) {
		mContext = context;
		mController = controller;
		init();
	}

	public PlayerOrientationMessageListener(Context context, int rate,
			BFMediaPlayerControllerBase controller) {
		mContext = context;
		mController = controller;
		init();
	}

	private void init() {
		if (mContext == null) {
			Log.d(TAG, "Context is null, it is invailid");
			return;
		}
	}

	/**
	 * 注册屏幕旋转的监听器.
	 */
	public void start() {
		Log.d(TAG, "registerSensor");
		if (null == mOrientationEventListener) {
			mOrientationEventListener = new BFOrientationEventListener(mContext);
		}
		boolean canDetect = mOrientationEventListener.canDetectOrientation();
		Log.d(TAG, "registerSensor,canDetect=" + canDetect);
		if (canDetect) {
			mOrientationEventListener.enable();
		}
	}

	/**
	 * 注销屏幕旋转监听器
	 */
	public void stop() {
		Log.d(TAG, "unRegisterSensor");
		if (mOrientationEventListener != null) {
			mOrientationEventListener.disable();
			mOrientationEventListener = null;
		}
	}
	
	public int getCurrentOrigentation(){
		return mCurrentOrigentation;
	}
	
	public int getLastOrientation(){
		return mLastOrientation;
	}

	private class BFOrientationEventListener extends OrientationEventListener {

		public BFOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
				Log.d(TAG,
						"PlayerOrientationEventListener,onOrientationChanged,ORIENTATION_UNKNOWN");
				// 平放
				mCurrentOrigentation = ORIENTATION_LIE;
			} else if ((orientation >= 0 && orientation < 10)
					|| (orientation >= 350 && orientation < 360)) {
				// 顶部向上
				mCurrentOrigentation = ORIENTATION_TOP;
			} else if (orientation >= 80 && orientation < 100) {
				// 左边向上
				mCurrentOrigentation = ORIENTATION_LEFT;
			} else if (orientation >= 170 && orientation < 190) {
				// 底部向上
				mCurrentOrigentation = ORIENTATION_BOTTOM;
			} else if (orientation >= 260 && orientation < 280) {
				// 右边向上
				mCurrentOrigentation = ORIENTATION_RIGHT;
			}

			if (mLastOrientation != mCurrentOrigentation) {
				Log.d(TAG,
						"PlayerOrientationEventListener,onOrientationChanged,mCurrentOrigentation="
								+ mCurrentOrigentation + ",mLastOrientation="
								+ mLastOrientation);
				mController
						.removeMessage(BFMediaPlayerControllerBase.MSG_ADJUST_ORIENTATION);
				Message msg = new Message();
				msg.what = BFMediaPlayerControllerBase.MSG_ADJUST_ORIENTATION;
				msg.arg1 = mCurrentOrigentation;
				msg.arg2 = mLastOrientation;
				mController.sendMessageDelayed(msg, 100);
				mLastOrientation = mCurrentOrigentation;
			}
		}

	}

}
