package bf.cloud.black_board;

import bf.cloud.android.playutils.VideoFrame;
import bf.cloud.android.playutils.VodPlayer;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class VodDemo extends Activity{
	private final String TAG = VodDemo.class.getSimpleName();
	private VodPlayer mPlayer = null;
	private VideoFrame mVideoFrame = null;
	private Button btStart = null;
	private Button btPause = null;
	private Button btResume = null;
	private Button btIncVolume = null;
	private Button btDecVolume = null;
	private Button btGetCurVolume = null;
	private Button btGetMAZVolume = null;
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
		btPause = (Button)findViewById(R.id.pause);
		btPause.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPlayer.pause();
			}
		});
		btResume = (Button)findViewById(R.id.resume);
		btResume.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPlayer.resume();
			}
		});
		btIncVolume = (Button)findViewById(R.id.inc_volume);
		btIncVolume.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPlayer.incVolume();
			}
		});
		btDecVolume = (Button)findViewById(R.id.dec_volume);
		btDecVolume.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPlayer.decVolume();
			}
		});
		btGetCurVolume = (Button)findViewById(R.id.get_current_volume);
		btGetCurVolume.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int value = mPlayer.getCurrentVolume();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			}
		});
		btGetMAZVolume = (Button)findViewById(R.id.get_max_volume);
		btGetMAZVolume.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int value = mPlayer.getMaxVolume();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			}
		});
		mPlayer = new VodPlayer(mVideoFrame, "/sdcard/");
	}
}
