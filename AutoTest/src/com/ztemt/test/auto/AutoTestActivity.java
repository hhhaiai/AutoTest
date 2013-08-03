package com.ztemt.test.auto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.ztemt.test.auto.item.AirplaneModeTest;
import com.ztemt.test.auto.item.BaseTest;
import com.ztemt.test.auto.item.BasebandVersionTest;
import com.ztemt.test.auto.item.BluetoothTest;
import com.ztemt.test.auto.item.CallTest;
import com.ztemt.test.auto.item.NetworkTest;
import com.ztemt.test.auto.item.RebootTest;
import com.ztemt.test.auto.item.RecoveryTest;
import com.ztemt.test.auto.item.RingtoneTest;
import com.ztemt.test.auto.item.SDCardTest;
import com.ztemt.test.auto.item.SleepWakeTest;
import com.ztemt.test.auto.item.SmsTest;
import com.ztemt.test.auto.item.WifiTest;
import com.ztemt.test.auto.util.DatabaseUtil;

public class AutoTestActivity extends PreferenceActivity implements
        BaseTest.AutoTestListener, OnItemLongClickListener,
        DialogInterface.OnClickListener, OnPreferenceChangeListener {

    private static final String LOG_TAG = "AutoTestActivity";
    private static final String CURRENT_TEST = "current_test";
    private static final String START_SELECTED_TEST = "start_selected_test";

    private static final int EVENT_UPDATE_INFO = 1;
    private static final int MENU_SELECT_ALL = Menu.FIRST;
    private static final int MENU_DESELECT_ALL = MENU_SELECT_ALL + 1;

    private Map<Integer, BaseTest> mMap = new HashMap<Integer, BaseTest>();
    private View mPrefView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_UPDATE_INFO:
                updateInfo(getCurrentTest(), true);
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        addPreferencesFromResource(R.xml.auto_test_list);

        ListView listView = getListView();
        listView.setOnItemLongClickListener(this);

        fillMap();
        populateList();

        if (mMap.get(1).isReboot()) {
            startTest();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference.getKey().equals(START_SELECTED_TEST)) {
            setCurrentTest(1);
            startTest();
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        log("onPreferenceChange : key = " + preference.getKey());

        if (preference instanceof CheckBoxPreference) {
            int key = Integer.parseInt(preference.getKey());
            mMap.get(key).setEnabled(Boolean.valueOf(String.valueOf(newValue)));
        }
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int key = Integer.parseInt(String.valueOf(mPrefView.getTag()));
        mMap.get(key).onPreferenceClick(mPrefView);
        updateInfo(key);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, MENU_SELECT_ALL, 1, R.string.select_all);
        menu.add(1, MENU_DESELECT_ALL, 2, R.string.deselect_all);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SELECT_ALL:
            selectOrDeselectAll(true);
            break;
        case MENU_DESELECT_ALL:
            selectOrDeselectAll(false);
            break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // Nothing to do!
    }

    @Override
    public void onTestStart() {
        mHandler.sendEmptyMessage(EVENT_UPDATE_INFO);
    }

    @Override
    public void onTestEnd() {
        int currentTest = getCurrentTest();
        updateInfo(currentTest);
        setCurrentTest(currentTest + 1);

        if (getCurrentTest() <= mMap.size()) {
            startTest();
        } else {
            getPreferenceScreen().setEnabled(true);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
            long id) {
        ListView listView = (ListView) parent;
        Preference p = (Preference) listView.getAdapter().getItem(position);

        if (p.isEnabled() && !p.getKey().equals(START_SELECTED_TEST)) {
            showPreferenceDialog(Integer.parseInt(p.getKey()));
        }
        return true;
    }

    private void fillMap() {
        // The first key must be 1
        mMap.put(1, new AirplaneModeTest(this));
        mMap.put(2, new BluetoothTest(this));
        mMap.put(3, new WifiTest(this));
        mMap.put(4, new RingtoneTest(this));
        mMap.put(5, new SDCardTest(this));
        mMap.put(6, new SleepWakeTest(this));
        mMap.put(7, new CallTest(this));
        mMap.put(8, new SmsTest(this));
        mMap.put(9, new RebootTest(this));
        mMap.put(10, new RecoveryTest(this));
        mMap.put(11, new NetworkTest(this));
        mMap.put(12, new BasebandVersionTest(this));
    }

    private void populateList() {
        Set<Integer> set = mMap.keySet();
        Integer[] keys = set.toArray(new Integer[set.size()]);

        Arrays.sort(keys);

        for (int key : keys) {
            CheckBoxPreference pref = new CheckBoxPreference(this);
            pref.setKey(String.valueOf(key));
            pref.setPersistent(false);
            pref.setChecked(mMap.get(key).isEnabled());
            pref.setOnPreferenceChangeListener(this);
            getPreferenceScreen().addPreference(pref);
            updateInfo(key);
        }
    }

    private void startTest() {
        int currentTest = getCurrentTest();
        BaseTest test = mMap.get(currentTest);

        if (test != null && test.isEnabled()) {
            test.setAutoTestListener(this);
            test.startTest();

            getPreferenceScreen().setEnabled(false);
            updateInfo(currentTest, true);
        } else {
            onTestEnd();
        }
    }

    private int getCurrentTest() {
        return DatabaseUtil.getInstance().getIntValue(CURRENT_TEST, 1);
    }

    private void setCurrentTest(int currentTest) {
        DatabaseUtil.getInstance().setIntValue(CURRENT_TEST, currentTest);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private void updateInfo(int key, boolean inProcess) {
        Preference ps = getPreferenceScreen().findPreference(String.valueOf(key));
        BaseTest test = mMap.get(key);

        ps.setTitle(test.getTestTitle() + " [" + test.getTestTimes()
                + "/" + test.getTotalTimes() + "] "
                + (inProcess ? "..." : ""));
        ps.setSummary(getString(R.string.test_summary, test.getSuccessTimes(),
                test.getFailureTimes()));
    }

    private void updateInfo(int key) {
        updateInfo(key, false);
    }

    private void showPreferenceDialog(int key) {
        BaseTest test = mMap.get(key);
        mPrefView = test.createPreferenceView();
        mPrefView.setTag(key);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(test.getTestTitle());
        builder.setView(mPrefView);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.show();
    }

    private void selectOrDeselectAll(boolean selectAll) {
        Set<Integer> set = mMap.keySet();
        Integer[] keys = set.toArray(new Integer[set.size()]);
        CheckBoxPreference p = null;

        for (int key : keys) {
            p = (CheckBoxPreference) getPreferenceScreen().findPreference(
                    String.valueOf(key));
            p.setChecked(selectAll);
            mMap.get(key).setEnabled(selectAll);
        }
    }
}
