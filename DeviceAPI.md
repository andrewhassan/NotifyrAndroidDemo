Notifyr Device API
===============

There are currently 2 possible commands allowed for the Notifyr. They're both sent over the following BLE characteristics:

Service UUID : `F9266FD7-EF07-45D6-8EB6-BD74F13620F9`
*  TX input `E788D73B-E793-4D9E-A608-2F2BAFC59A00` : Write up to the maximum allowable BLE length (20 bytes) at a time
*  TX done `2f7f24f2-0301-11e4-93b1-b2227cce2b54` : Write anything to tell the BLE module string is complete

Messages are sent as strings of up to 250 bytes, and with 0 values disallowed (actual 0 value bytes are used as string terminators device-side). If we change over to regular Bluetooth, this limit may be removed.

## Commands ##
 
The 2 commands currently available are : 

* Send a new notification (string msg)
* Send the current time (byte hour, byte minute, byte second)

These messages are sent in the format of: 

`[[Command Code(1 byte)][Command parameters(a lot of bytes)]]`

Additional API changes, such as a message removal command, will be made at a later point in time.

## Send new notification ##

Command code: `0x01`

Parameters: `String msg`

Sends a message to show on the Notifyr device, please attempt to limit sending rate, since firmware is really shitty atm

## Send current Time ##

Command code: `0x02`

Parameters: `byte hour_in_24_hours_format, byte minute, byte second`

Sends the current time for the Notifyr to show, after initial update, the real-time clock on the Notifyr will keep the time itself, so this is just for initial setup

**IMPORTANT**: Values **must** start from 1, since 0s are not valid values on the Notifyr, but are valid times obviously.
