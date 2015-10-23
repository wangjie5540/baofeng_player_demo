package bf.cloud.android.playutils;

import bf.cloud.android.modules.stat.StatInfo;
import bf.cloud.android.modules.stat.StatReporter;

public class VodPlayer extends BasePlayer{
	
	public VodPlayer(VideoFrame vf, String settingDataPath) {
		super(vf, settingDataPath);
	}
	
	@Override
	public void pause() {
		super.pause();
	}
	
	@Override
	public void resume() {
		super.resume();
	}
	
	@Override
	public void seekTo(int ms) {
		super.seekTo(ms);
	}
	
	@Override
	public void setDefinition(VideoDefinition definition) {
		super.setDefinition(definition);
	}
	
	@Override
	public VideoDefinition getCurrentDefinition() {
		return super.getCurrentDefinition();
	}
	
	@Override
	public long getDuration() {
		return super.getDuration();
	}
	
	@Override
	public long getCurrentPosition() {
		return super.getCurrentPosition();
	}

	@Override
	protected void reportPlayExperienceStatInfo() {
		if (!canReportStatInfo()) return;
		StatInfo statInfo = mVideoView.getStatInfo();
		if (statInfo == null && mVideoInfo == null)
			return;
		prepareBaseStatInfo(statInfo);
		StatReporter.getInstance().report(statInfo.makeVodExpUrl());
	}

	@Override
	protected void reportPlayProcessStatInfo() {
		if (!canReportStatInfo()) return;
		StatInfo statInfo = mVideoView.getStatInfo();
		if (statInfo == null || mVideoInfo == null)
			return;
		prepareBaseStatInfo(statInfo);
		StatReporter.getInstance().report(statInfo.makeVodProUrl());
	}
}
