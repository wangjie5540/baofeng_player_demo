package bf.cloud.black_board_ui;

import bf.cloud.android.playutils.VodPlayer;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class BFMediaPlayerControllerVod extends BFMediaPlayerControllerBase{
	private VodPlayer mVodPlayer = null;
	
	public BFMediaPlayerControllerVod(Context context) {
		super(context);
	}

	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public BFMediaPlayerControllerVod(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public void setPlayer(VodPlayer player){
		mVodPlayer = null;
	}

}
