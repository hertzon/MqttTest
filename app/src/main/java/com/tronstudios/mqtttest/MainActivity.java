package com.tronstudios.mqtttest;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    String TAG="mqtt";
    MqttAndroidClient client;
    Button button_publish;
    TextView textView_sub;
    int secuencia=0;
    Vibrator vibrator;
    Ringtone ring;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_publish=(Button)findViewById(R.id.button_publish);
        textView_sub=(TextView)findViewById(R.id.textView_sub);

        vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);

        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ring=RingtoneManager.getRingtone(getApplicationContext(),uri);

        button_publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pub();
            }
        });

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

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                textView_sub.setText(new String(message.getPayload()));
                vibrator.vibrate(1000);
                ring.play();
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
}
