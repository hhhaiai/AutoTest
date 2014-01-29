package com.ztemt.test.auto;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ztemt.test.auto.item.BaseTest;
import com.ztemt.test.auto.item.TestListener;
import com.ztemt.test.auto.util.PreferenceUtils;
import com.ztemt.test.platform.PlatformService;

public class AutoTestFragment extends ListFragment implements TestListener,
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
        mPrefUtils = new PreferenceUtils(getActivity());
        mAdapter = new AutoTestAdapter(getActivity(), getArguments());
        setListAdapter(mAdapter);
        getActivity().bindService(new Intent(PlatformService.class.getName()),
                mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.test_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setSelected(true);
        startTest(getArguments());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mServiceConnection);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(test.getTitle());
        builder.setView(mPrefView);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.show();
    }
}
