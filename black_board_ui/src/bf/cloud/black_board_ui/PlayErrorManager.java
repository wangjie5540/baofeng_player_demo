package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.PlayTaskType;

public class PlayErrorManager {
	private int mErrorCode = 0;
	// error tips below
	public final static String PLAYER_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	public final static String MEDIA_INFO_ERROR_SHOW_TIPS = "网络好慢啊，我都受不了了 :(";
	public final static String P2P_ERROR_SHOW_TIPS = "哎呀，不小心异常了 :(";
	public final static String MEDIA_INFO_ERROR_TIPS_VOD_2005 = "亲，你确定这个视频还在吗？";
	public final static String MEDIA_INFO_ERROR_TIPS_LIVE_2005 = "直播还没有开始:(";
	public final static String MEDIA_INFO_ERROR_TIPS_LIVE_2006 = "直播已经结束了,再见:)";
	public final static String MEDIA_INFO_ERROR_TIPS_2008 = "没有权限看，赶紧去要一个授权吧！";
	public final static String MEDIA_INFO_ERROR_TIPS_2009 = "授权失效了，重新要一个吧！";
	public final static String P2P_ERROR_TIPS_LIVE_3009 = "直播已经结束了,再见:)";
	public final static String P2P_ERROR_TIPS_LIVE_3010 = "直播还没有开始:)";

	public PlayErrorManager() {
	}

	public String getErrorShowTips(PlayTaskType type) {
		String tips = "";
		if (mErrorCode >= BasePlayer.ERROR_PLAYER_ERROR_MIN
				&& mErrorCode <= BasePlayer.ERROR_PLAYER_ERROR_MAX) {
			tips = PLAYER_ERROR_SHOW_TIPS;
		} else if (mErrorCode >= BasePlayer.ERROR_MEDIA_INFO_ERROR_MIN
				&& mErrorCode <= BasePlayer.ERROR_MEDIA_INFO_ERROR_MAX) {
			switch (mErrorCode) {
			case BasePlayer.ERROR_MEDIA_MOVIE_INFO_NOT_FOUND: {
				if (type == PlayTaskType.VOD) {
					tips = MEDIA_INFO_ERROR_TIPS_VOD_2005;
				} else if (type == PlayTaskType.LIVE) {
					tips = MEDIA_INFO_ERROR_TIPS_LIVE_2005;
				}
				break;
			}
			case BasePlayer.ERROR_MEDIA_MOVIE_INFO_LIVE_ENDED: {
				if (type == PlayTaskType.LIVE) {
					tips = MEDIA_INFO_ERROR_TIPS_LIVE_2006;
				}
				break;
			}
			case BasePlayer.ERROR_MEDIA_MOVIE_INFO_FORBIDDEN: {
				tips = MEDIA_INFO_ERROR_TIPS_2008;
				break;
			}
			case BasePlayer.ERROR_MEDIA_MOVIE_INFO_UNAUTHORIZED: {
				tips = MEDIA_INFO_ERROR_TIPS_2009;
				break;
			}
			default: {
				tips = MEDIA_INFO_ERROR_SHOW_TIPS;
				break;
			}
			}
		} else if (mErrorCode >= BasePlayer.ERROR_P2P_ERROR_MIN
				&& mErrorCode <= BasePlayer.ERROR_P2P_ERROR_MAX) {
			switch (mErrorCode) {
			case BasePlayer.ERROR_P2P_LIVE_ENDED: {
				if (type == PlayTaskType.LIVE) {
					tips = P2P_ERROR_TIPS_LIVE_3009;
				}
				break;
			}
			case BasePlayer.ERROR_P2P_LIVE_NOT_BEGIN: {
				if (type == PlayTaskType.LIVE) {
					tips = P2P_ERROR_TIPS_LIVE_3010;
				}
				break;
			}
			default: {
				tips = P2P_ERROR_SHOW_TIPS;
				break;
			}
			}
		}

		return tips;
	}

	public void setErrorCode(int errorCode) {
		mErrorCode = errorCode;
	}

	public int getErrorCode() {
		return mErrorCode;
	}
}
