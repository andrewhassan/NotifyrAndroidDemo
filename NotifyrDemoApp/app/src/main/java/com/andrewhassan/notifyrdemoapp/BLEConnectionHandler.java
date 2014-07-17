package com.andrewhassan.notifyrdemoapp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Applepie on 7/16/2014.
 */
public class BLEConnectionHandler extends BluetoothGattCallback {
    private static BLEConnectionHandler sInstance = null;
    private boolean isWriting = false;
    private Queue<byte[]> mQueue = new LinkedBlockingDeque<byte[]>();

    private BLEConnectionHandler(){
    }

    public static BLEConnectionHandler getInstance() {
        if(sInstance == null) {
            sInstance = new BLEConnectionHandler();
        }
        return sInstance;
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
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
            gatt.disconnect();
        }
    }

    public boolean isWriting() {
        return isWriting;
    }

    public void setWriting(boolean isWriting) {
        this.isWriting = isWriting;
    }

    @Override
    // New services discovered
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(Constants.NOTIFYR_SERVICE) != null) {
            Log.i(Constants.TAG, "onServicesDiscovered received: " + status);
            writeQueue(gatt);
        } else {
            Log.w(Constants.TAG, "onServicesDiscovered received: " + status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            writeQueue(gatt);
        } else {
            isWriting = false;
        }
    }

    private void writeQueue(BluetoothGatt gatt) {
        if (gatt.getService(Constants.NOTIFYR_SERVICE) == null) {
            isWriting = false;
            return;
        }

        if (mQueue.isEmpty()) {
            isWriting = false;
            gatt.disconnect();
            gatt.close();
            return;
        }

        if (mQueue.peek()[0] != 0x00) {
            gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_MSG).setValue(mQueue.remove());
            gatt.writeCharacteristic(gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_MSG));
        } else {
            gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_DONE).setValue(mQueue.remove());
            gatt.writeCharacteristic(gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_DONE));
        }
    }

    public void writeMessage( byte[] message,int length){
        int numMessages = length / 20;

        for (int i = 0; i < numMessages; i++) {
            mQueue.add(Arrays.copyOfRange(message, i * 20, (i + 1) * 20));
        }
        mQueue.add(Arrays.copyOfRange(message, numMessages * 20, length));
        mQueue.add(new byte[]{0x00});
    }

}
