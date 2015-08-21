package com.example.bfcloudplayerdemo;

import java.io.File;

import bf.cloud.android.base.BFYApplication;
import bf.cloud.android.components.mediaplayer.PlayErrorListener;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.fragment.VideoPlayerFragment;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.LivePlayer;
import bf.cloud.android.utils.BFYResUtil;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

//实现PlayErrorListener和PlayerController.PlayerViewControl.PlayerControllerListener
public class LiveActivity extends FragmentActivity implements
		PlayErrorListener,
		PlayerController.PlayerViewControl.PlayerControllerListener {

	private final static String TAG = MainActivity.class.getSimpleName();

	private LivePlayer mPlayer = new LivePlayer();// 创建直播播放器

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_live);

		BFYApplication.getInstance().setDebugMode(BuildConfig.DEBUG);

		// 获取播放窗口
		VideoPlayerFragment fragment = (VideoPlayerFragment) getSupportFragmentManager()
				.findFragmentById(BFYResUtil.getId(this, "playerFragment"));
		mPlayer.setPlayerFragment(fragment); // 播放器与播放窗口关联

		mPlayer.setDataPath(getDataPath());// 设置用于存储信息的目录
		mPlayer.setDecodeMode(DecodeMode.AUTO);// 设置播放器类型。“自动”默认为硬解播放
		mPlayer.setPlayErrorListener(this);// 设置错误监听者
		mPlayer.registerPlayEvent(this);// 设置事件监听者
		EditText et = (EditText)findViewById(R.id.urlEditText);
		et.setText("servicetype=2&uid=5294383&fid=C5F2E62ED6EE54F31FED0CA913077DE2518D539A");
	}

	@Override
	public void onError(int errorCode) {
		Log.d(TAG, "error:" + errorCode);
	}

	private String getDataPath() {
		String sdCardPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + File.separator;
		return sdCardPath + "BfCloudPlayer/.p2p/";
	}

	public void onSetDecodeModeButtonClick(View v) {
		String[] items = new String[] { "自动 (ExoPlayer优先)", "软解 (ffmpeg)" };
		new AlertDialog.Builder(this)
				.setSingleChoiceItems(items, mPlayer.getDecodeMode().value(),
						null)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								int selectedPosition = ((AlertDialog) dialog)
										.getListView().getCheckedItemPosition();
								mPlayer.stop();
								mPlayer.setDecodeMode(DecodeMode
										.valueOf(selectedPosition));
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	public void onSetLatencyButtonClick(View v) {
		String[] items = new String[] { "普通", "低延时" };
		new AlertDialog.Builder(this)
				.setSingleChoiceItems(items, mPlayer.getLowLatency() ? 1 : 0,
						null)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								int selectedPosition = ((AlertDialog) dialog)
										.getListView().getCheckedItemPosition();
								mPlayer.stop();
								mPlayer.setLowLatency(selectedPosition == 0 ? false
										: true);
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	public void onSetAutoFullscreenButtonClick(View v) {
		String[] items = new String[] { "不自动全屏", "横屏时自动全屏" };
		new AlertDialog.Builder(this)
				.setSingleChoiceItems(items,
						mPlayer.getAutoFullscreen() ? 1 : 0, null)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								int selectedPosition = ((AlertDialog) dialog)
										.getListView().getCheckedItemPosition();
								mPlayer.setAutoFullscreen(selectedPosition == 0 ? false
										: true);
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	public void onStartButtonClick(View v) {
		EditText urlEditText = (EditText) findViewById(R.id.urlEditText);
		String url = urlEditText.getText().toString();
		EditText tokenEditText = (EditText) findViewById(R.id.tokenEditText);
		String token = tokenEditText.getText().toString();
		mPlayer.setPlayToken(token);
		mPlayer.setDataSource(url);
		mPlayer.start();
	}

	public void onPauseButtonClick(View v) {
		mPlayer.pause();
	}

	public void onResumeButtonClick(View v) {
		mPlayer.resume();
	}

	public void onStopButtonClick(View v) {
		mPlayer.stop();
	}

	public void onGetCurrentPositionButtonClick(View v) {
		Toast.makeText(this, mPlayer.getCurrentPosition() + "",
				Toast.LENGTH_SHORT).show();
	}

	public void onIncVolumeButtonClick(View v) {
		mPlayer.incVolume();
	}

	public void onDecVolumeButtonClick(View v) {
		mPlayer.decVolume();
	}

	public void onGetMaxVolumeButtonClick(View v) {
		Toast.makeText(this, mPlayer.getMaxVolume() + "", Toast.LENGTH_SHORT)
				.show();
	}

	public void onGetCurrentVolumeButtonClick(View v) {
		Toast.makeText(this, mPlayer.getCurrentVolume() + "",
				Toast.LENGTH_SHORT).show();
	}

	public void onSetVolumeButtonClick(View v) {
		mPlayer.setVolume(30);
		Toast.makeText(this, mPlayer.getCurrentVolume() + "",
				Toast.LENGTH_SHORT).show();
	}

	public void onFullscreenButtonClick(View v) {
		mPlayer.setFullscreen(true);
		v.postDelayed(new Runnable() {
			@Override
			public void run() {
				mPlayer.setFullscreen(false);
			}
		}, 5000);
	}

	@Override
	public void onCompletion() {
		Log.d(TAG, "call back onCompletion");
	}

	@Override
	public void onPrepare() {
		Log.d(TAG, "call back onPrepare");
	}

	@Override
	public void onVideoSizeChanged() {
		Log.d(TAG, "call back onVideoSizeChanged");
	}

	@Override
	public void onReadytoPlay() {
		Log.d(TAG, "call back onReadyToPlay");
	}

}
