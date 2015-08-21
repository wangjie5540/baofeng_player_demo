package bf.cloud.android.components.popupwindow;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import bf.cloud.android.base.BFYBaseActivity;
import bf.cloud.android.utils.BFYResUtil;

/**
 * Created by gehuanfei on 2014-9-20.
 */
public class BFYCommonPopupwindow implements BFYAbsPopupWindow{

    private Button mBtnOk, mBtnCancel;
    private PopupWindow mMainWindow = null;
    private TextView mTips = null;
    private OnButtonClickListener mListener = null;

    @Override
    public PopupWindow getWindow() {
        return mMainWindow;
    }

    public interface OnButtonClickListener{
        public void onPositiveButtonClickListener();
        public void onNegativeButtonClickListener();
    }

//    public CommonPopupwindow(Context context){
//        if(context != null){
//            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            View view = inflater.inflate(R.layout.popupwindow_common_layout, null);
//            mMainWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//            mTips = (TextView)view.findViewById(R.id.popupwindow_common_tips);
//            mBtnOk = (Button)view.findViewById(R.id.popupwindow_common_ok);
//            mBtnCancel = (Button)view.findViewById(R.id.popupwindow_common_cancel);
//        }
//    }

    public BFYCommonPopupwindow(String text, OnButtonClickListener listener){
        Context context = BFYBaseActivity.getTopActivity();
        if(context != null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(BFYResUtil.getLayoutId(context, "confirm_window_layout"), null);
            mMainWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            mTips = (TextView)view.findViewById(BFYResUtil.getId(context, "confirm_window_tips"));
            mBtnOk = (Button)view.findViewById(BFYResUtil.getId(context, "confirm_window_confirm"));
            mBtnCancel = (Button)view.findViewById(BFYResUtil.getId(context, "confirm_window_cancel"));
            setText(text);
            setOnButtonClickListener(listener);
        }
    }

    public BFYCommonPopupwindow(String text, String leftButton, String rightButton, OnButtonClickListener listener){
        Context context = BFYBaseActivity.getTopActivity();
        if(context != null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(BFYResUtil.getLayoutId(context, "popupwindow_common_layout"), null);
            mMainWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            mTips = (TextView)view.findViewById(BFYResUtil.getId(context, "popupwindow_common_tips"));
            mBtnOk = (Button)view.findViewById(BFYResUtil.getId(context, "popupwindow_common_ok"));
            mBtnOk.setText(rightButton);
            mBtnCancel = (Button)view.findViewById(BFYResUtil.getId(context, "popupwindow_common_cancel"));
            mBtnCancel.setText(leftButton);
            setText(text);
            setOnButtonClickListener(listener);
        }
    }

    /**
     * 点击确定键监听
     */
    public void setOnButtonClickListener(OnButtonClickListener listener){
        mListener = listener;
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPositiveButtonClickListener();
                mMainWindow.dismiss();
            }
        });
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNegativeButtonClickListener();
                mMainWindow.dismiss();
            }
        });
    }

    /**
     * 设置提示区内容
     * @param text
     */
    public void setText(String text){
        mTips.setText(text);
    }

    public void show(View viewParent){
        if(!mMainWindow.isShowing()){
            mMainWindow.showAtLocation(viewParent, Gravity.CENTER, 0, 0);
        }
    }

    public void dismiss(){
        if(mMainWindow.isShowing()){
            mMainWindow.dismiss();
        }
    }
}
