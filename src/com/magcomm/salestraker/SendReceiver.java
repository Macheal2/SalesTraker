package com.magcomm.salestraker;

import java.io.File;
import java.util.ArrayList;

//import com.mediatek.common.featureoption.FeatureOption;
//import com.mediatek.telephony.TelephonyManagerEx;
//import com.mediatek.telephony.SmsManagerEx;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
//import android.telephony.gemini.GeminiSmsManager;
import android.util.Log;
import android.os.SystemProperties;
import android.view.WindowManager;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.os.SystemClock;
import android.os.UserHandle;

public class SendReceiver extends BroadcastReceiver{
	public int times = 0;
    private boolean isShowSuccessDialog = true;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		SharedPreferences sp = context.getSharedPreferences(Utils.STORE_SETTINGS,
                SettingActivity.MODE_PRIVATE);

		String num = sp.getString(Utils.ITEM_NUMBER, context.getString(R.string.default_number));
		int deftime = Integer.parseInt(context.getString(R.string.default_delay));
		int defdelay = Integer.parseInt(context.getString(R.string.default_interval));
		
		int first = sp.getInt(Utils.ITEM_DELAY, deftime);
		int delay = sp.getInt(Utils.ITEM_INTERVAL, defdelay);
		times = sp.getInt(Utils.ITEM_TIMES, 0);
		
		boolean hasSendsuccess = hasSend(context);
		boolean hasCard = isSimInsert(context);
		
		Log.i(Utils.TAG, "action --> " + action + ", num = " + num + ", times = "
				+ times + ", hasSend = " + hasSendsuccess + ", hasCard = " + hasCard);
		
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			if (hasSendsuccess) {
				save(context, Utils.STATUS_SUCCESS);
				return;
			} else if (!hasSendsuccess) {
				//showRegisterDialog(context);
				Intent i = new Intent(context, SendDelayService.class);
				context.startService(i);
				save(context, Utils.STATUS_NONE);
			}
		} else if (action.equals(Utils.SALES_TRACKER_SETTING_CHANGED)) {
			if (hasSendsuccess) {
				save(context, Utils.STATUS_SUCCESS);
				return;
			} else if (!hasSendsuccess) {
				sendSmsBroadcast(context, first);
				save(context, Utils.STATUS_NONE);
			}
		} else if (action.equals(Utils.SALES_TRACKER_SEND_SMS) && !hasSendsuccess && hasCard) {
			String body = getMessageBody(context);
            long uptime = SystemClock.elapsedRealtime();
			Log.i(Utils.TAG, "body: " + body + ", uptime = " + uptime);

            if (uptime >= first * 60 * 1000) {
			    sendSms(context, num, body);
            } else if (!hasSendsuccess) {
				sendSmsBroadcast(context, first);
				save(context, Utils.STATUS_NONE);
            }
		} else if (action.equals(Utils.SALES_TRACKER_SEND_SMS_RESULT)) {
			if (getResultCode() == Activity.RESULT_OK) {
				save(context, Utils.STATUS_SUCCESS);
                if (isShowSuccessDialog) {
    				showSuccessDialog(context);
                }
				Log.i(Utils.TAG, "send Result --> " + action);
			} else {
				save(context, Utils.STATUS_TRING + times + 1);
				times = sp.getInt(Utils.ITEM_TIMES, 0);
				if (times > 0 && times < Utils.DEFAULT_RETRY_TIMES) {
					sendSmsBroadcast(context, delay);
				}
			}
			Log.i(Utils.TAG, "send Result --> " + getResultCode());
			
		} else if (action.equals(Utils.SALES_TRACKER_DELIVERY_SMS_RESULT)) {
			if (getResultCode() == Activity.RESULT_OK) {
				//save(context, Utils.STATUS_SUCCESS);
				//showSuccessDialog(context);
			} else {
				save(context, Utils.STATUS_TRING + times + 1);
				times = sp.getInt(Utils.ITEM_TIMES, 0);
				if (times > 0 && times < Utils.DEFAULT_RETRY_TIMES) {
					sendSmsBroadcast(context, delay);
				}
			}
			Log.i(Utils.TAG, "delivery Result --> " + getResultCode());
		} else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
			Log.i(Utils.TAG, "SIM state changed");
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			int state = tm.getSimState();
			switch (state) {
			case TelephonyManager.SIM_STATE_READY:
				if (!hasSendsuccess) {
					//sendSmsBroadcast(context, first);
				}
				Log.i(Utils.TAG, "SIM state changed --- " + System.currentTimeMillis());
				break;
			case TelephonyManager.SIM_STATE_UNKNOWN:
			case TelephonyManager.SIM_STATE_ABSENT:
			case TelephonyManager.SIM_STATE_PIN_REQUIRED:
			case TelephonyManager.SIM_STATE_PUK_REQUIRED:
			case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			default:
				break;
			}
			Log.i(Utils.TAG, "SIM state changed " + state);
		} else if (action.equals("com.fussen.saletracker")) {
			Uri uri = intent.getData();
			Log.i(Utils.TAG, "SIM state changed " + uri);
			Intent i = new Intent(Intent.ACTION_MAIN);
			if (uri.equals(Uri.parse("sale_tracker_code://7758"))) {
				i.setComponent(new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SettingActivity"));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
		}
	}

	private void sendSmsBroadcast(Context context, int time) {
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent send_intent = new Intent(Utils.SALES_TRACKER_SEND_SMS);
		ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SendReceiver");
		send_intent.setComponent(component);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, send_intent, 0);
		am.cancel(sender);
		long ltime = time * 60 * 1000;
		Log.i(Utils.TAG, "1. --- System current time --> " + System.currentTimeMillis());
		
		/*try {
			Thread.sleep(3 * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		//if (System.currentTimeMillis() > beforeTime) {
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ltime, sender);
		Log.i(Utils.TAG, "2. -- System current time --> " + System.currentTimeMillis());
		//}
	}
	
	private void sendSms(Context context, String num, String body) {
		ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SendReceiver");
		Intent send = new Intent(Utils.SALES_TRACKER_SEND_SMS_RESULT);
		Intent delivery = new Intent(Utils.SALES_TRACKER_DELIVERY_SMS_RESULT);
		send.setComponent(component);
		delivery.setComponent(component);
		int block;
        int mcc = context.getResources().getConfiguration().mcc;
        /*if(mcc == 404 || mcc == 405){
        	num = "+919212230707";
            isShowSuccessDialog = true;
        }else if(mcc == 470){
        	num = "7464";
            isShowSuccessDialog = true;
        }else if(mcc == 413){
        	num = "94114339003";
            isShowSuccessDialog = true;
        }*/
		
		isShowSuccessDialog = true;
		Log.i(Utils.TAG, " Yar mcc --> " + mcc);
		SmsManager sms = SmsManager.getDefault();
		ArrayList<String> contents = sms.divideMessage(body);
		block = contents.size();
		ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(block);
		ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(block);
		
		for (int i = 0; i < block; i++) {
			sentIntents.add(PendingIntent.getBroadcast(context, 0, send, 0));
			deliveryIntents.add(PendingIntent.getBroadcast(context, 0, delivery, 0));
			Log.i(Utils.TAG, i + ". content --> " + contents.get(i));
		}
		
		/*if (block == 1) {
			sms.sendTextMessage(num, null, body, sentIntents.get(0), deliveryIntents.get(0));
			return;
		}*/
		
		/*try {
			sms.sendMultipartTextMessage(num, null, contents, sentIntents, deliveryIntents);
		} catch (Exception ex) {
			;
		}*/
		int phoneId = 0;//TelephonyManager.getDefault().getSmsDefaultSim();
        int state  = simState(context);
        switch(state) {
            case 0x01:
                phoneId = 0;
                break;
            case 0x10:
                phoneId = 1;
                break;
            case 0x11:
                phoneId = 0;
                break;
        }

		if (times >= Utils.SIM_RETRY_TIMES) {
			phoneId = 1;
		}
		Log.i(Utils.TAG, " Yar phone id --> " + phoneId);
		//SmsManagerEx.getDefault().sendMultipartTextMessage(num, null, contents, sentIntents, deliveryIntents, phoneId);
		SmsManager.getSmsManagerForSubscriptionId(phoneId).sendMultipartTextMessageWithoutPersisting(num, null, contents, sentIntents, deliveryIntents);
		return;
	}
	
	private String getMessageBody(Context context) {
		StringBuilder sb = new StringBuilder();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(android.content.Context.TELEPHONY_SERVICE);
        int lac = 0;
        int cellid = 0;
        int AscSum = 0;
		Log.i(Utils.TAG, "0. getMessageBody() tm = " + tm);
        String imei = "imei";
        String plmn = "plmn";
		if (tm != null) {
	        CellLocation location = tm.getCellLocation();
			Log.i(Utils.TAG, "0.0 getMessageBody() tm = " + tm + ", location = " + location);
			if (location != null) {
				if (location instanceof GsmCellLocation) {
		           lac = ((GsmCellLocation)location).getLac();
		           cellid = ((GsmCellLocation)location).getCid();
		           Log.i(Utils.TAG, "GsmCellLocation lac = " + lac + "  cellid = " + cellid);
		        } else if (location instanceof CdmaCellLocation) {
		           lac = ((CdmaCellLocation)location).getBaseStationLatitude();
		           cellid = ((CdmaCellLocation)location).getBaseStationId();
		           Log.i(Utils.TAG, "CdmaCellLocation lac = " + lac + "  cellid = " + cellid);
				}
			}
			//imei = tm.getDeviceId();
			//plmn = tm.getNetworkOperator();
		}
		Log.i(Utils.TAG, "1. getMessageBody() tm = " + tm);
        String sw_str = SystemProperties.get("ro.custom.build.version", "unknown");
        String model = SystemProperties.get("ro.product.model", "unknown");
		if (model != null) {
		//	model = ;
		}
        Log.i(Utils.TAG, "getMessageBody plmn = " + plmn + ", sw_str = " + sw_str);
        //int mnc = context.getResources().getConfiguration().mnc;
        //String iccid = tm.getSimSerialNumber();
        //String custom = context.getResources().getString(R.string.default_product_model);
        String imeiN = context.getResources().getString(R.string.default_imei_count);
        int imei_count = Integer.parseInt(imeiN);
        if (imei_count == 2) {
            String imei1 = tm.getDeviceId(0);
            String imei2 = tm.getDeviceId(1);
            sb.append("QSMART IMEI");
            sb.append(" ").append("LT900Pro");
            //sb.append(" ").append(Integer.toHexString(cellid));
            sb.append(" ").append(imei1).append(" ").append(imei2);
            sb.append(" ").append(Integer.toHexString(lac));
            sb.append(" ").append(cellid);
        } else {
            // sb.append(custom).append("\n");
            // sb.append("IMEI:").append(imei);
            //sb.append(imei);
			
			sb.append("REG:01");
            sb.append(":01").append(plmn);//append(mcc).append(mnc);
            sb.append(":02").append(Integer.toHexString(cellid));
            sb.append(":03").append(Integer.toHexString(lac));
            sb.append(":04").append("MOBHIG0001");
            sb.append(":05").append(imei);
            sb.append(":06").append("V1.1");
			sb.append(":07").append(sw_str).append(":");
			
	        
	        char[] apk = sb.toString().toCharArray();
	        for(int i:apk){
	            AscSum += i;
	        }
	        sb.append(Integer.toHexString(AscSum % 255)).append(":");
	        Log.i(Utils.TAG,"8F===" + Integer.toHexString(AscSum % 255));
        }
        Log.i(Utils.TAG, "sms body:" + sb.toString());
        return sb.toString();
	}
	
    private void showSuccessDialog(Context context) {
        Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.success_register);
        builder.setMessage(R.string.success_send_sms);
        builder.setPositiveButton(R.string.success_postive, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setCancelable(false);
        dialog.show();
    }
	
    private void showRegisterDialog(Context context) {
        Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.register_send_sms_tips);
        builder.setMessage(R.string.register_send_sms_content);
        builder.setPositiveButton(R.string.dialog_ok, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setCancelable(false);
        dialog.show();
    }
    
    private void save(Context context, int status) {
    	SharedPreferences sp = context.getSharedPreferences(Utils.STORE_SETTINGS,
                SettingActivity.MODE_PRIVATE);
    	int times = 0;
    	if (status == Utils.STATUS_SUCCESS) {
    		Settings.Global.putInt(context.getContentResolver(), Utils.SALE_SEND, Utils.RESULT_SUCCESS);
    		Intent intent = new Intent("android.intent.action.SEND_SALE");
			ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SendReceiver");
			intent.setComponent(component);
    		context.sendBroadcastAsUser(intent, UserHandle.ALL, "com.android.permission.SALE_TRAKER");
			boolean result = Utils.saveNvRamProinfo(1);
    	} else if (status >= Utils.STATUS_TRING) {
    		times = status - Utils.STATUS_TRING;
    	}
    	
    	SharedPreferences.Editor editor = sp.edit();
        editor.putInt(Utils.ITEM_STATUS, status);
        
        editor.putInt(Utils.ITEM_TIMES, times);
        editor.commit();

        Intent intent = new Intent(Utils.ACTION_CHANGE);
		ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SettingActivity$StatusReceiver");
		intent.setComponent(component);
        context.sendBroadcastAsUser(intent, UserHandle.ALL, "com.android.permission.SALE_TRAKER");
    }
	
    
    private boolean hasSend(Context context) {
        int result = Settings.Global.getInt(context.getContentResolver(), Utils.SALE_SEND, Utils.RESULT_NONE);
        File file = new File("/data/app/.sale");
		int proinfo = Utils.getNvRamProinfo();
        return /*(result == Utils.RESULT_SUCCESS || file.exists()) && */proinfo == Utils.RESULT_SUCCESS;
    }
    
    private boolean isSimInsert(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(android.content.Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Log.i(Utils.TAG, "isSimInsert simState:" + simState);
        return (simState == TelephonyManager.SIM_STATE_READY);
    }

    private int simState(Context context) {
        //TelephonyManagerEx tmE = TelephonyManagerEx.getDefault();
        //TelephonyManager tm = TelephonyManager.getDefault();
		TelephonyManager tm = (TelephonyManager) context.getSystemService(android.content.Context.TELEPHONY_SERVICE);
        int sim1 = tm.getSimState(0);
        int sim2 = tm.getSimState(1);
        int state = 0x00;
        if (sim1 == TelephonyManager.SIM_STATE_READY) {
            state |= 0x01;
        }
        if (sim2 == TelephonyManager.SIM_STATE_READY) {
            state |= 0x10;
        }
		Log.i(Utils.TAG, "simState() state = " + state);
        return state;
    }
}