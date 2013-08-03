package com.ztemt.test.auto.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseUtil {

    private static final String LOG_TAG = "DatabaseUtil";
    private static final String TARGET_PATH = "/persist/settings.db";
    private static final String DB_NAME = "settings.db";
    private static final String TABLE_NAME = "system";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private static DatabaseUtil sInstance;

    private Context mContext;

    private DatabaseUtil(Context context) {
        mContext = context;
    }

    public static void init(Context context) {
        if (sInstance != null) {
            Log.w(LOG_TAG, "Already initialized.");
        }
        sInstance = new DatabaseUtil(context);
    }

    public static DatabaseUtil getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

    public void copyDBFile() {
        if (new File(TARGET_PATH).exists()) {
            return;
        }

        byte[] buffer = new byte[1024];
        int count;

        try {
            FileOutputStream os = new FileOutputStream(TARGET_PATH);
            InputStream is = mContext.getAssets().open(DB_NAME);

            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }

            os.close();
            is.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    public String getValue(String name, String defValue) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(TARGET_PATH, null, SQLiteDatabase.OPEN_READONLY);
        String value = defValue;

        Cursor cursor = db.query(TABLE_NAME, new String[] { VALUE },
                NAME + " = ?", new String[] { name }, null, null, null);

        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                value = cursor.getString(cursor.getColumnIndex(VALUE));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return value;
    }

    public String getValue(String name) {
        return getValue(name, null);
    }

    public void setValue(String name, String value) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(TARGET_PATH, null, SQLiteDatabase.OPEN_READWRITE);

        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(VALUE, value);

        if (getValue(name) == null) {
            db.insert(TABLE_NAME, null, values);
        } else {
            db.update(TABLE_NAME, values, NAME + " = ?", new String[] { name });
        }
        db.close();
    }

    public int getIntValue(String name, int defValue) {
        String value = getValue(name);
        int intVal = defValue;

        if (value != null) {
            intVal = Integer.parseInt(value);
        }
        return intVal;
    }

    public int getIntValue(String name) {
        return getIntValue(name, 0);
    }

    public void setIntValue(String name, int value) {
        setValue(name, String.valueOf(value));
    }

    public boolean getBoolValue(String name, boolean defValue) {
        int value = getIntValue(name, defValue ? 1 : 0);
        return value == 1;
    }

    public boolean getBoolValue(String name) {
        return getBoolValue(name, false);
    }

    public void setBoolValue(String name, boolean value) {
        setIntValue(name, value ? 1 : 0);
    }
}
