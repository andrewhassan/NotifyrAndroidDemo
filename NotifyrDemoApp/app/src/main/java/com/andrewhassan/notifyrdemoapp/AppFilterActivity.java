package com.andrewhassan.notifyrdemoapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Applepie on 11/2/2014.
 */
public class AppFilterActivity extends Activity {
    //TODO:This honestly should be a fragement or something
    AutoCompleteTextView mAppEntryTextEntry;
    Button mAddBtn;
    SharedPreferences mPrefs;
    List<PackageInfo> mAllPackages;
    PackageInfo mAppToMute;
    Set<String> mMutedPackages;
    ListView mMutedAppsList;
    ListAdapter mMutedAppsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appfilter);
        mAddBtn = (Button) findViewById(R.id.add_app_btn);
        mAppEntryTextEntry = (AutoCompleteTextView) findViewById(R.id.appnametextentry);
        mMutedAppsList = (ListView) findViewById(R.id.muted_app_list);

        mMutedAppsList.setAdapter(mMutedAppsAdapter);
        mAppEntryTextEntry.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MySimpleArrayAdapter adapter = (MySimpleArrayAdapter) mAppEntryTextEntry.getAdapter();
                mAppToMute = adapter.getValues().get(position);
                mAppEntryTextEntry.setText(mAppToMute.applicationInfo.loadLabel(getPackageManager()), false);
            }
        });
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMutedPackages.add(mAppToMute.packageName);
                mPrefs.edit().putStringSet(Constants.FILTERED_APPS, mMutedPackages).commit();

                ArrayList<PackageInfo> temp = new ArrayList<PackageInfo>();
                for (String key : mMutedPackages) {
                    try {
                        temp.add(getPackageManager().getPackageInfo(key, 0));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                mMutedAppsAdapter = new MySimpleArrayAdapter(AppFilterActivity.this, temp);
                mMutedAppsList.setAdapter(mMutedAppsAdapter);
            }
        });

        mMutedAppsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                MySimpleArrayAdapter adapter = (MySimpleArrayAdapter) mMutedAppsList.getAdapter();
                String packageNameToRemove = adapter.getValues().get(position).packageName;
                Iterator<String> iterator = mMutedPackages.iterator();
                while (iterator.hasNext()) {
                    String packageName = iterator.next();
                    if (packageName.equals(packageNameToRemove)) {
                        iterator.remove();
                    }
                }
                mPrefs.edit().putStringSet(Constants.FILTERED_APPS, mMutedPackages).commit();
                adapter.getValues().remove(position);
                adapter.notifyDataSetChanged();

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs = this.getSharedPreferences(Constants.TAG, Context.MODE_PRIVATE);
        //TODO:Put dis shit in a asynctask
        mAllPackages = getPackageManager().getInstalledPackages(0);
        mMutedPackages = mPrefs.getStringSet(Constants.FILTERED_APPS, new HashSet<String>());
        ArrayList<PackageInfo> temp = new ArrayList<PackageInfo>();
        for (String key : mMutedPackages) {
            try {
                temp.add(getPackageManager().getPackageInfo(key, 0));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        mMutedAppsAdapter = new MySimpleArrayAdapter(this, temp);
        mMutedAppsList.setAdapter(mMutedAppsAdapter);
        MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, getPackageManager().getInstalledPackages(0));
        mAppEntryTextEntry.setAdapter(adapter);
    }

    class MySimpleArrayAdapter extends ArrayAdapter<PackageInfo> implements Filterable {
        private final Context context;
        private List<PackageInfo> values;

        public List<PackageInfo> getValues() {
            return values;
        }

        public MySimpleArrayAdapter(Context context, List<PackageInfo> values) {
            super(context, R.layout.app_list_entry, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder mViewHolder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.app_list_entry, parent, false);
                mViewHolder = new ViewHolder();
                mViewHolder.mAppName = (TextView) convertView.findViewById(R.id.textView);
                mViewHolder.mIcons = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(mViewHolder);
                convertView.setBackgroundColor((int) Math.random());
            } else {
                mViewHolder = (ViewHolder) convertView.getTag();
            }
            PackageInfo app = values.get(position);
            mViewHolder.mAppName.setText(
                    app.applicationInfo.loadLabel(getPackageManager()).toString());
            mViewHolder.mIcons.setImageDrawable(
                    app.applicationInfo.loadIcon(getPackageManager()));
            return convertView;
        }

        @Override
        public int getCount() {
            return values.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<PackageInfo> filteredResults = new ArrayList<PackageInfo>();
                    if (constraint == null || constraint.equals("")) {
                        filteredResults = values;
                    } else {
                        for (PackageInfo app : mAllPackages) {
                            if (
                                    app.applicationInfo.loadLabel(getPackageManager()).toString().toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                                filteredResults.add(app);
                            }
                        }
                    }
                    FilterResults returnValue = new FilterResults();
                    returnValue.values = filteredResults;
                    returnValue.count = filteredResults.size();
                    return returnValue;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    values = (List<PackageInfo>) results.values;
                    MySimpleArrayAdapter.this.notifyDataSetChanged();
                }
            };
        }

        class ViewHolder {
            TextView mAppName;
            ImageView mIcons;
        }
    }
}