package bf.cloud.android.components.player;

import bf.cloud.android.models.beans.BFYVideoInfo;


/**
 * 功能：控制播放器行为命令
 * Created by gehuanfei on 2014/9/18.
 */
public class PlayerCommand {

    private BFYVideoInfo mVideoInfo;
    private long mHistoryPosition = 0;
    private int mCommand = 0;
    private String mErrorMsg = "";
    private int mErrorCode = 0;
    
    public final static int START = 0;
    public final static int PAUSE = 1;
    public final static int RESET = 2;
    public final static int STOP = 3;
    public final static int COMPLETE = 4;
    public final static int ERROR = 5;
    public final static int NETWORK = 6;

    public PlayerCommand(BFYVideoInfo videoInfo, long historyPosition) {
    	mVideoInfo = videoInfo;
        mHistoryPosition = historyPosition;
    }

    public PlayerCommand() {
    }

    public void setCommand(int command) {
        mCommand = command;
    }

    public void setErrorMsg(String errorMsg) {
    	mErrorMsg = errorMsg;
    }
    
    public void setErrorCode(int errorCode) {
		mErrorCode = errorCode;
	}
    
    public BFYVideoInfo getVideoInfo() {
        return mVideoInfo;
    }

    public String getErrorMsg() {
    	return mErrorMsg;
    }
    
    public int getErrorCode() {
		return mErrorCode;
	}

    public int getCommand() {
        return mCommand;
    }

    public boolean isComplete() {
        return this.mCommand == COMPLETE;
    }

    public long getHistoryPosition() {
        return mHistoryPosition;
    }

    public void setHistoryPosition(int pos) {
        mHistoryPosition = pos;
    }
}
