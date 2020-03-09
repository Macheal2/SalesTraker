package com.magcomm.salestraker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.ComponentName;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.provider.Settings;
import android.os.UserHandle;
import android.net.Uri;

import java.lang.ref.WeakReference;

public class SettingActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    StatusReceiver mStatusReceiver = null;
    IntentFilter mIntentFilter = null;
    final String KEY_PARENT = "parent";
    final String KEY_ISTEST = "key_istest";
    final String KEY_DELAY = "key_delay";
    final String KEY_INTERVAL = "key_interval";
    final String KEY_NUMBER = "key_number";
    final String KEY_CLEAR = "key_clear";
    final String KEY_STATUS = "key_status";

    PreferenceScreen mParent = null;
    CheckBoxPreference mPrefTest = null;
    EditTextPreference mPrefDelay = null;
    EditTextPreference mPrefInterval = null;
    EditTextPreference mPrefNumber = null;
    Preference mPrefClear = null;
    Preference mPrefStatus = null;
    public static WeakReference<SettingActivity> wp;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
		wp = new WeakReference<>(this);
        addPreferencesFromResource(R.xml.settings);
        //mStatusReceiver = new StatusReceiver();
        //mIntentFilter = new IntentFilter();
        //mIntentFilter.addAction(Utils.ACTION_CHANGE);

        mParent = (PreferenceScreen) findPreference(KEY_PARENT);
        mPrefTest = (CheckBoxPreference) mParent.findPreference(KEY_ISTEST);

        mPrefDelay = (EditTextPreference) mParent.findPreference(KEY_DELAY);
        mPrefInterval = (EditTextPreference) mParent.findPreference(KEY_INTERVAL);
        mPrefNumber = (EditTextPreference) mParent.findPreference(KEY_NUMBER);
        mPrefDelay.getEditText().setKeyListener(new DigitsKeyListener(false, false));
        mPrefInterval.getEditText().setKeyListener(new DigitsKeyListener(false, false));
        mPrefNumber.getEditText().setKeyListener(new NumberKeyListener() {
                                                     @Override
                                                     protected char[] getAcceptedChars() {
                                                         char[] numberChars = {'1','2','3','4','5','6','7','8','9','0', '+'};
                                                         return numberChars;

                                                     }
                                                     @Override 
                                                     public int getInputType() { 
                                                         // TODO Auto-generated method stub
                                                         return android.text.InputType.TYPE_CLASS_PHONE; 
                                                     }

        });
        mPrefClear = (Preference) mParent.findPreference(KEY_CLEAR);
        mPrefClear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
		        SharedPreferences settings = getSharedPreferences(Utils.STORE_SETTINGS,
		                SettingActivity.MODE_PRIVATE);
		    	SharedPreferences.Editor editor = settings.edit();
		        editor.putInt(Utils.ITEM_STATUS, -1);
		        editor.putInt(Utils.ITEM_TIMES, 0);
		        editor.commit();
				
				Settings.Global.putInt(getContentResolver(), Utils.SALE_SEND, Utils.RESULT_NONE);
                updateStatus();
                return true;
            }
        });
        mPrefStatus = (Preference) mParent.findPreference(KEY_STATUS);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences settings = getSharedPreferences(Utils.STORE_SETTINGS,
                SettingActivity.MODE_PRIVATE);
        boolean isopen = settings.getBoolean(Utils.ITEM_ISTEST,
                Boolean.parseBoolean(getString(R.string.default_test)));
        mPrefTest.setChecked(isopen);
        mPrefTest.setSummary(isopen ? this.getString(R.string.setting_open) : this
                .getString(R.string.setting_close));

        int delay = settings.getInt(Utils.ITEM_DELAY,
                Integer.parseInt(getString(R.string.default_delay)));
        mPrefDelay.setSummary(delay + getString(R.string.setting_minute));

        int interval = settings.getInt(Utils.ITEM_INTERVAL,
                Integer.parseInt(getString(R.string.default_interval)));
        mPrefInterval.setSummary(interval + getString(R.string.setting_minute));

        String number = settings.getString(Utils.ITEM_NUMBER, getString(R.string.default_number));
        mPrefNumber.setSummary(number);
        
        int status = settings.getInt(Utils.ITEM_STATUS, Utils.STATUS_NONE);
        
        SharedPreferences.Editor editor = settings.edit();
        if (status == Utils.STATUS_SUCCESS) {
        	//do nothing
        } else if (isSimInsert()) {
        	editor.putInt(Utils.ITEM_STATUS, Utils.STATUS_DETSIM);
        } else {
        	editor.putInt(Utils.ITEM_STATUS, Utils.STATUS_NOSIM);
        }
        editor.commit();
    }

    @Override
    protected void onPause() {
        //unregisterReceiver(mStatusReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
		if (wp == null) {
			wp = new WeakReference<>(this);
		}
        updateStatus();
        //registerReceiver(mStatusReceiver, mIntentFilter);
    }

    private void updateStatus() {
        SharedPreferences settings = getSharedPreferences(Utils.STORE_SETTINGS,
                SettingActivity.MODE_PRIVATE);
        int status = settings.getInt(Utils.ITEM_STATUS, Utils.STATUS_NONE);
        if (status == Utils.STATUS_NONE) {
            mPrefStatus.setSummary("");
        } else if (status == Utils.STATUS_NOSIM) {
            mPrefStatus.setSummary(R.string.setting_no_sim);
        } else if (status == Utils.STATUS_SUCCESS) {
            mPrefStatus.setSummary(R.string.setting_success);
        } else if (status == Utils.STATUS_FAILED) {
            mPrefStatus.setSummary(R.string.setting_failed);
        } else if (status > Utils.STATUS_TRING) {
            mPrefStatus.setSummary(getString(R.string.setting_retry)
                    + (status - Utils.STATUS_TRING));
        } else if (status == Utils.STATUS_DETSIM) {
            mPrefStatus.setSummary(R.string.setting_det_sim);
        }
        Log.i(Utils.TAG, "updateStatus status:" + status);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        SharedPreferences.Editor editor = getSharedPreferences(Utils.STORE_SETTINGS,
                SettingActivity.MODE_PRIVATE).edit();
        Log.i(Utils.TAG, "key:" + key);
        if (key.equals(KEY_ISTEST)) {
            boolean isopen = mPrefTest.isChecked();
            mPrefTest.setSummary(isopen ? this.getString(R.string.setting_open) : this
                    .getString(R.string.setting_close));
            /*
             * editor.putBoolean(ITEM_ISTEST, isopen); int enable = (isopen ||
             * !isSendFinished()) ?
             * PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
             * PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
             * getPackageManager().setComponentEnabledSetting(new
             * ComponentName(this, BootReceiver.class), enable,
             * PackageManager.DONT_KILL_APP);
             */
        } else if (key.equals(KEY_DELAY)) {
            try {
                String text = mPrefDelay.getText();
                int delay = Integer.parseInt(text);
                mPrefDelay.setSummary(text + getString(R.string.setting_minute));
                Log.i(Utils.TAG, "value:" + text);

                editor.putInt(Utils.ITEM_DELAY, delay);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (key.equals(KEY_INTERVAL)) {
            try {
                String text = mPrefInterval.getText();
                int interval = Integer.parseInt(text);
                mPrefInterval.setSummary(text + getString(R.string.setting_minute));
                Log.i(Utils.TAG, "value:" + text);

                editor.putInt(Utils.ITEM_INTERVAL, interval);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (key.equals(KEY_NUMBER)) {
            try {
                String text = mPrefNumber.getText();
                if (!text.equals("") && text != null) {
                    mPrefNumber.setSummary(text);
                    editor.putString(Utils.ITEM_NUMBER, text);
                }
                Log.i(Utils.TAG, "value:" + text);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        editor.commit();
        
        if (key.equals(KEY_NUMBER) || key.equals(KEY_INTERVAL) || key.equals(KEY_DELAY)) {
            Intent intent = new Intent(Utils.SALES_TRACKER_SETTING_CHANGED);
			ComponentName component = new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SendReceiver");
			intent.setComponent(component);
            sendBroadcastAsUser(intent, UserHandle.ALL, "com.android.permission.SALE_TRAKER");
        }
    }

    public static class StatusReceiver extends BroadcastReceiver {
        /*public StatusReceiver() {
            super();
        }*/
		
        @Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
            Log.i(Utils.TAG, "action:" + action);
			if (wp != null && Utils.ACTION_CHANGE.equals(action)) {
				SettingActivity activity = wp.get();
				if (activity != null) {
					activity.updateStatus();
				}
			} else if ("android.provider.Telephony.SECRET_CODE".equals(action)) {
				Uri uri = intent.getData();
				Log.i(Utils.TAG, "SIM state changed " + uri);
				Intent i = new Intent(Intent.ACTION_MAIN);
				if (uri.equals(Uri.parse("android_secret_code://75237"))) {
					i.setComponent(new ComponentName("com.magcomm.salestraker", "com.magcomm.salestraker.SettingActivity"));
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
				}
			}
        }
    }
    
    private boolean isSimInsert() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(android.content.Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Log.i(Utils.TAG, "isSimInsert simState:" + simState);
        return (simState == TelephonyManager.SIM_STATE_READY);
    }

}
