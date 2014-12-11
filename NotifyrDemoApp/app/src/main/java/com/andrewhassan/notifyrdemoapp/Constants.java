package com.andrewhassan.notifyrdemoapp;

import java.util.UUID;

/**
 * Created by Applepie on 7/18/2014.
 */
public class Constants {

    public static final UUID NOTIFYR_SERVICE = UUID.fromString("F99F6A53-3A17-403C-B137-372E3330BC72");
    public static final UUID TX_MSG = UUID.fromString("287304EE-7141-4CF3-9F08-EE6FC7294F92");
    public static final UUID RX_MSG = UUID.fromString("4585C102-7784-40B4-88E1-3CB5C4FD37A3");

    public static final String NOTIFYR_BLE_SERVICE = "com.andrewhassan.notifyrdemoapp.bleservice";
    public static final String NOTIFYR_BLE_SERVICE_COMMAND = "notifyr_command";
    public static final String NOTIFYR_START_BLE_SERVICE = "start_notifyr_ble_service";
    public static final String NOTIFYR_STOP_BLE_SERVICE = "stop_notifyr_ble_service";

    public static final String NOTIFYR_NAME = "Notifyr";
    public static final String STORED_ADDRESS = "STORED_ADDRESS";
    public static final String TAG = "NOTIFYR_APP";
    public static final String FILTERED_APPS = "FILTERED_APPS";
    public static final String NOTIFYR_NOTIFICATION = "NOTIFYR_NOTIFICATION";
    public static final String NOTIFYR_NOTIFICATION_MSG = "NOTIFYR_NOTIFICATION_MSG";
    public static final String NOTIFYR_NOTIFICATION_MSG_LENGTH = "NOTIFYR_NOTIFICATION_MSG_LENGTH";

    public static final long SCAN_PERIOD = 3000;
}
