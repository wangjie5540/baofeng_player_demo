package bf.cloud.black_board;

import bf.cloud.android.playutils.VideoFrame;
import bf.cloud.android.playutils.VodPlayer;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class VodDemo extends Activity{
	private final String TAG = VodDemo.class.getSimpleName();
	private VodPlayer mPlayer = null;
	private VideoFrame mVideoFrame = null;
	private Button btStart = null;
	private String mUrls = "servicetype=1&uid=10279577&fid=7DC146B18442BC743AEBB67E43894B7D";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vod_demo);
		init();
	}

	private void init() {
		mVideoFrame = (VideoFrame) findViewById(R.id.video_frame);
		btStart = (Button) findViewById(R.id.start);
		btStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Start onClick");
				mPlayer.setDataSource(mUrls);
				mPlayer.stop();
				mPlayer.start();
			}
		});
		
		mPlayer = new VodPlayer(mVideoFrame, "");
	}
}
