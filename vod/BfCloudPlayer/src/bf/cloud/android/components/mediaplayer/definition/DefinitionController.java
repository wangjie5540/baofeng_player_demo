package bf.cloud.android.components.mediaplayer.definition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;

import java.util.ArrayList;

import bf.cloud.android.components.mediaplayer.Controller;
import bf.cloud.android.components.mediaplayer.MediaController;
import bf.cloud.android.modules.log.BFYLog;
import bf.cloud.android.utils.BFYResUtil;

/**
 * 清晰度控制器
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class DefinitionController extends Controller {

    private static final String TAG = DefinitionController.class.getSimpleName();

    private MediaController mMediaController;
    private Button mDefinationButton;
    private DefinitionPanel mDefinitionPanel;
    //private List<String> mDefinations;
    private ArrayList<String> mDefinitions;
    private OnDefinitionChangedListener mListener;
    private int mCurrentDefIndex;
    private String mShowName = null;

    public DefinitionController(Context context) {
        super(context);
        initController();
    }

    public DefinitionController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initController();
    }

    public DefinitionController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void hide() {
        if (null != mDefinitionPanel && mDefinitionPanel.isShowing()) {
            mDefinitionPanel.dismiss();
        }
        super.hide();
    }

    private void initController() {
        View v = makeController();
        addView(v);
    }

    private View makeController() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_definition_controller"), null);
        mDefinationButton = (Button) v.findViewById(BFYResUtil.getId(getContext(), "definitionButton"));
        if (null != mDefinationButton) {
            mDefinationButton.setOnClickListener(listener);
        }
        return v;
    }

    private void showPanel() {
        BFYLog.d(TAG, "showPanel");
        if (mDefinitions.size() == 0) {
        	return;
        }
        
        if (null == mDefinitionPanel) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View contentView = inflater.inflate(BFYResUtil.getLayoutId(getContext(), "vp_definition_panel"), null);
            mDefinitionPanel = new DefinitionPanel();
            mDefinitionPanel.setContentView(contentView);
            mDefinitionPanel.initPanel(mDefinitions);
            mDefinitionPanel.setWidth(mDefinationButton.getWidth());
            mDefinitionPanel.setHeight(LayoutParams.WRAP_CONTENT);
            mDefinitionPanel.setFocusable(true);
            mDefinitionPanel.setOutsideTouchable(true);
            mDefinitionPanel.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
            mDefinitionPanel.setCurrentDefIndex(mCurrentDefIndex);
            
            mDefinitionPanel.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    BFYLog.d(TAG, "onDismiss");
                    if (mMediaController.isShowing()) {
                        mMediaController.show();
                    }
                }
            });
            mDefinitionPanel.setTouchInterceptor(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    BFYLog.d(TAG, "onTouch,action=" + event.getAction());
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mMediaController.show();
                    }
                    return false;
                }
            });
            mDefinitionPanel.setOnDefClickListener(new DefinitionPanel.OnDefClickListener() {
                @Override
                public void onItemClick(int index, String name) {
                	BFYLog.d(TAG, "onItemClick,index=" + index + " name=" + name + " show=" + mShowName);
                    if (null != mDefinationButton) {
                    	if (mShowName != name) {
                    		mShowName = name;
                            mDefinationButton.setText(name);
                            //mDefinitionPanel.dismiss();
                            mListener.onDefinitionChanged(name);
                    	}                    	
                    }
                }
            });
        }
        BFYLog.d(TAG, "showPanel,mDefinationPanel.isShowing()=" + mDefinitionPanel.isShowing());
        if (mDefinitionPanel.isShowing()) {
            mDefinitionPanel.dismiss();
        } else {
            mMediaController.show(360000);
            mDefinitionPanel.showAsPullUp(mDefinationButton);
        }
    }

    public void setMediaController(MediaController mediaController) {
        mMediaController = mediaController;
    }

    public void setDefinitions(ArrayList<String> definitions) {
        mDefinitions = definitions;
    }

    public void setDefinitonEnabled(boolean enabled) {
        if (null != mDefinationButton) {
            mDefinationButton.setEnabled(enabled);
            if (enabled) {
            	if (null != mListener) {
            		String name;
            		if (null != mShowName) {
            			name = mShowName;
            		} else {
            			name = mListener.getShowDefinition();
            		}
            		
                	if (null != name) {
                		mDefinationButton.setText(name);
                        mCurrentDefIndex = mDefinitions.indexOf(name);
                	}   
            	}            	         	              
            }
        }
    }

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	if (v.getId() == BFYResUtil.getId(getContext(), "definitionButton")) {
        		showPanel();
        	}
        }
    };

    public void setOnDefChangedListener(OnDefinitionChangedListener l) {
        mListener = l;
    }

    public interface OnDefinitionChangedListener {
        void onDefinitionChanged(String name);
        String getShowDefinition();
    }
}
