package com.tronstudios.mqtttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.Circle;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.github.ybq.android.spinkit.style.FadingCircle;

import java.util.List;

import static android.R.attr.button;

public class Con1 extends AppCompatActivity {
    Button button_scanear;
    List<ScanResult> wifiList;
    String TAG="wifi1";
    WifiManager mainWifi;
    StringBuilder sb = new StringBuilder();
    String SSIDSensor="SensorMovimiento";

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"OnStop Con1");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_con1);

        button_scanear=(Button)findViewById(R.id.button_scanear);

        FadingCircle fadingCircle=new FadingCircle();
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.spin_kit);
        Circle circulo=new Circle();
        DoubleBounce doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(fadingCircle);

        button_scanear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Starting Scan WIFI available Networks...");
                mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                registerReceiver(mWifiScanReceiver,
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                mainWifi.startScan();
            }
        });




    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();

            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++){
                sb.append(new Integer(i+1).toString() + ".");
                sb.append((wifiList.get(i)).SSID);
                sb.append(" level: "+(wifiList.get(i)).level);
                sb.append("\n");

                if ((wifiList.get(i)).SSID.equals(SSIDSensor)){
                    Toast.makeText(getApplicationContext(),"Sensor encontrado",Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Con1.this);
                    alertDialogBuilder.setMessage("Sensor de movimiento encontrado :)");
                    alertDialogBuilder.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //Toast.makeText(Con1.this,"You clicked yes button",Toast.LENGTH_LONG).show();
                                    unregisterReceiver(mWifiScanReceiver);
                                    Intent i=new Intent(Con1.this,Con2.class);
                                    startActivity(i);

                                }
                            });
//                    alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            finish();
//                        }
//                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();



                }

            }
            wifiList.clear();
            Log.d(TAG,sb.toString());
        }
    };
}
