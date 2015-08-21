package bf.cloud.android.components.mediaplayer.playprogress;

import android.content.Context;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Locale;

import bf.cloud.android.components.mediaplayer.Controller;
import bf.cloud.android.components.mediaplayer.MediaController;
import bf.cloud.android.utils.BFYResUtil;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * 播放进度控制器
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class PlayProgressController extends Controller {

    private final String TAG = PlayProgressController.class.getSimpleName();

    private static final int SHOW_PROGRESS = 1;

    private Context mContext;
    //进度
	private ProgressBar mProgress;
    //视频总时长,当前播放时长
    private TextView mEndTime, mCurrentTime;
    private MediaController mMediaController;
    private MediaController.MediaPlayerControl mPlayer;
    private boolean mDragging;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private DecimalFormat mDecimalFormat;

    private PlayProgressHandler mHandler;

    public PlayProgressController(Context context) {
        super(context);
        init(context);
    }

    public PlayProgressController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayProgressController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void pause(){
        if(mPlayer.isPlaying()){
            mPlayer.pause();
        }
    }

    private void init(Context context) {
       BFYLog.d(TAG, "init");
        mContext = context;

        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        removeAllViews();
        View v = makeProgressView();
        addView(v, frameParams);

        mDecimalFormat = new DecimalFormat();
        mDecimalFormat.applyPattern("0.0");

        mHandler = new PlayProgressHandler(this);
    }

    private View makeProgressView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_play_progress_controller"), null);
        initProgressView(v);
        return v;
    }

    private void initProgressView(View v) {
        mProgress = (ProgressBar) v.findViewById(BFYResUtil.getId(getContext(), "mediacontroller_progress"));
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "time"));
        mCurrentTime = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "time_current"));

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    public void show() {
        BFYLog.d(TAG, "show");
        startProgress();
        if (!mDragging) {
            setProgress();
        }
        super.show();
    }

    @Override
    public Animation getShowAnimation() {
        return new TranslateAnimation(0, 0, -1, 1);
    }

    public void hide() {
        BFYLog.d(TAG, "hide");
        stopProgress();
        super.hide();
    }

    @Override
    public Animation getHideAnimation() {
        return new TranslateAnimation(0, 0, 1, -1);
    }

    public int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int duration = mPlayer.getDuration();
        int position = mPlayer.getCurrentPosition();
        if (duration != 0 && position > duration) {
            position = duration;
        }
        if (mProgress != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }

        return position;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mProgress.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        BFYLog.d(TAG, "onInterceptTouchEvent");
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            BFYLog.d(TAG, "onInterceptTouchEvent,ACTION_UP");
            mMediaController.show();
        } else {
            mMediaController.show(3600000);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        BFYLog.d(TAG, "onTouchEvent");
        return super.onTouchEvent(ev);
    }

    public void stopProgress() {
        BFYLog.d(TAG, "stopProgress");
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    public void startProgress() {
        BFYLog.d(TAG, "startProgress");
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    public void updateProgress(int current, int duration) {
        setCurrentTime(current, duration);
    }

    private static class PlayProgressHandler extends BFYWeakReferenceHandler<PlayProgressController> {
        public PlayProgressHandler(PlayProgressController reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(PlayProgressController reference, Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS:
                    if (!reference.mDragging) {
                        reference.setProgress();
                        if (reference.getVisibility() == View.VISIBLE && reference.mPlayer.isPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000);
                        }
                    }
                    break;
            }
        }
    }

    public void reset() {
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
    }

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            BFYLog.d(TAG, "onStartTrackingTouch");
            mDragging = true;
            mMediaController.show(3600000);
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            long duration = mPlayer.getDuration();
            setCurrentTime(progress, duration);
        }

        public void onStopTrackingTouch(SeekBar bar) {
            BFYLog.d(TAG, "onStopTrackingTouch");
            long duration = mPlayer.getDuration();
            long newposition = (duration * bar.getProgress()) / 1000L;
            if (newposition >= duration) {
            	newposition = duration - 50;
            }
            mPlayer.seekTo((int) newposition);

            mDragging = false;

            mMediaController.show();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    private void setCurrentTime(int progress, long duration) {
        long newposition = (duration * progress) / 1000L;
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime((int) newposition));
        }
    }

    public void setMediaController(MediaController controller) {
        this.mMediaController = controller;
        if (null != controller) {
            mPlayer = controller.getMediaPlayer();
        }
    }

    public interface OnPlayProgressChangeListener {
        void onStart(PlayProgressController controller);

        void onProgressChanged(PlayProgressController controller, int progress, boolean fromuser);

        void onStop(PlayProgressController controller);
    }
}
