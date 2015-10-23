package bf.cloud.black_board_ui;

import bf.cloud.android.base.BFYConst;
import bf.cloud.android.playutils.PlayTaskType;

public class PlayErrorManager {
	private int mErrorCode = 0;

	public PlayErrorManager() {
	}

	public String getErrorShowTips(PlayTaskType type) {
		String tips = "";
		if (mErrorCode >= BFYConst.PLAYER_ERROR_MIN
				&& mErrorCode <= BFYConst.PLAYER_ERROR_MAX) {
			tips = BFYConst.PLAYER_ERROR_SHOW_TIPS;
		} else if (mErrorCode >= BFYConst.MEDIA_INFO_ERROR_MIN
				&& mErrorCode <= BFYConst.MEDIA_INFO_ERROR_MAX) {
			switch (mErrorCode) {
			case BFYConst.MEDIA_MOVIE_INFO_NOT_FOUND: {
				if (type == PlayTaskType.VOD) {
					tips = BFYConst.MEDIA_INFO_ERROR_TIPS_VOD_2005;
				} else if (type == PlayTaskType.LIVE) {
					tips = BFYConst.MEDIA_INFO_ERROR_TIPS_LIVE_2005;
				}
				break;
			}
			case BFYConst.MEDIA_MOVIE_INFO_FORBIDDEN: {
				tips = BFYConst.MEDIA_INFO_ERROR_TIPS_2008;
				break;
			}
			case BFYConst.MEDIA_MOVIE_INFO_UNAUTHORIZED: {
				tips = BFYConst.MEDIA_INFO_ERROR_TIPS_2009;
				break;
			}
			default: {
				tips = BFYConst.MEDIA_INFO_ERROR_SHOW_TIPS;
				break;
			}
			}
		} else if (mErrorCode >= BFYConst.P2P_ERROR_MIN
				&& mErrorCode <= BFYConst.P2P_ERROR_MAX) {
			switch (mErrorCode) {
			case BFYConst.P2P_NO_DATA_SOURCE: {
				if (type == PlayTaskType.LIVE) {
					tips = BFYConst.P2P_ERROR_TIPS_LIVE_3006;
				}
				break;
			}
			default: {
				tips = BFYConst.P2P_ERROR_SHOW_TIPS;
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
