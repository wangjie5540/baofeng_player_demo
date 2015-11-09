package bf.cloud.black_board_ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;

/**
 * 清晰度面板
 * 
 * @author wang
 * 
 */
public class DefinitionPanel extends PopupWindow {

	private static final String TAG = DefinitionPanel.class.getSimpleName();
	private View mRoot = null;
	private ListView mDeflist = null;
	// private ArrayList<String> itemList;
	private OnDefinitionClickListener mListener = null;
	private int mDefinitionCount = 1;
	private int mDefinitionWidth = -1;
	private int mDefinitionHeight = -1;
	// private DefinitionAdapter mAdapter;
	private int mCurrentDefIndex;
	private Context mContext = null;
	private DefinitionAdapter mAdapter = null;
	private ArrayList<String> mDefinitions = null;

	public DefinitionPanel(Context context, ArrayList<String> definitions) {
		if (definitions == null || definitions.size() == 0){
			Log.d(TAG, "definitions is invailid");
			return;
		}
		if (context == null)
			throw new NullPointerException("context is null");
		mContext = context;
		mDefinitions = definitions;
		mDefinitionCount = mDefinitions.size();
		init();
	}

	/**
	 * 在指定控件上方显示
	 * 
	 * @param anchor
	 */
	public void showAsPullUp(View anchor) {
		// 高度是=item的高度*清晰度个数
		showAsPullUp(anchor, 0, -mDefinitionHeight * mDefinitionCount);
	}

	/**
	 * 在指定控件上方显示,默认x坐标与控件的x坐标相同
	 * 
	 * @param anchor
	 * @param xoff
	 * @param yoff
	 */
	private void showAsPullUp(View anchor, int xoff, int yoff) {
		Log.d(TAG, "showAsPullUp,xoff=" + xoff + ",yoff=" + yoff + "/anchor:"
				+ anchor);
		// 保存anchor在屏幕中的位置
		int[] location = new int[2];
		// 保存anchor左上点
		int[] anchorLefTop = new int[2];
		// 读取anchor坐标
		anchor.getLocationOnScreen(location);
		// 计算anchor左上坐标
		anchorLefTop[0] = location[0] + xoff;
		anchorLefTop[1] = location[1] + yoff;
		super.showAtLocation(anchor, Gravity.NO_GRAVITY, anchorLefTop[0],
				anchorLefTop[1]);
	}

	private void init() {
		mDefinitionWidth = (int) mContext.getResources().getDimension(
				R.dimen.vp_player_definite_width);
		mDefinitionHeight = (int) mContext.getResources().getDimension(
				R.dimen.vp_player_definite_height);
		setWidth(mDefinitionWidth);
		setHeight(LayoutParams.WRAP_CONTENT);
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRoot  = inflater.inflate(R.layout.vp_definition_panel, null);
		mDeflist = (ListView) mRoot.findViewById(R.id.definition_list);
		mAdapter  = new DefinitionAdapter(mContext, mDefinitions);
        mDeflist.setAdapter(mAdapter);
        mDeflist.setItemsCanFocus(false);
        mDeflist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long id) {
				Log.d(TAG, "position:" + position);
				mAdapter.setSelectedIndex(position);
				mDeflist.invalidate();
				if (mListener != null)
					mListener.onItemClick(position);
			}
		});
		setContentView(mRoot);
		setFocusable(true);
		setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), (Bitmap) null));
	}
	
	public interface OnDefinitionClickListener{
		void onItemClick(int position);
	}
	
	public void registOnClickListener(OnDefinitionClickListener listener){
		mListener = listener;
	}
}
