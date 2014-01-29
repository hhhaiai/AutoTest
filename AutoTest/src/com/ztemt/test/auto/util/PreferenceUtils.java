package com.ztemt.test.auto.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {

    private SharedPreferences mPrefs;

    public PreferenceUtils(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void putLong(String key, long value) {
        mPrefs.edit().putLong(key, value).commit();
    }

    public long getLong(String key, long defValue) {
        return mPrefs.getLong(key, defValue);
    }

    public void putInt(String key, int value) {
        mPrefs.edit().putInt(key, value).commit();
    }

    public int getInt(String key, int defValue) {
        return mPrefs.getInt(key, defValue);
    }

    public void putBoolean(String key, boolean value) {
        mPrefs.edit().putBoolean(key, value).commit();
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mPrefs.getBoolean(key, defValue);
    }

    public void putString(String key, String value) {
        mPrefs.edit().putString(key, value).commit();
    }

    public String getString(String key, String defValue) {
        return mPrefs.getString(key, defValue);
    }

    public void remove(String key) {
        mPrefs.edit().remove(key).commit();
    }

    public boolean isReboot() {
        return getBoolean("reboot", false);
    }

    public void setReboot(boolean reboot) {
        putBoolean("reboot", reboot);
    }
}
