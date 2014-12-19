package com.andrewhassan.notifyrdemoapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Applepie on 11/16/2014.
 */
public class BLEService extends Service {
    private boolean mForeground = false;
    private boolean mStarted = false;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private final Object mLock = new Object();
    private Queue<byte[]> mQueue = new LinkedBlockingDeque<byte[]>();
    private boolean isWriting = false;

    private final BluetoothGattCallback mNotifyrGattCallback = new BluetoothGattCallback() {

        private void writeQueue(BluetoothGatt gatt) {
            BluetoothGattService notifyrService = gatt.getService(Constants.NOTIFYR_SERVICE);
            if (notifyrService == null) {
                mQueue.clear();
                synchronized (mLock) {
                    isWriting = false;
                }
                return;
            }
            if (mQueue.isEmpty()) {
                synchronized (mLock) {
                    isWriting = false;
                }
                gatt.setCharacteristicNotification(notifyrService.getCharacteristic(Constants.RX_MSG), true);
                gatt.disconnect();
                gatt.close();
                return;
            } else {
                notifyrService.getCharacteristic(Constants.TX_MSG).setValue(mQueue.remove());
                gatt.writeCharacteristic(notifyrService.getCharacteristic(Constants.TX_MSG));
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(Constants.TAG, "Connected to GATT server.");
                if (gatt.getService(Constants.NOTIFYR_SERVICE) != null) {
                    writeQueue(gatt);
                } else {
                    Log.i(Constants.TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());
                }
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                Log.i(Constants.TAG, "Disconnected from GATT server.");
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                mQueue.clear();
                synchronized (mLock) {
                    isWriting = false;
                }
                gatt.close();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(Constants.NOTIFYR_SERVICE) != null) {
                Log.i(Constants.TAG, "onServicesDiscovered NotifyrService found: " + status);
                writeQueue(gatt);
            } else {
                Log.w(Constants.TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(Constants.TAG, characteristic.getUuid().toString());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeQueue(gatt);
            } else {
                mQueue.clear();
                synchronized (mLock) {
                    isWriting = false;
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    public boolean getWriting() {
        synchronized (mLock) {
            return isWriting;
        }
    }

    public void setWriting(boolean writing) {
        synchronized (mLock) {
            isWriting = writing;
        }
    }


    private void writeMessage(byte[] message, int length) {
        int numOfMessages = length / 20;
        for (int i = 0; i < numOfMessages; i++) {
            mQueue.add(Arrays.copyOfRange(message, i * 20, (i + 1) * 20));
        }
        mQueue.add(Arrays.copyOfRange(message, numOfMessages * 20, length));
        mQueue.add(new byte[]{0x00});
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] msgArray = new byte[0];
            int msgLength = 0;
            if (intent.hasExtra(Constants.NOTIFYR_NOTIFICATION_MSG)) {
                msgArray = intent.getByteArrayExtra(Constants.NOTIFYR_NOTIFICATION_MSG);
                msgLength = intent.getIntExtra(Constants.NOTIFYR_NOTIFICATION_MSG_LENGTH, 0);
            }
            writeMessage(msgArray, msgLength);
            if (!isWriting) {
                mDevice.connectGatt(BLEService.this, false, mNotifyrGattCallback);
            }
            isWriting = true;

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE);
        if (intent.getAction().equals(Constants.NOTIFYR_START_BLE_SERVICE) &&
                prefs.contains(Constants.STORED_ADDRESS)) {
            Log.i(Constants.TAG, "Received Start Foreground Intent ");
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 43,
                    notificationIntent, 0);

            Intent quitIntent = new Intent(this, BLEService.class);
            quitIntent.setAction(Constants.NOTIFYR_STOP_BLE_SERVICE);
            PendingIntent pQuitIntent = PendingIntent.getService(this, 0,
                    quitIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher);

            Notification notification = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("Notifyr Service")
                    .setContentText("Notifyr Service has started, and is relaying notifcations")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                            "Stop Notifyr Service", pQuitIntent).build();
            mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                startForeground(12345, notification);
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                mDevice = mBluetoothAdapter.getRemoteDevice(prefs.getString(Constants.STORED_ADDRESS, ""));

                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter(Constants.NOTIFYR_NOTIFICATION));
            } else {
                stopSelf();
            }

        } else if (intent.getAction().equals(
                Constants.NOTIFYR_STOP_BLE_SERVICE)) {
            Log.i(Constants.TAG, "Received Stop Foreground Intent");
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
            stopForeground(true);
            stopSelf();
        } else {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
            stopSelf();
        }
        return START_NOT_STICKY;
    }
}
