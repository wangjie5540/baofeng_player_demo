package bf.cloud.demo;

import bf.cloud.android.playutils.VideoFrame;
import bf.cloud.android.playutils.VodPlayer;
import bf.cloud.black_board_ui.R;
import bf.cloud.black_board_ui.VideoFrameLayout;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class VodDemo extends Activity{
	private Button btStart = null;
	private VodPlayer mVodPlayer = null;
	private VideoFrame mVideoFrame = null;
	private VideoFrameLayout mVideoFrameLayout = null;
	private String mUrls = "servicetype=1&uid=10279577&fid=7DC146B18442BC743AEBB67E43894B7D";
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_vod);
		init();
	}

	private void init() {
		mVideoFrame = (VideoFrame) findViewById(R.id.vod_frame);
		mVodPlayer = new VodPlayer(mVideoFrame, "/sdcard/");
		btStart = (Button) findViewById(R.id.start);
		btStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mVodPlayer.stop();
				mVodPlayer.setDataSource(mUrls);
				mVodPlayer.start();
			}
		});
	}
}