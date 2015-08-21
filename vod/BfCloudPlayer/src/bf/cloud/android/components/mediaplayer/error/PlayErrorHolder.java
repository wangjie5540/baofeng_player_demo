package bf.cloud.android.components.mediaplayer.error;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;


/**
 * 非自动播放前和播放完成时的显示
 *
 * Created by gehuanfei on 2014/9/23.
 */
public class PlayErrorHolder extends FrameLayout {

    private final String TAG = PlayErrorHolder.class.getSimpleName();

    private Context mContext;
    private View mAnchor;
    private ImageButton mPlayButton;
    private TextView mMessageTextView;
    private TextView mCodeTextView;
    private PlayerController mPlayerController;
    private int mCode = 0;
    private boolean mShowing;

    public PlayErrorHolder(Context context) {
        super(context);
    }

    public PlayErrorHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayErrorHolder(Context context, AttributeSet attrs, int defStyle) {
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
        View v = makeErrorView();
        addView(v, frameParams);
    }

    private View makeErrorView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_play_error"), null);
        initErrorView(v);
        return v;
    }

    private void initErrorView(View v) {
        mPlayButton = (ImageButton) v.findViewById(BFYResUtil.getId(getContext(), "error_play_button"));
        mPlayButton.setOnClickListener(listener);
        
        mMessageTextView = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "error_message_textview"));
        mMessageTextView.setVisibility(View.INVISIBLE);
        
        mCodeTextView = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "error_code_textview"));
        mCodeTextView.setVisibility(View.INVISIBLE);
    }
    
    private boolean canShowErrorCode(int errorCode) {
    	switch (errorCode) {
    	case 0:
    	case PlayErrorManager.NO_NETWORK:
    	case PlayErrorManager.MOBILE_NO_PLAY:
    		return false;
    		
    	default:
    		return true;
    	}
    }

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            BFYLog.d(TAG, "onClick,retry play, error code " + mCode);
            if (null != mPlayerController) {
            	if (mCode == PlayErrorManager.MOBILE_NO_PLAY) {
            		mPlayerController.continuePlay();
            	} else {
            		mPlayerController.restartPlay();
            	}            
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
        mShowing = false;
        setVisibility(View.GONE);
    }

    public void show() {
        BFYLog.d(TAG, "show");
        mShowing = true;
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
    
    public void setMessage(int code, String message) {
    	mCode = code;

    	mMessageTextView.setText(message);
		mMessageTextView.setVisibility(message.length() == 0 ? View.INVISIBLE : View.VISIBLE);
		
		String codeText = "";
		if (canShowErrorCode(code)) {
    		codeText = "错误代码：" + mCode;
		}
		mCodeTextView.setText(codeText);
		mCodeTextView.setVisibility(codeText.length() == 0 ? View.INVISIBLE : View.VISIBLE);
    }
    
    public boolean isShowing() {
        return mShowing;
    }
    
}
