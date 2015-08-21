package bf.cloud.android.components.mediaplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

/**
 * 视频控制器基类
 *
 * Created by gehuanfei on 2014/9/18.
 */
public abstract class Controller extends FrameLayout {

    public Controller(Context context) {
        super(context);
    }

    public Controller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Controller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    protected Animation getShowAnimation() {
        Animation animation = new AlphaAnimation(0, 1);
        return animation;
    }

    public void hide() {
        setVisibility(View.INVISIBLE);
    }

    protected Animation getHideAnimation() {
        return new AlphaAnimation(1, 0);
    }

    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }

    public void reset() {

    }
}
