package bf.cloud.android.components.mediaplayer.proxy;

import java.lang.ref.WeakReference;

import bf.cloud.android.components.mediaplayer.MediaPlayerConstant;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.components.player.PlayerCommand;
import bf.cloud.android.models.beans.BFYVideoInfo;
import bf.cloud.android.modules.log.BFYLog;


/**
 * Created by gehuanfei on 2014/9/20.
 */
public class P2pPlayProxy extends PlayProxy {
    private static final String TAG = P2pPlayProxy.class.getSimpleName();

    private static WeakReference<P2pPlayProxy> mPlayProxyRef;
    protected boolean mP2pStartSuccess;
    protected String mCurrentUrl;
    private int mPlayPosition;
    
    private P2pCallbackInner mP2pCallbackInner = new P2pCallbackInner() {
        @Override
        public void p2pStartSuccess() {
            BFYLog.d(TAG, "P2pCallbackInner,p2pStartSuccess");
            mP2pStartSuccess = true;
            PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
            if (null != mMediaPlayer) {
                mMediaPlayer.executePlay(mPlayPosition);
            }
        }

        @Override
        public void p2pStartFailure(int code) {
            BFYLog.d(TAG, "P2pCallbackInner,p2pStartFailure,code=" + code);
            PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
            if (null != mMediaPlayer)
                mMediaPlayer.sendEmptyMessage(MediaPlayerConstant.UI_P2P_START_FAILURE);
        }

        @Override
        public void p2pDestorySuccess() {
            BFYLog.d(TAG, "P2pCallbackInner,playStopSuccess");
            mP2pStartSuccess = false;
            startPlayTask(mCurrentUrl, this);
        }

        @Override
        public void p2pDestroyFailure() {

        }
    };
    private P2pPlayProxy.P2pProxyCallback mP2pProxyCallback = new P2pPlayProxy.P2pProxyCallback() {
        @Override
        public void onStarted() {//只执行一次
            BFYLog.d(TAG, "P2pProxyCallback,onStarted");                                    
            if (null != mP2pCallbackInner) {
            	mP2pCallbackInner.p2pDestorySuccess();
            }            
        }

        @Override
        public void onInitialFailure(int code) {
            BFYLog.d(TAG, "P2pProxyCallback,onInitialFailure,code=" + code);

            if (null == mMediaPlayerRef) return;
            PlayerController.PlayerViewControl mMediaPlayer = mMediaPlayerRef.get();
            if (null != mMediaPlayer) {
                mMediaPlayer.sendEmptyMessage(MediaPlayerConstant.UI_P2P_INIT_FAILURE);
            }
        }
    };
    protected P2pPlayProxy(WeakReference<PlayerController.PlayerViewControl> control) {
        BFYLog.d(TAG, "P2pPlayProxy(PlayerViewControl),threadId=" + Thread.currentThread().getId());
        init(control);
    }

    public static P2pPlayProxy getInstance(WeakReference<PlayerController.PlayerViewControl> controlRef) {
        BFYLog.d(TAG, "P2pPlayProxy,getInstance");
        if (null == mPlayProxyRef || null == mPlayProxyRef.get()) {
            BFYLog.d(TAG, "getInstance,null == mPlayProxy");
            synchronized (P2pPlayProxy.class) {
                if (null == mPlayProxyRef || null == mPlayProxyRef.get()) {
                    P2pPlayProxy p2pPlayProxy = new P2pPlayProxy(controlRef);
                    mPlayProxyRef = new WeakReference<P2pPlayProxy>(p2pPlayProxy);
                    p2pPlayProxy.init();
                }
            }
        } else {
            P2pPlayProxy mPlayProxy = mPlayProxyRef.get();
            BFYLog.d(TAG, "P2pPlayProxy,getInstance1,(null == mPlayProxy)=" + (null == mPlayProxy));
            if (null != mPlayProxy)
                mPlayProxy.init(controlRef);
        }
        PlayerController.PlayerViewControl control = controlRef.get();
        if (null != control) {
            P2pPlayProxy mPlayProxy = mPlayProxyRef.get();
            BFYLog.d(TAG, "P2pPlayProxy,getInstance2,setPlayProxy,(null == mPlayProxy)=" + (null == mPlayProxy));
            control.setPlayProxy(mPlayProxy);
        }
        return mPlayProxyRef.get();
    }

    private void init() {     
        if (null != mP2pProxyCallback) {
            mP2pProxyCallback.onStarted();
        }
    }

    public void init(WeakReference<PlayerController.PlayerViewControl> control) {
        super.init(control);
        BFYLog.d(TAG, "P2pPlayProxy,init," + playerTag());
    }

    public boolean hasVideoData() {
        if (null == mPlayerEvent || null == mPlayerEvent.getData()) return false;

        PlayerCommand commandManager = (PlayerCommand) mPlayerEvent.getData();
        int command = commandManager.getCommand();
        if (PlayerCommand.START == command) {
            BFYVideoInfo videoInfo = commandManager.getVideoInfo();
            if (null == videoInfo) return false;

            String strTemp = videoInfo.getUrl();
            if (null != strTemp && !"".equals(strTemp.trim())) return true;
        }
        return false;
    }

    /**
     * 停止所有p2p播放服务
     */
    public void stopPlayback(CallbackOuter callback) {
        BFYLog.d(TAG, "stopPlayback");
        if (null != callback) {
        	callback.playStopSuccess();
        }    
    }

    public void startPlayTask(String video, P2pCallbackInner callback) {
        BFYLog.d(TAG, "startPlayTask,(null == callback)=" + (null == callback) + "," + video);
        if (null == video || null == callback) return;
        callback.p2pStartSuccess();
    }

    @Override
    public void onDestory() {
        BFYLog.d(TAG, "onDestory");
    }

    public float getPlaySpeed(String qstp) {
        return 0;
    }

    public void release() {
        super.release();
        BFYLog.d(TAG, "release");
        mPlayProxyRef = null;
    }

    public boolean isPlayValid() {
        mProxyValid = mP2pStartSuccess;
        return mProxyValid;
    }

    public void executePlay(long pos) {
        BFYLog.d(TAG, "executePlay,pos=" + pos);
        mPlayPosition = (int) pos;

        mCurrentUrl = mVideoInfo.getUrl();
        BFYLog.d(TAG, "executePlay,mCurrentUrl=" + mCurrentUrl);
        mP2pStartSuccess = false;
        startPlayTask(mCurrentUrl, mP2pCallbackInner);
    }

    public interface P2pCallbackInner {

        /**
         * p2p启动成功
         */
        void p2pStartSuccess();

        void p2pStartFailure(int code);

        /**
         * p2p停止成功
         */
        void p2pDestorySuccess();

        void p2pDestroyFailure();
    }

    public interface P2pProxyCallback {
        void onStarted();

        void onInitialFailure(int code);
    }
}
