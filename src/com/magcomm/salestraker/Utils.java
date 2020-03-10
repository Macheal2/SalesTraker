
package com.magcomm.salestraker;

public class Utils {
    public static final String TAG = "Sale";

    public static final String SALE_SEND = "sale_send";

    public static final int RESULT_NONE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_FAILED = 2;
    public static final int DEFAULT_RETRY_TIMES = 6;
    public static final int SIM_RETRY_TIMES = 3;

    public static final String STORE_SETTINGS = "settings";
    public static final String ITEM_ISTEST = "istest";
    public static final String ITEM_DELAY = "delay";
    public static final String ITEM_INTERVAL = "interval";
    public static final String ITEM_NUMBER = "number";
    public static final String ITEM_STATUS = "status";
    public static final String ITEM_TIMES = "times";

    public static final String ACTION_SEND = "com.magcomm.salestraker.ACTION_SEND";
    public static final String ACTION_CHANGE = "com.magcomm.salestraker.ACTION_CHANGE";
    public static final String ACTION_DISMMIS = "com.magcomm.salestraker.ACTION_DISMISS";

    public static final int STATUS_NONE = -1;
    public static final int STATUS_DETSIM = 0;
    public static final int STATUS_NOSIM = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_TRING = 4;
    
    public static final String SALES_TRACKER_SEND_SMS = "com.yar.SalesTracker.SendSMS";
    public static final String SALES_TRACKER_DELIVERY_SMS_RESULT = "com.yar.SalesTracker.DeliverySMS.Result";
    public static final String SALES_TRACKER_SEND_SMS_RESULT = "com.yar.SalesTracker.SendSMS.Result";
    public static final String SALES_TRACKER_SETTING_CHANGED = "com.yar.SalesTracker.Setting.Changed";
	
	public static boolean saveNvRamProinfo(int value) {
		boolean result = false;
		result = NvRAMAgent.setProinfo(value);
		if (result == false) {
			result = saveNvRamProinfo(value);
		}
		return result;
	}
	
	public static int getNvRamProinfo() {
		int result = -1;
		result = NvRAMAgent.getProinfo();
		if (result == -1) {
			result = getNvRamProinfo();
		}
		return result;
	}
}
