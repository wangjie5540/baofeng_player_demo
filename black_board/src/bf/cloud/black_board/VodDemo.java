package bf.cloud.black_board;

import bf.cloud.android.playutils.BasePlayer.PlayErrorListener;
import bf.cloud.android.playutils.BasePlayer.PlayEventListener;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.VodPlayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * 
 * @author BfCloud. Note: Please set your project to Utf8
 * 
 */
public class VodDemo extends Activity implements PlayErrorListener,
		PlayEventListener {
	private final String TAG = VodDemo.class.getSimpleName();
	private VodPlayer mPlayer = null;
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

	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.change_decode_mode: {
			String[] items = { "自动(ExoPlayer优先)", "软解(ffmpeg)" };
			int checkedItem = -1;
			Log.d(TAG, "mPlayer.getDecodeMode():" + mPlayer.getDecodeMode());
			if (mPlayer.getDecodeMode() == DecodeMode.AUTO) {
				checkedItem = 0;
			} else {
				checkedItem = 1;
			}
			new AlertDialog.Builder(mContext)
					.setSingleChoiceItems(items, checkedItem, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									mPlayer.stop();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mPlayer.setDecodeMode(DecodeMode.AUTO);
									} else if (position == 1) {
										mPlayer.setDecodeMode(DecodeMode.SOFT);
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
		case R.id.start:{
			Log.d(TAG, "Start onClick");
			mPlayer.stop();
			mPlayer.setVrFlag(mVrFlag);
			mPlayer.setDataSource(mUrls);
			mPlayer.start();
		}
		case R.id.stop:
			mPlayer.stop();
			break;
		case R.id.pause:
			mPlayer.pause();
			break;
		case R.id.resume:
			mPlayer.resume();
			break;
		case R.id.seekto:
			mPlayer.seekTo(30000);
			break;
		case R.id.inc_volume:
			mPlayer.incVolume();
			break;
		case R.id.dec_volume:
			mPlayer.decVolume();
			break;
		case R.id.get_current_volume:{
			int value = mPlayer.getCurrentVolume();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.get_max_volume:{
			int value = mPlayer.getMaxVolume();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.get_cur_position:{
			long value = mPlayer.getCurrentPosition();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.get_duration:{
			long value = mPlayer.getDuration();
			Toast.makeText(VodDemo.this, "" + value, Toast.LENGTH_SHORT)
					.show();
			break;
		}
		case R.id.change_player_type:{
			String[] items = new String[] { "切换至普通播放器", "切换至全景播放器" };
			new AlertDialog.Builder(mContext)
					.setSingleChoiceItems(items, mVrFlag ? 1 : 0, null)
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									int position = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									if (position == 0) {
										mVrFlag = false;
									} else if (position == 1) {
										mVrFlag = true;
									}
									mPlayer.stop();
									mPlayer.setVrFlag(mVrFlag);
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
		default:
			break;
		}
	}

	private void init() {
		mContext = this;
		// 获取播放器
		mPlayer = (VodPlayer) findViewById(R.id.vod_player);
		mPlayer.registPlayErrorListener(this);
		mPlayer.registPlayEventListener(this);
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

	@Override
	public void onEvent(int eventCode) {
		Log.d(TAG, "onEvent eventCode:" + eventCode);
	}

	@Override
	public void onError(int errorCode) {
		Log.d(TAG, "errorCode eventCode:" + errorCode);
	}
}
