package bf.cloud.android.components.mediaplayer.definition;

import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;

import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;


/**
 * 清晰度面板
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class DefinitionPanel extends PopupWindow {

    private static final String TAG = DefinitionPanel.class.getSimpleName();
    //private List<Map<String, Object>> mData;
    //private ArrayList<String> itemList;
    private ListView mDeflist;
    private OnDefClickListener mListener;
    private int mDefinitionCount;
    private int mDefinitionHeight = 60;
    private DefinitionAdapter mAdapter;
    private int mCurrentDefIndex;
    
    public DefinitionPanel() {
    }

    /**
     * 在指定控件上方显示
     *
     * @param anchor
     */
    public void showAsPullUp(View anchor) {
    	//高度是=item的高度*清晰度个数
    	
        showAsPullUp(anchor, 0, -mDefinitionHeight*mDefinitionCount);
    }

    /**
     * 在指定控件上方显示,默认x坐标与控件的x坐标相同
     *
     * @param anchor
     * @param xoff
     * @param yoff
     */
    public void showAsPullUp(View anchor, int xoff, int yoff) {
        BFYLog.d(TAG, "showAsPullUp,xoff=" + xoff + ",yoff=" + yoff);
        //保存anchor在屏幕中的位置
        int[] location = new int[2];
        //保存anchor左上点
        int[] anchorLefTop = new int[2];
        //读取anchor坐标
        anchor.getLocationOnScreen(location);
        //计算anchor左上坐标
        anchorLefTop[0] = location[0] + xoff;
        anchorLefTop[1] = location[1] + yoff;
        BFYLog.d(TAG, "showAsPullUp,x=" + anchorLefTop[0] + ",y=" + anchorLefTop[1]);
        super.showAtLocation(anchor, Gravity.NO_GRAVITY, anchorLefTop[0], anchorLefTop[1]);
        
    }

    //获取清晰度，初始化列表
    public void initPanel(ArrayList<String> definitions) {
    	mDefinitionCount = definitions.size(); 
        if (0 == mDefinitionCount) {
            return;
        }
        
        mDeflist = (ListView) getContentView().findViewById(BFYResUtil.getId(getContentView().getContext(), "definition_list"));
        mAdapter = new DefinitionAdapter(getContentView().getContext(), definitions);
        mDeflist.setAdapter(mAdapter);
        mDeflist.setItemsCanFocus(false);
        mDefinitionHeight = mAdapter.getItemHeight();

        mDeflist.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                DefinitionAdapter.ViewHolder vHolder = (DefinitionAdapter.ViewHolder) view.getTag();
                boolean changed = true;
                //先将上一次选中的状态恢复为未选中、可点击
                for (int i = 0; i < DefinitionAdapter.isSelected.size(); i++) {
                    if (DefinitionAdapter.isSelected.get(i) == true) {
                        if (i == position) {
                            changed = false;
                            break;
                        } else {
                            DefinitionAdapter.isSelected.put(i, false);
                            mDeflist.getChildAt(i).setEnabled(true);
                            //设置字体颜色为未选中状态时的颜色                            
                            //((TextView)vHolder.groupItem).setTextColor(context.getResources().getColor(BFYResUtil.getColorId(context, "vp_color_text_lightwhite")));
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) {
                    //设置新选中的状态为当前、不可点击
                    DefinitionAdapter.isSelected.put(position, true);
                    mDeflist.getChildAt(position).setEnabled(false);
                    //设置字体颜色为选中状态时的颜色
                    //((TextView)vHolder.groupItem).setTextColor(context.getResources().getColor(BFYResUtil.getColorId(context, "vp_color_text_gray")));
                    //隐藏清晰度面板、设置当前清晰度为新选中
                    //dismiss();
                }
                
                //隐藏清晰度面板、设置当前清晰度为新选中
                dismiss();

                if (null != mListener) {
                    mListener.onItemClick(position, vHolder.groupItem.getText().toString());
                    //mListener.onItemClick(position, DefinitionAdapter.getItem(position).get("definition").toString());
                }
            }
        });
    }

    public void setCurrentDefIndex(int index) {
    	mCurrentDefIndex = index;
    	mAdapter.setSelectedIndex(index);
    }
    
    //设置item点击监听器
    public void setOnDefClickListener(OnDefClickListener listener) {
        this.mListener = listener;
    }

    public interface OnDefClickListener {
        public void onItemClick(int index, String name);
    }
}
