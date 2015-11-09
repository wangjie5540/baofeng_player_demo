package bf.cloud.black_board_ui;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import bf.cloud.android.playutils.BasePlayer;
import bf.cloud.android.playutils.BasePlayer.STATE;
import bf.cloud.android.playutils.PlayTaskType;
import bf.cloud.android.playutils.VodPlayer;
import bf.cloud.black_board_ui.DefinitionPanel.OnDefinitionClickListener;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
	private boolean mDragging = false;
	private static final int MSG_HIDE_DEFINITION_PANEL = 2008;
	private static final int MSG_SHOW_DEFINITION_PANEL = 2009;
	private static final int MEG_UPDATE_PROGRESS = 1000;
	protected final static int DELAY_TIME_LONG = 10000; // ms
	private DefinitionPanel mDefinitionPanel = null;
	private ArrayList<String> mDefinitions = null;
	// 清晰度图标
	private TextView mControllerDefinition = null;
	private Handler mProgressHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			if (mControllerProgressBar == null)
				return false;
			mProgressHandler.removeMessages(MEG_UPDATE_PROGRESS);
			mProgressHandler.sendEmptyMessageDelayed(MEG_UPDATE_PROGRESS, 1000);
			updateProgressBar();
			return false;
		}
	});

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
		// 子类处理个别错误码
	}

	@Override
	public void onEvent(int eventCode) {
		switch (eventCode) {
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_ENDED:
			showPlayCompleteFrame(true);
			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_START:
			showPlayCompleteFrame(false);
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_STARTED:
			mDefinitions = mVodPlayer.getAllDefinitions();
			mControllerProgressBar.setProgress(0);
			if (mVodPlayer != null)
				mControllerVideoTitle.setText(mVodPlayer.getVideoName());
			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_PAUSE:
			updateButtonUI();
			break;
		case BasePlayer.EVENT_TYPE_MEDIAPLAYER_RESUME:
			updateButtonUI();
			break;

		default:
			break;
		}
		super.onEvent(eventCode);
		// 子类处理个别事件
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
		mControllerHead = (RelativeLayout) mPlayerController
				.findViewById(R.id.head);
		mControllerHead.setVisibility(View.INVISIBLE);
		mControllerBack = (ImageView) mPlayerController
				.findViewById(R.id.back_button);
		mControllerBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				backToPortrait();
			}
		});
		mControllerVideoTitle = (TextView) mPlayerController
				.findViewById(R.id.videoTitle);
		// init bottom section
		mControllerBottom = (RelativeLayout) mPlayerController
				.findViewById(R.id.bottom);
		mControllerBottom.setVisibility(View.INVISIBLE);
		mControllerPlayPause = (ImageButton) mPlayerController
				.findViewById(R.id.pause_play_button);
		mControllerPlayPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mVodPlayer == null)
					return;
				STATE playerState = mVodPlayer.getState();
				if (playerState == STATE.PLAYING)
					mVodPlayer.pause();
				else
					mVodPlayer.resume();
			}
		});
		mControllerChangeScreen = (Button) mPlayerController
				.findViewById(R.id.full_screen);
		mControllerChangeScreen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				changeToLandscape();
				mIsFullScreen = true;
			}
		});
		mControllerDefinition = (TextView) mPlayerController
				.findViewById(R.id.definition);
		mControllerDefinition.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDefinitionPanel(true);
			}
		});
		mControllerCurentPlayTime = (TextView) mPlayerController
				.findViewById(R.id.time_current);
		mControllerDuration = (TextView) mPlayerController
				.findViewById(R.id.time_duration);
		mControllerProgressBar = (SeekBar) mPlayerController
				.findViewById(R.id.mediacontroller_progress);
		mProgressHandler.sendEmptyMessage(MEG_UPDATE_PROGRESS);
		if (mControllerProgressBar != null) {
			if (mControllerProgressBar instanceof SeekBar) {
				SeekBar seeker = (SeekBar) mControllerProgressBar;
				seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						Log.d(TAG, "onStartTrackingTouch");
						mDragging = true;
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						Log.d(TAG, "onStopTrackingTouch");
						long duration = mVodPlayer.getDuration();
						long newposition = (duration * seekBar.getProgress())
								/ seekBar.getMax();
						// if (newposition >= duration) {
						// newposition = duration - 50;
						// }
						mVodPlayer.seekTo((int) newposition);
						mDragging = false;
						mMessageHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
					}
				});
			}
			mControllerProgressBar.setMax(1000);
		}

		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
	}

	private void showDefinitionPanel(boolean flag) {
		Log.d(TAG, "showDefinitionPanel");
		if (mDefinitionPanel == null) {
			mDefinitionPanel = new DefinitionPanel(mContext, mDefinitions);
			mDefinitionPanel
					.registOnClickListener(new OnDefinitionClickListener() {

						@Override
						public void onItemClick(int position) {
							mDefinitionPanel.dismiss();
							mMessageHandler
									.sendEmptyMessage(MSG_HIDE_CONTROLLER);
							if (mDefinitions != null)
								mVodPlayer.setDefinition(mDefinitions.get(position));
						}
					});
		}
		mDefinitionPanel.showAsPullUp(mControllerDefinition);
		mMessageHandler.sendEmptyMessage(MSG_SHOW_DEFINITION_PANEL);
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

	public VodPlayer getVodPlayer() {
		return mVodPlayer;
	}

	@Override
	protected void showErrorFrame(int errorCode) {
		mPlayErrorManager.setErrorCode(errorCode);
		TextView tipsTv = (TextView) mErrorFrame
				.findViewById(R.id.error_message_textview);
		String tips = mPlayErrorManager.getErrorShowTips(PlayTaskType.VOD);
		tipsTv.setText(tips);
		TextView codeTv = (TextView) mErrorFrame
				.findViewById(R.id.error_code_textview);
		String codeText = "错误代码：" + errorCode;
		codeTv.setText(codeText);
		mErrorFrame.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onClickPlayButton() {
		if (mVodPlayer != null) {
			mVodPlayer.stop();
			mVodPlayer.setForceStartFlag(true);
			mVodPlayer.start();
		}
	}

	private void showPlayCompleteFrame(boolean flag) {
		if (flag) {
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.VISIBLE);
		} else {
			if (mPlayCompleteFrame != null)
				mPlayCompleteFrame.setVisibility(View.INVISIBLE);
		}
	}

	private void updateProgressBar() {
		if (mVodPlayer == null || mDragging) {
			return;
		}
		long duration = mVodPlayer.getDuration();
		long position = mVodPlayer.getCurrentPosition();
		if (duration != 0 && position > duration) {
			position = duration;
		}
		if (mControllerProgressBar != null) {
			if (duration > 0) {
				long pos = mControllerProgressBar.getMax() * position
						/ duration;
				mControllerProgressBar.setProgress((int) pos);
			}
		}

		if (mControllerCurentPlayTime != null)
			mControllerCurentPlayTime.setText(stringForTime(position));
		if (mControllerDuration != null) {
			mControllerDuration.setText(stringForTime(duration));
		}
	}

	private void updateButtonUI() {
		if (mVodPlayer == null)
			return;
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING)
			mControllerPlayPause.setBackgroundResource(R.drawable.vp_pause);
		else
			mControllerPlayPause.setBackgroundResource(R.drawable.vp_play);

	}

	@Override
	protected void doMoveLeft() {
		if (moveDirection == MOVE_NONE || moveDistanceX < 0
				|| mVodPlayer == null) {
			Log.d(TAG, "invailid params during dealing doMoveLeft");
			return;
		}
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING || state == STATE.PAUSED) {
			int newPosition = (int) (mVodPlayer.getCurrentPosition() - moveDistanceX
					* mVodPlayer.getDuration() / (mScreenWidth * DIVISION));
			mVodPlayer.seekTo(newPosition);
		}
	}

	@Override
	protected void doMoveRight() {
		if (moveDirection == MOVE_NONE || moveDistanceX < 0
				|| mVodPlayer == null) {
			Log.d(TAG, "invailid params during dealing doMoveLeft");
			return;
		}
		STATE state = mVodPlayer.getState();
		if (state == STATE.PLAYING || state == STATE.PAUSED) {
			int newPosition = (int) (mVodPlayer.getCurrentPosition() + moveDistanceX
					* mVodPlayer.getDuration() / (mScreenWidth * DIVISION));
			mVodPlayer.seekTo(newPosition);
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		int what = msg.what;
		switch (what) {
		case MSG_HIDE_DEFINITION_PANEL:
			mMessageHandler.removeMessages(MSG_HIDE_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_DEFINITION_PANEL);
			mMessageHandler.sendEmptyMessage(MSG_HIDE_CONTROLLER);
			mDefinitionPanel.dismiss();
			return true;
		case MSG_SHOW_DEFINITION_PANEL:
			mMessageHandler.removeMessages(MSG_HIDE_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_DEFINITION_PANEL);
			mMessageHandler.removeMessages(MSG_SHOW_CONTROLLER);
			mMessageHandler.removeMessages(MSG_HIDE_CONTROLLER);
			showController();
			mMessageHandler.sendEmptyMessageDelayed(MSG_HIDE_DEFINITION_PANEL,
					DELAY_TIME_LONG);
			return true;
		}
		return super.handleMessage(msg);
	}
	
	@Override
	public void changeToPortrait() {
		if (mContext == null)
			return;
		// 隐藏清晰度图标
		mControllerDefinition.setVisibility(View.INVISIBLE);
		super.changeToPortrait();
	}
	
	@Override
	public void changeToLandscape() {
		if (mContext == null)
			return;
		// 显示清晰度图标
		mControllerDefinition.setVisibility(View.VISIBLE);
		super.changeToLandscape();
	}
}
