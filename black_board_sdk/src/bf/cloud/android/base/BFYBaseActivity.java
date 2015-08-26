package bf.cloud.android.base;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Stack;

import bf.cloud.android.utils.BFYHandlerManager;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * 界面基类
 * Created by gehuanfei on 2014/9/15.
 */
public abstract class BFYBaseActivity extends FragmentActivity {

    public static final String TAG = "BaseActivity";

    /**
     * 没有布局文件，动态添加View 时使用的ID
     */
    protected static final int NO_LAYOUT = 0;
    private static Stack<BFYBaseActivity> sActivities = new Stack<BFYBaseActivity>();
    /**
     * 界面刷新Handler
     */
    private Handler mHandler;
    private View mActivityRootView;
    private BaseWindowHandler mBaseWindowHandler;

    protected View setLayout() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BFYLog.d(TAG, "onCreate");
        addActivity(this);
        int id = setLayoutId();
        if (id != NO_LAYOUT) {
            mActivityRootView = LayoutInflater.from(this).inflate(id, null);
            setContentView(mActivityRootView);
        } else {
            mActivityRootView = setLayout();
            if (mActivityRootView != null) {
                setContentView(mActivityRootView);
            }
        }
        handleIntent();
        mHandler = initHandler();
        BFYHandlerManager.register(mHandler);
        initView(this);
        initParam(this);
        initListener(this);
        onCreate();
    }

    /**
     * 如果需要使用BaseWindow 必须在使用的Activity的初始化方法中调用此方法。
     */
    protected void getWindowHandler() {
        if (mBaseWindowHandler == null) {
            mBaseWindowHandler = new BaseWindowHandler(this);
        }
    }

    /**
     * 空的onCreate，当初始化任务执行完后，如果还有非ui任务。在这里干
     */
    protected void onCreate() {
    }

    /**
     * 界面Activity入栈
     *
     * @param activity
     */
    private void addActivity(BFYBaseActivity activity) {
        if (activity == null) {
            return;
        }
        sActivities.push(activity);
    }

    /**
     * 获取acitivty栈
     *
     * @return
     */
    public Stack<BFYBaseActivity> getActivityStack() {
        return sActivities;
    }

    /**
     * 获取栈顶界面Activity
     */
    public static BFYBaseActivity getTopActivity() {
        if (sActivities.empty()) {
            return null;
        } else {
            return sActivities.peek();
        }
    }

    /**
     * 界面Activity移出栈
     *
     * @param activity
     */
    private void removeActivity(BFYBaseActivity activity) {
        if (activity == null) {
            return;
        }

        if (sActivities.contains(activity)) {
            sActivities.remove(activity);
        }
    }

    /**
     * 关闭客户端
     */
    public void closeApplication() {
        if (!sActivities.empty()) {
            for (BFYBaseActivity activity : sActivities) {
                if (activity != null && !activity.isFinishing())
                    activity.finish();
            }
            sActivities.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        removeActivity(this);
        BFYHandlerManager.remove(mHandler);
        super.onDestroy();
    }

    /**
     * 1处理Intent
     */
    protected abstract void handleIntent();

    /**
     * 2返回 当前页面布局的 ID
     *
     * @return
     */
    protected abstract int setLayoutId();

    /**
     * 3加载控件
     *
     * @param context
     */
    protected abstract void initView(Context context);

    /**
     * 4初始化成员变量
     *
     * @param context
     */
    protected abstract void initParam(Context context);

    /**
     * 5初始化界面刷新handler
     */
    protected abstract Handler initHandler();

    /**
     * 6初始化监听器
     *
     * @param context
     */
    protected abstract void initListener(Context context);

    protected View getRootView() {
        return mActivityRootView;
    }

    public BFYBaseWindow getToast(String info) {
        return BFYBaseWindow.getToast(info);
    }

    public BFYBaseWindow getBlockToast(String info) {
        return BFYBaseWindow.getBlockToast(info);
    }

    private int mShowWindowState;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        checkWindow(hasFocus);
    }

    private BFYBaseWindow mWindow;
    private BFYBaseWindow mBlockWindow;
    private BFYBaseWindow mPopupWindow;


    private void checkWindow(boolean hasFocus) {
        if (!hasFocus) {
            return;
        }
        if (mShowWindowState == -1) {
            if (mWindow != null) {
                BFYLog.d(TAG, "checkWindow mWindow");
                mWindow.showWindow();
            }
            if (mBlockWindow != null) {
                BFYLog.d(TAG, "checkWindow mBlockWindow");
                mBlockWindow.showWindow();
            }
        }
        mShowWindowState = 1;
    }

    void showWindowHandler(BFYBaseWindow window) {
        if (mBaseWindowHandler == null) {
            BFYLog.d(TAG, ">>>>>>>>未初始化handler<<<<<<<<");
            return;
        }
        mBaseWindowHandler.sendMessage(mBaseWindowHandler.obtainMessage(0xFF,window));
    }

    void dismissWindowHandler(BFYBaseWindow window) {
        if (mBaseWindowHandler == null) {
            BFYLog.d(TAG, ">>>>>>>>未初始化handler<<<<<<<<");
            return;
        }
        mBaseWindowHandler.sendMessage(mBaseWindowHandler.obtainMessage(0xEE,window));
    }

    private void destroyWindow() {
        if (mWindow != null) {
            BFYLog.d(TAG, "destroyWindow mWindow");
            mWindow.dismissWindow();
            mWindow = null;
        }
        if (mBlockWindow != null) {
            BFYLog.d(TAG, "destroyWindow mBlockWindow");
            mBlockWindow.dismissWindow();
            mBlockWindow = null;
        }
    }

    private class BaseWindowHandler extends BFYWeakReferenceHandler<BFYBaseActivity> {
        public BaseWindowHandler(BFYBaseActivity reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(final BFYBaseActivity reference, Message msg) {
            if (reference == null) {
                return;
            }
            BFYBaseWindow window = (BFYBaseWindow) msg.obj;
            switch (msg.what) {
                case 0xFF:
                    if (window.getType() == BFYBaseWindow.TYPE_BLOCK_TOAST) {
                        if (mBlockWindow != null) {
                            mBlockWindow.dismissWindow();
                        }
                        mBlockWindow = window;
                    } else if (window.getType() == BFYBaseWindow.TYPE_UNBLOCK_TOAST) {
                        if (mWindow != null) {
                            mWindow.dismissWindow();
                        }
                        mWindow = window;
                    } else {
                        if (mPopupWindow != null) {
                            mPopupWindow.dismissWindow();
                        }
                        mPopupWindow = window;
                    }
                    if (mShowWindowState == 1) {//根据状态，如果初始化完成（1），则handle显示。否则准备显示（-1）。activity进入前台显示后再show
                        window.showWindow();
                    } else {
                        mShowWindowState = -1;
                    }
                    break;
                case 0XEE:
                    window.dismissWindow();
                    break;
            }
        }
    }
}
