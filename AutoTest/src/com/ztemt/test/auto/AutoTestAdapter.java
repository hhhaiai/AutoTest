package com.ztemt.test.auto;

import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.ztemt.test.auto.item.AirplaneModeTest;
import com.ztemt.test.auto.item.BaseTest;
import com.ztemt.test.auto.item.BasebandVersionTest;
import com.ztemt.test.auto.item.BluetoothTest;
import com.ztemt.test.auto.item.CallTest;
import com.ztemt.test.auto.item.NetworkTest;
import com.ztemt.test.auto.item.RebootTest;
import com.ztemt.test.auto.item.RingtoneTest;
import com.ztemt.test.auto.item.SDCardTest;
import com.ztemt.test.auto.item.SleepWakeTest;
import com.ztemt.test.auto.item.SmsTest;
import com.ztemt.test.auto.item.WifiTest;

public class AutoTestAdapter extends BaseAdapter {

    private Context mContext;
    private BaseTest[] mTests;

    private Comparator<BaseTest> mComparator = new Comparator<BaseTest>() {

        @Override
        public int compare(BaseTest lhs, BaseTest rhs) {
            return lhs.getOrdinal() - rhs.getOrdinal();
        }
    };

    public AutoTestAdapter(Context context, Bundle bundle) {
        mContext = context;
        mTests = createTests(context);
        for (int i = 0; i < mTests.length; i++) {
            mTests[i].setExtras(bundle);
        }
        Arrays.sort(mTests, mComparator);
    }

    @Override
    public int getCount() {
        return mTests.length;
    }

    @Override
    public BaseTest getItem(int position) {
        return mTests[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BaseTest test = mTests[position];
        ViewHolder holder = new ViewHolder();
        convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
        convertView.setBackgroundColor(test.isRunning() ? Color.BLUE : Color.WHITE);
        holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
        holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
        holder.checkbox = (CheckBox) convertView.findViewById(android.R.id.checkbox);

        holder.text1.setText(test.getTitle() + " [" + test.getTestTimes()
                + "/" + test.getTotalTimes() + "]");
        holder.text2.setText(mContext.getString(R.string.test_summary,
                test.getSuccessTimes(), test.getFailureTimes()));
        holder.checkbox.setChecked(test.isEnabled());
        holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                test.setEnabled(isChecked);
            }
        });
        return convertView;
    }

    public void disableAll() {
        for (int i = 0; i < mTests.length; i++) {
            mTests[i].setEnabled(false);
        }
        notifyDataSetChanged();
    }

    public byte[] report() {
        StringBuffer sb = new StringBuffer(mContext.getString(R.string.report_titles));
        for (int i = 0; i < mTests.length; i++) {
            sb.append((i + 1) + "\t");
            sb.append(mTests[i].getTotalTimes() + "\t");
            sb.append(mTests[i].getTestTimes() + "\t");
            sb.append(mTests[i].getSuccessTimes() + "\t");
            sb.append(mTests[i].getFailureTimes() + "\t");
            sb.append(mTests[i].getTitle() + "\n");
        }
        return sb.toString().getBytes();
    }

    private BaseTest[] createTests(Context context) {
        return new BaseTest[] {
                new AirplaneModeTest(context),
                new BluetoothTest(context),
                new WifiTest(context),
                new RingtoneTest(context),
                new SDCardTest(context),
                new SleepWakeTest(context),
                new CallTest(context),
                new SmsTest(context),
                new RebootTest(context),
                //new RecoveryTest(context),
                new NetworkTest(context),
                new BasebandVersionTest(context)
        };
    }

    private class ViewHolder {
        TextView text1;
        TextView text2;
        CheckBox checkbox;
    }
}
