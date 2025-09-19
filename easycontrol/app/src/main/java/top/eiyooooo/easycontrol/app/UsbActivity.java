package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.MyBroadcastReceiver;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

public class UsbActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SharedPreferences sharedPreferences = this.getSharedPreferences("setting", Context.MODE_PRIVATE);
    if (sharedPreferences.getBoolean("isActive", false)) {
      if (AppData.Activity == null) startActivity(new Intent(this, Activity .class));
      else {
        Intent intent = new Intent();
        intent.setAction(MyBroadcastReceiver.ACTION_UPDATE_USB);
        sendBroadcast(intent);
      }
    }
    finish();
  }
}
