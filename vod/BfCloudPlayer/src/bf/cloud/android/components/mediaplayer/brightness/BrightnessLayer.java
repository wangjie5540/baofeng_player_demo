package bf.cloud.android.components.mediaplayer.brightness;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * 屏幕亮度浮层
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class BrightnessLayer extends FrameLayout {

    private static final String TAG = BrightnessLayer.class.getSimpleName();

    private static final int FADE_OUT = 0;
    private static final int DISAPPEAR = 1;

    //最大亮度
    private static final float mMaxBrightness = 255f;
    //最小亮度
    private final float mMinBrightness = 25f;
    //调节亮度增量
    private final int mIncrement = 10;
    //初始亮度
    private static int mInitialBrightness;
    //初始时是否是自动调节亮度
    private static boolean mAutoBrightness;

    private TextView mBrightnessPercent;
    private boolean mIsShowing;
    private MotionEvent mLastEvent;
    private BrightnessHandler mHandler;

    public BrightnessLayer(Context context) {
        super(context);
        initLayer();
    }

    public BrightnessLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayer();
    }

    public BrightnessLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void initLayer() {
        mHandler = new BrightnessHandler(this);

        View view = makeLayer();
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        addView(view, params);

        Activity act = (Activity) getContext();
        mInitialBrightness = getBrightness(act);
        mAutoBrightness = isAutoBrightness(act);
    }

    public View makeLayer() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_brightness_layer"), null);
        mBrightnessPercent = (TextView) view.findViewById(BFYResUtil.getId(getContext(), "brightnessPercent"));
        return view;
    }

    public void show() {
        int percent = (int) (getBrightness((Activity) getContext()) * 100 / mMaxBrightness);
        mBrightnessPercent.setText(percent + "%");
        setVisibility(View.VISIBLE);
        mIsShowing = true;
    }

    public void onScroll(MotionEvent e2, int displayHeight) {
        if (!mIsShowing) {
            show();
        }
        if (null != mLastEvent) {
            float offset = e2.getRawY() - mLastEvent.getRawY();
            int value = getBrightness((Activity) getContext());
            BFYLog.d(TAG, "onScroll,current brightness=" + value);
            if (offset < 0) {
                value += Math.abs((int)(offset * (mMaxBrightness-mMinBrightness) / displayHeight)) ;
            } else if (offset > 0) {
                value -= Math.abs((int)(offset * (mMaxBrightness-mMinBrightness) / displayHeight));
            }
            if (value < mMinBrightness) {
                value = (int) mMinBrightness;
            } else if (value > mMaxBrightness) {
                value = (int) mMaxBrightness;
            }
            setBrightness((Activity) getContext(), value);
        }
        mLastEvent = MotionEvent.obtain(e2);
    }
    
    public void clearLastTouchEvent(){
    	mLastEvent = null;
    }

    public void hide(boolean fadeOut) {
    	if (fadeOut) {
    		mHandler.sendEmptyMessageDelayed(FADE_OUT, 3000);
    	} else {
    		mHandler.sendEmptyMessage(DISAPPEAR);
    	}        
    }

    /**
     * 设置屏幕亮度
     *
     * @param value
     */
    public void setBrightness(Activity act, int value) {
        BFYLog.d(TAG, "setBrightness,value=" + value);
        if (isAutoBrightness(act)) {
            stopAutoBrightness(act);
        }
        effectBrightness(act, value);

        int percent = (int) (value * 100 / mMaxBrightness);
        mBrightnessPercent.setText(percent + "%");
    }

    private static void effectBrightness(Activity act, int value) {
        ContentResolver resolver = act.getContentResolver();
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value);
        //保存修改
        Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        resolver.notifyChange(uri, null);
        Window localWindow = act.getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        float f = value / mMaxBrightness;
        localLayoutParams.screenBrightness = f;
        localWindow.setAttributes(localLayoutParams);
    }

    /**
     * 获取屏幕亮度
     *
     * @return
     */
    public static int getBrightness(Activity act) {
        try {
            return Settings.System.getInt(act.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
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
     * 停止自动亮度调节
     */
    public static void stopAutoBrightness(Activity act) {
        BFYLog.d(TAG, "stopAutoBrightness");
        Settings.System.putInt(act.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * 开启亮度自动调节
     */
    public static void startAutoBrightness(Activity act) {
        BFYLog.d(TAG, "startAutoBrightness start");
        if (!mAutoBrightness) return;

        effectBrightness(act, mInitialBrightness);
        ContentResolver contentResolver = act.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        //保存修改
        Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        contentResolver.notifyChange(uri, null);
        BFYLog.d(TAG, "startAutoBrightness end");
    }

    private static class BrightnessHandler extends BFYWeakReferenceHandler<BrightnessLayer> {

        public BrightnessHandler(BrightnessLayer reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(BrightnessLayer reference, Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    reference.setVisibility(View.GONE);
                    reference.mIsShowing = false;
                    reference.mLastEvent = null;
                    break;
                case DISAPPEAR:
                	reference.setVisibility(View.GONE);
                    reference.mIsShowing = false;
                    reference.mLastEvent = null;
                    break;
            }
        }
    }
}
