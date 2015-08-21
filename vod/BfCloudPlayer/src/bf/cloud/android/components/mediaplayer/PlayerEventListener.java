package bf.cloud.android.components.mediaplayer;

public interface PlayerEventListener {
	
	public void onRestartPlay();
	public void onContinuePlay();
	public boolean onRestartFromBeginning();
	public void onNetworkError(int errorCode);
	
}

