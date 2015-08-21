package bf.cloud.android.components.mediaplayer.proxy;

import android.content.Context;
import android.os.Message;

import java.lang.ref.WeakReference;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.base.BFYEventBus;
import bf.cloud.android.components.BFYNetworkStatusData;
import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayErrorManager;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.player.PlayerCommand;
import bf.cloud.android.events.BFYNetworkStatusReportEvent;
import bf.cloud.android.events.PlayerEvent;
import bf.cloud.android.models.beans.BFYVideoInfo;
import bf.cloud.android.modules.p2p.MediaCenter;
import bf.cloud.android.modules.player.videoviewexo.VideoViewExo;
import bf.cloud.android.modules.player.videoviewsd.VideoViewStormSw;
import bf.cloud.android.utils.BFYWeakReferenceHandler;
import bf.cloud.android.modules.log.BFYLog;

/**
 * Created by gehuanfei on 2014/9/19.
 */
public abstract class PlayProxy {

    private static final String TAG = PlayProxy.class.getSimpleName();

    protected Context mContext;
    protected WeakReference<PlayerController.PlayerViewControl> mMediaPlayerRef;    
    protected BFYVideoInfo mVideoInfo;
    protected PlayerEvent mPlayerEvent;
    protected String mP2pStreamServer = MediaPlayerConstant.P2PSERVER;
    protected int mP2pStreamPort = 0;
    protected String mVideoUrl;

    //上次调用来自onEvent
    protected boolean mLastFromEvent;
    protected boolean mProxyValid;
    //activity是否在活动状态
    protected boolean mActivityRunning;

    protected PlayProxy() {

    }

    protected PlayProxy(WeakReference<PlayerController.PlayerViewControl> controlRef) {
        BFYLog.d(TAG, "PlayProxy() start");
        init(controlRef);
        BFYLog.d(TAG, "PlayProxy() end");
    }

    public void init(WeakReference<PlayerController.PlayerViewControl> controlRef) {
        mMediaPlayerRef = controlRef;
        BFYLog.d(TAG, "PlayProxy,init," + playerTag());
        if (null == mNetworkHandler) {
            synchronized (PlayProxy.class) {
                if (null == mNetworkHandler) {
                    mNetworkHandler = new NetworklHandler(this);
                }
            }
        }
        if (null != controlRef) {
            PlayerController.PlayerViewControl control = controlRef.get();
            if (null != control) {
                mContext = control.getControlContext();
                BFYLog.d(TAG, "PlayProxy,init,setPlayProxy," + playerTag());
                control.setPlayProxy(this);
            }
        }
    }

    protected String playerTag() {
        String className = getClass().getSimpleName();
        String tag = className + "#########playerTag###########";
        if (null == mMediaPlayerRef) return tag;
        PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
        if (null != mMediaPlayer) {
            if (mMediaPlayer instanceof VideoViewExo) {
                tag = className + ",invoke by VideoViewExo";
            } else if (mMediaPlayer instanceof VideoViewStormSw) {
                tag = className + ",invoke by VideoViewStormSw";
            }
        }
        return tag;
    }

    /**
     * 播放视频
     *
     * @param pos         起始播放位置
     */
    public abstract void executePlay(long pos);

    public void release() {
        BFYLog.d(TAG, "release");
        mActivityRunning = false;
        mContext = null;
        mPlayerEvent = null;
        mMediaPlayerRef = null;
        mNetworkHandler = null;
    }

    public void destroyPlayTask() {

    }

    public abstract boolean hasVideoData();

    /**
     * 处理播放,数据初始化、网络判断等.onResume先执行,onEvent后执行
     *
     * @param fromEvent onResume先执行,为false;onEvent后执行,为true
     */
    public void playEventProceed(boolean fromEvent) {
        BFYLog.d(TAG, "playEventProceed start,fromEvent=" + fromEvent + "," + playerTag());

        if (null == mPlayerEvent) {
            BFYLog.d(TAG, "playEventProceed,null == mMediaPlayer.getPlayerEvent()");
            mNetworkHandler.sendEmptyMessage(MediaPlayerConstant.NO_NETWORK);
            return;
        }

        if (!hasVideoData()) {
            BFYLog.d(TAG, "playEventProceed,!mMediaPlayer.hasVideoData()");
            mNetworkHandler.sendEmptyMessage(MediaPlayerConstant.NO_NETWORK);
            return;
        }
        
        start();

        BFYLog.d(TAG, "playEventProceed end");
    }

    private void start() {
        BFYLog.d(TAG, "start," + playerTag());
        PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
        if (null != mMediaPlayer && mMediaPlayer.isInPlaybackState()) {
            boolean complete = mMediaPlayer.isPlayCompete();
            BFYLog.d(TAG, "start,inplayback,complete=" + complete);
            if (!complete) {
                mMediaPlayer.start();
            }
        } else {
            BFYLog.d(TAG, "start,not inplayback");
            if (null == mPlayerEvent) {
                BFYLog.d(TAG, "start,null == mMediaPlayer.getPlayerEvent()");
                return;
            }
            PlayerCommand commandManager = (PlayerCommand) mPlayerEvent.getData();
            if (null == commandManager) {
                BFYLog.d(TAG, "start,null == commandManager");
                return;
            }
            int command = commandManager.getCommand();
            if (PlayerCommand.START == command) {
                long historyPosition = commandManager.getHistoryPosition();
                playPrepare((int) historyPosition);
            }
        }
    }

    public void onResume() {
        BFYLog.d(TAG, "onResume");
        mActivityRunning = true;
    }

    /**
     * 操作播放器
     *
     * @param historyPosition 初始化时为播放历史,seek是定位位置
     */
    public void playPrepare(int historyPosition) {
        BFYLog.d(TAG, "playPrepare,historyPosition=" + historyPosition);
        if (historyPosition < 0) return;
        PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
        if (null != mMediaPlayer) {
            mMediaPlayer.playPrepare(historyPosition);
        }
    }


    public void onDestory() {
        release();
    }

    public void setLastFromEvent(boolean fromEvent) {
        mLastFromEvent = fromEvent;
    }

    private NetworklHandler mNetworkHandler;

    public void doNetworkEvent(BFYNetworkStatusReportEvent event) {
        BFYLog.d(TAG, "doNetworkEvent,NetworkStatusReportEvent," + playerTag());
        if (null == event) return;

        BFYNetworkStatusData status = (BFYNetworkStatusData) event.getData();
        if (null == status) return;
        
        PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();        
        if (null == mMediaPlayer) return;
        if (mMediaPlayer.isPlayCompete() || mMediaPlayer.getCurrentState() == MediaPlayerConstant.STATE_ERROR) return;  
        
        BFYLog.d(TAG, "doNetworkEvent,status=" + status.getStatusCode());
        
        if (status.getStatusCode() == BFYNetworkStatusData.NETWORK_CONNECTION_NONE) {
            mNetworkHandler.sendEmptyMessage(MediaPlayerConstant.NO_NETWORK);
        } else if (status.getStatusCode() == BFYNetworkStatusData.NETWORK_CONNECTION_WIFI) {
            mNetworkHandler.sendEmptyMessage(MediaPlayerConstant.NETWORK_WIFI);
        } else if (status.getStatusCode() == BFYNetworkStatusData.NETWORK_CONNECTION_MOBILE) {
            mNetworkHandler.sendEmptyMessage(MediaPlayerConstant.NETWORK_MOBILE);
        }
    }

    public float getPlaySpeed(String videoPath) {
        return 0;
    }

    public void onPause() {
        mActivityRunning = false;
    }

    public void onStop() {
        mActivityRunning = false;
    }

    public boolean isAlive() {
        return false;
    }

    public void stopPlayback(CallbackOuter callback) {
        BFYLog.d(TAG, "stopPlayback");
    }

    public boolean isPlayValid() {
        return mProxyValid;
    }

    public void setPlayValid(boolean valid) {
        mProxyValid = valid;
    }

    public void setPlayEvent(PlayerEvent event) {
        mPlayerEvent = event;
    }

    public void setP2pStreamServerPort(int port, int streamMode) {
        mP2pStreamPort = port;
        if (MediaCenter.StreamMode.STREAM_MP4_MODE == streamMode) {
            mVideoUrl = mP2pStreamServer + ":" + port;
        } else if (MediaCenter.StreamMode.STREAM_HLS_MODE == streamMode) {
            mVideoUrl = mP2pStreamServer + ":" + port + "/bfcloud.m3u8";
        } else {
            mVideoUrl = mP2pStreamServer + ":" + port;
        }
    }

    public String getVideoUrl() {
        if (mP2pStreamPort == 0)  {
            return "";
        } else {
            return mVideoUrl;
        }
    }

    public void setActivityRunning(boolean running) {
        BFYLog.d(TAG, "setActivityRunning");
        mActivityRunning = running;
    }

    public void setVideoInfo(BFYVideoInfo videoInfo) {
        mVideoInfo = videoInfo;
        PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
        if (null != mMediaPlayer) {
            mMediaPlayer.setVideoInfo(videoInfo);
        }
    }
    
    private static class NetworklHandler extends BFYWeakReferenceHandler<PlayProxy> {
    	private final String NO_NETWORK = "没有网络连接";
        private final String NET_2G3G = "非WiFi网络下,是否继续播放?";

        public NetworklHandler(PlayProxy reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(PlayProxy reference, Message msg) {
            if (msg == null) {
                return;
            }

            PlayerController.PlayerViewControl mMediaPlayer = reference.mMediaPlayerRef.get();
            if (null == mMediaPlayer) return;

            int what = msg.what;
            if (what == MediaPlayerConstant.NO_NETWORK) {
                BFYLog.d(TAG, "NetworklHandler,NO_NETWORK");
                mMediaPlayer.hideStatusController();
                BFYLog.d(TAG, "NetworklHandler,no Network");
                //没有网络,提示"没有网络"
                noneNetwork(reference, mMediaPlayer);                
            } else if (what == MediaPlayerConstant.NETWORK_WIFI) {
                BFYLog.d(TAG, "NetworklHandler,NETWORK_WIFI");
                //wifi网络下直接播放
            } else if (what == MediaPlayerConstant.NETWORK_MOBILE) {//2g/3g网络下
                BFYLog.d(TAG, "NetworklHandler,NETWORK_MOBILE");
                //2g/3g网络下,提示用户是否继续使用2g/3g网络播放
                if (mMediaPlayer.isInPlaybackState()) {
                    //如果在播放中,先停止播放
                    mMediaPlayer.pause();
                }
            } else if (what == MediaPlayerConstant.POPUP_DISMISS) {
                BFYLog.d(TAG, "NetworklHandler,what == POPUP_DISMISS");
            }
        }

        private void noneNetwork(PlayProxy reference, PlayerController.PlayerViewControl mMediaPlayer) {
            BFYLog.d(TAG, "noneNetwork,reference.mActivityRunning=" + reference.mActivityRunning);
            if (reference.mActivityRunning) {
            	reference.sendErrorCommand(PlayErrorManager.NO_NETWORK);
                //mMediaPlayer.showPlaceHolder();
            }
        }
    }
    
    public void sendErrorCommand(int errorCode) {
    	if (mActivityRunning) {
            PlayerCommand playerCommand = new PlayerCommand();
            playerCommand.setCommand(PlayerCommand.ERROR);
            playerCommand.setErrorMsg("error: " + errorCode);
            playerCommand.setErrorCode(errorCode);
            PlayerEvent event = new PlayerEvent(playerCommand);
            BFYEventBus.getInstance().post(event);
    	}
    }
    
    public interface CallbackOuter {
        /**
         * 播放服务停止成功
         */
        void playStopSuccess();

        void playStopFailure();
    }
}
