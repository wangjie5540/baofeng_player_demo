package bf.cloud.android.playutils;

import android.util.Log;
import bf.cloud.android.base.BFYConst;
import bf.cloud.android.components.mediaplayer.VideoViewBase;
import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.BFStream.BFStreamMessageListener;
import bf.cloud.android.modules.p2p.MediaCenter.NetState;

/**
 * Created by wangtonggui
 */
public abstract class BasePlayer implements BFStreamMessageListener{
	public final String TAG = BasePlayer.class.getSimpleName();
	private VideoViewBase mVideoView = null;
	protected String mToken = "";
	// 这里的变化必须要对应一个变化mode的消息
	protected DecodeMode mDecodeMode = BFYConst.DEFAULT_DECODE_MODE; 
	protected VideoFrame mVideoFrame = null;
	private BFStream mBfStream = null;
	private String mDataSource = null;
	
	protected BasePlayer(){
		
	}
	
	protected BasePlayer(VideoFrame vf, String settingDataPath){
		if (vf == null){
			throw new NullPointerException("VideoFrame is null");
		}
		mVideoFrame = vf;
		mVideoView = mVideoFrame.getVideoView();
		mBfStream = new BFStream("/sdcard", NetState.NET_WIFI_REACHABLE);
		mBfStream.registerListener(this);
	}

	/**
	 * 设置播放数据源
	 * 
	 * @param url 播放数据源 (如："servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0")
	 */
	public void setDataSource(String url) {
		mDataSource = url;
	}
	
	/**
	 * 设置播放数据源
	 * 
	 * @param url 播放数据源 (如："servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0")
	 * @param playToken 播放token
	 */
	public void setDataSource(String url, String playToken) {
		mDataSource = url;
		mToken = playToken;
	}

	/**
	 * 设置用于显示界面上的视频名称 (可选)
	 */
	// public void setVideoName(String videoName) {
	// if (mPlayerFragment != null) {
	// mPlayerFragment.setVideoName(videoName);
	// }
	// }

	/**
	 * 设置用于存储信息的本地目录
	 */
	public void setDataPath(String dataPath) {

	}

	/**
	 * 设置视频解码方式
	 */
	public void setDecodeMode(DecodeMode decodeMode) {
		if (decodeMode == null)
			decodeMode = DecodeMode.AUTO;
		mVideoFrame.setDecodeMode(decodeMode);
		mVideoView = mVideoFrame.getVideoView();
	}

	/**
	 * 设置播放错误事件监听器
	 */
	// public void setPlayErrorListener(PlayErrorListener listener) {
	// if (mPlayerFragment != null) {
	// mPlayerFragment.setPlayErrorListener(listener);
	// }
	// }

	/**
	 * 开始播放
	 */
	public void start() {
		Log.d(TAG, "start");
		//这个时候要保证p2p已经启动，创建stream
		int ret = mBfStream.createStream(mDataSource, mToken, 0);
		if (ret < 0)
			Log.d(TAG, "createStream error");
			
		//启动播放器
//		if (mVideoView != null)
//			mVideoView.start();
//		else {
//
//		}
	}

	/**
	 * 停止播放
	 */
	public void stop() {
	}

	/**
	 * 暂停播放
	 */
	protected void pause() {

	}

	 /**
	 * 继续播放
	 */
	 public void resume() {
//		 if (mPlayerController != null) {
//		 mPlayerController.getMediaController().getMediaPlayer().start();
//		 // mPlayerController.continuePlay();
//		 }
	 }

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

	@Override
	public void onMessage(int type, int data, int error) {
		Log.d(TAG, "onMessage type:" + type + ",data:" + data + ",error:" + error);
	}

	@Override
	public void onStreamReady() {
		Log.d(TAG, "onStreamReady");
		int ret = mBfStream.startStream();
		if (ret < 0)
			Log.d(TAG, "startStream error");
		//mVideoView.setDataSource(mBfStream.getStreamUrl());
	}

	@Override
	public void onMediaCenterInitSuccess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMediaCenterInitFailed(int error) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 取得当前音量
	 */
	// public int getCurrentVolume() {
	// }

	/**
	 * 取得最大音量
	 */
	// public int getMaxVolume() {
	// }

	/**
	 * 取得当前的解码模式
	 */
	// public DecodeMode getDecodeMode() {
	// }

	/**
	 * 取得当前播放位置 (毫秒)
	 */
	// public int getCurrentPosition() {
	// }

	/**
	 * 取得当前是否自动全屏
	 */
	// public boolean getAutoFullscreen() {
	// if (mPlayerController != null) {
	// return mPlayerController.getAutoFullscreen();
	// } else {
	// return true;
	// }
	// }

	// public void
	// registerPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener
	// eventListener) {
	// if (mPlayerController != null) {
	// mPlayerController.registerPlayerVideoEventListener(eventListener);
	// }
	// }

	// public void
	// unregisterPlayEvent(PlayerController.PlayerViewControl.PlayerControllerListener
	// eventListener) {
	// if (mPlayerController != null) {
	// mPlayerController.unregisterPlayerVideoEventListener(eventListener);
	// }
	// }

}
