package com.andrewhassan.notifyrdemoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {


    private HashMap<String, BluetoothDevice> mDevices;
    private DeviceAdapter mNames;

    private ProgressDialog barProgressDialog;

    private BluetoothAdapter mBtAdapter;

    private Handler mHandler;
    private boolean mRunning;
    SharedPreferences mPrefs;

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
        if (str.length() > 200) {
            str = str.substring(0, 200) + "...";
        }
        //create byte string
        buf.put((byte) 0x01);
        buf.put(str.getBytes());
        outputValue = buf.array();

        BLEConnectionHandler.getInstance().writeMessage(outputValue,str.length()+1);
        BluetoothDevice device = mBtAdapter.getRemoteDevice(mPrefs.getString(Constants.STORED_ADDRESS, ""));
        Log.i(Constants.TAG, "is writing? " + BLEConnectionHandler.getInstance().getWriting());
        if (device != null) {
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
        BluetoothDevice device = mBtAdapter.getRemoteDevice(mPrefs.getString(Constants.STORED_ADDRESS, ""));
        if (device != null) {
            BLEConnectionHandler.getInstance().setWriting(true);
            device.connectGatt(this, false, BLEConnectionHandler.getInstance());
        }

    }

    public void showResults() {
        if (mNames == null || mNames.isEmpty()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("aaa").setAdapter(mNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString(Constants.STORED_ADDRESS, mDevices.get(mNames.getItem(which)).getAddress()).apply();
                BluetoothDevice device = mBtAdapter.getRemoteDevice(mPrefs.getString(Constants.STORED_ADDRESS, ""));
                if (device != null) {
                    mPrefs.edit().putString(Constants.STORED_ADDRESS, mDevices.get(mNames.getItem(which)).getAddress()).apply();
                }
            }
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = this.getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        BluetoothManager bt_manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBtAdapter = bt_manager.getAdapter();
        mHandler = new Handler();
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
                mPrefs.edit().clear().commit();
                return true;
            case R.id.filter_apps:
                Intent intent = new Intent(this, AppFilterActivity.class);
                this.startActivity(intent);
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
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Intent enable_bt_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enable_bt_intent);
            if (mBtAdapter == null) finish();
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
            mNames = new DeviceAdapter(MainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<String>());
            mDevices = new HashMap<String, BluetoothDevice>();

            barProgressDialog = new ProgressDialog(MainActivity.this);
            barProgressDialog.setTitle("Searching for devices");
            barProgressDialog.setMessage("Device search in progress...");
            barProgressDialog.setProgressStyle(barProgressDialog.STYLE_SPINNER);
            barProgressDialog.setCancelable(false);
            barProgressDialog.show();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRunning = false;
                    mBtAdapter.stopLeScan(mLeScanCallback);
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

            mRunning = true;
            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            if (barProgressDialog != null) {
                barProgressDialog.dismiss();
            }
            mRunning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.i(Constants.TAG, "Added device:" + device.getName());
                    if (!mDevices.containsKey(device.getName()) && device.getName().contains(Constants.NOTIFYR_NAME)) {
                        mDevices.put(device.getName(), device);
                        mNames.add(device.getName());
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
