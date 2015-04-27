package eztofu_gpstracker.luffytech.com.eztofu_gpstracker;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {


    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    private boolean isGpsServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GPSTrackingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startGpsTrackService() {
        Intent startService = new Intent(this, GPSTrackingService.class);
        startService(startService);
    }

    private void stopGpsTrackService() {
        Intent stopService = new Intent(this, GPSTrackingService.class);
        stopService(stopService);
    }

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GPSTrackingService.ACTION_ERROR);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(GPSTrackingService.ACTION_ERROR)) {
                    int errorCode = intent.getIntExtra(GPSTrackingService.EXTRA_ERROR_CODE, 0);
                    int stringId;
                    switch (errorCode) {
                        case GPSTrackingService.ERROR_CANNOT_CONNECT_SERVER:
                            stringId = R.string.cannot_connect_server;
                            break;
                        default:
                            stringId = R.string.no_google_play;
                    }
                    Toast.makeText(MainActivity.this, stringId, Toast.LENGTH_SHORT).show();
                }
            }
        };

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcast() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onPostResume();
        if (!Utils.isGpsEnabled(this)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            if (!Utils.isNetworkAvailable(this)) {
                Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            }
            registerBroadcast();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcast();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        private Switch mGpsSwitch;
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mGpsSwitch = (Switch) rootView.findViewById(R.id.gps_switch);
            boolean isServiceRunning = MainActivity.this.isGpsServiceRunning();
            mGpsSwitch.setChecked(isServiceRunning);
            mGpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        MainActivity.this.startGpsTrackService();
                    } else {
                        MainActivity.this.stopGpsTrackService();
                    }
                }
            });
            return rootView;
        }
    }
}
