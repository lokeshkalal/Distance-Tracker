package com.example.lokesh.distancecalculator;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MyService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public MyService() {
    }

    private Messenger mClient;
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_UPDATE_VALUE = 3;

    public static final long UPDATE_INTERVAL = 15000;
    public static final long FASTEST_INTERVAL = 5000;


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    Location mLastSavedLocation;
    float totalDistanceCovered = 0.0f;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();

        // Create the LocationRequest object
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mGoogleApiClient = null;

    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    sendDistanceCoveredToClient(totalDistanceCovered);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClient = null;
                    break;
                case MSG_UPDATE_VALUE:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }


    @Override
    public void onDestroy() {
        stopLocationUpdates();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLastSavedLocation != null) {
            totalDistanceCovered += (mLastSavedLocation.distanceTo(location));
            mLastSavedLocation = location;
            sendDistanceCoveredToClient(totalDistanceCovered);
        } else {
            mLastSavedLocation = location;
        }


    }

    private void sendDistanceCoveredToClient(float distance) {
        if (mClient != null) {
            Message msg = Message.obtain(null,
                    MyService.MSG_UPDATE_VALUE);
            Bundle bundle = new Bundle();
            bundle.putFloat(Utils.BUNDLE_ARG_DISTANCE, distance);
            msg.setData(bundle);
            try {
                mClient.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
}
