package bf.cloud.android.playutils;

import android.content.Context;
import android.util.AttributeSet;
import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.modules.stat.StatReporter;

public class LivePlayer extends BasePlayer{
	protected LivePlayer(Context c) {
		super(c);
		// TODO Auto-generated constructor stub
	}
	
	protected LivePlayer(Context c, AttributeSet attrs) {
		super(c, attrs);
		// TODO Auto-generated constructor stub
	}
	
	protected LivePlayer(Context c, AttributeSet attrs, int defStyleAttr) {
		super(c, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
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
