package com.tronstudios.mqtttest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    String TAG="mqtt";
    MqttAndroidClient client;
    Button button_publish;
    TextView textView_sub;
    int secuencia=0;
    Vibrator vibrator;
    Ringtone ring;
    NotificationCompat.Builder mBuilder;
    int mNotificationId=1;

    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    StringBuilder sb = new StringBuilder();
    List<ScanResult> wifiList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_publish=(Button)findViewById(R.id.button_publish);
        textView_sub=(TextView)findViewById(R.id.textView_sub);


        vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);

        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ring=RingtoneManager.getRingtone(getApplicationContext(),uri);

        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mainWifi.isWifiEnabled() == false){
            // If wifi disabled then enable it
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        Log.d(TAG,"Starting Scan Wifi...");



        Log.d(TAG,"no hay extras!! creando mqtt client:");
        final String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://138.197.20.62:1883",
                        clientId);
        Log.d(TAG,"clientId: "+clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected

                    Log.d(TAG, "onSuccess");
                    setSubscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }



        button_publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainWifi.startScan();
                pub();
            }
        });



        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload;
                payload=new String(message.getPayload());
                Log.d(TAG,"on messageArrived: "+payload);
                textView_sub.setText(new String(message.getPayload()));
                vibrator.vibrate(1000);
                ring.play();

                mBuilder= (NotificationCompat.Builder) new NotificationCompat.Builder(MainActivity.this)
                        .setSmallIcon(android.R.drawable.ic_notification_overlay)
                        .setContentTitle("Sensor Movimiento")
                        .setAutoCancel(true)
                        .setContentText("Hola se ha detectado movimiento");
                Intent resultIntent=new Intent(MainActivity.this, MainActivity.class);
                resultIntent.putExtra("NotificationMessage", "hola");
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);


                PendingIntent resultPendingIntent=PendingIntent.getActivity(MainActivity.this,0,resultIntent,PendingIntent.FLAG_CANCEL_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotifyMgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId,mBuilder.build());


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });



    }

    public void pub(){
        String topic = "test";
        String payload = "the payload: "+secuencia;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
        secuencia++;
    }
    public void setSubscription(){
        String topic="estado";
        try {
            client.subscribe(topic,0);


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("NotificationMessage");
            Log.d(TAG,"Extras en MainActivity: "+value);

            //The key argument here must match that used in the other activity
        }else {
            Log.d(TAG,"no hay extras!!! :(");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();
            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++){
                sb.append(new Integer(i+1).toString() + ".");
                sb.append((wifiList.get(i)).SSID);
                sb.append("\n");
            }
           Log.d(TAG,"list wifi: "+"\r\n"+sb.toString());
        }

    }
}
