package s175179.joachim.bluetoothletest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

  Button client, server;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    client = findViewById(R.id.m_button_client);
    server = findViewById(R.id.m_button_server);

    client.setOnClickListener(this);
    server.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    if(v==server){
      startActivity(new Intent(this, GattServerActivity.class));
    } else if(v==client){
      startActivity(new Intent(this, GattClientActivity.class));
    }
  }
}
