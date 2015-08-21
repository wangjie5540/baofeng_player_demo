package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public class VodPlayer extends BasePlayer {

	@Override
	protected void initPlayerFragment() {
		mPlayerFragment.setPlayTaskType(PlayTaskType.VOD);
	}

	/**
	 * 设置视频播放清晰度
	 */
	public void setDefinition(VideoDefinition definition) {
    	if (mPlayerFragment != null) {
    		mPlayerFragment.setDefinition(definition);
    	}
	}
	
    /**
     * 拖动到指定播放点
     */
    public void seekTo(int ms) {
    	if (mPlayerFragment != null) {
    		mPlayerFragment.seekTo(ms);
    	}
    }
	
	/**
	 * 取得片长 (毫秒)
	 */
	public int getDuration() {
		if (mPlayerController != null) {
			return mPlayerController.getDuration();
		} else {
			return -1;
		}
	}
	
	/**
	 * 取得当前视频清晰度
	 */
	public VideoDefinition getCurrentDefinition() {
    	if (mPlayerFragment != null) {
    		return mPlayerFragment.getCurrentDefinition();
    	} else {
    		return VideoDefinition.UNKNOWN;
    	}
	}
	
}
