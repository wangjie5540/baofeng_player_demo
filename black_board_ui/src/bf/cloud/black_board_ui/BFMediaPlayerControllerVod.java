package bf.cloud.black_board_ui;

import java.util.Formatter;
import java.util.Locale;

import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.PlayTaskType;
import bf.cloud.android.playutils.VodPlayer;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class BFMediaPlayerControllerVod extends BFMediaPlayerControllerBase {
	private VodPlayer mVodPlayer = null;
	private RelativeLayout mPlayCompleteFrame = null;
	// 播放结束层的按钮
	private View btPlayCompleteFrameStart = null;
	// 提示语
	private TextView tvPlayCompleteFrameMessage = null;

	public BFMediaPlayerControllerVod(Context context) {
		super(context);
		initViews();
		attachPlayer(mVodPlayer);
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs) {
		super(context, attrs);
		initViews();
		attachPlayer(mVodPlayer);
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initViews();
		attachPlayer(mVodPlayer);
	}

	@Override
	public void onError(int errorCode) {
		super.onError(errorCode);
		//子类处理个别错误码
	}

	@Override
	public void onEvent(int eventCode) {
		switch (eventCode) {
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_ENDED:
			showPlayCompleteFrame(true);
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_START:
			showPlayCompleteFrame(false);
			break;

		default:
			break;
		}
		super.onEvent(eventCode);
		//子类处理个别事件
	}

	@Override
	protected void initViews() {
		removeAllViews();
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		// 视频层在最下层
		mVodPlayer = (VodPlayer) mLayoutInflater.inflate(
				R.layout.vp_video_vod_player, this, false);
		mVodPlayer.setBackgroundColor(Color.BLACK);
		addView(mVodPlayer, layoutParams);
		// 播放结束层
		mPlayCompleteFrame = (RelativeLayout) mLayoutInflater.inflate(
				R.layout.vp_play_complete, this, false);
		initPlayCompleteFrame();
		addView(mPlayCompleteFrame, layoutParams);
		// 公共层
		super.initViews();
	}
	
	protected void initPlayerControllerFrame() {
		// init head section
		mControllerHead = (RelativeLayout) mPlayerController.findViewById(R.id.head);
		mControllerBack = (ImageView) mPlayerController.findViewById(R.id.backButton);
		mControllerVideoTitle = (TextView) mPlayerController.findViewById(R.id.videoTitle);
		// init bottom section
		mControllerBottom = (RelativeLayout) mPlayerController.findViewById(R.id.bottom);
		mControllerCurentPlayTime = (TextView) mPlayerController.findViewById(R.id.time_current);
		mControllerDuration = (TextView) mPlayerController.findViewById(R.id.time_duration);
		mControllerProgressBar = (SeekBar) mPlayerController.findViewById(R.id.mediacontroller_progress);
		if (mControllerProgressBar != null) {
            if (mControllerProgressBar instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mControllerProgressBar;
                seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					private boolean mDragging = false;

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
			            Log.d(TAG, "onStopTrackingTouch");
			            long duration = mVodPlayer.getDuration();
			            long newposition = (duration * seekBar.getProgress()) / 1000L;
			            if (newposition >= duration) {
			            	newposition = duration - 50;
			            }
			            mVodPlayer.seekTo((int) newposition);

			            mDragging = false;
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						Log.d(TAG, "onStartTrackingTouch");
			            mDragging  = true;
			            
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						
					}
				});
            }
            mControllerProgressBar.setMax(1000);
        }

        mFormatBuilder  = new StringBuilder();
        mFormatter  = new Formatter(mFormatBuilder, Locale.getDefault());
	}

	private void initPlayCompleteFrame() {
		btPlayCompleteFrameStart = mPlayCompleteFrame
				.findViewById(R.id.play_button);
		btPlayCompleteFrameStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mVodPlayer != null) {
					// 如果希望无论在什么网络下都播放视频，就设置这个标志
					mVodPlayer.stop();
					mVodPlayer.start();
				}
			}
		});
		tvPlayCompleteFrameMessage = (TextView) mPlayCompleteFrame
				.findViewById(R.id.message_textview);
		tvPlayCompleteFrameMessage.setText("");
		mPlayCompleteFrame.setVisibility(View.INVISIBLE);
	}
	
	public VodPlayer getVodPlayer(){
		return mVodPlayer;
	}

	@Override
	protected void showErrorFrame(int errorCode) {
		mPlayErrorManager.setErrorCode(errorCode);
		TextView tipsTv = (TextView) mErrorFrame.findViewById(R.id.error_message_textview);
		String tips = mPlayErrorManager.getErrorShowTips(PlayTaskType.VOD);
		tipsTv.setText(tips);
		TextView codeTv = (TextView) mErrorFrame.findViewById(R.id.error_code_textview);
		String codeText = "错误代码：" + errorCode;
		codeTv.setText(codeText);
		mErrorFrame.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onClickPlayButton() {
		if (mVodPlayer != null){
			mVodPlayer.stop();
			mVodPlayer.setForceStartFlag(true);
			mVodPlayer.start();
		}
	}
	
	private void showPlayCompleteFrame(boolean flag){
		if (flag){
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.VISIBLE);
		} else {
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.INVISIBLE);
		}
	}
}
