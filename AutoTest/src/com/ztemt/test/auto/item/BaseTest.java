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
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ztemt.test.auto.R;
import com.ztemt.test.auto.util.DatabaseUtil;

public abstract class BaseTest implements Runnable {

    private static final String TIMEOUT_ACTION = "android.intent.action.AUTOTEST_TIMEOUT";
    private static final String SUCCESS_TIMES = "_success_times";
    private static final String FAILURE_TIMES = "_failure_times";
    private static final String TOTAL_TIMES = "_total_times";
    private static final String ENABLED = "_enabled";

    private static final int AUTOTEST_START = Integer.MIN_VALUE;
    private static final int AUTOTEST_END   = Integer.MAX_VALUE;

    private ScheduledThreadPoolExecutor mTimerTask;
    private AlarmManager mAlarmManager;
    private PendingIntent mPendingIntent;

    private Context mContext;
    private Thread mThread;
    private int mIndex;

    private AutoTestListener mAutoTestListener;
    private DatabaseUtil mDatabaseUtil;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
            case AUTOTEST_START:
                mThread = new ExecuteThread();
                mThread.start();
                break;
            case AUTOTEST_END:
                if (mAutoTestListener != null) {
                    mAutoTestListener.onTestEnd();
                }
                setReboot(false);
                break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TIMEOUT_ACTION)) {
                // Play a alert sound and resume thread
                playAlertRingtone();
                setFailure();
                resume();
            }
        }
    };

    private class ExecuteThread extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                if (getTestTimes() >= getTotalTimes()) {
                    setTestTimer(AUTOTEST_END);
                } else {
                    IntentFilter filter = new IntentFilter(TIMEOUT_ACTION);
                    mContext.registerReceiver(mReceiver, filter);

                    executeTest();

                    mAlarmManager.cancel(mPendingIntent);
                    try {
                        mContext.unregisterReceiver(mReceiver);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                    if (mAutoTestListener != null) {
                        mAutoTestListener.onTestStart();
                    }
                    if (getTestTimes() >= getTotalTimes()) {
                        setTestTimer(AUTOTEST_END);
                    } else {
                        setTestTimer(AUTOTEST_START);
                    }
                }
            }
        }
    }

    public BaseTest(Context context) {
        mContext = context;

        mDatabaseUtil = DatabaseUtil.getInstance();
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void startTest() {
        if (!isReboot()) {
            setSuccessTimes(0);
            setFailureTimes(0);
        }
        setTestTimer(AUTOTEST_START);
    }

    public void setTestTimer(final int stepIndex, int delay) {
        cancelTimerTask();

        mIndex = stepIndex;

        mTimerTask = new ScheduledThreadPoolExecutor(10);
        mTimerTask.schedule(this, delay, TimeUnit.MILLISECONDS);
    }

    public void setTestTimer(final int stepIndex) {
        setTestTimer(stepIndex, 0);
    }

    @Override
    public void run() {
        Message message = mHandler.obtainMessage();
        message.arg1 = mIndex;
        mHandler.sendMessage(message);
    }

    public void cancelTimerTask() {
        if (mTimerTask != null) {
            mTimerTask.remove(this);
            mTimerTask.shutdownNow();
            mTimerTask = null;
        }
    }

    public void setTimeout(long milliseconds) {
        Intent intent = new Intent(TIMEOUT_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.set(AlarmManager.RTC, System.currentTimeMillis()
                + milliseconds, mPendingIntent);
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
        return mDatabaseUtil.getIntValue(getClass().getSimpleName() + SUCCESS_TIMES, 0);
    }

    public void setSuccessTimes(int value) {
        mDatabaseUtil.setIntValue(getClass().getSimpleName() + SUCCESS_TIMES, value);
    }

    public int getFailureTimes() {
        return mDatabaseUtil.getIntValue(getClass().getSimpleName() + FAILURE_TIMES, 0);
    }

    public void setFailureTimes(int value) {
        mDatabaseUtil.setIntValue(getClass().getSimpleName() + FAILURE_TIMES, value);
    }

    public int getTotalTimes() {
        return mDatabaseUtil.getIntValue(getClass().getSimpleName() + TOTAL_TIMES, 1);
    }

    public void setTotalTimes(int value) {
        mDatabaseUtil.setIntValue(getClass().getSimpleName() + TOTAL_TIMES, value);
    }

    public void setAutoTestListener(AutoTestListener listener) {
        mAutoTestListener = listener;
    }

    public abstract void executeTest();

    public abstract String getTestTitle();

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

    public interface AutoTestListener {
        public void onTestStart();
        public void onTestEnd();
    }

    public void setReboot(boolean reboot) {
        mDatabaseUtil.setBoolValue("is_reboot", reboot);
    }

    public boolean isReboot() {
        return mDatabaseUtil.getBoolValue("is_reboot");
    }

    public void setEnabled(boolean enabled) {
        mDatabaseUtil.setBoolValue(getClass().getSimpleName() + ENABLED, enabled);
    }

    public boolean isEnabled() {
        return mDatabaseUtil.getBoolValue(getClass().getSimpleName() + ENABLED, true);
    }

    public View createPreferenceView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pref_edit, null);
        EditText testTime = (EditText) view.findViewById(R.id.test_time);
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
        EditText testTime = (EditText) view.findViewById(R.id.test_time);
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
}
