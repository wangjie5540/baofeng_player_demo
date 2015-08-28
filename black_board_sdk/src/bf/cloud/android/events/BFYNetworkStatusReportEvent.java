package bf.cloud.android.events;

import bf.cloud.android.components.BFYNetworkStatusData;

public class BFYNetworkStatusReportEvent implements BFYICustomEvent {

    private Object data;

    public BFYNetworkStatusReportEvent(BFYNetworkStatusData data) {
        this.data = data;
    }

    @Override
    public Object getData() {
        return (Object) data;
    }
}
