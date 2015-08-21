package bf.cloud.android.base;

import bf.cloud.android.modules.log.BFYLog;

public class BFYApplication {
	
    private static BFYApplication instance;
    
    private BFYApplication() {}
    
    public static BFYApplication getInstance() {
        if (instance == null) {
            synchronized (BFYApplication.class) {
                if (instance == null) {
                    instance = new BFYApplication();
                }
            }
        }
        return instance;
    }
    
    public void setDebugMode(boolean debugMode) {
    	BFYLog.setDebugMode(debugMode);
    }
    
    public boolean isDebugMode() {
    	return BFYLog.isDebugMode();
    }
    
}
