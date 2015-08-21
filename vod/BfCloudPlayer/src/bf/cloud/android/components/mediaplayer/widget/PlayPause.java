package bf.cloud.android.components.mediaplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.util.Log;
import  bf.cloud.android.components.mediaplayer.MediaController;
import bf.cloud.android.utils.BFYResUtil;

/**
 * Created by gehuanfei on 2014/9/18.
 */
public class PlayPause extends FrameLayout implements View.OnClickListener {
    private String TAG = PlayPause.class.getSimpleName();

    private MediaController mediaController;
    private ImageButton playPauseBtn;

    public PlayPause(Context context) {
        super(context);
        init(context);
    }

    public PlayPause(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayPause(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Context context) {
        Log.d(TAG, "init");
        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeProgressView(context);
        addView(v, frameParams);

        v.setClickable(true);
        v.setOnClickListener(this);
    }

    private View makeProgressView(Context context) {
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_play_pause"), null);
        initProgressView(v);
        return v;
    }

    private void initProgressView(View v) {
        playPauseBtn = (ImageButton) v.findViewById(BFYResUtil.getId(getContext(), "pausePlayButton"));
        if (null != playPauseBtn) {
        	playPauseBtn.setOnClickListener(new OnClickListener() {
        		 @Override
                 public void onClick(View v) {
        		    Log.d(TAG, "playPauseBtn onClick");
        		    if (null != mediaController) {
        		        mediaController.doPauseResume();
        		    }
                 }
             });        	
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (null != mediaController) {
            mediaController.doPauseResume();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
    	playPauseBtn.setEnabled(enabled);
    	setClickable(enabled);
    	super.setEnabled(enabled);
    }

    public void setMediaController(MediaController mediaController) {
        this.mediaController = mediaController;
    }

    public void updatePausePlay(boolean playing) {
        Log.d(TAG, "updatePausePlay,playing=" + playing);
        if (playing) {
            playPauseBtn.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_pause"));
        } else {
            playPauseBtn.setBackgroundResource(BFYResUtil.getDrawableId(getContext(), "vp_play"));
        }
    }
}
