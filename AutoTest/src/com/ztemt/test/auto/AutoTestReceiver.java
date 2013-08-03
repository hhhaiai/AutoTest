package com.ztemt.test.auto;

import com.ztemt.test.auto.util.DatabaseUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoTestReceiver extends BroadcastReceiver {

    private static final String TAG = "AutoTestReceiver";

    private void launchUI(Context context) {
        Intent intent = new Intent(context, AutoTestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: action = " + action);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            boolean reboot = DatabaseUtil.getInstance().getBoolValue("is_reboot");

            if (reboot) {
                launchUI(context);
            }
        }
    }
}
