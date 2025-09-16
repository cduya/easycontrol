package top.eiyooooo.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ScrollView;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ItemReconnectBinding;
import top.eiyooooo.easycontrol.app.databinding.ModuleDialogBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;

public class ConnectHelper {
    public static boolean status;

    private final Context context;

    public ConnectHelper(Context c) {
        context = c;
    }

    public static void show(ConnectHelper connectHelper, String uuid, int mode) {
        if (connectHelper != null) {
            AppData.uiHandler.post(() -> connectHelper.showDialog(uuid, mode));
        }
    }

    public void showDialog(String uuid, int mode) {
        ItemReconnectBinding reconnectView = ItemReconnectBinding.inflate(LayoutInflater.from(context));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
        dialogView.addView(reconnectView.getRoot());
        dialogView.setPadding(0, 0, 0, 0);
        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        int reconnectTime;
        try {
            reconnectTime = Integer.parseInt(AppData.setting.getCountdownTime());
        } catch (NumberFormatException ignored) {
            reconnectTime = 0;
        }
        if (reconnectTime == 0 || !status) {
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                DeviceListAdapter.startByUUID(uuid, mode);
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> dialog.cancel());
        } else {
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, reconnectTime, false);
            AppData.uiHandler.post(countdownRunnable);
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                DeviceListAdapter.startByUUID(uuid, mode);
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                dialog.cancel();
            });
        }
    }

    public static final Map<String, Device> needStartDefaultUSB = new ConcurrentHashMap<>();
    public static boolean showingUSBDialog;
    public final Runnable showStartDefaultUSB = new Runnable() {
        @Override
        public void run() {
            if (status && !needStartDefaultUSB.isEmpty()) {
                if (!AppData.setting.getShowConnectUSB()) {
                    for (Map.Entry<String, Device> entry : needStartDefaultUSB.entrySet()) {
                        DeviceListAdapter.startDevice(entry.getValue(), AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
                        needStartDefaultUSB.remove(entry.getKey());
                    }
                    return;
                }
                if (!showingUSBDialog) {
                    showUSBDialog();
                }
            }
        }
    };

    public void showUSBDialog() {
        showingUSBDialog = true;
        ItemReconnectBinding USBView = ItemReconnectBinding.inflate(LayoutInflater.from(context));
        USBView.text.setText(AppData.main.getString(R.string.tip_default_usb));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
        dialogView.addView(USBView.getRoot());
        dialogView.setPadding(0, 0, 0, 0);
        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setOnCancelListener((dialog1) -> {
            needStartDefaultUSB.clear();
            showingUSBDialog = false;
        });
        dialog.show();

        int waitTime;
        try {
            waitTime = Integer.parseInt(AppData.setting.getCountdownTime());
        } catch (NumberFormatException ignored) {
            waitTime = 0;
        }
        if (waitTime == 0) {
            USBView.buttonConfirm.setOnClickListener(v -> {
                for (Map.Entry<String, Device> entry : needStartDefaultUSB.entrySet()) {
                    DeviceListAdapter.startDevice(entry.getValue(), AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
                    needStartDefaultUSB.remove(entry.getKey());
                }
                dialog.cancel();
            });
            USBView.buttonCancel.setOnClickListener(v -> dialog.cancel());
        } else {
            Runnable countdownRunnable = getCountdownRunnable(USBView, waitTime, true);
            AppData.uiHandler.post(countdownRunnable);
            USBView.buttonConfirm.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                for (Map.Entry<String, Device> entry : needStartDefaultUSB.entrySet()) {
                    DeviceListAdapter.startDevice(entry.getValue(), AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
                    needStartDefaultUSB.remove(entry.getKey());
                }
                dialog.cancel();
            });
            USBView.buttonCancel.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                dialog.cancel();
            });
        }
    }

    private static Runnable getCountdownRunnable(ItemReconnectBinding view, int reconnectTime, boolean USB) {
        final int[] secondsLeft = {reconnectTime};
        return new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (USB && needStartDefaultUSB.isEmpty()) {
                    view.buttonCancel.performClick();
                    return;
                }
                if (!status) {
                    view.buttonConfirm.setText(AppData.main.getString(R.string.confirm));
                    return;
                }
                if (secondsLeft[0] > 0) {
                    view.buttonConfirm.setText(AppData.main.getString(R.string.confirm) + " (" + secondsLeft[0] + "s)");
                    secondsLeft[0]--;
                    AppData.uiHandler.postDelayed(this, 1000);
                } else {
                    view.buttonConfirm.performClick();
                }
            }
        };
    }
}