package bf.cloud.android.playutils;

import bf.cloud.android.components.mediaplayer.VideoViewBase;

/**
 * Created by wangtonggui
 */
public abstract class BasePlayer {
	public final String TAG = BasePlayer.class.getSimpleName();
	private VideoViewBase mVideoView = null;
	protected String mToken = null;


	/**
	 * 设置播放的表面
	 */
	public void setVideoView(VideoViewBase vvb) {
		if (vvb != null)
			mVideoView = vvb;
	}

	/**
	 * 设置播放数据源
	 * 
	 * @param url 播放数据源 (如："servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0")
	 */
	public void setDataSource(String url) {
		
	}

	/**
	 * 设置播放 token (播放私有视频时需要指定 token)
	 */
	public void setPlayToken(String playToken) {
		if (playToken != null)
			mToken = playToken;
	}

	/**
	 * 设置用于显示界面上的视频名称 (可选)
	 */
//	public void setVideoName(String videoName) {
//		if (mPlayerFragment != null) {
//			mPlayerFragment.setVideoName(videoName);
//		}
//	}
	
	/**
	 * 设置用于存储信息的本地目录
	 */
	public void setDataPath(String dataPath) {
		
	}
	
    /**
     * 设置视频解码方式
     */
//    public void setDecodeMode(DecodeMode decodeMode) {
//		if (mPlayerFragment != null) {
//	    	mPlayerFragment.setDecodeMode(decodeMode);
//		}
//    }
    
    /**
     * 设置播放错误事件监听器
     */
//    public void setPlayErrorListener(PlayErrorListener listener) {
//		if (mPlayerFragment != null) {
//	    	mPlayerFragment.setPlayErrorListener(listener);
//		}
//    }
    
    /**
     * 开始播放
     */
    public void start() {
    	if (mVideoView != null)
    		mVideoView.start();
    	else{
    		
    	}
    }
    
    /**
     * 停止播放
     */
    public void stop() {
    }

	/**
	 * 暂停播放
	 */
	public void pause() {
		
	}

//	/**
//	 * 继续播放
//	 */
//	public void resume() {
//		if (mPlayerController != null) {
//			mPlayerController.getMediaController().getMediaPlayer().start();
////			mPlayerController.continuePlay();
//		}
//	}

	/**
     * 重置播放器
     */
//    public void reset() {
//    }
    
	/**
	 * 增加音量
	 */
	public void incVolume() {
	}

	/**
	 * 减小音量
	 */
	public void decVolume() {
	}

	/**
	 * 设置音量
	 */
	public void setVolume(int value) {
	}

	/**
	 * 设置是否全屏
	 */
	public void setFullscreen(boolean fullscreen) {
	}
	
	/**
	 * 设置移动设备横屏时是否自动全屏
	 */
	public void setAutoFullscreen(boolean autoFullscreen) {
	}
	
	/**
	 * 取得当前音量
	 */
//	public int getCurrentVolume() {
//	}

	/**
	 * 取得最大音量
	 */
//	public int getMaxVolume() {
//	}

    /**
     * 取得当前的解码模式
     */
//    public DecodeMode getDecodeMode() {
//    }
    
	/**
	 * 取得当前播放位置 (毫秒)
	 */
//	public int getCurrentPosition() {
//	}
	
	/**
	 * 取得当前是否自动全屏 
	 */
//	public boolean getAutoFullscreen() {
//		if (mPlayerController != null) {
//			return mPlayerController.getAutoFullscreen();
//		} else {
//			return true;
//		}
//	}
	
//	public void registerPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener eventListener) {
//		if (mPlayerController != null) {
//			mPlayerController.registerPlayerVideoEventListener(eventListener);
//		}
//	}

//	public void unregisterPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener eventListener) {
//		if (mPlayerController != null) {
//			mPlayerController.unregisterPlayerVideoEventListener(eventListener);
//		}
//	}
	
}
