package bf.cloud.android.playutils;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.PlayErrorListener;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.fragment.VideoPlayerFragment;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public abstract class BasePlayer {

	protected VideoPlayerFragment mPlayerFragment;
	protected PlayerController mPlayerController;

	protected BasePlayer() {
	}

	protected abstract void initPlayerFragment();

	/**
	 * 设置播放器的 fragment
	 */
	public void setPlayerFragment(VideoPlayerFragment fragment) {
		mPlayerFragment = fragment;
		mPlayerController = mPlayerFragment.getPlayerController();
		initPlayerFragment();
	}

	/**
	 * 设置播放数据源
	 * 
	 * @param url 播放数据源 (如："servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0")
	 */
	public void setDataSource(String url) {
		if (mPlayerFragment != null) {
			mPlayerFragment.setDataSource(url);
		}
	}

	/**
	 * 设置播放 token (播放私有视频时需要指定 token)
	 */
	public void setPlayToken(String playToken) {
		if (mPlayerFragment != null) {
			mPlayerFragment.setPlayToken(playToken);
		}
	}

	/**
	 * 设置用于显示界面上的视频名称 (可选)
	 */
	public void setVideoName(String videoName) {
		if (mPlayerFragment != null) {
			mPlayerFragment.setVideoName(videoName);
		}
	}
	
	/**
	 * 设置用于存储信息的本地目录
	 */
	public void setDataPath(String dataPath) {
		if (mPlayerFragment != null) {
			mPlayerFragment.setDataPath(dataPath);
		}
	}
	
    /**
     * 设置视频解码方式
     */
    public void setDecodeMode(DecodeMode decodeMode) {
		if (mPlayerFragment != null) {
	    	mPlayerFragment.setDecodeMode(decodeMode);
		}
    }
    
    /**
     * 设置播放错误事件监听器
     */
    public void setPlayErrorListener(PlayErrorListener listener) {
		if (mPlayerFragment != null) {
	    	mPlayerFragment.setPlayErrorListener(listener);
		}
    }
    
    /**
     * 开始播放
     */
    public void start() {
    	if (mPlayerFragment != null) {
    		mPlayerFragment.start();
    	}
    }
    
    /**
     * 停止播放
     */
    public void stop() {
    	if (mPlayerFragment != null) {
    		mPlayerFragment.stop();
    	}
    }

	/**
	 * 暂停播放
	 */
	public void pause() {
		if (mPlayerController != null) {
			mPlayerController.getMediaController().getMediaPlayer().pause();
		}
	}

	/**
	 * 继续播放
	 */
	public void resume() {
		if (mPlayerController != null) {
			mPlayerController.getMediaController().getMediaPlayer().start();
//			mPlayerController.continuePlay();
		}
	}

	/**
     * 重置播放器
     */
    public void reset() {
    	if (mPlayerFragment != null) {
    		mPlayerFragment.reset();
    	}
    }
    
	/**
	 * 增加音量
	 */
	public void incVolume() {
		if (mPlayerController != null) {
			mPlayerController.incVolume();
		}
	}

	/**
	 * 减小音量
	 */
	public void decVolume() {
		if (mPlayerController != null) {
			mPlayerController.decVolume();
		}
	}

	/**
	 * 设置音量
	 */
	public void setVolume(int value) {
		if (mPlayerController != null) {
			mPlayerController.setVolume(value);
		}
	}

	/**
	 * 设置是否全屏
	 */
	public void setFullscreen(boolean fullscreen) {
		if (mPlayerController != null) {
			mPlayerController.setFullScreen(fullscreen);
		}
	}
	
	/**
	 * 设置移动设备横屏时是否自动全屏
	 */
	public void setAutoFullscreen(boolean autoFullscreen) {
		if (mPlayerController != null) {
			mPlayerController.setAutoFullscreen(autoFullscreen);
		}
	}
	
	/**
	 * 取得当前音量
	 */
	public int getCurrentVolume() {
		if (mPlayerController != null) {
			return mPlayerController.getCurrentVolume();
		} else {
			return 0;
		}
	}

	/**
	 * 取得最大音量
	 */
	public int getMaxVolume() {
		if (mPlayerController != null) {
			return mPlayerController.getMaxVolume();
		} else {
			return 0;
		}
	}

    /**
     * 取得当前的解码模式
     */
    public DecodeMode getDecodeMode() {
    	if (mPlayerFragment != null) {
    		return mPlayerFragment.getDecodeMode();
    	} else {
    		return BFYConst.DEFAULT_DECODE_MODE;
    	}
    }
    
	/**
	 * 取得当前播放位置 (毫秒)
	 */
	public int getCurrentPosition() {
		if (mPlayerController != null) {
			return mPlayerController.getCurPosition();
		} else {
			return -1;
		}
	}
	
	/**
	 * 取得当前是否自动全屏 
	 */
	public boolean getAutoFullscreen() {
		if (mPlayerController != null) {
			return mPlayerController.getAutoFullscreen();
		} else {
			return true;
		}
	}
	
	public void registerPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener eventListener) {
		if (mPlayerController != null) {
			mPlayerController.registerPlayerVideoEventListener(eventListener);
		}
	}

	public void unregisterPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener eventListener) {
		if (mPlayerController != null) {
			mPlayerController.unregisterPlayerVideoEventListener(eventListener);
		}
	}
	
}
