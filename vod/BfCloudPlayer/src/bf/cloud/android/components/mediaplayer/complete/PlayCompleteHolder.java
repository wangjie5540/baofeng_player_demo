package bf.cloud.android.components.mediaplayer.complete;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;


/**
 * 非自动播放前和播放完成时的显示
 *
 * Created by gehuanfei on 2014/9/23.
 */
public class PlayCompleteHolder extends FrameLayout {

    private final String TAG = PlayCompleteHolder.class.getSimpleName();

    private Context mContext;
    private View mAnchor;
    private ImageButton mPlayButton;
    private TextView mMessageTextView;
    private PlayerController mPlayerController;

    public PlayCompleteHolder(Context context) {
        super(context);
    }

    public PlayCompleteHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayCompleteHolder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Context context) {
        BFYLog.d(TAG, "init");
        mContext = context;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeCompleteView();
        addView(v, frameParams);
    }

    private View makeCompleteView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_play_complete"), null);
        initCompleteView(v);
        return v;
    }

    private void initCompleteView(View v) {
        mPlayButton = (ImageButton) v.findViewById(BFYResUtil.getId(getContext(), "play_button"));
        mPlayButton.setOnClickListener(listener);
        
        mMessageTextView = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "message_textview"));
        mMessageTextView.setVisibility(View.INVISIBLE);
    }

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            BFYLog.d(TAG, "onClick,playback");
            if (null != mPlayerController) {
                mPlayerController.startFromBeginning();
            }
        }
    };

    public void orientationChanged() {
        BFYLog.d(TAG, "orientationChanged");
        LayoutParams params = (LayoutParams) getLayoutParams();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        setLayoutParams(params);
        requestLayout();
    }

    public void hide() {
        BFYLog.d(TAG, "hide");
        setVisibility(View.GONE);
    }

    public void show() {
        BFYLog.d(TAG, "show");
        if (mAnchor != null) {
            int[] anchorpos = new int[2];
            mAnchor.getLocationOnScreen(anchorpos);

            BFYLog.d(TAG, "show,anchor width=" + mAnchor.getWidth() + ",height=" + mAnchor.getHeight());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mAnchor.getLayoutParams().width, mAnchor.getLayoutParams().height);
            setLayoutParams(lp);
            setVisibility(View.VISIBLE);
        }
    }

    public void setPlayerController(PlayerController controller, View anchor) {
		mPlayerController = controller;
		mAnchor = anchor;
    }
    
    public void setMessage(String message) {
    	mMessageTextView.setText(message);
		mMessageTextView.setVisibility(message.length() == 0 ? View.INVISIBLE : View.VISIBLE);
    }
    
}
