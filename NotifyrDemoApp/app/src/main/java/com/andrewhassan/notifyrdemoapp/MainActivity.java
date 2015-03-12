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
import android.support.v4.content.LocalBroadcastManager;
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
        ByteBuffer titleBuf = ByteBuffer.allocate(40);
        EditText text_area = (EditText) this.findViewById(R.id.editText);
        String str = text_area.getText().toString();
        // Clear text area
        text_area.setText("");
        if (str.length() > 211) {
            str = str.substring(0, 211) + "...";
        }
        //create byte string
        buf.put((byte) 0x01);
        buf.put((byte) Constants.CHAT_ICON);
        titleBuf.put("Notifyr".getBytes());
        for (int i = 0; i < 40 - ("Notifyr".length()); i++) {
            titleBuf.put((byte) 1);
        }
        buf.put(titleBuf.array());
        buf.put(str.getBytes());
        outputValue = buf.array();
        Intent msgIntent = new Intent();
        msgIntent.setAction(Constants.NOTIFYR_NOTIFICATION);
        msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG, outputValue);
        msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG_LENGTH, 42 + str.length());

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgIntent);
    }

    public void sendTime(View v) {
        //send time as a string, must be hours,minutes, and seconds. All values must be plus one, since we can have 0 values
        //(CANNOT HAVE!!!)
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR_OF_DAY);
        byte[] outputValue = new byte[]{0x02,(byte) (hours + 1), (byte) (minutes + 1), (byte) (seconds + 1)};

        Intent msgIntent = new Intent();
        msgIntent.setAction(Constants.NOTIFYR_NOTIFICATION);
        msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG, outputValue);
        msgIntent.putExtra(Constants.NOTIFYR_NOTIFICATION_MSG_LENGTH, outputValue.length);

        getApplicationContext().sendBroadcast(msgIntent);

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
                invalidateOptionsMenu();
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
        if (!mPrefs.contains(Constants.STORED_ADDRESS)) {
            menu.findItem(R.id.start_service).setVisible(false);
            menu.findItem(R.id.stop_service).setVisible(false);
        }
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
                invalidateOptionsMenu();
                return true;
            case R.id.filter_apps:
                Intent filterActivityIntent = new Intent(this, AppFilterActivity.class);
                this.startActivity(filterActivityIntent);
                return true;
            case R.id.start_service:
                Intent startServiceIntent = new Intent(this, BLEService.class);
                startServiceIntent.setAction(Constants.NOTIFYR_START_BLE_SERVICE);
                startService(startServiceIntent);
                return true;

            case R.id.stop_service:
                Intent stopServiceIntent = new Intent(this, BLEService.class);
                stopServiceIntent.setAction(Constants.NOTIFYR_STOP_BLE_SERVICE);
                startService(stopServiceIntent);
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
