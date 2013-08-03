package com.ztemt.test.auto.item;

import java.lang.reflect.Method;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.android.internal.telephony.ITelephony;
import com.ztemt.test.auto.R;
import com.ztemt.test.auto.util.DatabaseUtil;

public class CallTest extends BaseTest {

    private static final String LOG_TAG = "CallTest";

    private Context mContext;
    private EditText mOutgoingEdit;

    private TelephonyManager mTelephonyManager;
    private ITelephony mTelephony;

    /*private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(LOG_TAG, "IDLE");
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.d(LOG_TAG, "OFFHOOK");
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                Log.d(LOG_TAG, "RINGING incomingNumber : " + incomingNumber);
                break;
            }
        }
    };*/

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                setSuccess();
                resume();
            }
        }
    };

    public CallTest(Context context) {
        super(context);
        mContext = context;
        mTelephony = getTelephony();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void executeTest() {
        if (mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
            sleep(2000);
            setFailure();
        } else {
            registerReceiver();
            setTimeout(30000);
            dial();
            pause();
            sleep(10000);
            endCall();
            sleep(2000);
            unregisterReceiver();
            backToTest();
        }
    }

    @Override
    public String getTestTitle() {
        return mContext.getString(R.string.call_test);
    }

    @Override
    public View createPreferenceView() {
        View view = super.createPreferenceView();
        mOutgoingEdit = addPreferenceEdit(view, R.string.outgoing_number_label, getOutgoingNumber());
        return view;
    }

    @Override
    public void onPreferenceClick(View view) {
        super.onPreferenceClick(view);
        setOutgoingNumber(mOutgoingEdit.getText().toString());
    }

    private ITelephony getTelephony() {
        IBinder binder = null;
        try {
            Class<?> c = Class.forName("android.os.ServiceManager");
            Method m = c.getMethod("getService", String.class);
            binder = (IBinder) m.invoke(c, Context.TELEPHONY_SERVICE);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
        return ITelephony.Stub.asInterface(binder);
    }

    private void dial() {
        String phoneNumber = getOutgoingNumber();
        String operator = mTelephonyManager.getSimOperator();
        if (TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(operator)) {
            if (operator.equals("46000") || operator.equals("46002")) {
                // China Mobile
                phoneNumber = "10086";
            } else if (operator.equals("46001")) {
                // China Unicom
                phoneNumber = "10010";
            } else if (operator.equals("46003")) {
                // China Telecom
                phoneNumber = "10000";
            }
        }

        Intent call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        mContext.startActivity(call);
    }

    private void endCall() {
        try {
            mTelephony.endCall();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String getOutgoingNumber() {
        return DatabaseUtil.getInstance().getValue("outgoing_number", "");
    }

    private void setOutgoingNumber(String outgoingNumber) {
        DatabaseUtil.getInstance().setValue("outgoing_number", outgoingNumber);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void backToTest() {
        /*Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(mContext.getPackageName(), AutoTestActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);*/
        Instrumentation inst = new Instrumentation();
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    }
}
