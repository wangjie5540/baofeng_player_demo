package bf.cloud.black_board;

import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.VideoFrame;
import bf.cloud.android.playutils.VodPlayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class VodDemo extends Activity {
	private final String TAG = VodDemo.class.getSimpleName();
	private VodPlayer mPlayer = null;
	private VideoFrame mVideoFrame = null;
	private Button btChangeDecodeMode = null;
	private Button btStart = null;
	private Button btStop = null;
	private Button btPause = null;
	private Button btResume = null;
	private Button btSeekTo = null;
	private Button btIncVolume = null;
	private Button btDecVolume = null;
	private Button btGetCurVolume = null;
	private Button btGetMAZVolume = null;
	private Button btGetCurrentPosition = null;
	private Button btGetDuration = null;
	private Button btChangePlayerType = null;
	private boolean mVrFlag = false;
	private Context mContext = null;
	// private String mUrls =
	// "servicetype=1&uid=10279577&fid=7DC146B18442BC743AEBB67E43894B7D";
	private String mUrls = "servicetype=1&uid=10279577&fid=C86AE21E1853B0339F6D15241EDF79E4";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vod_demo);
		init();
	}

	private void init() {
		mContext = this;
		mVideoFrame = (VideoFrame) findViewById(R.id.video_frame);
		btChangeDecodeMode = (Button)findViewById(R.id.change_decode_mode);
		btChangeDecodeMode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String[] items = {"自动(ExoPlayer优先)", "软解(ffmpeg)"};
				int checkedItem = -1;
				Log.d(TAG, "mPlayer.getDecodeMode():" + mPlayer.getDecodeMode());
				if (mPlayer.getDecodeMode() == DecodeMode.AUTO){
					checkedItem = 0;
				}else{
					checkedItem = 1;
				}
				new AlertDialog.Builder(mContext)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							mPlayer.stop();
							int position = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
							if (position == 0){
								mPlayer.setDecodeMode(DecodeMode.AUTO);
							} else if (position == 1){
								mPlayer.setDecodeMode(DecodeMode.SOFT);
							}
						}
					})
					.setNegativeButton("取消", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).show();
			}
		});
		btStart = (Button) findViewById(R.id.start);
		btStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Start onClick");
				mPlayer.stop();
				mPlayer.setVrFlag(mVrFlag);
				mPlayer.setDataSource(mUrls);
				mPlayer.start();
			}
		});
		btStop = (Button) findViewById(R.id.stop);
		btStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.stop();
			}
		});
		btPause = (Button) findViewById(R.id.pause);
		btPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.pause();
			}
		});
		btResume = (Button) findViewById(R.id.resume);
		btResume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.resume();
			}
		});
		btSeekTo = (Button) findViewById(R.id.seekto);
		btSeekTo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.seekTo(30000);
			}
		});
		btIncVolume = (Button) findViewById(R.id.inc_volume);
		btIncVolume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.incVolume();
			}
		});
		btDecVolume = (Button) findViewById(R.id.dec_volume);
		btDecVolume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPlayer.decVolume();
			}
		});
		btGetCurVolume = (Button) findViewById(R.id.get_current_volume);
		btGetCurVolume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int value = mPlayer.getCurrentVolume();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
						.show();
			}
		});
		btGetMAZVolume = (Button) findViewById(R.id.get_max_volume);
		btGetMAZVolume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int value = mPlayer.getMaxVolume();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
						.show();
			}
		});
		btGetCurrentPosition = (Button) findViewById(R.id.get_cur_position);
		btGetCurrentPosition.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				long value = mPlayer.getCurrentPosition();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
						.show();
			}
		});
		btGetDuration = (Button) findViewById(R.id.get_duration);
		btGetDuration.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				long value = mPlayer.getDuration();
				Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
						.show();
			}
		});
		btChangePlayerType = (Button) findViewById(R.id.change_player_type);
		btChangePlayerType.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String[] items = new String[] { "切换至普通播放器", "切换至全景播放器" };
				new AlertDialog.Builder(mContext)
					.setSingleChoiceItems(items, mVrFlag ? 1 : 0, null)
					.setPositiveButton("确认", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							int position = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
							if (position == 0){
								mVrFlag = false;
							}else if (position == 1){
								mVrFlag = true;
							}
							mPlayer.stop();
							mPlayer.setVrFlag(mVrFlag);
						}
					})
					.setNegativeButton("取消", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).show();
			}
		});
		mPlayer = new VodPlayer(mVideoFrame, "/sdcard/");
	}

	@Override
	protected void onPause() {
		mPlayer.pause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mPlayer.stop();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		mPlayer.resume();
		super.onStart();
	}
}
