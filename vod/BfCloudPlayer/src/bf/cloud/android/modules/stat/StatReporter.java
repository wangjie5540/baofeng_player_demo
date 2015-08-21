package bf.cloud.android.modules.stat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 报数
 *
 * Created by gehuanfei on 2014/9/18.
 */
public class StatReporter {

    private static StatReporter instance;
    
	private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1, 3, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	public static StatReporter getInstance() {
        if (instance == null) {
            synchronized (StatReporter.class) {
                if (instance == null) {
                    instance = new StatReporter();
                }
            }
        }
        return instance;
    }
    
    private StatReporter() {
    }
    
    @Override
    public void finalize() {
    	mExecutor.shutdown();
    }
    
    public void report(final String url) {
    	mExecutor.execute(new Runnable() {
    		public void run() {
    			doReport(url);
    		}
    	});
    }

    private void doReport(String reportUrl) {
		try {
	    	URL url = new URL(reportUrl);
			HttpURLConnection httpConn = (HttpURLConnection)url.openConnection(); 
			httpConn.setReadTimeout(15 * 1000);
			httpConn.setUseCaches(false);
			httpConn.setRequestMethod("GET");
			httpConn.setRequestProperty("Connection", "close");
			httpConn.setRequestProperty("Content-Type", "application/octet-stream");
			try {
				httpConn.connect();
				
				int responseCode = httpConn.getResponseCode();
				if (responseCode == 200) {
					InputStream inStream = httpConn.getInputStream();
					ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
					int c;
					while ((c = inStream.read()) != -1) {
						outBytes.write(c);
					}
				}
			} finally {
				httpConn.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}
