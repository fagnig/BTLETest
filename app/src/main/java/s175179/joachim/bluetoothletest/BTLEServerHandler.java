package s175179.joachim.bluetoothletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BTLEServerHandler {

  private BluetoothManager m_server_BluetoothManager;
  private BluetoothGattServer m_server_BluetoothGattServer;
  private BluetoothLeAdvertiser m_server_BluetoothLeAdvertiser;
  private Set<BluetoothDevice> m_server_RegisteredDevices = new HashSet<>();


  private byte[] outstream;


  /**
   * @param context current context
   * @param btman Find BTMan with : (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
   * @param pman Find pman with : getPackageManager()
   * @return 0 if initted correctly
   */
  public int init(Context context, BluetoothManager btman, PackageManager pman) {

    m_server_BluetoothManager = btman;

    BluetoothAdapter bluetoothAdapter = m_server_BluetoothManager.getAdapter();
    // We can't continue without proper Bluetooth support
    if (checkBluetooth(bluetoothAdapter, pman) != 2)
    {
      return -1;
    }

    // Register for system Bluetooth events
    //IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    //registerReceiver(m_BluetoothReceiver, filter);
    if (!bluetoothAdapter.isEnabled())

    {
      bluetoothAdapter.enable();
    } else

    {
      startAdvertising();
      startServer(context);
    }
    return 0;
  }

  private int checkBluetooth(BluetoothAdapter bt, PackageManager pman) {
    if (bt == null) {
      return 0;
    }

    if (!pman.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      return 1;
    }

    return 2;
  }

  private void startAdvertising() {
    BluetoothAdapter bluetoothAdapter = m_server_BluetoothManager.getAdapter();
    m_server_BluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    if (m_server_BluetoothLeAdvertiser == null) {
      return;
    }

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build();

    AdvertiseData data = new AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(new ParcelUuid(BTLEPayload.NUMBER_SERVICE))
            .build();

    m_server_BluetoothLeAdvertiser
            .startAdvertising(settings, data, m_AdvertiseCallback);
  }


  private void startServer(Context context) {
    m_server_BluetoothGattServer = m_server_BluetoothManager.openGattServer(context, m_GattServerCallback);
    if (m_server_BluetoothGattServer == null) {
      return;
    }

    m_server_BluetoothGattServer.addService(BTLEPayload.createService());

  }

  private AdvertiseCallback m_AdvertiseCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      Log.i("INS", "LE Advertise Started.");
    }

    @Override
    public void onStartFailure(int errorCode) {
      Log.w("INS", "LE Advertise Failed: "+errorCode);
    }
  };

  private BluetoothGattServerCallback m_GattServerCallback = new BluetoothGattServerCallback() {

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        m_server_RegisteredDevices.remove(device);
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
      if (BTLEPayload.THIS_NUMBER.equals(characteristic.getUuid())) {
        m_server_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                outstream);
      } else {
        // Invalid characteristic
        m_server_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                null);
      }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                        BluetoothGattDescriptor descriptor) {
      if (BTLEPayload.CLIENT_CONFIG.equals(descriptor.getUuid())) {
        //Log.d(TAG, "Config descriptor read");
        byte[] returnValue;
        returnValue = device.getAddress().getBytes();
        m_server_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                returnValue);
      } else {
        //Log.w(TAG, "Unknown descriptor read request");
        m_server_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                null);
      }
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded,
                                         int offset, byte[] value) {
      if (BTLEPayload.CLIENT_CONFIG.equals(descriptor.getUuid())) {
        if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
          //Log.d(TAG, "Subscribe device to notifications: " + device);
          m_server_RegisteredDevices.add(device);
        } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
          //Log.d(TAG, "Unsubscribe device from notifications: " + device);
          m_server_RegisteredDevices.remove(device);
        }

        if (responseNeeded) {
          m_server_BluetoothGattServer.sendResponse(device,
                  requestId,
                  BluetoothGatt.GATT_SUCCESS,
                  0,
                  null);
        }
      } else {
        //Log.w(TAG, "Unknown descriptor write request");
        if (responseNeeded) {
          m_server_BluetoothGattServer.sendResponse(device,
                  requestId,
                  BluetoothGatt.GATT_FAILURE,
                  0,
                  null);
        }
      }
    }
  };

    public byte[] getOutstream() { return outstream; }
    public void setOutstream(byte[] outstream) { this.outstream = outstream; }


  public void notifyRegisteredDevices() {
    //if (m_RegisteredDevices.isEmpty()) {
    //  return;
    //}

    //Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
    BluetoothGattCharacteristic numberChar = m_server_BluetoothGattServer
            .getService(BTLEPayload.NUMBER_SERVICE)
            .getCharacteristic(BTLEPayload.THIS_NUMBER);
    numberChar.setValue(outstream);



    for (BluetoothDevice device : m_server_RegisteredDevices) {
      m_server_BluetoothGattServer.notifyCharacteristicChanged(device, numberChar, false);
    }
  }
}