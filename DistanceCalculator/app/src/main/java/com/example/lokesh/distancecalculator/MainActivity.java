package com.example.lokesh.distancecalculator;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    TextView mDistanceView, mServiceStatus;
    ToggleButton mToggle;
    Messenger mService = null;
    boolean mIsBound;
    CardView mCardView;
    boolean isServiceRunning = false;


    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    protected void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Utils.PREF_IS_SERVICE_STATRED, isServiceRunning).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isServiceRunning = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Utils.PREF_IS_SERVICE_STATRED, false);
        mServiceStatus = (TextView) findViewById(R.id.service_status);
        mDistanceView = (TextView) findViewById(R.id.distace_textview);
        mCardView = (CardView) findViewById(R.id.cardview);

        mToggle = (ToggleButton) findViewById(R.id.start_service);
        mToggle.setChecked(false);


        mServiceStatus.setText("Disconnected.");

        if (isServiceRunning) {
            mToggle.setChecked(true);
        }
        mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    if (isGpsEnabled()) {
                        startAndBindService();
                    } else {
                        ShowDialog();
                    }


                } else {
                    doUnbindService();
                    stopDistanceTrackerService();

                    mServiceStatus.setText("Disconnected.");
                }
            }
        });


    }

    void startAndBindService() {
        startDistanceTrackerService();
        doBindService();
        isServiceRunning = true;
    }

    private void ShowDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Enable GPS to improve accuracy in device settings");
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                        startAndBindService();
                    }
                });

        alertDialogBuilder.setNegativeButton("approx is fine",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                        startAndBindService();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private boolean isGpsEnabled() {
        LocationManager mlocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        ;
        boolean enabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return enabled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceRunning) {
            doBindService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isServiceRunning && mIsBound) {
            doUnbindService();
        }
    }


    void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
            unbindService(mConnection);
            mIsBound = false;
            mServiceStatus.setText("Unbinding");
        }
    }


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_UPDATE_VALUE:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        Integer distance = ((int) bundle.getFloat(Utils.BUNDLE_ARG_DISTANCE));
                        mDistanceView.setText(distance.toString());
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            mServiceStatus.setText("Attached.");

            try {
                Message msg = Message.obtain(null,
                        MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {

            }

            Toast.makeText(MainActivity.this, "service Connected",
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {

            mService = null;
            mServiceStatus.setText("Disconnected.");

            Toast.makeText(MainActivity.this, "service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };


    void doBindService() {
        bindService(new Intent(MainActivity.this,
                MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mServiceStatus.setText("Binding.");
    }


    void stopDistanceTrackerService() {
        Intent serviceIntent = new Intent(MainActivity.this, MyService.class);
        stopService(serviceIntent);
        isServiceRunning = false;

    }

    void startDistanceTrackerService() {
        Intent serviceIntent = new Intent(MainActivity.this, MyService.class);
        startService(serviceIntent);

    }
}
