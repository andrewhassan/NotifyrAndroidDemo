package com.andrewhassan.notifyrdemoapp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Applepie on 7/18/2014.
 */
public class BLEConnectionHandler extends BluetoothGattCallback {

    //TODO: WTF is this not a service??? We need it for notifications from the device!
     private static BLEConnectionHandler sInstance;

    private final Object mLock = new Object();
    private Queue<byte[]> mQueue = new LinkedBlockingDeque<byte[]>();
    private boolean isWriting = false;

    private BLEConnectionHandler(){
    }

    public static BLEConnectionHandler getInstance(){
        if(sInstance == null){
            sInstance = new BLEConnectionHandler();
        }
        return sInstance;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(Constants.TAG, "Connected to GATT server.");
            if(gatt.getService(Constants.NOTIFYR_SERVICE)!= null){
                writeQueue(gatt);
            } else{
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
            synchronized (mLock){
                isWriting = false;
            }
            gatt.close();
        }
    }

    @Override
    // New services discovered
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(Constants.NOTIFYR_SERVICE)!= null) {
            Log.i(Constants.TAG, "onServicesDiscovered received: " + status);
            writeQueue(gatt);
        } else {
            Log.w(Constants.TAG, "onServicesDiscovered received: " + status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.i(Constants.TAG,characteristic.getUuid().toString());

        if(status == BluetoothGatt.GATT_SUCCESS) {
            writeQueue(gatt);
        } else{
            mQueue.clear();
            synchronized (mLock){
                isWriting = false;
            }
        }
    }

    public boolean getWriting(){
        synchronized (mLock) {
           return isWriting;
        }
    }

    public void setWriting(boolean writing){
        synchronized (mLock) {
            isWriting = writing;
        }
    }

    public void writeMessage(byte[] message,int length){
        int numOfMessages = length / 20;
        for (int i = 0; i < numOfMessages; i++) {
            mQueue.add(Arrays.copyOfRange(message, i * 20, (i + 1) * 20));
        }
        mQueue.add(Arrays.copyOfRange(message, numOfMessages * 20, length));

        mQueue.add(new byte[]{0x00});
    }

    private void writeQueue(BluetoothGatt gatt) {
        if(gatt.getService(Constants.NOTIFYR_SERVICE) == null){
            mQueue.clear();
            synchronized (mLock){
                isWriting = false;
            }
            return;
        }

        if(mQueue.isEmpty()){
            synchronized (mLock){
                isWriting = false;
            }
            gatt.disconnect();
            gatt.close();
            return;
        }

        if (!mQueue.isEmpty()) {
            gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_MSG).setValue(mQueue.remove());
            gatt.writeCharacteristic(gatt.getService(Constants.NOTIFYR_SERVICE).getCharacteristic(Constants.TX_MSG));
        }
    }
}
