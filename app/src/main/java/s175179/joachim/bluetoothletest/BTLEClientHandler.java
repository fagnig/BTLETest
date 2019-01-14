package s175179.joachim.bluetoothletest;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class BTLEClientHandler {
  private BluetoothAdapter m_bluetoothAdapter;
  private BluetoothManager m_bluetoothManager;

  private BluetoothDevice m_bluetoothDevice;
  private BluetoothGatt m_gatt;

  private Context context;

  private String deviceName;

  private byte[] instream;

  public int init(Context context, BluetoothManager btman) {
    m_bluetoothManager = btman;
    m_bluetoothAdapter = m_bluetoothManager.getAdapter();
    if (m_bluetoothAdapter == null || !m_bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      if (!m_bluetoothAdapter.isEnabled()) {
        m_bluetoothAdapter.enable();
      }
    }
    //autosubscribe to service

    this.context = context;

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      startScan();
    } else{
      startScanLegacy();
    }

    return 0;
  }

  @TargetApi(23)
  private void startScan(){
    ScanCallback scanCallback = new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
        m_bluetoothDevice = result.getDevice();
        deviceName = result.getDevice().getName();
      }

      @Override
      public void onBatchScanResults(List<ScanResult> results){
        for(ScanResult result : results) {
          m_bluetoothDevice = result.getDevice();
          deviceName = result.getDevice().getName();
        }
      }
    };
    //bluetoothAdapter.startLeScan(new UUID[] {BTLEPayload.NUMBER_SERVICE}, scanCallback);

    List<ScanFilter> filter = new ArrayList<ScanFilter>();
    ScanFilter.Builder tmp = new ScanFilter.Builder();
    tmp.setServiceUuid(new ParcelUuid(BTLEPayload.NUMBER_SERVICE));
    filter.add(tmp.build());

    final ScanSettings.Builder builder = new ScanSettings.Builder();
    builder.setCallbackType(CALLBACK_TYPE_ALL_MATCHES);

    builder.setScanMode(SCAN_MODE_LOW_LATENCY);

    m_bluetoothAdapter.getBluetoothLeScanner().startScan(filter, builder.build(), scanCallback);
    //bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
  }

  private void startScanLegacy(){
    ScanCallback scanCallback = new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
        m_bluetoothDevice = result.getDevice();
        deviceName = result.getDevice().getName();
      }

      @Override
      public void onBatchScanResults(List<ScanResult> results){
        for(ScanResult result : results) {
          m_bluetoothDevice = result.getDevice();
          deviceName = result.getDevice().getName();
        }
      }
    };
    //bluetoothAdapter.startLeScan(new UUID[] {BTLEPayload.NUMBER_SERVICE}, scanCallback);

    List<ScanFilter> filter = new ArrayList<ScanFilter>();
    ScanFilter.Builder tmp = new ScanFilter.Builder();
    tmp.setServiceUuid(new ParcelUuid(BTLEPayload.NUMBER_SERVICE));
    filter.add(tmp.build());

    final ScanSettings.Builder builder = new ScanSettings.Builder();

    builder.setScanMode(SCAN_MODE_LOW_LATENCY);

    m_bluetoothAdapter.getBluetoothLeScanner().startScan(filter, builder.build(), scanCallback);
    //bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
  }

  public void processData(byte[] data, boolean shouldclose){
    instream = data;

    //if(shouldclose)
      //close();
  }

  public void close() {
    if (m_gatt == null) {
      return;
    }
    m_gatt.disconnect();
    m_gatt.close();
    m_gatt = null;
  }

  public void grabServices(){
    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == STATE_CONNECTED){
          gatt.discoverServices();
        }
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status){
        if (status == BluetoothGatt.GATT_SUCCESS) {

          BluetoothGattCharacteristic characteristic =
                  gatt.getService(BTLEPayload.NUMBER_SERVICE)
                          .getCharacteristic(BTLEPayload.THIS_NUMBER);
          gatt.setCharacteristicNotification(characteristic, true);

          BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BTLEPayload.CLIENT_CONFIG);

          descriptor.setValue(
                  BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
          gatt.writeDescriptor(descriptor);


          for (BluetoothGattService gattService : gatt.getServices()) {
            System.out.println("Found service: " + gattService.getUuid());
            //Log.i(TAG, "Service UUID Found: " + gattService.getUuid().toString());
          }
        }

      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        processData(characteristic.getValue(), false);
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        processData(characteristic.getValue(), true);
      }
    };

    if(m_bluetoothDevice != null) {
      m_gatt = m_bluetoothDevice.connectGatt(context, false, gattCallback);
    }
  }

  public void scan(){
    if(m_gatt != null){

      m_gatt.connect();

      if(m_gatt.getServices().size() == 0)
        m_gatt.discoverServices();

      BluetoothGattService service = m_gatt.getService(BTLEPayload.NUMBER_SERVICE);
      BluetoothGattCharacteristic characteristic;

      if(service!=null) {
        characteristic = service.getCharacteristic(BTLEPayload.THIS_NUMBER);

        if (characteristic != null)
          m_gatt.readCharacteristic(characteristic);
      }
    } else {
      grabServices();
    }
  }

  public String getDeviceName() {
    return deviceName;
  }

  public byte[] getInstream() {
    return instream;
  }
}
