package bf.cloud.black_board_ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import bf.cloud.android.utils.BFYResUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author wang
 * 
 */
public class DefinitionAdapter extends BaseAdapter {
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private List<String> mData = null;
	public Map<Integer, Boolean> isSelected = null;
	private int mSelectedIndex = -1;
	private ArrayList<String> mDefinitions = null;

	public DefinitionAdapter(Context context, ArrayList<String> definitions) {
		mContext = context;
		mData = definitions;
		mInflater = LayoutInflater.from(context);
		init();
	}

	// 初始化
	private void init() {
		
	}

	public void setSelectedIndex(int index) {
		mSelectedIndex = index;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.vp_definition_item, null);
		}
		tv = (TextView) convertView.findViewById(R.id.definition);
		tv.setText(mData.get(position));
		if (position == mSelectedIndex){
			tv.setTextColor(mContext.getResources().getColor(R.color.vp_color_text_lightwhite));
		}else{
			tv.setTextColor(mContext.getResources().getColor(R.color.vp_color_text_gray));
		}

		return convertView;
	}

	public final class ViewHolder {
		TextView groupItem;
	}
}