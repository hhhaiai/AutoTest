package com.ztemt.test.auto;

import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
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
    private static BaseTest[] sTests;

    private Comparator<BaseTest> mComparator = new Comparator<BaseTest>() {

        @Override
        public int compare(BaseTest lhs, BaseTest rhs) {
            return lhs.getOrdinal() - rhs.getOrdinal();
        }
    };

    public AutoTestAdapter(Context context, Bundle bundle) {
        mContext = context;
        createTests(context);
        for (int i = 0; i < sTests.length; i++) {
            sTests[i].setExtras(bundle);
        }
        Arrays.sort(sTests, mComparator);
    }

    @Override
    public int getCount() {
        return sTests.length;
    }

    @Override
    public BaseTest getItem(int position) {
        return sTests[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BaseTest test = sTests[position];
        ViewHolder holder = new ViewHolder();
        convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
        holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
        holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
        holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
        holder.checkbox = (CheckBox) convertView.findViewById(android.R.id.checkbox);

        holder.icon.setBackgroundResource(test.isRunning() ? android.R.drawable.ic_media_play : 0);
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
        for (int i = 0; i < sTests.length; i++) {
            sTests[i].setEnabled(false);
        }
        notifyDataSetChanged();
    }

    public byte[] report() {
        StringBuffer sb = new StringBuffer(mContext.getString(R.string.report_titles));
        for (int i = 0; i < sTests.length; i++) {
            sb.append((i + 1) + "\t");
            sb.append(sTests[i].getTotalTimes() + "\t");
            sb.append(sTests[i].getTestTimes() + "\t");
            sb.append(sTests[i].getSuccessTimes() + "\t");
            sb.append(sTests[i].getFailureTimes() + "\t");
            sb.append(sTests[i].getTitle() + "\n");
        }
        return sb.toString().getBytes();
    }

    private void createTests(Context context) {
        if (sTests == null) {
            sTests = new BaseTest[] {
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
    }

    private class ViewHolder {
        ImageView icon;
        TextView text1;
        TextView text2;
        CheckBox checkbox;
    }
}
