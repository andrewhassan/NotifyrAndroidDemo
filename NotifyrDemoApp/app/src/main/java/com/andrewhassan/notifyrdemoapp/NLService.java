package com.andrewhassan.notifyrdemoapp;

import android.content.Context;
import android.content.Intent;
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
        byte outputValue[];
        String address = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getString(Constants.STORED_ADDRESS, "");

        if(sbn.isClearable()){
            String msg = "A message has arrived!";
            try {
                if(sbn.getNotification().tickerText != null){
                    msg = sbn.getNotification().tickerText.toString();
                } else{
                    msg = "A message has arrived!";
                }
                msg = Normalizer.normalize(msg, Normalizer.Form.NFD);
                msg = new String(msg.getBytes("ascii"), "ascii");
                if (msg.length() > 200) {
                    msg = msg.substring(0, 200) + "...";
                }
            } catch (UnsupportedEncodingException e) {
                return;
            }

            Log.i(Constants.TAG, "Writing msg " + msg + "to " + address);
            buf.put((byte) 0x01);
            buf.put( msg.getBytes());
            outputValue = buf.array();

            Intent msgIntent = new Intent();
            msgIntent.setAction(Constants.NOTIFYR_NOTIFICATION);
            msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG, outputValue);
            msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG_LENGTH, msg.length() + 1);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgIntent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String address = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getString(Constants.STORED_ADDRESS, "");
        Log.i(Constants.TAG, "Removing msg" + sbn.getNotification().tickerText + "to " + address);
    }


}
