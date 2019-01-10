package s175179.joachim.bluetoothletest;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class BTLEPayload {
  /* Current Time Service UUID */
  public static UUID TIME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
  /* Mandatory Current Time Information Characteristic */
  public static UUID CURRENT_TIME    = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
  /* Optional Local Time Information Characteristic */
  public static UUID LOCAL_TIME_INFO = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");
  /* Mandatory Client Characteristic Config Descriptor */
  public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  // Adjustment Flags
  public static final byte ADJUST_NONE     = 0x0;
  public static final byte ADJUST_MANUAL   = 0x1;
  public static final byte ADJUST_EXTERNAL = 0x2;
  public static final byte ADJUST_TIMEZONE = 0x4;
  public static final byte ADJUST_DST      = 0x8;

  public static BluetoothGattService createService() {
    BluetoothGattService service = new BluetoothGattService(TIME_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY);

    // Current Time characteristic
    BluetoothGattCharacteristic currentTime = new BluetoothGattCharacteristic(CURRENT_TIME,
            //Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);
    BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
            //Read/write descriptor
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
    currentTime.addDescriptor(configDescriptor);

    // Local Time Information characteristic
    BluetoothGattCharacteristic localTime = new BluetoothGattCharacteristic(LOCAL_TIME_INFO,
            //Read-only characteristic
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);

    service.addCharacteristic(currentTime);
    service.addCharacteristic(localTime);

    return service;
  }
}
