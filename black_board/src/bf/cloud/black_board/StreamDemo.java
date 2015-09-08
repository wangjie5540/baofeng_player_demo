package bf.cloud.black_board;

import bf.cloud.android.components.BFYNetworkStatusData;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.modules.p2p.BFStream;
import bf.cloud.android.modules.p2p.BFStream.BFStreamMessageListener;
import bf.cloud.android.modules.p2p.MediaCenter;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class StreamDemo extends Activity{
	private final String TAG = StreamDemo.class.getSimpleName();
	private final int STREAM_READY = 1;
	private final int STREAM_STATE = 2;
	private BFStream mStream = null;
	private Button btOpenStream = null;
	private Button btCloseStream = null;
	private Button btCreateStream = null;
	private Button btDestoryStream = null;
	private TextView tvState = null;
	private TextView tvStreamId = null;
	private TextView tvStreamUrl = null;
	private StreamHandler mHandler = new StreamHandler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stream_layout);
		init();
	}

	private void init() {
		mStream = new BFStream("/sdcard/", MediaCenter.NetState.NET_WIFI_REACHABLE);
		mStream.registerListener(new BFStreamMessageListener() {
			
			@Override
			public void onStreamReady() {
				Log.d(TAG, "onStreamReady");
				mHandler.sendEmptyMessage(STREAM_READY);
			}
			
			@Override
			public void onMessage(int type, int data, int error) {
				Log.d(TAG, "onMessage " + type + "/" + data + "/" + error);
				if (type == BFStreamMessageListener.MSG_TYPE_ERROR){
					
				}else if (type == BFStreamMessageListener.MSG_TYPE_NORMAL){
					mHandler.obtainMessage(STREAM_STATE, data, 0).sendToTarget();
				}
			}

			@Override
			public void onMediaCenterInitSuccess() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onMediaCenterInitFailed(int error) {
				// TODO Auto-generated method stub
				
			}
		});
		EditText url = (EditText)findViewById(R.id.url);
		url.setText("servicetype=1&uid=5284077&fid=5ABDC9CF335D035A78BA78A89A59EFE0");
		tvState = (TextView) findViewById(R.id.stream_state);
		tvStreamId = (TextView)findViewById(R.id.stream_id);
		tvStreamUrl = (TextView)findViewById(R.id.stream_url);
		btCreateStream = (Button) findViewById(R.id.create_stream);
		btCreateStream.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				createStream();
			}
		});
		btOpenStream = (Button) findViewById(R.id.open_stream);
		btOpenStream.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startStream();
			}
		});
		
		btCloseStream = (Button) findViewById(R.id.close_stream);
		btCloseStream.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closeStream();
			}
		});
		btDestoryStream = (Button)findViewById(R.id.destory_stream);
		btDestoryStream.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				destoryStream();
			}
		});
	}
	
	private void createStream(){
		String url = ((EditText)findViewById(R.id.url)).getText().toString();
		
		String token = ((EditText)findViewById(R.id.token)).getText().toString();
		int ret = mStream.createStream(url, token, MediaCenter.StreamMode.STREAM_HLS_MODE);
		Log.d(TAG, "ret = " + ret);
	}
	
	private void startStream(){
		mStream.startStream();
		tvStreamId.setText("" + mStream.getStreamId());
		tvStreamUrl.setText(mStream.getStreamUrl());
	}
	
	private void closeStream(){
		int ret = mStream.closeStream();
		tvStreamId.setText("" + mStream.getStreamId());
		tvStreamUrl.setText("");
		Log.d(TAG, "" + ret);
	}
	
	private void destoryStream(){
		int ret1 = mStream.destoryStream();
	}
	
	private class StreamHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case STREAM_READY:
				btOpenStream.setEnabled(true);
				btCloseStream.setEnabled(true);
				break;
			case STREAM_STATE:
				tvState.setText(getStateInfo(msg.arg1));

			default:
				break;
			}
			super.handleMessage(msg);
		}
	}
	
	private String getStateInfo(int state){
		String str = null;
		switch (state) {
		case MediaCenter.MediaHandleState.MEDIA_HANDLE_IDLE:
			str = "0 还未创建流";
			break;
		case MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNABLE:
			str = "1 已经创建流，还未开启流";
			break;
		case MediaCenter.MediaHandleState.MEDIA_HANDLE_RUNNING:
			str = "2  已经开启流";
			break;
		case MediaCenter.MediaHandleState.MEDIA_HANDLE_ACCOMPLISH:
			str = "3  流的缓冲区满";
			break;

		default:
			break;
		}
		return str;
	}
	
}
