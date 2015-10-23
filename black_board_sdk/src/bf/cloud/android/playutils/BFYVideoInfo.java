package bf.cloud.android.playutils;

/**
 * Created by gehuanfei on 2014-9-21.
 */

/**
 * 视频信息
 */
public class BFYVideoInfo {

    private String mUrl;
    private long mDuration;

    public BFYVideoInfo(String url) {
        this.mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        this.mDuration = duration;
    }

    public String toString() {
        return "[" + mUrl + "]";
    }

}
