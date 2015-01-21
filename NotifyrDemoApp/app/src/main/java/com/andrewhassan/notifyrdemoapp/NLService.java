package com.andrewhassan.notifyrdemoapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.HashSet;

/**
 * Created by Applepie on 7/18/2014.
 */
public class NLService extends NotificationListenerService{
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getStringSet(Constants.FILTERED_APPS, new HashSet<String>()).contains(sbn.getPackageName())) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(1024);
        byte msgTextFinal[];
        ByteBuffer msgTitleFinal = ByteBuffer.allocate(40);
        String address = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getString(Constants.STORED_ADDRESS, "");

        if(sbn.isClearable()){
            String msg = "A message has arrived!";
            String title = "An app";
            int type = Constants.CHAT_ICON;

            try {
                if (sbn.getPackageName() != null) {
                    try {
                        PackageManager pm = getApplicationContext().getPackageManager();
                        title = (String) pm.getApplicationLabel(pm.getApplicationInfo(sbn.getPackageName(), 0));
                        type = Constants.appIconMapping.getDefault(title, Constants.CHAT_ICON);
                        title = Normalizer.normalize(title, Normalizer.Form.NFD);
                        title = new String(title.getBytes("ascii"), "ascii");
                        if (title.length() > 36) {
                            title = title.substring(0, 36) + "...";
                        }

                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                if(sbn.getNotification().tickerText != null){
                    msg = sbn.getNotification().tickerText.toString();
                }

                msg = Normalizer.normalize(msg, Normalizer.Form.NFD);
                msg = new String(msg.getBytes("ascii"), "ascii");
                if (msg.length() > 211) {
                    msg = msg.substring(0, 211) + "...";
                }
            } catch (UnsupportedEncodingException e) {
                return;
            }

            Log.i(Constants.TAG, "Writing msg " + msg + "to " + address);
            buf.put((byte) 0x01);
            buf.put((byte) type);
            msgTitleFinal.put(title.getBytes());
            for (int i = 0; i < 40 - (title.length()); i++) {
                msgTitleFinal.put((byte) 1);
            }
            buf.put(msgTitleFinal.array());
            buf.put(msg.getBytes());
            msgTextFinal = buf.array();

            Intent msgIntent = new Intent();
            msgIntent.setAction(Constants.NOTIFYR_NOTIFICATION);
            msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG, msgTextFinal);
            msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG_LENGTH, 1 + 40 + msg.length() + 1);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgIntent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String address = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getString(Constants.STORED_ADDRESS, "");
        Log.i(Constants.TAG, "Removing msg" + sbn.getNotification().tickerText + "to " + address);
    }


}
