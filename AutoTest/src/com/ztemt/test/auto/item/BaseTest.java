package com.ztemt.test.auto.item;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ztemt.test.auto.R;
import com.ztemt.test.auto.util.PreferenceUtils;

public abstract class BaseTest implements Runnable {

    private static final String LOG_TAG = "AutoTest";
    private static final String ACTION_TIMEOUT = "com.ztemt.test.auto.action.TIMEOUT";
    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private static final String TIMES   = "times";
    private static final String ENABLED = "enabled";
    private static final String ORDINAL = "ordinal";

    private static final int MSG_START = Integer.MIN_VALUE;
    private static final int MSG_STOP  = Integer.MAX_VALUE;

    private ScheduledThreadPoolExecutor mTimerTask;
    private AlarmManager mAlarmManager;
    private PendingIntent mPendingIntent;
    private Thread mThread;

    private TestListener mListener;
    private PreferenceUtils mPrefUtils;
    private String mSuccess;
    private String mFailure;
    private String mTimes;
    private String mEnabled;
    private String mOrdinal;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_TIMEOUT)) {
                // Play a alert sound and resume thread
                playAlertRingtone();
                setFailure();
                resume();
            }
        }
    };

    private Runnable mStartRunnable = new Runnable() {

        @Override
        public void run() {
            mThread = new Thread(BaseTest.this);
            mThread.start();
        }
    };

    private Runnable mStopRunnable = new Runnable() {

        @Override
        public void run() {
            if (mListener != null) {
                mListener.onTestStop();
            }
            mPrefUtils.setReboot(false);
        }
    };

    protected Context mContext;

    protected abstract void onRun();

    public abstract String getTitle();

    public BaseTest(Context context) {
        mContext = context;
        mPrefUtils = new PreferenceUtils(context);

        mSuccess = getPrefixName(SUCCESS);
        mFailure = getPrefixName(FAILURE);
        mTimes   = getPrefixName(TIMES);
        mEnabled = getPrefixName(ENABLED);
        mOrdinal = getPrefixName(ORDINAL);

        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void startTest() {
        //if (!mPrefUtils.isReboot()) {
        //    setSuccessTimes(0);
        //    setFailureTimes(0);
        //}
        setTestTimer(MSG_START);
    }

    public void setTestTimer(final int msg, int delay) {
        cancelTimerTask();

        if (msg == MSG_START) {
            mTimerTask = new ScheduledThreadPoolExecutor(10);
            mTimerTask.schedule(mStartRunnable, delay, TimeUnit.MILLISECONDS);
        } else if (msg == MSG_STOP) {
            mTimerTask = new ScheduledThreadPoolExecutor(10);
            mTimerTask.schedule(mStopRunnable, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void setTestTimer(final int msg) {
        setTestTimer(msg, 0);
    }

    public void cancelTimerTask() {
        if (mTimerTask != null) {
            mTimerTask.remove(mStartRunnable);
            mTimerTask.remove(mStopRunnable);
            mTimerTask.shutdownNow();
            mTimerTask = null;
        }
    }

    public void setTimeout(long milliseconds) {
        Intent intent = new Intent(ACTION_TIMEOUT);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.set(AlarmManager.RTC, System.currentTimeMillis()
                + milliseconds, mPendingIntent);
    }

    @Override
    public void run() {
        synchronized (this) {
            if (isEnabled() && getTestTimes() < getTotalTimes()) {
                Log.d(LOG_TAG, String.format("%s[%d/%d]", getClass()
                    .getSimpleName(), getTestTimes() + 1, getTotalTimes()));
                IntentFilter filter = new IntentFilter(ACTION_TIMEOUT);
                mContext.registerReceiver(mReceiver, filter);
                onRun();
                mAlarmManager.cancel(mPendingIntent);
                mContext.unregisterReceiver(mReceiver);
                if (mListener != null) {
                    mListener.onTestStart();
                }
                setTestTimer(MSG_START);
            } else {
                setTestTimer(MSG_STOP);
            }
        }
    }

    public int getTestTimes() {
        return getSuccessTimes() + getFailureTimes();
    }

    public void setSuccess() {
        setSuccessTimes(getSuccessTimes() + 1);
    }

    public void setFailure() {
        setFailureTimes(getFailureTimes() + 1);
    }

    public int getSuccessTimes() {
        return mPrefUtils.getInt(mSuccess, 0);
    }

    public void setSuccessTimes(int value) {
        mPrefUtils.putInt(mSuccess, value);
    }

    public int getFailureTimes() {
        return mPrefUtils.getInt(mFailure, 0);
    }

    public void setFailureTimes(int value) {
        mPrefUtils.putInt(mFailure, value);
    }

    public int getTotalTimes() {
        return mPrefUtils.getInt(mTimes, 1);
    }

    public void setTotalTimes(int value) {
        mPrefUtils.putInt(mTimes, value);
    }

    public boolean isEnabled() {
        return mPrefUtils.getBoolean(mEnabled, true);
    }

    public void setEnabled(boolean enabled) {
        mPrefUtils.putBoolean(mEnabled, enabled);
    }

    public int getOrdinal() {
        return mPrefUtils.getInt(mOrdinal, 1);
    }

    public void setOrdinal(int ordinal) {
        mPrefUtils.putInt(mOrdinal, ordinal);
    }

    public void setExtras(Bundle bundle) {
        if (bundle == null) return;

        if (bundle.containsKey(mTimes)) {
            setTotalTimes(bundle.getInt(mTimes, 10));
        } else if (bundle.containsKey(TIMES)) {
            setTotalTimes(bundle.getInt(TIMES, 10));
        }
        if (bundle.containsKey(mEnabled)) {
            setEnabled(bundle.getBoolean(mEnabled, true));
        } else if (bundle.containsKey(ENABLED)) {
            setEnabled(bundle.getBoolean(ENABLED, true));
        }
        if (bundle.containsKey(mOrdinal)) {
            setOrdinal(bundle.getInt(mOrdinal));
        }
    }

    public void setTestListener(TestListener listener) {
        mListener = listener;
    }

    public boolean isRunning() {
        return mThread != null && mThread.isAlive();
    }

    public void pause() {
        synchronized (mThread) {
            try {
                mThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void resume() {
        synchronized (mThread) {
            mThread.notify();
        }
    }

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public View createPreferenceView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pref_edit, null);
        EditText testTime = (EditText) view.findViewById(R.id.test_times);
        testTime.setText(String.valueOf(getTotalTimes()));
        return view;
    }

    public EditText addPreferenceEdit(View view, int labelResId, String defValue) {
        TableRow tr = (TableRow) LayoutInflater.from(mContext).inflate(R.layout.pref_item, null);
        TextView tv = (TextView) tr.findViewById(R.id.item_label);
        tv.setText(labelResId);
        EditText et = (EditText) tr.findViewById(R.id.item_value);
        et.setText(defValue);
        TableLayout layout = (TableLayout) view.findViewById(R.id.table_layout);
        layout.addView(tr);
        return et;
    }

    public void onPreferenceClick(View view) {
        EditText testTime = (EditText) view.findViewById(R.id.test_times);
        try {
            int value = Integer.parseInt(testTime.getText().toString());
            setTotalTimes(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void playAlertRingtone() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
        ringtone.play();

        // Stop after 5 seconds
        new Thread() {
            public void run() {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    ringtone.stop();
                }
            }
        }.start();
    }

    private String getPrefixName(String prefix) {
        return String.format("%s_%s", getClass().getSimpleName(), prefix);
    }
}
