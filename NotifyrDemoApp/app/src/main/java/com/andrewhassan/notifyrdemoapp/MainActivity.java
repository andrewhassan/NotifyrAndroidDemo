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
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class MainActivity extends Activity {


    private HashMap<String,BluetoothDevice> m_devices;
    private DeviceAdapter m_names;

    private ProgressDialog barProgressDialog;

    private BluetoothAdapter m_bt_adapter;

    private Handler m_handler;
    private boolean m_running;
    SharedPreferences prefs;




    public void sendData(View view) {
        BluetoothGattService notifyrService;
        BluetoothGattCharacteristic txCharacteristic;
        BluetoothGattCharacteristic doneCharacteristic;

        ByteBuffer buf = ByteBuffer.allocate(1024);
        byte outputValue[];
        EditText text_area = (EditText) this.findViewById(R.id.editText);
        String str = text_area.getText().toString();
        // Clear text area
        text_area.setText("");

        //create byte string
        buf.put((byte) 0x01);
        buf.put(str.getBytes());
        outputValue = buf.array();

        BLEConnectionHandler.getInstance().writeMessage(outputValue,str.length()+1);
        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(Constants.STORED_ADDRESS, ""));
        if(device != null && !BLEConnectionHandler.getInstance().getWriting()) {
            BLEConnectionHandler.getInstance().setWriting(true);
            device.connectGatt(this, false, BLEConnectionHandler.getInstance());
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
        byte[] outputValue = new byte[]{0x02,(byte) (hours + 1), (byte) (minutes + 1), (byte) (seconds + 1)};

        BLEConnectionHandler.getInstance().writeMessage(outputValue,outputValue.length);
        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(Constants.STORED_ADDRESS, ""));
        if(device != null && !BLEConnectionHandler.getInstance().getWriting()) {
            BLEConnectionHandler.getInstance().setWriting(true);
            device.connectGatt(this, false, BLEConnectionHandler.getInstance());
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
                prefs.edit().putString(Constants.STORED_ADDRESS, m_devices.get(m_names.getItem(which)).getAddress()).apply();
                BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(Constants.STORED_ADDRESS, ""));
                if (device != null) {
                    prefs.edit().putString(Constants.STORED_ADDRESS, m_devices.get(m_names.getItem(which)).getAddress()).apply();
                }
            }
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = this.getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE);
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
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume(){
        super.onResume();
        // If BT isn't supported or turned off
        if (m_bt_adapter == null || !m_bt_adapter.isEnabled()) {
            Intent enable_bt_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enable_bt_intent);
            if (m_bt_adapter == null) finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection

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
            }, Constants.SCAN_PERIOD);

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
                    Log.i(Constants.TAG, "Added device:" + device.getName());
                    if(!m_devices.containsKey(device.getName()) && device.getName().contains(Constants.NOTIFYR_NAME)) {
                        m_devices.put(device.getName(), device);
                        m_names.add(device.getName());
                    }
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





}
