package s175179.joachim.bluetoothletest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GattServerActivity extends Activity implements View.OnClickListener {

  private BluetoothManager m_BluetoothManager;
  private BluetoothGattServer m_BluetoothGattServer;
  private BluetoothLeAdvertiser m_BluetoothLeAdvertiser;
  private Set<BluetoothDevice> m_RegisteredDevices = new HashSet<>();

  private Button send;
  private TextView addressfield;
  private EditText sendtext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_gatt_server);


    m_BluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = m_BluetoothManager.getAdapter();
    // We can't continue without proper Bluetooth support
    if (checkBluetooth(bluetoothAdapter) != 2) {

      finish();
    }

    // Register for system Bluetooth events
    //IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    //registerReceiver(m_BluetoothReceiver, filter);
    if (!bluetoothAdapter.isEnabled()) {
      bluetoothAdapter.enable();
    } else {
      startAdvertising();
      startServer();
    }

    send = findViewById(R.id.m_button_serversend);
    sendtext = findViewById(R.id.m_edittext_servernumber);
    addressfield = findViewById(R.id.m_server_address);

    addressfield.setText(m_BluetoothManager.getAdapter().getAddress());

    send.setOnClickListener(this);
  }

  /**
   * Checks the level of bluetooth supported.
   * @param bt System {@link BluetoothAdapter}.
   * @return 0 if not supported, 1 if regular bluetooth is supported, 2 if all needed bluetooth is supported.
   */
  private int checkBluetooth(BluetoothAdapter bt) {
    if (bt == null) {
      return 0;
    }

    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      return 1;
    }

    return 2;
  }


  /**
   * Begin advertising over Bluetooth that this device is connectable
   * and supports the Current Time Service.
   */
  private void startAdvertising() {
    BluetoothAdapter bluetoothAdapter = m_BluetoothManager.getAdapter();
    m_BluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    if (m_BluetoothLeAdvertiser == null) {
      System.out.println("hvad fanden sker der ????hvad fanden sker der ????hvad fanden sker der ????");
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

    m_BluetoothLeAdvertiser
            .startAdvertising(settings, data, m_AdvertiseCallback);
  }

  private void startServer() {
    m_BluetoothGattServer = m_BluetoothManager.openGattServer(this, m_GattServerCallback);
    if (m_BluetoothGattServer == null) {
      return;
    }

    m_BluetoothGattServer.addService(BTLEPayload.createService());

  }

  private AdvertiseCallback m_AdvertiseCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      Log.i("tis", "LE Advertise Started.");
    }

    @Override
    public void onStartFailure(int errorCode) {
      Log.w("tis", "LE Advertise Failed: "+errorCode);
    }
  };

  private BluetoothGattServerCallback m_GattServerCallback = new BluetoothGattServerCallback() {

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        m_RegisteredDevices.remove(device);
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
      if (BTLEPayload.THIS_NUMBER.equals(characteristic.getUuid())) {
        m_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                sendtext.getText().toString().getBytes());
      } else {
        // Invalid characteristic
        m_BluetoothGattServer.sendResponse(device,
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
        m_BluetoothGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                returnValue);
      } else {
        //Log.w(TAG, "Unknown descriptor read request");
        m_BluetoothGattServer.sendResponse(device,
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
          m_RegisteredDevices.add(device);
        } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
          //Log.d(TAG, "Unsubscribe device from notifications: " + device);
          m_RegisteredDevices.remove(device);
        }

        if (responseNeeded) {
          m_BluetoothGattServer.sendResponse(device,
                  requestId,
                  BluetoothGatt.GATT_SUCCESS,
                  0,
                  null);
        }
      } else {
        //Log.w(TAG, "Unknown descriptor write request");
        if (responseNeeded) {
          m_BluetoothGattServer.sendResponse(device,
                  requestId,
                  BluetoothGatt.GATT_FAILURE,
                  0,
                  null);
        }
      }
    }
  };

  @Override
  public void onClick(View v) {
    if(v==send){
      notifyRegisteredDevices();
    }
  }

  private void notifyRegisteredDevices() {
    //if (m_RegisteredDevices.isEmpty()) {
    //  return;
    //}
    byte[] number = sendtext.getText().toString().getBytes();

    //Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
    BluetoothGattCharacteristic numberChar = m_BluetoothGattServer
            .getService(BTLEPayload.NUMBER_SERVICE)
            .getCharacteristic(BTLEPayload.THIS_NUMBER);
    numberChar.setValue(number);

    addressfield.setText(new String(m_BluetoothGattServer
            .getService(BTLEPayload.NUMBER_SERVICE)
            .getCharacteristic(BTLEPayload.THIS_NUMBER).getValue()));
    for (BluetoothDevice device : m_RegisteredDevices) {
      m_BluetoothGattServer.notifyCharacteristicChanged(device, numberChar, false);
    }
  }
}

