package bf.cloud.android.base;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.TextView;
import bf.cloud.android.components.popupwindow.BFYAbsPopupWindow;
import bf.cloud.android.components.popupwindow.BFYCommonPopupwindow;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;


/**
 * Created by gehuanfei on 2014-9-20.
 * 注意：在使用BaseWindow时，对应的BaseActivity必须使用getWindowHandler()初始化Handler
 */
public class BFYBaseWindow {
    private static int baseWindowId;
    private PopupWindow mPopupWindow;
    private boolean isBlocked;
    private TextView mText;
    private int mStrId;
    private String mInfo;
    private long mShowTime;
    private BFYBaseActivity mShowActivity;
    private int mId;
    private int mType = TYPE_BLOCK_TOAST;           //0-阻塞性TOAST 1-非阻塞Toast 2-弹出窗口
    public final static int TYPE_BLOCK_TOAST = 0;
    public final static int TYPE_UNBLOCK_TOAST = 1;
    public final static int TYPE_POPUPWINDOW = 3;
    /**
     * toast弹出世间
     */
    public static final int TOAST_SHOW_TIME = 2000;


    public static BFYBaseWindow getToast(String info) {
        return new BFYBaseWindow(false, info);
    }

    public static BFYBaseWindow getToast(int strId) {
        return new BFYBaseWindow(false, strId);
    }

    public static BFYBaseWindow getBlockToast(String info) {
        return new BFYBaseWindow(true, info);
    }

    /**
     * 批量删除确认对话框
     *
     * @param info
     * @param listener
     * @return
     */
    public static BFYBaseWindow getConfirmWindow(String info, BFYCommonPopupwindow.OnButtonClickListener listener) {
        return new BFYBaseWindow(new BFYCommonPopupwindow(info, listener));
    }

    /**
     * 左右按钮风格对话框
     *
     * @param info
     * @param lBtn
     * @param rBtn
     * @param listener
     * @return
     */
    public static BFYBaseWindow getPopupWindow(String info, String lBtn, String rBtn, BFYCommonPopupwindow.OnButtonClickListener listener) {
        return new BFYBaseWindow(new BFYCommonPopupwindow(info, lBtn, rBtn, listener));
    }

    boolean isBlocked() {
        return isBlocked;
    }

    private BFYBaseWindow(boolean isBlocked, int id) {
        this.isBlocked = isBlocked;
        mStrId = id;
        mId = baseWindowId++;
        mType = isBlocked ? TYPE_BLOCK_TOAST : TYPE_UNBLOCK_TOAST;
    }

    private BFYBaseWindow(boolean isBlocked, String value) {
        this.isBlocked = isBlocked;
        mInfo = value;
        mId = baseWindowId++;
//        mPopupWindow = getWindow();
        mType = isBlocked ? TYPE_BLOCK_TOAST : TYPE_UNBLOCK_TOAST;
    }

    private BFYBaseWindow(BFYAbsPopupWindow popupWindow) {
        mPopupWindow = popupWindow.getWindow();
        mType = TYPE_POPUPWINDOW;
    }

    public int getType() {
        return mType;
    }

    private PopupWindow getWindow() {
        PopupWindow window = null;
        if (isBlocked) {
            View layout = BFYBaseActivity.getTopActivity().getLayoutInflater().inflate(BFYResUtil.getLayoutId(BFYBaseActivity.getTopActivity(), "view_base_activity_blocked_toast"), null);
            mText = (TextView) layout.findViewById(BFYResUtil.getId(BFYBaseActivity.getTopActivity(), "blocked_toast_text"));
            if (mInfo != null) {
                mText.setText(mInfo);
            } else {
                mText.setText(mStrId);
            }

            window = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, true);
            mText.startAnimation(AnimationUtils.loadAnimation(BFYBaseActivity.getTopActivity(),
            		BFYResUtil.getAnimId(BFYBaseActivity.getTopActivity(), "app_toast_in")));
        } else {
            View layout = BFYBaseActivity.getTopActivity().getLayoutInflater().inflate(
            		BFYResUtil.getLayoutId(BFYBaseActivity.getTopActivity(), "view_base_activity_toast"), null);
            TextView text = (TextView) layout.findViewById(BFYResUtil.getId(BFYBaseActivity.getTopActivity(), "toast_text"));
            if (mInfo != null) {
                text.setText(mInfo);
            } else {
                text.setText(mStrId);
            }
            window = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, false);
        }
        return window;
    }

    public int getId() {
        return mId;
    }

    public void show() {
        show(0);
    }

    public long getShowTime() {
        return mShowTime;
    }

    public void show(long showTime) {
        mShowTime = showTime;
        mShowActivity = BFYBaseActivity.getTopActivity();
        if (mShowActivity != null) {
        	mShowActivity.showWindowHandler(this);
        }
    }

    public void dismiss() {
        if (mPopupWindow == null) {
            return;
        }
        mShowActivity.dismissWindowHandler(this);
    }

    void showWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
        if (mType != TYPE_POPUPWINDOW) {
            mPopupWindow = getWindow();
        }
        if (isBlocked) {
            mPopupWindow.showAtLocation(mShowActivity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        } else {
            mPopupWindow.showAtLocation(mShowActivity.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        }
        if (mShowTime > 0) {
            new Thread(new ShowTask()).start();
        }
    }

    void dismissWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
        mPopupWindow = null;
    }

    /**
     * 是否正在显示
     *
     * @return
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    private class ShowTask implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(mShowTime);
            } catch (InterruptedException e) {
            }
            mShowActivity.dismissWindowHandler(BFYBaseWindow.this);
        }
    }

    protected void lv(String s) {
        BFYLog.d("BaseWindow", "daincly: " + s);
    }

}