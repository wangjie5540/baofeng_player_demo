package bf.cloud.android.components.mediaplayer.playprogress;

import android.content.Context;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import bf.cloud.android.components.mediaplayer.MediaController;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * 播放进度浮层,横向滑动屏幕时显示
 *
 * @author gehuanfei
 */
public class ProgressLayer extends FrameLayout {
    private final String TAG = ProgressLayer.class.getSimpleName();

    private static final int FADE_OUT = 0;
    private static final int DISAPPEAR = 1;
    private boolean mIsShowing;
    private MediaController mMediaController;
    private MediaController.MediaPlayerControl mPlayer;
    private ProgressLayerHandler mHandler;

    public ProgressLayer(Context context) {
        super(context);
    }

    public ProgressLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayer();
    }

    public ProgressLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        BFYLog.d(TAG, "onInterceptTouchEvent");
        if (mIsShowing) return true;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        BFYLog.d(TAG, "onTouchEvent");
        if (mIsShowing) return true;
        return super.onTouchEvent(ev);
    }

    private void initLayer() {
        mHandler = new ProgressLayerHandler(this);

        View view = makeLayer();
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(view, params);
    }

    public View makeLayer() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_progress_layer"), null);
        return view;
    }

    public void show() {
        BFYLog.d(TAG, "show");
        if (!mIsShowing) {
            mIsShowing = true;
            setVisibility(View.VISIBLE);
        }
    }

    public void hide(boolean fadeOut) {
        BFYLog.d(TAG, "hide");
        //mIsShowing = false;
        if (fadeOut) {
        	mHandler.sendEmptyMessageDelayed(FADE_OUT, 1500);
        } else {
        	mHandler.sendEmptyMessage(DISAPPEAR);
        }
    }

    public void setMediaController(MediaController controller) {
        this.mMediaController = controller;
        if (null != controller) {
            mPlayer = controller.getMediaPlayer();
        }
    }

    private static class ProgressLayerHandler extends BFYWeakReferenceHandler<ProgressLayer> {

        public ProgressLayerHandler(ProgressLayer reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(ProgressLayer reference, Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    reference.setVisibility(View.GONE);
                    reference.mIsShowing = false;
                    break;
                case DISAPPEAR:
                    reference.setVisibility(View.GONE);
                    reference.mIsShowing = false;
                    break;
            }
        }
    }

    public boolean isShowing() {
        return mIsShowing ? true : false;
    }
}
