package com.andrewhassan.notifyrdemoapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity {

    private BluetoothAdapter m_bt_adapter;

    public void sendData(View view) {
        EditText text_area = (EditText)this.findViewById(R.id.editText);
        String str = text_area.getText().toString();
        int length = str.length()/20;


        // Clear text area
        text_area.setText("");

        //must split string up into 20 byte chunks
        for(int i=0; i<length; i++){
            Log.d("MainActivity", "sendData...going to send message string \"" + str.substring(i*20,(i+1)*20) + "\"");
        }

        Log.d("MainActivity", "sendData...going to send message string \"" + str.substring(length*20) + "\"");
        // Initialize BT connection
        // Send str using BT interface
    }

    public void sendTime(View v){
        //send time as a string, must be hours,minutes, and seconds. All values must be plus one, since we can have 0 values
        //(CANNOT HAVE!!!)
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR_OF_DAY);
        byte[] valuesToSend = new byte[]{(byte) hours+1, (byte) minutes+1, (byte)(seconds+1) };
        Log.d("MainActivity", "sendData...going to send date string \"" +valuesToSend[0]+valuesToSend[1]+valuesToSend[2] + "\"");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager bt_manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        m_bt_adapter = bt_manager.getAdapter();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
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
}
