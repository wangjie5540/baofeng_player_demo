package bf.cloud.android.components.mediaplayer;

import bf.cloud.android.utils.BFYResUtil;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

/**
 * 视频状态控制器
 */
public class StatusController extends Controller {

    private final String TAG = StatusController.class.getSimpleName();

    public static final int IDLE = -1;
    public static final int INITTING = 0;
    public static final int BUFFERRING = 1;

    public int mCurrentState = IDLE;
    private boolean mShowing;
    private TextView mStatusTextView;

    public StatusController(Context context) {
        super(context);
    }

    public StatusController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init() {
        Log.d(TAG, "init");
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_status_controller"), null);
        addView(v);
        initStatusView(v);
    }

    private void initStatusView(View v) {
        mStatusTextView = (TextView) v.findViewById(BFYResUtil.getId(getContext(), "statusTextView"));
    }
    public void pause(){
    }

    @Override
    public void show() {
        Log.d(TAG, "show");
        mShowing = true;
        super.show();
    }

    public void show(String msg) {
        show(INITTING, msg);
    }

    public void show(int state, String msg) {
        Log.d(TAG, "show,state=" + state + "--msg=" + msg);
        
        mCurrentState = state;
        if (mCurrentState == IDLE) return;

        show();

        if (msg != null && msg != "") {
	        if (mStatusTextView != null) {
	        	mStatusTextView.setText(msg);
	        }
        }
    }

    public void hide() {
        Log.d(TAG, "hide");
        mShowing = false;
        mCurrentState = IDLE;
        setVisibility(View.INVISIBLE);
    }

    public boolean isShowing() {
        return mShowing;
    }

}
