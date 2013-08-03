package com.ztemt.test.auto.item;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.ztemt.test.auto.R;
import com.ztemt.test.auto.util.DatabaseUtil;

public class SmsTest extends BaseTest {

    private static final String LOG_TAG = "SmsTest";
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private Context mContext;
    private EditText mPhoneNumberEdit;

    private TelephonyManager mTelephonyManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setSuccess();
            resume();
        }
    };

    public SmsTest(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void executeTest() {
        if (mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
            Log.e(LOG_TAG, "SIM not ready.");
            sleep(2000);
            setFailure();
        } else {
            registerReceiver();
            setTimeout(60000);
            sendMessage();
            pause();
            unregisterReceiver();
        }
    }

    @Override
    public String getTestTitle() {
        return mContext.getString(R.string.sms_test);
    }

    @Override
    public View createPreferenceView() {
        View view = super.createPreferenceView();
        mPhoneNumberEdit = addPreferenceEdit(view, R.string.my_phone_number_label, getMyPhoneNumber());
        return view;
    }

    @Override
    public void onPreferenceClick(View view) {
        super.onPreferenceClick(view);
        setMyPhoneNumber(mPhoneNumberEdit.getText().toString());
    }

    private void sendMessage() {
        String phoneNumber = getMyPhoneNumber();

        if (!TextUtils.isEmpty(phoneNumber)) {
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                    new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, "sms test!",
                    pendingIntent, null);
        }
    }

    private String getMyPhoneNumber() {
        return DatabaseUtil.getInstance().getValue("my_phone_number", "");
    }

    private void setMyPhoneNumber(String myPhoneNumber) {
        DatabaseUtil.getInstance().setValue("my_phone_number", myPhoneNumber);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }
}
