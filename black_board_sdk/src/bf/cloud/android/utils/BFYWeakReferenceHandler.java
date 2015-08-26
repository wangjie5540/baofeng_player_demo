package bf.cloud.android.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * 弱引用Handler工具类,防止handler持有context对象导致界面activity无法释放资源
 *
 * Created by gehuanfei on 2014/9/18.
 */
public abstract class BFYWeakReferenceHandler<T> extends Handler {

    private WeakReference<T> mReference;

    public BFYWeakReferenceHandler(T reference) {
        mReference = new WeakReference<T>(reference);
    }

    public BFYWeakReferenceHandler(T reference, Looper looper) {
        super(looper);
        mReference = new WeakReference<T>(reference);
    }

    @Override
    public final void handleMessage(Message msg) {
        T t = mReference.get();
        if (t == null||msg==null){
            return;
        }
        handleMessage(t, msg);
    }

    protected abstract void handleMessage(T reference, Message msg);

}

