package com.ztemt.test.auto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

public class AutoTestActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            handleIntent(getIntent().getExtras());
        }
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

    private void handleIntent(Bundle bundle) {
        AutoTestFragment fragment = new AutoTestFragment();
        fragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                fragment).commit();
    }
}
