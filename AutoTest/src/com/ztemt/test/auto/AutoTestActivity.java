package com.ztemt.test.auto;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;

import com.ztemt.test.auto.item.BaseTest;
import com.ztemt.test.auto.item.TestListener;
import com.ztemt.test.auto.util.PreferenceUtils;
import com.ztemt.test.platform.PlatformService;

public class AutoTestActivity extends ListActivity implements TestListener,
        DialogInterface.OnClickListener {

    private static final String LOG_TAG = "AutoTest";
    private static final String CURRENT = "current";

    private AutoTestAdapter mAdapter;
    private PreferenceUtils mPrefUtils;
    private Handler mHandler = new Handler();
    private View mPrefView;

    private PlatformService mPlatformService;
    private boolean mBound = false;

    private Runnable mUpdateRunnable = new Runnable() {

        @Override
        public void run() {
            mAdapter.notifyDataSetChanged();
        }
    };

    private Runnable mNotifyRunnable = new Runnable() {

        @Override
        public void run() {
            Log.d(LOG_TAG, "Notify platform to stop");
            if (mBound) {
                try {
                    mPlatformService.notifyStop(mAdapter.report());
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "notifyStop", e);
                }
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlatformService = null;
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlatformService = PlatformService.Stub.asInterface(service);
            mBound = true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.test_list);

        getListView().setSelected(true);

        mPrefUtils = new PreferenceUtils(this);
        if (savedInstanceState == null) {
            handleIntent(getIntent().getExtras());
        }

        bindService(new Intent(PlatformService.class.getName()),
                mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.manual_stop:
            mAdapter.disableAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent.getExtras());
    }

    @Override
    public void onBackPressed() {
        // Nothing to do!
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void onTestStart() {
        updateInfo();
    }

    @Override
    public void onTestStop() {
        updateInfo();
        int current = getCurrent();
        current++;
        setCurrent(current);

        if (current < mAdapter.getCount()) {
            startTest(new Bundle());
        } else {
            mHandler.postDelayed(mNotifyRunnable, 3000);
            getListView().setFocusable(true);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int position = Integer.parseInt(String.valueOf(mPrefView.getTag()));
        mAdapter.getItem(position).onPreferenceClick(mPrefView);
        updateInfo();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (l.isFocusable()) {
            showPreferenceDialog(position);
        }
    }

    private void startTest(Bundle bundle) {
        if (bundle != null || mPrefUtils.isReboot()) {
            if (bundle != null && "auto".equals(bundle.getString("mode"))) {
                setCurrent(0);
            }

            int current = getCurrent();
            BaseTest test = mAdapter.getItem(current);

            if (test != null && test.isEnabled()) {
                getListView().smoothScrollToPosition(current);
                test.setTestListener(this);
                test.startTest();

                getListView().setFocusable(false);
                updateInfo();
            } else {
                onTestStop();
            }
        }
    }

    private void updateInfo() {
        mHandler.post(mUpdateRunnable);
    }

    private int getCurrent() {
        return mPrefUtils.getInt(CURRENT, 0);
    }

    private void setCurrent(int current) {
        mPrefUtils.putInt(CURRENT, current);
    }

    private void showPreferenceDialog(int position) {
        BaseTest test = mAdapter.getItem(position);
        mPrefView = test.createPreferenceView();
        mPrefView.setTag(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(test.getTitle());
        builder.setView(mPrefView);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.show();
    }

    private void handleIntent(Bundle bundle) {
        mAdapter = new AutoTestAdapter(this, bundle);
        setListAdapter(mAdapter);
        startTest(bundle);
    }
}
