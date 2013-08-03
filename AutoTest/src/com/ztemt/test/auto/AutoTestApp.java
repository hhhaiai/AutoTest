package com.ztemt.test.auto;

import com.ztemt.test.auto.util.DatabaseUtil;

import android.app.Application;

public class AutoTestApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseUtil.init(this);
        DatabaseUtil.getInstance().copyDBFile();
    }
}
