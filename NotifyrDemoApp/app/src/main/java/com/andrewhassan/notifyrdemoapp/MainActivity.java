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

    private Queue<byte[]> mQueue = new LinkedBlockingDeque<byte[]>();
    private boolean isWriting = false;

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

        for (int i = 0; i < length; i++) {
            mQueue.add(Arrays.copyOfRange(outputValue, i * 20, (i + 1) * 20));
        }
        mQueue.add(Arrays.copyOfRange(outputValue, length * 20, str.length() + 1));
        mQueue.add(new byte[]{0x00});

        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(STORED_ADDRESS, ""));
        if(device != null && !isWriting) {
            isWriting = true;
            device.connectGatt(this, false, mGattCallback);
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

        mQueue.add(valuesToSend);
        mQueue.add(new byte[]{0x00});

        BluetoothDevice device = m_bt_adapter.getRemoteDevice(prefs.getString(STORED_ADDRESS, ""));
        if(device != null && !isWriting) {
            isWriting = true;
            device.connectGatt(this, false, mGattCallback);
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
                    prefs.edit().putString(STORED_ADDRESS, m_devices.get(m_names.getItem(which)).getAddress()).apply();
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
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
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
                    if(!m_devices.containsKey(device.getName())) {
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

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                if(gatt.getService(NOTIFYR_SERVICE)!= null){
                    writeQueue(gatt);
                } else{
                    Log.i(TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());
                }
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                Log.i(TAG, "Disconnected from GATT server.");
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(NOTIFYR_SERVICE)!= null) {
                Log.i(TAG, "onServicesDiscovered received: " + status);
                writeQueue(gatt);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG,characteristic.getUuid().toString());
            if(status == BluetoothGatt.GATT_SUCCESS) {
                writeQueue(gatt);
            } else{
                isWriting = false;
            }
        }


    };

    private void writeQueue(BluetoothGatt gatt) {
        if(gatt.getService(NOTIFYR_SERVICE) == null){
            isWriting = false;
            return;
        }

        if(mQueue.isEmpty()){
            isWriting = false;
            gatt.disconnect();
            gatt.close();
            return;
        }

        if(mQueue.peek()[0] != 0x00){
            gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_MSG).setValue(mQueue.remove());
            gatt.writeCharacteristic(gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_MSG));
        }else {
            gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_DONE).setValue(mQueue.remove());
            gatt.writeCharacteristic(gatt.getService(NOTIFYR_SERVICE).getCharacteristic(TX_DONE));
        }
    }


}
