package bf.cloud.android.components.mediaplayer.util;

/**
 * Created by gehuanfei on 2014/9/18.
 */
public enum BFYMediaErrors {
    ERROR_ALREADY_CONNECTED(-1000, "ERROR_ALREADY_CONNECTED"),
    ERROR_NOT_CONNECTED(-1001, "ERROR_NOT_CONNECTED"),
    ERROR_UNKNOWN_HOST(-1002, "ERROR_UNKNOWN_HOST"),
    ERROR_CANNOT_CONNECT(-1003, "ERROR_CANNOT_CONNECT"),
    ERROR_IO(-1004, "ERROR_IO"),
    ERROR_CONNECTION_LOST(-1005, "ERROR_CONNECTION_LOST"),
    ERROR_MALFORMED(-1007, "ERROR_MALFORMED"),
    ERROR_OUT_OF_RANGE(-1008, "ERROR_OUT_OF_RANGE"),
    ERROR_BUFFER_TOO_SMALL(-1009, "ERROR_BUFFER_TOO_SMALL"),
    ERROR_UNSUPPORTED(-1010, "ERROR_UNSUPPORTED"),
    ERROR_END_OF_STREAM(-1011, "ERROR_END_OF_STREAM"),

    // Not technically an error.
    INFO_FORMAT_CHANGED(-1012, "INFO_FORMAT_CHANGED"),
    INFO_DISCONTINUITY(-1013, "INFO_DISCONTINUITY"),
    INFO_OUTPUT_BUFFERS_CHANGED(-1014, "INFO_OUTPUT_BUFFERS_CHANGED"),

    // The following constant values should be in sync with
    // drm/drm_framework_common.h
    ERROR_DRM_UNKNOWN(-2000, "ERROR_DRM_UNKNOWN"),
    ERROR_DRM_NO_LICENSE(-2001, "ERROR_DRM_NO_LICENSE"),
    ERROR_DRM_LICENSE_EXPIRED(-2002, "ERROR_DRM_LICENSE_EXPIRED"),
    ERROR_DRM_SESSION_NOT_OPENED(-2003, "ERROR_DRM_SESSION_NOT_OPENED"),
    ERROR_DRM_DECRYPT_UNIT_NOT_INITIALIZED(-2004, "ERROR_DRM_DECRYPT_UNIT_NOT_INITIALIZED"),
    ERROR_DRM_DECRYPT(-2005, "ERROR_DRM_DECRYPT"),
    ERROR_DRM_CANNOT_HANDLE(-2006, "ERROR_DRM_CANNOT_HANDLE"),
    ERROR_DRM_TAMPER_DETECTED(-2007, "ERROR_DRM_TAMPER_DETECTED"),
    ERROR_DRM_NOT_PROVISIONED(-2008, "ERROR_DRM_NOT_PROVISIONED"),
    ERROR_DRM_DEVICE_REVOKED(-2009, "ERROR_DRM_DEVICE_REVOKED"),
    ERROR_DRM_RESOURCE_BUSY(-2010, "ERROR_DRM_RESOURCE_BUSY"),

    //
    ERROR_DRM_VENDOR_MAX(-2500, "ERROR_DRM_VENDOR_MAX"),
    ERROR_DRM_VENDOR_MIN(-2999, "ERROR_DRM_VENDOR_MIN"),

    // Heartbeat Error Codes
    ERROR_HEARTBEAT_TERMINATE_REQUESTED(-3000, "ERROR_HEARTBEAT_TERMINATE_REQUESTED");

    private String mError;
    private int mCode;

    BFYMediaErrors(int code, String error) {
        mError = error;
        mCode = code;
    }

    String getError() {
        return mError;
    }

    void setError(String error) {
        mError = error;
    }

    int getCode() {
        return mCode;
    }

    void setCode(int code) {
        mCode = code;
    }


    public static BFYMediaErrors mediaError(int code) {
        BFYMediaErrors[] values = BFYMediaErrors.values();
        int count = values.length;
        for (int i = 0; i < count; i++) {
            if (values[i].mCode == code) return values[i];
        }
        ERROR_ALREADY_CONNECTED.setCode(code);
        ERROR_ALREADY_CONNECTED.setError("unknown code");
        return ERROR_ALREADY_CONNECTED;
    }

    @Override
    public String toString() {
        return "MediaErrors{" +
                "mError='" + mError + '\'' +
                ", mCode=" + mCode +
                '}';
    }
}
