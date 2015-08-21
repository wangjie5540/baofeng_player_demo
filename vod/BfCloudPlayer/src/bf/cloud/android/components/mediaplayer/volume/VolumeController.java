package bf.cloud.android.components.mediaplayer.volume;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import bf.cloud.android.components.mediaplayer.Controller;
import bf.cloud.android.components.mediaplayer.MediaController;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;

/**
 * 音量控制器,包含音量调节条、静音按钮
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class VolumeController extends Controller {

    private final String TAG = VolumeController.class.getSimpleName();

    private Context mContext;
    /**
     * 媒体播放控制器
     */
    private MediaController mMediaController;
    /**
     * 视频播放器
     */
    private MediaController.MediaPlayerControl mPlayer;
    /**
     * 音量条
     */
    private ProgressBar mVolumeBar;
    /**
     * 静音按钮
     */
    private ImageButton mSilenceButton;
    private OnVolumeChangedListener mOnVolumeChangedListener;
    private boolean mDragging;
    private int lastVolume;
    //是否静音
    private boolean isMute = false;

    public VolumeController(Context context) {
        super(context);
        init(context);
    }

    public VolumeController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VolumeController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public void pause(){
        if(mPlayer.isPlaying()){
            mPlayer.pause();
        }
    }

    private void init(Context context) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_volume_controller"), null);
        initVolumeView(v);
        addView(v);

        lastVolume = getStreamMaxVolume() / 2;
    }

    private void initVolumeView(View v) {
        mVolumeBar = (ProgressBar) v.findViewById(BFYResUtil.getId(getContext(), "volumeBar"));
        if (mVolumeBar != null) {
            if (mVolumeBar instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mVolumeBar;
                seeker.setOnSeekBarChangeListener(mVolumeChangeListener);
            }
            mVolumeBar.setMax(getStreamMaxVolume());
            mVolumeBar.setProgress(getCurrentVolume());
        }

        //静音按钮
        mSilenceButton = (ImageButton) v.findViewById(BFYResUtil.getId(getContext(), "silenceButton"));
        if (null != mSilenceButton) {
            mSilenceButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMute) {
                        //恢复静音前音量
                        mVolumeBar.setProgress(lastVolume);
                        setVolume();
                    } else {
                        //静音
                        setMute();
                    }
                }
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mVolumeBar.setEnabled(enabled);
        mSilenceButton.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BFYLog.d(TAG, "onKeyDown,keyCode=" + keyCode);
        int volume = getCurrentVolume();
        BFYLog.d(TAG, "volume=" + volume);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mVolumeBar.setProgress(volume);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mVolumeBar.setProgress(volume);
            if (volume == 0) {//静音
                setMute();
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {//静音键
            setMute();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 设置静音
     */
    private void setMute() {
        BFYLog.d(TAG, "setMute");
        isMute = true;
        lastVolume = getCurrentVolume();
        mVolumeBar.setProgress(0);
        setVolume();
    }

    private void setVolume() {
        BFYLog.d(TAG, "setVolume()");
        setVolume(mVolumeBar.getProgress());
    }

    private void setVolume(int volume) {
        BFYLog.d(TAG, "setVolume1,volume=" + volume + ",isMute=" + isMute);
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        if (isMute) {
            if (volume > 0) {//转为非静音状态
                isMute = false;
                lastVolume = 0;
            }
        } else {
            if (volume <= 0) {//转为静音状态
                isMute = true;
            }
        }
        BFYLog.d(TAG, "setVolume2,volume=" + volume + ",isMute=" + isMute);
        if (isMute) {
            mSilenceButton.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_volume_mute"));
        } else {
            mSilenceButton.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_volume_normal"));
        }
    }

    private int getCurrentVolume() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private int getStreamMaxVolume() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 显示音量控制器
     */
    @Override
    public void show() {
        BFYLog.d(TAG, "show");
        int volume = getCurrentVolume();
        mVolumeBar.setProgress(volume);
        if (volume > 0) {
            //非静音
            mSilenceButton.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_volume_normal"));
        } else {
            //静音
            mSilenceButton.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_volume_mute"));
        }
        super.show();
    }

    @Override
    public Animation getShowAnimation() {
        return new TranslateAnimation(-1, 1, 0, 0);
    }

    @Override
    public Animation getHideAnimation() {
        return new TranslateAnimation(1, -1, 0, 0);
    }

    @Override
    public void reset() {
        BFYLog.d(TAG, "reset,isMute=" + isMute + ",lastVolume=" + lastVolume);
        if (isMute) {
            isMute = false;
            if (lastVolume == 0) {
                lastVolume = getStreamMaxVolume() * 2 / 5;
            }
            BFYLog.d(TAG, "reset,lastVolume=" + lastVolume);
//            setVolume(lastVolume);
        }
        lastVolume = 0;
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
//        int childCount = getChildCount();
//        if (childCount > 0) {
//            getChildAt(0).onTouchEvent(ev);
//        }
        return super.onTouchEvent(ev);
    }

    /**
     * 设置音量变化控制器
     *
     * @param l
     */
    public void setOnVolumeChangedListener(OnVolumeChangedListener l) {
        mOnVolumeChangedListener = l;
    }

    private SeekBar.OnSeekBarChangeListener mVolumeChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            setVolume(progress);
            if (!fromUser) {
                return;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            mMediaController.show(3600000);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            setVolume();
            mMediaController.show();
        }
    };

    public void setMediaController(MediaController controller) {
        this.mMediaController = controller;
        if (null != controller) {
            this.mPlayer = controller.getMediaPlayer();
        }
    }

    public interface OnVolumeChangedListener {
        void onStart(VolumeController controller);

        void onVolumeChanged(VolumeController vcl, float volume, boolean fromUser);

        void onStop(VolumeController controller);
    }
}
