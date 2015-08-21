package bf.cloud.android.events;

import java.util.HashMap;


/**
 * Created by gehuanfei on 2014/9/18..
 */
public class PlayCompleteEvent implements BFYICustomEvent {
    int ptime = -1;
    int mtime = -1;

    public PlayCompleteEvent(int ptime, int mtime) {
        this.ptime = ptime;
        this.mtime = mtime;
    }

    @Override
    public Object getData() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("ptime", this.ptime);
        map.put("mtime", this.mtime);
        return map;
    }
}
