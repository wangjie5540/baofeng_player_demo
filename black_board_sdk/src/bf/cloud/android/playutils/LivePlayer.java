package bf.cloud.android.playutils;

import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.modules.stat.StatReporter;

public class LivePlayer extends BasePlayer{
	
	public LivePlayer(VideoFrame vf, String settingDataPath) {
		super(vf, settingDataPath);
	}

	@Override
	protected void reportPlayExperienceStatInfo() {
		if (!canReportStatInfo()) return;
		StatInfo statInfo = mVideoView.getStatInfo();
		if (statInfo == null && mVideoInfo == null)
			return;
		prepareBaseStatInfo(statInfo);
		StatReporter.getInstance().report(statInfo.makeLiveExpUrl());
	}

	/**
	 */
	@Override
	protected void reportPlayProcessStatInfo() {
		if (!canReportStatInfo()) return;
		StatInfo statInfo = mVideoView.getStatInfo();
		if (statInfo == null || mVideoInfo == null)
			return;
		prepareBaseStatInfo(statInfo);
		StatReporter.getInstance().report(statInfo.makeLiveProUrl());
	}
}
