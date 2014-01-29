package com.ztemt.test.auto.item;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ztemt.test.auto.R;
import com.ztemt.test.auto.util.PreferenceUtils;

public class RecoveryTest extends BaseTest {

    private static final String LOG_TAG = "RecoveryTest";

    public RecoveryTest(Context context) {
        super(context);
    }

    @Override
    public void onRun() {
        setSuccess();
        recovery();
        pause();
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.recovery_test);
    }

    private void recovery() {
        new PreferenceUtils(mContext).setReboot(true);

        Log.d(LOG_TAG, "Performing recovery system...");
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        mContext.sendBroadcast(intent);
    }
}
