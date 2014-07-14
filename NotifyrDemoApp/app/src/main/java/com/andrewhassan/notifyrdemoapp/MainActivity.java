package com.andrewhassan.notifyrdemoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity {


    private HashMap<String,BluetoothDevice> m_devices;
    private DeviceAdapter m_names;

    private ProgressDialog barProgressDialog;

    private BluetoothAdapter m_bt_adapter;
    private BluetoothGatt mBluetoothGatt;

    private Handler m_handler;
    private boolean m_running;
    SharedPreferences prefs;

    private static final UUID NOTIFYR_SERVICE = UUID.fromString("F9266FD7-EF07-45D6-8EB6-BD74F13620F9");
    private static final UUID TX_MSG = UUID.fromString("E788D73B-E793-4D9E-A608-2F2BAFC59A00");
    private static final UUID TX_DONE = UUID.fromString("2f7f24f2-0301-11e4-93b1-b2227cce2b54");

    private static final String STORED_ADDRESS = "STORED_ADDRESS";
    private static final String TAG = "NOTIFYR_APP";
    private static final long SCAN_PERIOD = 3000;

    private boolean sendDoneFlag = false;

    public void sendData(View view) {
        BluetoothGattService notifyrService;
        BluetoothGattCharacteristic txCharacteristic;
        BluetoothGattCharacteristic doneCharacteristic;

        ByteBuffer buf = ByteBuffer.allocate(1024);
        byte outputValue[];
        EditText text_area = (EditText) this.findViewById(R.id.editText);
        String str = text_area.getText().toString();
        int length = str.length() / 20;

        // Clear text area
        text_area.setText("");

        //create byte string
        buf.put((byte) 0x01);
        buf.put(str.getBytes());
        outputValue = buf.array();


        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(STORED_ADDRESS, ""));
        if (device != null && mBluetoothGatt != null) {
            notifyrService = mBluetoothGatt.getService(NOTIFYR_SERVICE);
            txCharacteristic = notifyrService.getCharacteristic(TX_MSG);
            doneCharacteristic = notifyrService.getCharacteristic(TX_DONE);
            if(txCharacteristic != null && doneCharacteristic != null){
                //must split string up into 20 byte chunks
                sendDoneFlag = false;
                for (int i = 0; i < length; i++) {

                    Log.d("MainActivity", "sendData...going to send message string \"" +  new String(Arrays.copyOfRange(outputValue,i * 20,(i + 1) * 20)) + "\"");
                    txCharacteristic.setValue(Arrays.copyOfRange(outputValue,i * 20,(i + 1) * 20));
                    mBluetoothGatt.writeCharacteristic(txCharacteristic);

                }

                Log.d("MainActivity", "sendData...going to send message string \"" + new String(Arrays.copyOfRange(outputValue,length*20, str.length()+1)) + "\"");
                sendDoneFlag = true;
                txCharacteristic.setValue(Arrays.copyOfRange(outputValue, length * 20,str.length()+1));
                mBluetoothGatt.writeCharacteristic(txCharacteristic);
            }
        }

        // Initialize BT connection
        // Send str using BT interface
    }

    public void sendTime(View v) {
        BluetoothGattService notifyrService;
        BluetoothGattCharacteristic txCharacteristic;
        BluetoothGattCharacteristic doneCharacteristic;
        //send time as a string, must be hours,minutes, and seconds. All values must be plus one, since we can have 0 values
        //(CANNOT HAVE!!!)
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR_OF_DAY);
        byte[] valuesToSend = new byte[]{0x02,(byte) (hours + 1), (byte) (minutes + 1), (byte) (seconds + 1)};

        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(STORED_ADDRESS, ""));

        if (device != null && mBluetoothGatt != null) {
            notifyrService = mBluetoothGatt.getService(NOTIFYR_SERVICE);
            txCharacteristic = notifyrService.getCharacteristic(TX_MSG);
            doneCharacteristic = notifyrService.getCharacteristic(TX_DONE);
            if(txCharacteristic != null && doneCharacteristic != null){
                //must split string up into 20 byte chunks
                Log.d("MainActivity", "sendData...going to send date string \"" + valuesToSend[0] + valuesToSend[1] + valuesToSend[2] + valuesToSend[3] + "\"");
                sendDoneFlag = true;
                txCharacteristic.setValue(valuesToSend);
                mBluetoothGatt.writeCharacteristic(txCharacteristic);
            }
        }

    }

    public void showResults() {
        if (m_names == null || m_names.isEmpty()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("aaa").setAdapter(m_names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putString(STORED_ADDRESS, m_devices.get(m_names.getItem(which)).getAddress()).apply();
                BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(STORED_ADDRESS, ""));
                if (device != null) {
                    mBluetoothGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                }
            }
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = this.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        BluetoothManager bt_manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        m_bt_adapter = bt_manager.getAdapter();
        m_handler = new Handler();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.find_devices:
                scanLeDevice(true);
                return true;
            case R.id.forget_device:
                prefs.edit().clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If BT isn't supported or turned off
        if (m_bt_adapter == null || !m_bt_adapter.isEnabled()) {
            Intent enable_bt_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enable_bt_intent);
            if (m_bt_adapter == null) finish();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            m_names = new DeviceAdapter(MainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<String>());
            m_devices = new HashMap<String, BluetoothDevice>();

            barProgressDialog = new ProgressDialog(MainActivity.this);
            barProgressDialog.setTitle("Searching for devices");
            barProgressDialog.setMessage("Device search in progress...");
            barProgressDialog.setProgressStyle(barProgressDialog.STYLE_SPINNER);
            barProgressDialog.setCancelable(false);
            barProgressDialog.show();

            // Stops scanning after a pre-defined scan period.
            m_handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    m_running = false;
                    m_bt_adapter.stopLeScan(mLeScanCallback);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (barProgressDialog != null) {
                                barProgressDialog.dismiss();
                            }
                            showResults();
                        }
                    });
                }
            }, SCAN_PERIOD);

            m_running = true;
            m_bt_adapter.startLeScan(mLeScanCallback);
        } else {
            if (barProgressDialog != null) {
                barProgressDialog.dismiss();
            }
            m_running = false;
            m_bt_adapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.i(TAG, "Added device:" + device.getName());
                    m_devices.put(device.getName(), device);
                    m_names.add(device.getName());
                }
            };

    private class DeviceAdapter extends ArrayAdapter<String> {

        public DeviceAdapter(Context context, int textViewResourceId,
                             List<String> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(NOTIFYR_SERVICE)!= null) {
                Log.i(TAG, "onServicesDiscovered received: " + status);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG,characteristic.getUuid().toString());
            if(sendDoneFlag) {
                gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_DONE).setValue(new byte[]{0x00});
                mBluetoothGatt.writeCharacteristic(gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_DONE));
                sendDoneFlag = false;
            }
        }
    };


}
