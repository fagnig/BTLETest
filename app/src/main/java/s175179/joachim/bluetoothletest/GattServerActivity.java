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
import android.os.Handler;
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

  private Button send;
  private TextView addressfield;
  private EditText sendtext;

  private BTLEServerHandler serverHandler = new BTLEServerHandler();

  private Handler handler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_gatt_server);

    serverHandler.init(this, (BluetoothManager) getSystemService(BLUETOOTH_SERVICE), getPackageManager());

    send = findViewById(R.id.m_button_serversend);
    sendtext = findViewById(R.id.m_edittext_servernumber);
    addressfield = findViewById(R.id.m_server_address);

    //addressfield.setText(m_BluetoothManager.getAdapter().getAddress());

    send.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    if(v==send){
      serverHandler.setOutstream(sendtext.getText().toString().getBytes());
      serverHandler.notifyRegisteredDevices();
    }
  }
}

