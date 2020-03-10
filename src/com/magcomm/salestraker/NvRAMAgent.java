package com.magcomm.salestraker;

import vendor.mediatek.hardware.nvram.V1_0.INvram;
import android.util.Log;
import android.os.RemoteException;

import com.android.internal.util.HexDump;
import java.util.ArrayList;

public class NvRAMAgent {

    private static final String DEFAULT_MAC_ADDRESS = "01:02:03:04:05:06";

    private static final String MAC_ADDRESS_FILENAME = "/mnt/vendor/nvdata/APCFG/APRDEB/WIFI";
    private static final int MAC_ADDRESS_OFFSET = 4;
    private static final int MAC_ADDRESS_DIGITS = 6;
    private static final String TAG = Utils.TAG;

    private static final String PROINFO_FILENAME = "/mnt/vendor/nvdata/APCFG/APRDEB/PRODUCT_INFO";
    private static final int PROINFO_OFFSET = 466;//170+64+40+64+128;
    private static final int PROINFO_DIGITS = 1;
    private static final int DEFAULT_PROINFO = -1;

    public static int getProinfo() {
        int result = -1;//DEFAULT_PROINFO;
        StringBuffer nvramBuf = new StringBuffer();
        try {
            int i = 0;
            String buff = null;
            INvram agent = INvram.getService();
            if (agent != null) {
                try {
                    buff = agent.readFileByName(
                            PROINFO_FILENAME, PROINFO_OFFSET + PROINFO_DIGITS);
                } catch (Exception e) {
                    e.printStackTrace();
                    return result;
                }

                Log.i(TAG, "getProinfo buff:" + buff + ", len = " + buff.length());

                // Remove \0 in the end
                byte[] buffArr = HexDump.hexStringToByteArray(buff.substring(0, buff.length()-1));

                ArrayList<Byte> dataArray = new ArrayList<Byte>(PROINFO_OFFSET + PROINFO_DIGITS);
                /*for (i = 0; i < PROINFO_OFFSET + PROINFO_DIGITS; i++) {
                    if (i >= PROINFO_OFFSET) {
                        byte info = (byte)value;
                        dataArray.add(i, info);
                    } else {
                        dataArray.add(i, new Byte(buffArr[i]));
                    }
                }*/
                result = buffArr[PROINFO_OFFSET+PROINFO_DIGITS-1];
                Log.i(TAG, " getProinfo result = " + result);

                /*if (buff.length() >= 2 * (PROINFO_OFFSET + PROINFO_DIGITS)) {
                    // Remove the \0 special character.
                    int infoLen = buff.length() - 1;
                    for (i = PROINFO_OFFSET * 2; i < infoLen; i += 2) {
                        nvramBuf.append(buff.substring(i));
                    }
                    String info = nvramBuf.toString();
                    try {
                        result = Integer.parseInt(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return result;
                    }
                    Log.i(TAG, " getProinfo result = " + result);
                } else {
                    Log.e(TAG, "Fail to read proinfo address");
                }*/
            } else {
                Log.e(TAG, "Nvram is null");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean setProinfo(int value) {
        boolean result = false;
        try {
            int i = 0;
            String buff = null;
            INvram agent = INvram.getService();
            if (agent != null) {
                try {
                    buff = agent.readFileByName(
                            PROINFO_FILENAME, PROINFO_OFFSET + PROINFO_DIGITS);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                Log.i(TAG, "getProinfo buff:" + buff + ", len = " + buff.length());
                // Remove \0 in the end
                byte[] buffArr = HexDump.hexStringToByteArray(buff.substring(0, buff.length()-1));

                ArrayList<Byte> dataArray = new ArrayList<Byte>(PROINFO_OFFSET + PROINFO_DIGITS);
                for (i = 0; i < PROINFO_OFFSET + PROINFO_DIGITS; i++) {
                    if (i >= PROINFO_OFFSET) {
                        byte info = (byte)value;
                        dataArray.add(i, info);
                    } else {
                        dataArray.add(i, new Byte(buffArr[i]));
                    }
                }
                Log.i(TAG, "setProinfo dataArray[466] = " + dataArray.get(PROINFO_OFFSET+PROINFO_DIGITS-1));

                int flag = 0;
                try {
                    flag = agent.writeFileByNamevec(PROINFO_FILENAME,
                            PROINFO_OFFSET + PROINFO_DIGITS, dataArray);
                    result = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                Log.e(TAG, "Nvram is null");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getMacAddress() {
        String result = DEFAULT_MAC_ADDRESS;
        StringBuffer nvramBuf = new StringBuffer();
        try {
            int i = 0;
            String buff = null;
            INvram agent = INvram.getService();
            if (agent != null) {
                buff = agent.readFileByName(
                        MAC_ADDRESS_FILENAME, MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS);

                Log.i(TAG, "Raw data:" + encryptMessage(buff));
                if (buff.length() >= 2 * (MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS)) {
                    // Remove the \0 special character.
                    int macLen = buff.length() - 1;
                    for (i = MAC_ADDRESS_OFFSET * 2; i < macLen; i += 2) {
                        if ((i + 2) < macLen) {
                            nvramBuf.append(buff.substring(i, i + 2));
                            nvramBuf.append(":");
                        } else {
                            nvramBuf.append(buff.substring(i));
                        }
                    }
                    result = nvramBuf.toString();
                } else {
                    Log.e(TAG, "Fail to read mac address");
                }
            } else {
                Log.e(TAG, "Nvram is null");
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (result.length() > DEFAULT_MAC_ADDRESS.length()) {
            // remove extra characters if length longer than expected
            result = result.substring(0, DEFAULT_MAC_ADDRESS.length());

        } else if (result.length() < DEFAULT_MAC_ADDRESS.length()) {
            // set to default if length shorted than expected
            result = DEFAULT_MAC_ADDRESS;
        }
        Log.d(TAG, "result: " + encryptMessage(result));
        return result;
    }

    public static String encryptMessage(int value) {
        return encryptMessage("" + value);
    }

    public static String encryptMessage(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }

        String result = createAsterisks(text.length() / 2)
                + text.substring(text.length() / 2);
        return result;
    }

    private static String createAsterisks(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append("*");
        }
        return builder.toString();
    }	
}
