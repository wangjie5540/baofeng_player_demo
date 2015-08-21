package bf.cloud.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.base.BFYEventBus;
import bf.cloud.android.components.BFYNetworkStatusData;
import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayErrorListener;
import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.mediaplayer.PlayerEventListener;
import bf.cloud.android.components.mediaplayer.definition.DefinitionController;
import bf.cloud.android.components.player.PlayerCommand;
import bf.cloud.android.events.PlayerEvent;
import bf.cloud.android.managers.BFYMessageKeys;
import bf.cloud.android.models.beans.BFYVideoInfo;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.MediaCenter;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.PlayTaskType;
import bf.cloud.android.playutils.VideoDefinition;
import bf.cloud.android.utils.BFYNetworkUtil;
import bf.cloud.android.utils.BFYResUtil;

/**
 * Created by gehuanfei on 2014/9/12.
 */
public class VideoPlayerFragment extends Fragment 
	implements DefinitionController.OnDefinitionChangedListener, PlayErrorListener, PlayerEventListener {

    private final static String TAG = VideoPlayerFragment.class.getSimpleName();

    private String mPlayUrl = "";
    private String mPlayToken = "";
    private String mVideoName = "";
    private String mDataPath = "";
    private DecodeMode mDecodeMode = BFYConst.DEFAULT_DECODE_MODE;
    private PlayTaskType mPlayTaskType = PlayTaskType.VOD;

    private boolean mFullScreen = false;
    private int mNetStatus = MediaCenter.NetState.NET_NOT_REACHABLE;
    
    private Context mContext;

    private PlayerController mPlayerController = null;
    private FrameLayout mFrameLayout = null;
    private VideoPlayerHandler mVideoPlayerHandler = new VideoPlayerHandler(this);
    private BFStream mBfStream = null;
    private P2pPlayThread mP2pThread = null;
    private int mStreamPort = 0;
    private ArrayList<MediaCenter.StreamInfo> mStreams;
    private ArrayList<String> mDefinitionNames = new ArrayList<String>();
    private boolean mDefinitionChanged = false;
    private String mCurDefinitionName;
    private VideoDefinition mExpectedDefinition = VideoDefinition.UNKNOWN;
    private int mHistoryPosition = 0;
    private int mDuration = 0;
    private PlayErrorListener mPlayErrorListener = null;
    private boolean mDecodeChanged = false;
    private int mCurStreamId = MediaCenter.INVALID_STREAM_ID;
    private boolean mP2pTaskStarted = false;
    private boolean mStopped = false;
    private boolean mResuming = false;
    private boolean mLiveLowLatency = false;
    private int mPlayError = MediaCenter.NativeReturnType.NO_ERROR;
    private boolean mIsFragmentStop = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BFYLog.d(TAG, "onCreate");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
    	BFYLog.d(TAG, "onCreateView");
    	mContext = getActivity();
    	
    	IntentFilter filter = new IntentFilter(); 
    	filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); 
    	mContext.registerReceiver(BFYNetworkUtil.getInstance(), filter);     	

    	mFrameLayout = (FrameLayout)(inflater.inflate(BFYResUtil.getLayoutId(mContext, "vp_video_player"), container, false));
    	init();
        if (null == mP2pThread) {
            synchronized (VideoPlayerFragment.class) {
                if (null == mP2pThread) {
                	BFYLog.d(TAG, "null == mP2pThread,new and start");
                    mP2pThread = new P2pPlayThread("P2pPlayThread", Process.THREAD_PRIORITY_BACKGROUND);
                    mP2pThread.start();
                }
            }
        } else {
        	BFYLog.d(TAG, "null != mP2pThread");
        }       
        
        mFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BFYLog.d(TAG, "onGlobalLayout");
                if (null != mPlayerController) {
                    View decorView = getActivity().getWindow().peekDecorView();
                    if (null != decorView) {
                        mPlayerController.setDisplayWidth(decorView.getMeasuredWidth());
                    }
                }
            }
        });

        return mFrameLayout;
    }
    
	/**
	 * 设置播放数据源
	 * 
	 * @param url 播放数据源 (如："servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0")
	 */
	public void setDataSource(String url) {
		mPlayUrl = url;
	}
	
	/**
	 * 设置播放 token (播放私有视频时需要指定 token)
	 */
	public void setPlayToken(String playToken) {
		mPlayToken = playToken;
	}

	/**
	 * 设置用于显示界面上的视频名称 (可选)
	 */
	public void setVideoName(String videoName) {
		mVideoName = videoName;
	}
	
	/**
	 * 设置用于存储信息的本地目录
	 */
	public void setDataPath(String dataPath) {
		mDataPath = dataPath;
	}
	
    /**
     * 设置播放错误事件监听器
     */
    public void setPlayErrorListener(PlayErrorListener listener) {
    	mPlayErrorListener = listener;
    }
    
    /**
     * 设置视频解码方式
     */
    public void setDecodeMode(DecodeMode decodeMode) {
    	if (decodeMode != mDecodeMode) {
	    	mDecodeMode = decodeMode;
	        mPlayerController.setDecodeMode(mDecodeMode);
    	}
    }
    
    /**
     * 设置播放任务类型 (点播、直播)
     */
    public void setPlayTaskType(PlayTaskType playTaskType) {
    	mPlayTaskType = playTaskType;
    	mPlayerController.setPlayTaskType(mPlayTaskType);
    }
    
    /**
     * 设置是否启用低延时直播 
     */
    public void setLiveLowLatency(boolean liveLowLatency) {
    	mLiveLowLatency = liveLowLatency;
    }

    /**
     * 返回是否 启用低延时直播 
     */
    public boolean getLiveLowLatency() {
    	return mLiveLowLatency;
    }

    /**
     * 开始播放
     */
    public void start() {
    	beforeStart();
    	startPlayTask();
    }
    
    /**
     * 停止播放
     */
    public void stop() {
    	stopPlayTask();
    	afterStop();
    }
    
    /**
     * 重置播放器 
     */
    public void reset() {
    	stop();
    }
    
    /**
     * 拖动到指定播放点 
     */
    public void seekTo(int ms) {
    	if (mPlayerController != null) {
    		mPlayerController.seekTo(ms);
    	}
    }

	/**
	 * 设置视频播放清晰度
	 */
	public void setDefinition(VideoDefinition definition) {
		mExpectedDefinition = definition;
		if (mCurStreamId != MediaCenter.INVALID_STREAM_ID) {
			int streamId = getStreamIdByDefinition(definition);
			changeDefinition(streamId);
		}
	}
	
    /**
     * 取得当前的解码模式 
     */
    public DecodeMode getDecodeMode() {
    	return mDecodeMode;
    }
    
	/**
	 * 取得当前视频清晰度
	 */
	public VideoDefinition getCurrentDefinition() {
		if (mCurStreamId != MediaCenter.INVALID_STREAM_ID) {
			return getDefinitionByStreamId(mCurStreamId);
		} else {
			return mExpectedDefinition;
		}
	}
    
    public void changeDecodeMode(DecodeMode decodeMode) {
        BFYLog.d(TAG, "changeDecodeMode. " + "decodeMode=" + decodeMode);
        mVideoPlayerHandler.obtainMessage(BFYMessageKeys.MESSAGE_CHANGE_DECODE_MODE, decodeMode.value(), 0).sendToTarget();
    }

    public PlayerController getPlayerController() {
        return mPlayerController;
    }

    public View getRootView() {
        return mFrameLayout;
    }

    private void init() {
    	mPlayUrl = "";
    	mPlayToken = "";
    	mVideoName = "";
    	mPlayErrorListener = null;
    	
    	resetStatus();
    }
    
    private void buildPlayerCtrl() {
        BFYLog.d(TAG, "initPlayerCtrl");                
        mPlayerController = (PlayerController)mFrameLayout.findViewById(BFYResUtil.getId(mContext, "playerController"));
	 	mPlayerController.onCreate();
	 	initPlayerCtrl();
        mPlayerController.setFullScreen(mFullScreen);
        mPlayerController.setDecodeMode(mDecodeMode);
    }
    
    private void initPlayerCtrl() {
        mPlayerController.setFocusable(true);
        mPlayerController.setFocusableInTouchMode(true);
        mPlayerController.requestFocus();
        mPlayerController.setVideoInfo(new BFYVideoInfo(mPlayUrl));
	 	mPlayerController.setDefinitions(mDefinitionNames);
        mPlayerController.setPlayErrorListener(this);
        mPlayerController.setPlayerEventListener(this);
        mPlayerController.setOrientationChangedListener(mOritationChangedListener);
	 	mPlayerController.setDefChangedListener(this);
    }
    
    private void beforeStart() {
        stopPlayTask();
        resetStatus();
       	initPlayerCtrl();
    }
    
    private void afterStop() {
    	resetStatus();
    }

    private void resetStatus() {
   		mDefinitionNames.clear();
    	mDefinitionChanged = false;
    	mHistoryPosition = 0;
    	mDuration = 0;
    	mDecodeChanged = false;
    	mCurStreamId = MediaCenter.INVALID_STREAM_ID;
    	mResuming = false;
   		mVideoPlayerHandler.removeCallbacksAndMessages(null);

        if (mPlayerController != null && mPlayerController.isStarted()) {
            mPlayerController.onDestroy();
            mPlayerController = null;
        }

        if (mPlayerController == null) {
        	buildPlayerCtrl();
        }
    }
    
    private void startStream() {
    	BFYLog.d(TAG, "startStream");
    	if (BFYNetworkUtil.isWifiEnabled(mContext)) {
    		BFYNetworkUtil.getInstance().setNetworkCode(BFYNetworkStatusData.NETWORK_CONNECTION_WIFI);
    		continueStartStream(MediaCenter.NetState.NET_WIFI_REACHABLE);            
    	} else if (BFYNetworkUtil.isMobileEnabled(mContext)) {
    		BFYNetworkUtil.getInstance().setNetworkCode(BFYNetworkStatusData.NETWORK_CONNECTION_MOBILE);
    		// 提醒是否继续播放
       		mVideoPlayerHandler.sendEmptyMessage(BFYMessageKeys.MESSAGE_P2P_INIT_MOBILE_NETWORK);       
    	} else {
    		mNetStatus = MediaCenter.NetState.NET_NOT_REACHABLE;
    		BFYNetworkUtil.getInstance().setNetworkCode(BFYNetworkStatusData.NETWORK_CONNECTION_NONE);
    		// 错误提示，报错
       		mVideoPlayerHandler.sendEmptyMessage(BFYMessageKeys.MESSAGE_P2P_INIT_NO_NETWORK_ERROR);       
    	}
    }
    
    private void continueStartStream(int netState) {
    	BFYLog.d(TAG, "continueStartStream");
    	// 直接启动P2P进行播放
		int error = MediaCenter.NativeReturnType.NO_ERROR;
		if (null == mBfStream) {        		
    		BFYLog.d(TAG, "startStream,p2p init path=" + mDataPath);        		
    		mNetStatus = netState;
        	mBfStream = new BFStream();
        	error = mBfStream.init(mDataPath, mNetStatus);            	
        } else if (mNetStatus != netState) {
        	mBfStream.uninit();
        	mNetStatus = netState;
        	error = mBfStream.init(mDataPath, mNetStatus);
        }
		if (error != MediaCenter.NativeReturnType.NO_ERROR) {
    		if (null != mVideoPlayerHandler) {
    			mPlayError = error;
        		mVideoPlayerHandler.sendEmptyMessage(BFYMessageKeys.MESSAGE_P2P_INIT_ERROR);
        	}	        		
    		return;
    	}
		
		mP2pTaskStarted = true;
    	mBfStream.registerListener(mStreamListener);
        
    	String playUrl = mPlayUrl;
    	if (mResuming) {
    		mResuming = false;
    		if (mPlayTaskType == PlayTaskType.LIVE)
    			playUrl += "&liveresume=1";
    	}
    	
    	if (mLiveLowLatency) {
    		playUrl += "&livelowlatency=1";
    	}
    	
    	mBfStream.play(playUrl, mPlayToken, MediaCenter.INVALID_STREAM_ID, MediaCenter.StreamMode.STREAM_HLS_MODE);	
    }
    
    private void stopStream() {
    	if (mBfStream != null) {    		
    		mBfStream.unregisterListener();
    		mBfStream.destroyCurMediaHandle();
    		mP2pTaskStarted = false;
    	}
    }

    private void doStop() {
    	if (mPlayerController != null) {
            mPlayerController.stop();
        }
   		stopPlayTask();
   		mStopped = true;
    }
    
    private void doResume() {
    	if (mStopped) {
    		mStopped = false;
        	mResuming = true;
        	
        	if (mPlayerController != null) {
                mPlayerController.reloadDecodeView();
            }
        	startPlayTask();
    	}
    }
    
    private int getStreamIdByDefinition(VideoDefinition definition) {
    	int result = MediaCenter.INVALID_STREAM_ID;
    	int streamCount = (mStreams != null) ? mStreams.size() : 0;
    	
		if (streamCount > 0) {
			int defaultStreamId = MediaCenter.INVALID_STREAM_ID;
    		for (int i = 0; i < streamCount; ++i) {
    			MediaCenter.StreamInfo streamInfo = mStreams.get(i);
    			if (streamInfo.defaultStream) {
    				defaultStreamId = streamInfo.streamId;
    				break;
    			}
    		}
    		
			if (definition == VideoDefinition.UNKNOWN) {
				result = defaultStreamId;
			} else {
	    		for (int i = 0; i < streamCount; ++i) {
	    			MediaCenter.StreamInfo streamInfo = mStreams.get(i);
	    			if (streamInfo.streamName.equalsIgnoreCase(definition.toString())) {
	    				result = streamInfo.streamId;
	    				break;
	    			}
	    		}
			}
    		
    		if (result == MediaCenter.INVALID_STREAM_ID) {
    			switch (definition) {
    			case FLUENT:
    				result = mStreams.get(0).streamId;
    				break;
    			case STANDARD:
    				result = mStreams.get(1).streamId;
    			case HIGH:
    				result = mStreams.get(streamCount / 2).streamId;
    				break;
    			case P1080:
    				result = mStreams.get(streamCount / 2 + 1).streamId;
    				break;
    			case K2:
    				result = mStreams.get(streamCount - 1).streamId;
    				break;
    			default:
    				result = mStreams.get(0).streamId;
    				break;
    			}
    		}

    		if (result == MediaCenter.INVALID_STREAM_ID) {
				result = defaultStreamId;
    		}
		}
    	
    	return result;
    }
    
    public VideoDefinition getDefinitionByStreamId(int streamId) {
    	VideoDefinition result = VideoDefinition.UNKNOWN;
    	int streamCount = (mStreams != null) ? mStreams.size() : 0;

		if (streamCount > 0) {
    		for (int i = 0; i < streamCount; ++i) {
    			MediaCenter.StreamInfo streamInfo = mStreams.get(i);
    			if (streamId == streamInfo.streamId) {
    				VideoDefinition d = VideoDefinition.fromString(streamInfo.streamName);
    				if (d != null) {
    					result = d;
    				}
    			}
    		}
		}
		
    	return result;
    }
    
    @Override
    public void onPause() {
        BFYLog.d(TAG, "onPause");
    	if (mPlayerController != null) {
            mPlayerController.onPause();
        }
    	
    	super.onPause();
    }

    @Override
    public void onResume() {
        BFYLog.d(TAG, "onResume");
        super.onResume();
    	if (mPlayerController != null) {
            mPlayerController.onResume();
        }

    	doResume();
    }

    @Override
    public void onStop() {
        BFYLog.d(TAG, "onStop");
        if (mPlayerController != null) {
            mPlayerController.onStop();
        }
        if (mPlayTaskType == PlayTaskType.VOD)
        	mHistoryPosition = mPlayerController.getCurPosition();
		doStop();
    	mIsFragmentStop = true;
        super.onStop();
    }
    
    @Override
    public void onStart() {
    	BFYLog.d(TAG, "onStart");
    	if (mPlayerController != null) {
            mPlayerController.onStart();
        }
    	if (mIsFragmentStop){
//    		startPlayTask();
    		mIsFragmentStop = false;
    	}
    	super.onStart();
    }

    @Override
    public void onDestroy() {
//    	if (mBfStream != null) {
//        	mBfStream.Uninit();
//    	}
    	if (mContext != null) {
    		mContext.unregisterReceiver(BFYNetworkUtil.getInstance());
    	}
    	
    	BFYLog.d(TAG, "onDestory,(null == mP2pThread)=" + (null == mP2pThread));
        if (null != mP2pThread)
            mP2pThread.releaseP2p();
        if (null != mPlayerController) {
            mPlayerController.onDestroy();
        }
        mPlayerController = null;
        mP2pThread = null;
        super.onDestroy();
    }
    
    /**
     * 横竖屏切换
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        BFYLog.d(TAG, "onConfigurationChanged==" + newConfig.orientation);
        if (null != mPlayerController) {
            mPlayerController.configurationChanged(newConfig);
        }
    }

    private class VideoPlayerHandler extends Handler {
        private final String TAG = VideoPlayerHandler.class.getSimpleName();

        private WeakReference<Fragment> mWeekReferenceFragment;

        public VideoPlayerHandler(Fragment fragment) {
            mWeekReferenceFragment = new WeakReference<Fragment>(fragment);
        }

        public void handleMessage(Message msg) {
            VideoPlayerFragment fragment = (VideoPlayerFragment) mWeekReferenceFragment.get();
            BFYLog.d(TAG, "handleMessage" + msg.what);
            switch (msg.what) {
				case BFYMessageKeys.MESSAGE_P2P_STREAM_START_SUCCESS:
					if (false == fragment.mDefinitionChanged || false == fragment.mDecodeChanged) {
						if (null != fragment.mPlayerController) {
							if (null == fragment.mVideoName || fragment.mVideoName == "") {
								MediaCenter.MediaInfo mediaInfo = fragment.mBfStream.getMediaInfo();
								if (null != mediaInfo) {
									fragment.mVideoName = mediaInfo.mediaName;
								}
							}
							int dotIndex = fragment.mVideoName.lastIndexOf(".");
							if (dotIndex != -1) {
								fragment.mVideoName = fragment.mVideoName.substring(0, dotIndex);
							}
	
							fragment.mPlayerController.setVideoTitle(fragment.mVideoName);
							fragment.mPlayerController.setMinfoButtonEnable(false);
	
							// fragment.mPlayerController.setDefChangedListener(fragment);
	
							// start play
							if (null != fragment.mPlayerController) {
								BFYLog.d(TAG, "PlayerController set p2p stream server port:" + fragment.mStreamPort);
								fragment.mDefinitionNames.clear();
								fragment.mPlayerController.setP2pStreamServerPort(fragment.mStreamPort);
								fragment.mStreams = fragment.mBfStream.getStreamInfoList();
								
								int streamCount = fragment.mStreams.size();
								int selectedStreamId = fragment.getStreamIdByDefinition(fragment.mExpectedDefinition);
								BFYVideoInfo videoInfo = null;
								
								for (int i = 0; i < streamCount; i++) {
									MediaCenter.StreamInfo stream = fragment.mStreams.get(i);
									if (stream.streamId == selectedStreamId) {
										BFYLog.d(TAG, "current stream id:" + stream.streamId);
										BFYVideoInfo item = new BFYVideoInfo(fragment.mPlayUrl);
										item.setDuration(stream.duration);
										videoInfo = item;
										fragment.mCurDefinitionName = stream.streamName;
										fragment.mDuration = stream.duration;
										fragment.mCurStreamId = stream.streamId;
									}
	
									// get definitions
									fragment.mDefinitionNames.add(stream.streamName);
								}
	
								fragment.mPlayerController.setDefinitions(fragment.mDefinitionNames);
								if (videoInfo != null) {
									fragment.sendPlayCommand(videoInfo, fragment.mHistoryPosition);
								}
							}
						}
					} else {
						fragment.changePlay();
					}
            		break;
            		
            	case BFYMessageKeys.MESSAGE_P2P_STREAM_START_FAILURE:
            		BFYLog.d(TAG, "P2P start stream error");
           			fragment.sendErrorCommand(fragment.mPlayError);
            		break;
            		
            	case BFYMessageKeys.MESSAGE_P2P_INIT_ERROR:
            		BFYLog.d(TAG, "P2P init error");            	
            		fragment.sendErrorCommand(fragment.mPlayError);
            		break;
            		
            	case BFYMessageKeys.MESSAGE_P2P_INIT_NO_NETWORK_ERROR:
            		BFYLog.d(TAG, "P2P init no network");            	
            		fragment.sendErrorCommand(PlayErrorManager.NO_NETWORK);            		
            		break;
            		
            	case BFYMessageKeys.MESSAGE_P2P_INIT_MOBILE_NETWORK:
            		BFYLog.d(TAG, "P2P init mobile network");
            		if (null != fragment.mBfStream) {
            			fragment.mBfStream.uninit();
            			fragment.mBfStream = null;
            		}
            		/*if (null != fragment.mPlayerController) {
            			fragment.mPlayerController.onMobileNetwork();
            		}*/
            		fragment.sendNetworkCommand();
            		break;            	
            		
            	case BFYMessageKeys.MESSAGE_CHANGE_DECODE_MODE:
                    if (null != fragment.mPlayerController) {
                    	DecodeMode newDecodeMode = DecodeMode.valueOf(msg.arg1);
                        if (fragment.mDecodeMode != newDecodeMode) {
                        	fragment.mDecodeChanged = true;
                        	fragment.setDecodeMode(newDecodeMode);
                        	fragment.initPlayerCtrl();
                        	
                            if (null != fragment.mBfStream) {
                            	fragment.mBfStream.play(fragment.mPlayUrl, fragment.mPlayToken, fragment.mCurStreamId, MediaCenter.StreamMode.STREAM_HLS_MODE);
                            } else {
                            	fragment.mP2pThread.sendStartStream();
                            }
                        }
                    }
            		break;
            }
        }
    }
    
    private void sendPlayCommand(BFYVideoInfo videoInfo, long historyPosition) {
        PlayerCommand playerCommand = new PlayerCommand(videoInfo, historyPosition);
        playerCommand.setCommand(PlayerCommand.START);
        PlayerEvent event = new PlayerEvent(playerCommand);
        BFYEventBus.getInstance().post(event);
    }
    
    private void sendPauseCommand() {
        PlayerCommand playerCommand = new PlayerCommand();
        playerCommand.setCommand(PlayerCommand.PAUSE);
        PlayerEvent event = new PlayerEvent(playerCommand);
        BFYEventBus.getInstance().post(event);
    }

    private void sendErrorCommand(int errorCode) {
        PlayerCommand playerCommand = new PlayerCommand();
        playerCommand.setCommand(PlayerCommand.ERROR);
        playerCommand.setErrorMsg("error: " + errorCode);
        playerCommand.setErrorCode(errorCode);
        PlayerEvent event = new PlayerEvent(playerCommand);
        BFYEventBus.getInstance().post(event);
    }
    
    private void sendNetworkCommand() {
        PlayerCommand playerCommand = new PlayerCommand();
        playerCommand.setCommand(PlayerCommand.NETWORK);
        PlayerEvent event = new PlayerEvent(playerCommand);
        BFYEventBus.getInstance().post(event);
    }
    
    private void changePlay() {
        int count = mStreams.size();
        BFYVideoInfo videoInfo = null;
        for (int i = 0; i < count; i ++) {
        	MediaCenter.StreamInfo stream = mStreams.get(i);
            if (mCurDefinitionName.equalsIgnoreCase(stream.streamName)) {
                BFYLog.d(TAG, "current stream id:" + stream.streamId);
                BFYVideoInfo item = new BFYVideoInfo(mPlayUrl);
                item.setDuration(stream.duration);
                videoInfo = item;
                mDuration = stream.duration;
                break;
            }
        }

        if (videoInfo != null) {
        	sendPlayCommand(videoInfo, mHistoryPosition);
        }
    }

    private BFStream.BFStreamMessageListener mStreamListener = new BFStream.BFStreamMessageListener() {
        @Override
        public void onMessage(int type, int data) {
            BFYLog.d(TAG, "BFStreamMessageListener");
            switch (type) {
                case BFStream.BFStreamMessageListener.MSG_TYPE_CREATE_STREAM_SUCCESS:                	
                    mStreamPort = data;
                    if (null != mP2pThread) {
                    	mP2pThread.sendP2pStartSuccess();
                    }             
                    break;
                case BFStream.BFStreamMessageListener.MSG_TYPE_ERROR:
                	mPlayError = data;
                	if (null != mP2pThread) {
                    	mP2pThread.sendP2pStartFailure();
                    }
                	break;
            }
        }
    };
    
    public void changeDefinition(int streamId) {
    	
    	BFYLog.d(TAG, "changeDefinition. streamId:" + streamId);
    	
    	if (streamId == MediaCenter.INVALID_STREAM_ID || mCurStreamId == streamId) {
    		return;
    	}
    	
        mDefinitionChanged = true;

        for (int i = 0; i < mStreams.size(); i++) {
            if (mStreams.get(i).streamId == streamId) {
                mCurDefinitionName = mStreams.get(i).streamName;
                break;
            }
        }

        if (null != mPlayerController) {
            mHistoryPosition = mPlayerController.getCurPosition();
            if (-1 == mHistoryPosition) {
                mHistoryPosition = 0;
            }
        }

        for (int i = 0; i < mStreams.size(); i++) {
			MediaCenter.StreamInfo streamInfo = mStreams.get(i);
            if (streamInfo.streamId == streamId) {
                if (mHistoryPosition >= streamInfo.duration) {
                    mHistoryPosition = 0;
                }

                sendPauseCommand();
                
                mCurStreamId = streamInfo.streamId;
                mBfStream.play(mPlayUrl, mPlayToken, streamInfo.streamId, MediaCenter.StreamMode.STREAM_HLS_MODE);
                break;
            }
        }
    }
    
    @Override
    public void onDefinitionChanged(String name) {
        if (mCurDefinitionName.equalsIgnoreCase(name)) {
            return;
        }
        
        int streamId = MediaCenter.INVALID_STREAM_ID;
        for (int i = 0; i < mStreams.size(); i++) {
            if (mStreams.get(i).streamName.equalsIgnoreCase(name)) {
            	streamId = mStreams.get(i).streamId;
                break;
            }
        }
        
        changeDefinition(streamId);
    }

    @Override
    public String getShowDefinition() {
        return mCurDefinitionName;
    }

    @Override
    public void onError(int errorCode) {
        BFYLog.d(TAG, "onError");
        
        PlayErrorListener playErrorListener = mPlayErrorListener;

        if (null != mPlayerController) {
            mHistoryPosition = mPlayerController.getCurPosition();
            if (-1 == mHistoryPosition || mHistoryPosition >= mDuration) {
                mHistoryPosition = 0;
            }
         // 已经出错，不再继续
            if (mPlayerController.isPlayWaitState()) return;
        }               
        
        if (mDecodeMode == DecodeMode.AUTO) {
        	changeDecodeMode(DecodeMode.SOFT);
        } else {
	        //reset();
	        sendErrorCommand(errorCode);
	
	        if (null != playErrorListener) {
	        	playErrorListener.onError(errorCode);
	        }
        }
    }
    
    @Override
    public void onNetworkError(int errorCode) {
        if (null != mPlayerController) {
            mHistoryPosition = mPlayerController.getCurPosition();
            if (-1 == mHistoryPosition || mHistoryPosition >= mDuration) {
                mHistoryPosition = 0;
            }
        }
        /*if (null != mPlayerController) {
        	mPlayerController.onPause();
        }*/       
    }
    
    @Override
    public void onRestartPlay() {
    	BFYLog.d(TAG, "onRestartPlay");
        /*if (null != mPlayerController) {
        	mPlayerController.onResume();
        }*/    
    	if (mPlayTaskType == PlayTaskType.VOD)
    		mHistoryPosition = mPlayerController.getCurPosition();
		startPlayTask();
    }
    
    @Override
    public void onContinuePlay() {
    	BFYLog.d(TAG, "onContinuePlay");
    	/*if (null != mPlayerController) {
        	mPlayerController.onResume();
        }*/    
//    	if (mPlayTaskType == PlayTaskType.VOD)
//    		mHistoryPosition = mPlayerController.getCurPosition();
    	continuePlayTask();
    }
    
    @Override
    public boolean onRestartFromBeginning() {
    	BFYLog.d(TAG, "onRestartFromBeginning");
    	mHistoryPosition = 0;
    	if (BFYNetworkUtil.hasNetwork(mContext)) {  
    		int netState = MediaCenter.NetState.NET_NOT_REACHABLE;
    		if (BFYNetworkUtil.getNetworkCode() == BFYNetworkStatusData.NETWORK_CONNECTION_WIFI) {
    			netState = MediaCenter.NetState.NET_WIFI_REACHABLE;
    		} else {
    			netState = MediaCenter.NetState.NET_WWAN_REACHABLE;
    		}
    		
    		if (netState != mNetStatus) {    			
    			return true;
    		} else {
    			return false;
    		}
    	} else {    		
    		return true;
    	}
    }
    
    public void continuePlayTask() {
        BFYLog.d(TAG, "continuePlayTask start");
        if (null != mPlayerController) {
        	mPlayerController.hidTipsHolder();	
        }   
        if (null == mP2pThread) return;
        mP2pThread.sendContinueStartStream();
        BFYLog.d(TAG, "continuePlayTask end");
    }
    
    public void startPlayTask() {
        BFYLog.d(TAG, "startPlayTask start");     
        if (null == mP2pThread) return;
        mP2pThread.sendStartStream();
        BFYLog.d(TAG, "startPlayTask end");
    }
    
    public void stopPlayTask() {
    	BFYLog.d(TAG, "stopPlayback start");
        if (null == mP2pThread) return;
        mP2pThread.sendStopStream();
        BFYLog.d(TAG, "stopPlayback end");
    }
    
    private class P2pPlayThread extends HandlerThread {
    	private boolean mLooperAvailable;
        private Handler mP2pHandler;
        private boolean mDelayStartStream;
        
        public P2pPlayThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        public void run() {
            BFYLog.d(TAG, "P2pPlayThread,run in,threadId=" + getId());
            super.run();
            BFYLog.d(TAG, "P2pPlayThread,run out");
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            BFYLog.d(TAG, "P2pPlayThread,onLooperPrepared in");
            mP2pHandler = new Handler(getLooper(), mCallback);
            mLooperAvailable = true;
            BFYLog.d(TAG, "P2pPlayThread,onLooperPrepared out");
            if (mDelayStartStream) {
            	sendStartStream();
            }
        }

        private void p2pRelease() {
        	BFYLog.d(TAG, "p2pRelease start");
        	mLooperAvailable = false;
        	if (mBfStream != null) {
            	mBfStream.uninit();
            	mBfStream = null;
        	}
            int quited = 0;
            BFYLog.d(TAG, "p2pRelease,Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quited = quitSafely() ? 1 : -1;
            } else {
                quited = quit() ? 1 : -1;
            }
            BFYLog.d(TAG, "p2pRelease,quited=" + quited);
        }

        private Handler.Callback mCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                int what = msg.what;
                BFYLog.d(TAG, "P2pPlayProxyThread,handleMessage,what=" + what);
                if (what == MediaPlayerConstant.P2P_DESTORY_TASK_CALLBACK) {
                	BFYLog.d(TAG, "P2pPlayProxyThread,P2P_DESTORY_TASK_CALLBACK,msg.arg1=" + msg.arg1);
                } else if (what == MediaPlayerConstant.P2P_STOP_PLAYBACK) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_STOP_PLAYBACK,msg.arg1=" + msg.arg1);
                    stopStream();
                    return true;
                } else if (what == MediaPlayerConstant.P2P_START_PLAYBACK) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_START_PLAYBACK,msg.arg1=" + msg.arg1);
                	startStream();
                    return true;
                } else if (what == MediaPlayerConstant.P2P_CONTINUE_PLAYBACK) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_CONTINUE_PLAYBACK,msg.arg1=" + msg.arg1);
                	continueStartStream(MediaCenter.NetState.NET_WWAN_REACHABLE);
                    return true;
                } else if (what == MediaPlayerConstant.P2P_START_PLAY_SUCCESS) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_START_PLAY_SUCCESS,msg.arg1=" + msg.arg1);
                	if (mP2pTaskStarted) {
                   		mVideoPlayerHandler.sendEmptyMessage(BFYMessageKeys.MESSAGE_P2P_STREAM_START_SUCCESS);
                	}                	                
                    return true;
                } else if (what == MediaPlayerConstant.P2P_START_PLAY_FAILURE) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_START_PLAY_FAILURE,msg.arg1=" + msg.arg1);
                	if (mP2pTaskStarted) {
                   		mVideoPlayerHandler.sendEmptyMessage(BFYMessageKeys.MESSAGE_P2P_STREAM_START_FAILURE);
                	}                	                
                    return true;
                } else if (what == MediaPlayerConstant.P2P_RELEASE) {
                	BFYLog.d(TAG, "P2pPlayThread,P2P_RELEASE");
                    p2pRelease();
                } else {
                	BFYLog.d(TAG, "P2pPlayThread,handleMessage unknown what");
                }
                return false;
            }
        };
        
        private void sendStartStream() {
        	BFYLog.d(TAG, "P2pPlayThread,sendStart" + ",mLooperAvailable=" + mLooperAvailable);
            if (mLooperAvailable && null != mP2pHandler) {
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_START_PLAYBACK, -1, -1, null).sendToTarget();
            } else {
            	mDelayStartStream = true;
            }
        }
        
        private void sendContinueStartStream() {
        	BFYLog.d(TAG, "P2pPlayThread,sendContinueStartStream" + ",mLooperAvailable=" + mLooperAvailable);
            if (mLooperAvailable && null != mP2pHandler) {
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_CONTINUE_PLAYBACK, -1, -1, null).sendToTarget();
            }
        }
        
        private void sendStopStream() {
        	BFYLog.d(TAG, "P2pPlayThread,sendStop,mLooperAvailable=" + mLooperAvailable);
            if (mLooperAvailable && null != mP2pHandler) {
            	BFYLog.d(TAG, "P2pPlayProxyThread,sendStop callback,mLooperAvailable && null != mP2pHandler");
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_STOP_PLAYBACK, -1, -1, null).sendToTarget();
            }
        }
        
        private void sendP2pStartSuccess() {
        	BFYLog.d(TAG, "P2pPlayThread,sendP2pStartSuccess" + ",mLooperAvailable=" + mLooperAvailable);
            if (mLooperAvailable && null != mP2pHandler) {
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_START_PLAY_SUCCESS, -1, -1, null).sendToTarget();
            }
        }
        
        private void sendP2pStartFailure() {
        	BFYLog.d(TAG, "P2pPlayThread,sendP2pStartFailure" + ",mLooperAvailable=" + mLooperAvailable);
            if (mLooperAvailable && null != mP2pHandler) {
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_START_PLAY_FAILURE, -1, -1, null).sendToTarget();
            }
        }        
        
        public void releaseP2p() {
        	BFYLog.d(TAG, "P2pPlayThread,releaseP2p" + ",mLooperAvailable=" + mLooperAvailable);
            if (null != mP2pHandler) {
                mP2pHandler.obtainMessage(MediaPlayerConstant.P2P_RELEASE, -1, -1, null).sendToTarget();
            }
        }
    }
    
    private PlayerController.OrientationChangedListener mOritationChangedListener = new PlayerController.OrientationChangedListener() {
        @Override
        public void onOrientationChanged(boolean isFullScreen, int screenWidth, int screenHeight) {
   
            	ViewGroup parentLayout = (ViewGroup)((Activity) mContext).findViewById(BFYResUtil.getId(mContext, "playerFragment"));
            	if (null != parentLayout) {
            		ViewGroup.LayoutParams parentParams = (ViewGroup.LayoutParams)parentLayout.getLayoutParams();
            		if (null != parentParams) {           			
            			if (isFullScreen) {
            				parentParams.width = screenHeight;
                			parentParams.height = screenWidth;
            			} else {
            				parentParams.width = screenWidth;
                			parentParams.height = (int) (screenWidth * BFYConst.DEFAULT_VIDEO_VIEW_ASPECT_RATIO);
            			}
            			
            			parentLayout.setLayoutParams(parentParams);
            			parentLayout.requestLayout();
            		}
            	}
            }
    	};
}
