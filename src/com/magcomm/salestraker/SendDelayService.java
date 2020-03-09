package com.magcomm.salestraker;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.content.ComponentName;
import android.os.UserHandle;

public class SendDelayService extends Service {
	private static final int SEND_DELAY = 15;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mHandler.sendEmptyMessageDelayed(SEND_DELAY, 15 * 1000);
	}
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			int what = msg.what;
			Log.i(Utils.TAG, "what --> " + what);
			switch (what) {
			case SEND_DELAY:
				Intent intent = new Intent(Utils.SALES_TRACKER_SETTING_CHANGED);
				ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SendReceiver");
				intent.setComponent(component);
				sendBroadcastAsUser(intent, UserHandle.ALL, "com.android.permission.SALE_TRAKER");
				stopSelf();
				break;
			default:
				break;
			}
		}
		
	};

}
