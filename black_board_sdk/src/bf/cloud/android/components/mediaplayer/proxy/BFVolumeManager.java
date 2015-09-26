package bf.cloud.android.components.mediaplayer.proxy;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class BFVolumeManager {
	private final String TAG = BFVolumeManager.class.getSimpleName();
	private static BFVolumeManager mBfVolumeManager = null;
	private Context mContext = null;
	private AudioManager mAudioManager = null;
	private BFVolumeManager(Context c) {
		mContext = c;
		if (mAudioManager == null)
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	}
	
	public static BFVolumeManager getInstance(Context c){
		synchronized (BFVolumeManager.class) {
			if (mBfVolumeManager == null)
				mBfVolumeManager = new BFVolumeManager(c);
		}
		return mBfVolumeManager;
	}
	
	public void incVolume(){
		Log.d(TAG, "incVolume");
		synchronized (BFVolumeManager.class) {
			int value = getCurrentVolume() + 1;
			if (value < getMaxVolume())
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
		}
	}
	
	public void decVolume(){
		synchronized (BFVolumeManager.class) {
			int value = getCurrentVolume() - 1;
			if (value <= getMaxVolume() && value >= 0)
				setVolume(value);
		}
	}
	
	public void setVolume(int value){
		synchronized (BFVolumeManager.class) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
		}
	}
	
	public int getCurrentVolume(){
		synchronized (BFVolumeManager.class) {
			return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		}
	}
	
	public int getMaxVolume(){
		synchronized (BFVolumeManager.class) {
			return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		}
	}
}
