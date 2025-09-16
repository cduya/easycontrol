package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import top.eiyooooo.easycontrol.app.adb.AdbKeyPair;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.databinding.ActivityAdbKeyBinding;

public class AdbKeyActivity extends Activity {
  private ActivityAdbKeyBinding activityAdbKeyBinding;
  private final File privateKey = new File(AppData.main.getApplicationContext().getFilesDir(), "private.key");
  private final File publicKey = new File(AppData.main.getApplicationContext().getFilesDir(), "public.key");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    activityAdbKeyBinding = ActivityAdbKeyBinding.inflate(this.getLayoutInflater());
    setContentView(activityAdbKeyBinding.getRoot());
    readKey();
    activityAdbKeyBinding.backButton.setOnClickListener(v -> finish());
    activityAdbKeyBinding.ok.setOnClickListener(v -> writeKey());
  }

  // 读取旧的密钥公钥文件
  private void readKey() {
    try {
      byte[] publicKeyBytes = new byte[(int) publicKey.length()];
      byte[] privateKeyBytes = new byte[(int) privateKey.length()];

      try (FileInputStream stream = new FileInputStream(publicKey)) {
        stream.read(publicKeyBytes);
        activityAdbKeyBinding.adbKeyPub.setText(new String(publicKeyBytes));
      }
      try (FileInputStream stream = new FileInputStream(privateKey)) {
        stream.read(privateKeyBytes);
        activityAdbKeyBinding.adbKeyPri.setText(new String(privateKeyBytes));
      }
    } catch (IOException ignored) {
    }
  }

  // 写入新的密钥公钥文件
  private void writeKey() {
    try {
      try (FileWriter publicKeyWriter = new FileWriter(publicKey)) {
        publicKeyWriter.write(String.valueOf(activityAdbKeyBinding.adbKeyPub.getText()));
        publicKeyWriter.flush();
      }
      try (FileWriter privateKeyWriter = new FileWriter(privateKey)) {
        privateKeyWriter.write(String.valueOf(activityAdbKeyBinding.adbKeyPri.getText()));
        privateKeyWriter.flush();
      }
      AppData.keyPair = AdbKeyPair.read(privateKey, publicKey);
      Toast.makeText(this, getString(R.string.adb_key_button_code), Toast.LENGTH_SHORT).show();
    } catch (Exception ignored) {
    }
  }
}