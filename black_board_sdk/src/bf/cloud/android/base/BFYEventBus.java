package bf.cloud.android.base;

import java.util.ArrayList;

public class BFYEventBus {

	/**
	 * 事件监听接口
	 */
	public interface OnEventListener {
		public void onEvent(Object event);
	}

	private volatile static BFYEventBus instance;
	private ArrayList<OnEventListener> mListeners = new ArrayList<OnEventListener>();
	
    private BFYEventBus() {}

    public static BFYEventBus getInstance() {
        if (instance == null) {
            synchronized (BFYEventBus.class) {
                if (instance == null) {
                    instance = new BFYEventBus();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册事件监听者
     */
    public synchronized void registerListener(OnEventListener listener) {
    	if (listener != null && !mListeners.contains(listener)) {
    		mListeners.add(listener);
    	}
    }

    /**
     * 注销事件监听者
     */
    public synchronized void unregisterListener(OnEventListener listener) {
    	mListeners.remove(listener);
    }
    
    /**
     * 发送事件
     */
    public synchronized void post(Object event) {
    	for (OnEventListener listener: mListeners) {
    		listener.onEvent(event);
    	}
    }

}
