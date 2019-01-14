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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class GattClientActivity extends AppCompatActivity implements View.OnClickListener {

  TextView textView,resulttext;
  Button scan;

  BTLEClientHandler clientHandler = new BTLEClientHandler();
  Handler handler;
  int tis = 250;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_gatt_client);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 456);

    BluetoothManager tmp = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

    clientHandler.init(this, tmp);

    textView = findViewById(R.id.m_text_client);
    resulttext = findViewById(R.id.m_client_resultlist);
    scan = findViewById(R.id.m_button_clientsearch);

    handler = new Handler();
    startRepeatingTask();

    scan.setOnClickListener(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    clientHandler.close();
    stopRepeatingTask();
  }

  @Override
  public void onClick(View v) {
    clientHandler.scan();
  }

  Runnable mStatusChecker = new Runnable() {
    @Override
    public void run() {
      try {
        if(clientHandler.getInstream() != null) {
          textView.setText(new String(clientHandler.getInstream()));
          resulttext.setText(clientHandler.getDeviceName());
        }
      } finally {
        // 100% guarantee that this always happens, even if
        // your update method throws an exception
        handler.postDelayed(mStatusChecker, tis);
      }
    }
  };

  void startRepeatingTask() {
    mStatusChecker.run();
  }

  void stopRepeatingTask() {
    handler.removeCallbacks(mStatusChecker);
  }
}
