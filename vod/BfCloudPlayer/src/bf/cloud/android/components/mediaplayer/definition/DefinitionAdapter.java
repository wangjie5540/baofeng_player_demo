package bf.cloud.android.components.mediaplayer.definition;

import android.content.Context;
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
 * Created by gehuanfei on 2014/10/29.
 */
//适配器
public class DefinitionAdapter extends BaseAdapter {
	private Context mContext;
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mData;
    public static Map<Integer, Boolean> isSelected;
    private int mItemHeight;
    private int mSelectedIndex;
    
    public DefinitionAdapter(Context context, ArrayList<String> definitions) {
    	mContext = context;
        mInflater = LayoutInflater.from(context);
        init(definitions);
        
        View convertView = mInflater.inflate(BFYResUtil.getLayoutId(mContext, "vp_definition_item"), null);
        TextView item = (TextView) convertView.findViewById(BFYResUtil.getId(mContext, "definition"));
        mItemHeight = item.getLayoutParams().height;
    }

    //初始化
    private void init(ArrayList<String> definitions) {
        mData = new ArrayList<Map<String, Object>>();
        for (int i = definitions.size() - 1; i >= 0; i --) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("definition", definitions.get(i));
            mData.add(map);
        }
        //记录每个listitem的状态，初始状态全部为false。
        isSelected = new HashMap<Integer, Boolean>();
        for (int i = 0; i < mData.size(); i ++) {
            isSelected.put(i, false);
        }
    }

    public int getItemHeight() {
    	return mItemHeight;
    }
    
    public void setSelectedIndex(int index) {
    	mSelectedIndex = getCount() - index - 1;
    	isSelected.put(mSelectedIndex, true);
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(BFYResUtil.getLayoutId(mContext, "vp_definition_item"), null);
            holder = new ViewHolder();
            holder.groupItem = (TextView) convertView.findViewById(BFYResUtil.getId(mContext, "definition"));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (isSelected.get(position)) {
        	holder.groupItem.setTextColor(mContext.getResources().getColor(BFYResUtil.getColorId(mContext, "vp_color_text_gray")));
        } else {
        	holder.groupItem.setTextColor(mContext.getResources().getColor(BFYResUtil.getColorId(mContext, "vp_color_text_lightwhite")));
        }
        holder.groupItem.setText(mData.get(position).get("definition").toString());

        return convertView;
    }

    public final class ViewHolder {
        TextView groupItem;
    }
}