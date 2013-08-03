package com.ztemt.test.auto.item;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.ztemt.test.auto.R;

public class AirplaneModeTest extends BaseTest {

    private Context mContext;

    private TelephonyManager mTelephonyManager;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            int state = serviceState.getState();

            if (state == ServiceState.STATE_POWER_OFF) {
                setSuccess();
                resume();
            }
        }
    };

    public AirplaneModeTest(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void executeTest() {
        if (isAirplaneModeOn()) {
            setAirplaneModeOn(false);
        }

        sleep(2000);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        setTimeout(10000);
        setAirplaneModeOn(true);

        // Pause the thread, resume after airplane mode turn on
        pause();

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        setAirplaneModeOn(false);
    }

    @Override
    public String getTestTitle() {
        return mContext.getString(R.string.airplane_mode_test);
    }

    private void setAirplaneModeOn(boolean enabling) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON,
                enabling ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcast(intent);
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }
}
