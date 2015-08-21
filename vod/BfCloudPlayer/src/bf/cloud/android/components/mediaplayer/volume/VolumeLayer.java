package bf.cloud.android.components.mediaplayer.volume;

import android.content.Context;
import android.media.AudioManager;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * 音量浮层
 *
 * Created by gehuanfei on 2014/9/19.
 */
public class VolumeLayer extends FrameLayout {

    private static final String TAG = VolumeLayer.class.getSimpleName();

    private static final int FADE_OUT = 0;
    private static final int DISAPPEAR = 1;

    private TextView mVolumePercentTxt;
    private int mMaxVolume;
    private boolean mIsShowing;
    private MotionEvent mLastEvent;
    private VolumeHandler mHandler;

    public VolumeLayer(Context context) {
        super(context);
        initLayer();
    }

    public VolumeLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayer();
    }

    public VolumeLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void initLayer() {
        mHandler = new VolumeHandler(this);

        View view = makeLayer();
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        addView(view, params);

        mMaxVolume = getStreamMaxVolume();
    }

    public View makeLayer() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_volume_layer"), null);
        mVolumePercentTxt = (TextView) view.findViewById(BFYResUtil.getId(getContext(), "volumePercent"));
        return view;
    }

    public void show() {
        setVisibility(View.VISIBLE);
        mIsShowing = true;

        int percent = getCurrentVolume() * 100 / mMaxVolume;
        mVolumePercentTxt.setText(percent + "%");
    }

    public void onScroll(MotionEvent e2, int displayHeight) {
        if (!mIsShowing) {
            show();
        }
        if (null != mLastEvent) {
            float offset = e2.getRawY() - mLastEvent.getRawY();
            if (ignoreIt(Math.abs(offset), displayHeight)){
            	return;
            }
            if (offset < 0) {
                incVolume();
            } else if (offset > 0) {
                decVolume();
            }
        }
        mLastEvent = MotionEvent.obtain(e2);
    }
    
    private boolean ignoreIt(float move, int whole) {
    	if (move < whole / 20)
    		return true;
		return false;
	}

    public void clearLastTouchEvent(){
    	mLastEvent = null;
    }

    public void incVolume(){
        int value = getCurrentVolume() + 1;
        if(value <= mMaxVolume){
            setVolume(value);
        }
    }
    
    public void decVolume(){
        int value = getCurrentVolume() - 1;
        if(value <= mMaxVolume && value >= 0){
            setVolume(value);
        }
    }

    public void setVolume(int value) {
        BFYLog.d(TAG, "setVolume,value=" + value);
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
        int percent = value * 100 / mMaxVolume;
        mVolumePercentTxt.setText(percent + "%");
    }

    public int getCurrentVolume() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public int getStreamMaxVolume() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public void hide(boolean fadeOut) {
    	if (fadeOut) {
    		mHandler.sendEmptyMessageDelayed(FADE_OUT, 1000);
    	} else {
    		mHandler.sendEmptyMessage(DISAPPEAR);
    	}        
    }

    private static class VolumeHandler extends BFYWeakReferenceHandler<VolumeLayer> {

        public VolumeHandler(VolumeLayer reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(VolumeLayer reference, Message msg) {
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

    ;
}
