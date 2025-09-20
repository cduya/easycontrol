package top.eiyooooo.easycontrol.app.client;

import android.content.ClipData;
import android.view.MotionEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.buffer.BufferStream;

public class ControlPacket {
  private final MyFunctionByteBuffer write;

  public ControlPacket(MyFunctionByteBuffer write) {
    this.write = write;
  }

  public byte[] readFrame(BufferStream bufferStream) throws IOException, InterruptedException {
    return bufferStream.readByteArray(bufferStream.readInt()).array();
  }

  // 发送触摸事件
  public void sendTouchEvent(int action, int p, float x, float y, int offsetTime) {
    if (x < 0 || x > 1 || y < 0 || y > 1) {
      // 超出范围则改为抬起事件
      if (x < 0) x = 0;
      if (x > 1) x = 1;
      if (y < 0) y = 0;
      if (y > 1) y = 1;
      action = MotionEvent.ACTION_UP;
    }
    ByteBuffer byteBuffer = ByteBuffer.allocate(15);
    // 触摸事件
    byteBuffer.put((byte) 1);
    // 触摸类型
    byteBuffer.put((byte) action);
    // pointerId
    byteBuffer.put((byte) p);
    // 坐标位置
    byteBuffer.putFloat(x);
    byteBuffer.putFloat(y);
    // 时间偏移
    byteBuffer.putInt(offsetTime);
    byteBuffer.flip();
    write.run(byteBuffer);
  }

  // 发送按键事件
  public void sendKeyEvent(int key, int meta, int displayIdToInject) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(13);
    // 输入事件
    byteBuffer.put((byte) 2);
    // 按键类型
    byteBuffer.putInt(key);
    byteBuffer.putInt(meta);
    byteBuffer.putInt(displayIdToInject);
    byteBuffer.flip();
    write.run(byteBuffer);
  }

  // 发送心跳包
  public void sendKeepAlive() {
    write.run(ByteBuffer.wrap(new byte[]{4}));
  }

  // 发送更新事件
  public void sendConfigChangedEvent(int mode) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 5);
    byteBuffer.putInt(mode);
    byteBuffer.flip();
    write.run(byteBuffer);
  }

  // 发送旋转请求事件
  public void sendRotateEvent() {
    sendRotateEvent(-1);
  }

  public void sendRotateEvent(int rotation) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    byteBuffer.put((byte) 6);
    byteBuffer.putInt(rotation);
    byteBuffer.flip();
    write.run(byteBuffer);
  }

  // 发送背光控制事件
  public void sendLightEvent(int mode) {
    write.run(ByteBuffer.wrap(new byte[]{7, (byte) mode}));
  }

  // 发送电源键事件
  public void sendPowerEvent() {
    write.run(ByteBuffer.wrap(new byte[]{8}));
  }

  public interface MyFunctionByteBuffer {
    void run(ByteBuffer byteBuffer);
  }

}
