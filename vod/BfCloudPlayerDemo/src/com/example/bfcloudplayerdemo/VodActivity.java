package com.example.bfcloudplayerdemo;

import java.io.File;

import bf.cloud.android.base.BFYApplication;
import bf.cloud.android.components.mediaplayer.PlayErrorListener;
import bf.cloud.android.components.mediaplayer.PlayerController;
import bf.cloud.android.fragment.VideoPlayerFragment;
import bf.cloud.android.playutils.DecodeMode;
import bf.cloud.android.playutils.VodPlayer;
import bf.cloud.android.playutils.VideoDefinition;
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

public class VodActivity extends FragmentActivity implements PlayErrorListener, PlayerController.PlayerViewControl.PlayerControllerListener {

	private final static String TAG = MainActivity.class.getSimpleName();
	
	private VodPlayer mPlayer = new VodPlayer();
	
	private String[] mUrls = new String[] {
		"servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0",  // ms surface ad	
		"servicetype=1&uid=4995606&fid=A70A09721538A3C9273E2AE8C2461141",  // baishechuanshuo
		"servicetype=1&uid=4995606&fid=1F06FBA6620B72FB12A19FB5B079F50B",  // wujiandao
		"servicetype=1&uid=11618581&fid=515FCD7611811CB3184D6260A4146191",
	};
	
	private int mCurrentUrlIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vod);
		
		BFYApplication.getInstance().setDebugMode(BuildConfig.DEBUG);
		
		VideoPlayerFragment fragment = (VideoPlayerFragment)getSupportFragmentManager().findFragmentById(BFYResUtil.getId(this, "playerFragment"));
		mPlayer.setPlayerFragment(fragment);
		
		mPlayer.setDataPath(getDataPath());
		mPlayer.setDecodeMode(DecodeMode.AUTO);
		mPlayer.setPlayErrorListener(this);
		mPlayer.registerPlayEvent(this);
	}
	
    @Override
    public void onError(int errorCode) {
    	Log.d(TAG, "error:" + errorCode);
    }
    
	private String getDataPath() {
		String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
		return sdCardPath + "BfCloudPlayer/.p2p/";
	}

	public void onSetDecodeModeButtonClick(View v) {
		String[] items = new String[] {"自动 (ExoPlayer优先)", "软解 (ffmpeg)"};
		new AlertDialog.Builder(this)
			.setSingleChoiceItems(items, mPlayer.getDecodeMode().value(), null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int whichButton) {
			        dialog.dismiss();
			        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
			        mPlayer.stop();
			        mPlayer.setDecodeMode(DecodeMode.valueOf(selectedPosition));
			    }
			})
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}
	
	public void onSetDefinitionButtonClick(View v) {
		String[] items = new String[] {"流畅", "标清", "高清", "1080P", "2K"};
		new AlertDialog.Builder(this)
			.setSingleChoiceItems(items, mPlayer.getCurrentDefinition().value(), null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int whichButton) {
			        dialog.dismiss();
			        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
			        mPlayer.setDefinition(VideoDefinition.valueOf(selectedPosition));
			    }
			})
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}
	
	public void onSetAutoFullscreenButtonClick(View v) {
		String[] items = new String[] {"不自动全屏", "横屏时自动全屏"};
		new AlertDialog.Builder(this)
			.setSingleChoiceItems(items, mPlayer.getAutoFullscreen() ? 1 : 0, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int whichButton) {
			        dialog.dismiss();
			        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
			        mPlayer.setAutoFullscreen(selectedPosition == 0 ? false : true);
			    }
			})
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}
	
	public void onStartButtonClick(View v) {
		EditText tokenEditText = (EditText) findViewById(R.id.tokenEditText);
		String token = tokenEditText.getText().toString();
		mPlayer.setPlayToken(token);
		mPlayer.setDataSource(mUrls[mCurrentUrlIndex]);
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
	
	public void onChangeButtonClick(View v) {
		mCurrentUrlIndex++;
		if (mCurrentUrlIndex >= mUrls.length) {
			mCurrentUrlIndex = 0;
		}
		
		mPlayer.stop();
		mPlayer.setDataSource(mUrls[mCurrentUrlIndex]);
		mPlayer.start();
	}
	
	public void onSeekButtonClick(View v) {
		mPlayer.seekTo(6121000);
	}

	public void onIncVolumeButtonClick(View v) {
		mPlayer.incVolume();
	}

	public void onDecVolumeButtonClick(View v) {
		mPlayer.decVolume();
	}

	public void onGetMaxVolumeButtonClick(View v) {
		Toast.makeText(this, mPlayer.getMaxVolume() + "", Toast.LENGTH_SHORT).show();
	}

	public void onGetCurrentVolumeButtonClick(View v) {
		Toast.makeText(this, mPlayer.getCurrentVolume() + "", Toast.LENGTH_SHORT).show();
	}

	public void onGetCurrentPositionButtonClick(View v) {
		Toast.makeText(this, mPlayer.getCurrentPosition() + "", Toast.LENGTH_SHORT).show();
	}

	public void onSetVolumeButtonClick(View v) {
		mPlayer.setVolume(30);
		Toast.makeText(this, mPlayer.getCurrentVolume() + "", Toast.LENGTH_SHORT).show();
	}

	public void onFullscreenButtonClick(View v) {
		mPlayer.setFullscreen(true);
		v.postDelayed(new Runnable() {
			@Override
			public void run() {
				mPlayer.setFullscreen(false);
			}
		},5000);
	}

	@Override
	public void onCompletion() {
		Log.d(TAG,"call back onCompletion");
	}

	@Override
	public void onPrepare() {
		Log.d(TAG,"call back onPrepare");
	}

	@Override
	public void onVideoSizeChanged() {
		Log.d(TAG,"call back onVideoSizeChanged");
	}

	@Override
	public void onReadytoPlay() {
		Log.d(TAG,"call back onReadyToPlay");
	}

}
