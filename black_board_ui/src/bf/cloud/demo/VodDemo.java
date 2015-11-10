package bf.cloud.demo;

import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.VodPlayer;
import bf.cloud.black_board_ui.R;
import bf.cloud.black_board_ui.BFMediaPlayerControllerVod;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class VodDemo extends Activity {
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
		mMediaController = (BFMediaPlayerControllerVod) findViewById(R.id.vod_media_controller);
		mVodPlayer = mMediaController.getVodPlayer();
	}

	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.change_decode_mode: {
			String[] items = { "自动(ExoPlayer优先)", "软解(ffmpeg)" };
			int checkedItem = -1;
			Log.d(TAG,
					"mVodPlayer.getDecodeMode():" + mVodPlayer.getDecodeMode());
			if (mVodPlayer.getDecodeMode() == DecodeMode.AUTO) {
				checkedItem = 0;
			} else {
				checkedItem = 1;
			}
			new AlertDialog.Builder(this)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									mVodPlayer.stop();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mVodPlayer
												.setDecodeMode(DecodeMode.AUTO);
									} else if (position == 1) {
										mVodPlayer
												.setDecodeMode(DecodeMode.SOFT);
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
			break;
		}
		case R.id.start: {
			Log.d(TAG, "Start onClick");
			mVodPlayer.stop();
			mVodPlayer.setDataSource(mUrls);
			mVodPlayer.start();
		}
		case R.id.stop:
			mVodPlayer.stop();
			break;
		case R.id.pause:
			mVodPlayer.pause();
			break;
		case R.id.resume:
			mVodPlayer.resume();
			break;
		case R.id.seekto:
			mVodPlayer.seekTo(30000);
			break;
		case R.id.inc_volume:
			mVodPlayer.incVolume();
			break;
		case R.id.dec_volume:
			mVodPlayer.decVolume();
			break;
		case R.id.get_current_volume: {
			int value = mVodPlayer.getCurrentVolume();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			break;
		}
		case R.id.get_max_volume: {
			int value = mVodPlayer.getMaxVolume();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			break;
		}
		case R.id.get_cur_position: {
			long value = mVodPlayer.getCurrentPosition();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			break;
		}
		case R.id.get_duration: {
			long value = mVodPlayer.getDuration();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT).show();
			break;
		}
		default:
			break;
		}

	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}
}
