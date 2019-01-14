package s175179.joachim.bluetoothletest;

import android.Manifest;
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
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class GattClientActivity2 extends AppCompatActivity implements View.OnClickListener {

  TextView textView,resulttext;
  Button scan;

  BluetoothAdapter bluetoothAdapter;
  BluetoothManager bluetoothManager;

  BluetoothDevice bluetoothDevice;
  BluetoothGatt gatt;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_gatt_client);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

    textView = findViewById(R.id.m_text_client);
    resulttext = findViewById(R.id.m_client_resultlist);
    scan = findViewById(R.id.m_button_clientsearch);

    scan.setOnClickListener(this);

    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 456);

    bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      if (!bluetoothAdapter.isEnabled()) {
        bluetoothAdapter.enable();
      }
    }
    //autosubscribe to service

    startScan();
  }

  private void startScan(){
    ScanCallback scanCallback = new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
        bluetoothDevice = result.getDevice();
        resulttext.setText(result.getDevice().getName());
      }

      @Override
      public void onBatchScanResults(List<ScanResult> results){
        for(ScanResult result : results) {
          bluetoothDevice = result.getDevice();
          resulttext.setText(result.getDevice().getName());
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

    bluetoothAdapter.getBluetoothLeScanner().startScan(filter, builder.build(), scanCallback);
    //bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
  }

  public void processData(byte[] data, boolean shouldclose){
    String tmp = new String(data);
    textView.setText(tmp);

    if(shouldclose)
      close();
  }

  public void close() {
    if (gatt == null) {
      return;
    }
    gatt.disconnect();
    gatt.close();
    gatt = null;
  }

  public void lugt(){
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
        //if(characteristic.getDescriptor(BTLEPayload.CLIENT_CONFIG).toString().equals(bluetoothAdapter.getAddress())) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            processData(characteristic.getValue(), false);
          }
        });

        //}
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
          processData(characteristic.getValue(), true);
      }
    };

    if(bluetoothDevice != null) {
      gatt = bluetoothDevice.connectGatt(this, false, gattCallback);
    }
  }

  @Override
  public void onClick(View v) {
    if(v==scan){
      //startScan();

      if(gatt != null){

        gatt.connect();

        if(gatt.getServices().size() == 0)
          gatt.discoverServices();

        BluetoothGattService test = gatt.getService(BTLEPayload.NUMBER_SERVICE);
        BluetoothGattCharacteristic characteristic;



        if(test!=null) {
          characteristic = test.getCharacteristic(BTLEPayload.THIS_NUMBER);

          if (characteristic != null)
            gatt.readCharacteristic(characteristic);
        }
      } else {
        lugt();
      }
    }
  }

}
