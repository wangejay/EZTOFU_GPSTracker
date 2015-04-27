package eztofu_gpstracker.luffytech.com.eztofu_gpstracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

public class GPSTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    public static final String TAG = "GPSTrackingService";

    public static final String ACTION_ERROR = "com.luffytech.eztofu.gpstracker.action.ERROR";
    public static final String EXTRA_ERROR_CODE = "com.luffytech.eztofu.gpstracker.ERROR_CODE";

    public static final int ERROR_CANNOT_CONNECT_SERVER = -1000;

    private static final long GPS_UPDATE_INTERVAL = 10000; // 10 sec
    
    private LocalBroadcastManager mBroadcastManager;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String mUid;

    AsyncHttpClient mHttpClient = new AsyncHttpClient();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(GPS_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(GPS_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mGoogleApiClient.connect();
        TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mUid = tManager.getDeviceId();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG, "gps tracking is stopped");
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "gps tracking is started");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location update, latitude= " + location.getLatitude() + ", longitude=" + location.getLongitude());
        RequestParams params = new RequestParams();
        String x = String.valueOf(location.getLongitude());
        String y = String.valueOf(location.getLatitude());
        params.add("func", "setgps");
        params.add("x", x);
        params.add("y", y);
        params.add("id", mUid);
        mHttpClient.post("http://59.127.47.8:100/eztofu/phone/gps.ashx", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Log.d(TAG, "update gps info successfully");
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Log.e(TAG, "cannot connect to server");
                broadcastError(ERROR_CANNOT_CONNECT_SERVER);
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        broadcastError(connectionResult.getErrorCode());
    }

    private void broadcastError(int errorCode) {
        Intent errorIntent = new Intent();
        errorIntent.setAction(ACTION_ERROR);
        errorIntent.putExtra(EXTRA_ERROR_CODE, errorCode);
        mBroadcastManager.sendBroadcast(errorIntent);
    }
}
