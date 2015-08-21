package bf.cloud.android.components;

public class BFYNetworkStatusData {
    public static final int NETWORK_CONNECTION_NONE = 4;
    public static final int NETWORK_CONNECTION_MOBILE = 2;
    public static final int NETWORK_CONNECTION_WIFI = 1;

    private int statusCode = NETWORK_CONNECTION_NONE;

    public BFYNetworkStatusData(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
