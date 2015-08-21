package bf.cloud.android.components.mediaplayer.placeholder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;


/**
 * 视频加载时显示的图层
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class BFYPlaceHolder extends FrameLayout {
    private final String TAG = BFYPlaceHolder.class.getSimpleName();

    private Context mContext;
    private View mAnchor;

    public BFYPlaceHolder(Context context) {
        super(context);
    }

    public BFYPlaceHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BFYPlaceHolder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Context context) {
       BFYLog.d(TAG, "init");
        mContext = context;

        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeHolderView();
        addView(v, frameParams);
    }

    private View makeHolderView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflate.inflate(BFYResUtil.getLayoutId(getContext(), "vp_placeholder"), null);
        initHolderView(v);
        return v;
    }

    private void initHolderView(View v) {

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
            LayoutParams lp = new LayoutParams(mAnchor.getWidth(), mAnchor.getHeight());
            setLayoutParams(lp);
            setVisibility(View.VISIBLE);
        }
    }

    public void setAnchorView(View view) {
        mAnchor = view;
    }

}
