package com.andrewhassan.notifyrdemoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.Normalizer;

/**
 * Created by Applepie on 7/18/2014.
 */
public class NLService extends NotificationListenerService{
    private BluetoothAdapter m_bt_adapter;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
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
            BluetoothManager bt_manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            m_bt_adapter = bt_manager.getAdapter();
            buf.put((byte) 0x01);
            buf.put( msg.getBytes());
            outputValue = buf.array();

            BLEConnectionHandler.getInstance().writeMessage(outputValue,msg.length()+1);

            BluetoothDevice device = m_bt_adapter.getRemoteDevice(address);
            Log.i(Constants.TAG, "is writing? " + BLEConnectionHandler.getInstance().getWriting());
            if (device != null) {
                BLEConnectionHandler.getInstance().setWriting(true);
                device.connectGatt(this, false, BLEConnectionHandler.getInstance());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String address = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE).getString(Constants.STORED_ADDRESS, "");
        Log.i(Constants.TAG, "Removing msg" + sbn.getNotification().tickerText + "to " + address);
    }


}
