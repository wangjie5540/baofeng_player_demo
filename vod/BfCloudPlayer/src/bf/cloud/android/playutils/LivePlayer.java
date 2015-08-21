package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public class LivePlayer extends BasePlayer {

	@Override
	protected void initPlayerFragment() {
		mPlayerFragment.setPlayTaskType(PlayTaskType.LIVE);
	}

	public void setLowLatency(boolean lowLatency) {
		if (mPlayerFragment != null) {
	    	mPlayerFragment.setLiveLowLatency(lowLatency);
		}
	}
	
	public boolean getLowLatency() {
    	if (mPlayerFragment != null) {
    		return mPlayerFragment.getLiveLowLatency();
    	} else {
    		return false;
    	}
	}
	
}
