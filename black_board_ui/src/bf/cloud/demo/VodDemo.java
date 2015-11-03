package bf.cloud.demo;

import bf.cloud.android.playutils.VodPlayer;
import bf.cloud.black_board_ui.R;
import bf.cloud.black_board_ui.BFMediaPlayerControllerVod;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class VodDemo extends Activity{
	private final String TAG = VodDemo.class.getSimpleName();
	
	private VodPlayer mVodPlayer = null;
	private BFMediaPlayerControllerVod mMediaController = null;
	private String mUrls = "servicetype=1&uid=10279577&fid=7DC146B18442BC743AEBB67E43894B7D";
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_vod);
		init();
	}

	private void init() {
		mMediaController = (BFMediaPlayerControllerVod)findViewById(R.id.vod_media_controller);
		mVodPlayer = mMediaController.getVodPlayer();
	}
	
	public void onClick(View view){
		int id = view.getId();
		switch (id) {
		case R.id.start:
			mVodPlayer.stop();
			mVodPlayer.setDataSource(mUrls);
			mVodPlayer.start();
			break;

		default:
			break;
		}
	}
	
	@Override
	protected void onPause() {
		mVodPlayer.pause();
		super.onPause();
	}
	
	@Override
	protected void onStart() {
		mVodPlayer.resume();
		super.onStart();
	}
	
	@Override
	protected void onDestroy() {
		mVodPlayer.stop();
		super.onDestroy();
	}
}
