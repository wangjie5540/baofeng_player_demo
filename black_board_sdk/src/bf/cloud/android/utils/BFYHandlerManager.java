package bf.cloud.android.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;

/**
 * Created by gehuanfei on 2014/9/12.
 */


public class BFYHandlerManager {

  private static final Object sMsgHandlerLock = new Object();

  private static ArrayList<Handler> sList = new ArrayList<Handler>();

  public static void sendMsg(int what, int arg1, int arg2) {
    synchronized (sMsgHandlerLock) {
      for (Handler handler : sList) {
        if (handler != null) {
          Message msg = handler.obtainMessage();
          msg.what = what;
          msg.arg1 = arg1;
          msg.arg2 = arg2;
          msg.sendToTarget();
        }
      }
    }
  }

  public static void sendMsgWithObj(int what, int arg1, int arg2, Object obj) {
    synchronized (sMsgHandlerLock) {
      for (Handler handler : sList) {
        if (handler != null) {

          Message msg = handler.obtainMessage();
          msg.what = what;
          msg.arg1 = arg1;
          msg.arg2 = arg2;
          msg.obj = obj;
          msg.sendToTarget();
        }
      }

    }
  }

  public static void sendMsg(int what, int arg1, int arg2, Object obj, Bundle data) {
    if (sList == null || sList.isEmpty()) {
      return;
    } else {
      synchronized (sMsgHandlerLock) {
        for (Handler item : sList) {
          Message msg = Message.obtain();
          msg.what = what;
          msg.arg1 = arg1;
          msg.arg2 = arg2;
          msg.obj = obj;
          msg.setData(data);
          item.sendMessage(msg);
        }
      }
    }
  }

  public static void sendMsg(int what) {
    sendMsg(what, 0, 0, null, null);
  }

  /**
   * 注册handler对象
   *
   * @param item
   *          handler对象
   */
  public static void register(Handler item) {
    if (item == null) {
          return;
      }
    synchronized (sMsgHandlerLock) {
      sList.add(item);
    }
  }

  /**
   * 注销handler对象
   *
   * @param item
   *          handler对象
   */
  public static void remove(Handler item) {
    synchronized (sMsgHandlerLock) {
      sList.remove(item);
    }
  }

}
