package com.tronstudios.mqtttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlsList extends AppCompatActivity {
    String[] countries = new String[] {
            "Sala",
            "Comedor"
    };
    public boolean[] status = {
            true,
            false
    };
    String TAG="mqtt";
    ListView controlslv;
    ImageView imageViewRssi;
    MqttAndroidClient client;
    int secuencia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls_list);
        if(savedInstanceState!=null){
            status = savedInstanceState.getBooleanArray("status");
        }
        controlslv= (ListView) findViewById(R.id.lv_controls);
        imageViewRssi=(ImageView)findViewById(R.id.imageViewRssi);

        final String clientId = MqttClient.generateClientId();//138.197.20.62
        client =new MqttAndroidClient(this.getApplicationContext(), "tcp://138.197.20.62:1883",clientId);
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


            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload;
                    payload=new String(message.getPayload());
                    //Log.d(TAG,"on messageArrived: "+payload);
                    JSONObject jsonObject = new JSONObject(payload);
                    Log.d(TAG,jsonObject.toString());
                    Log.d(TAG,"id: "+jsonObject.getString("id"));
                    Log.d(TAG,"state: "+jsonObject.getString("state"));
                    Log.d(TAG,"rssi: "+jsonObject.getInt("rssi"));
                    int RSSI=jsonObject.getInt("rssi");
                    //Log.d(TAG,"RSSI: "+RSSI);
                    int quality=jsonObject.getInt("quality");
                    ImageView imageView = (ImageView) controlslv.getChildAt(0).findViewById(R.id.imageViewRssi);

                    if (quality>75){
                        imageView.setImageResource(R.drawable.wifi4);
                    }else if (quality>50){
                        imageView.setImageResource(R.drawable.wifi3);
                    }else if (quality>25){
                        imageView.setImageResource(R.drawable.wifi2);
                    }else if (quality>10){
                        imageView.setImageResource(R.drawable.wifi1);
                    }
//                    if (RSSI > -55) {
//                        Log.d(TAG,"1");
//                        imageView.setImageResource(R.drawable.wifi4);
//
//                        //bars = 5;
//                    } else if (RSSI < -55 && RSSI > -65) {
//                        Log.d(TAG,"2");
//                        imageView.setImageResource(R.drawable.wifi3);
//
//                       // bars = 4;
//                    } else if (RSSI < -65 && RSSI > -70) {
//                        Log.d(TAG,"3");
//                        imageView.setImageResource(R.drawable.wifi2);
//
//                        //bars = 3;
//                    } else if (RSSI < -70 && RSSI > -78) {
//                        Log.d(TAG,"4");
//                        imageView.setImageResource(R.drawable.wifi2);
//
//                        //bars = 2;
//                    } else if (RSSI < -78 && RSSI > -82) {
//                        Log.d(TAG,"5");
//                        imageView.setImageResource(R.drawable.wifi1);
//
//                        //bars = 1;
//                    } else {
//                        //bars = 0;
//                    }


                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
            //publish();


        } catch (MqttException e) {
            e.printStackTrace();
        }


        // Each row in the list stores country name and its status
        List<HashMap<String,Object>> aList = new ArrayList<HashMap<String,Object>>();
        for(int i=0;i<countries.length;i++){
            HashMap<String, Object> hm = new HashMap<String,Object>();
            hm.put("txt", countries[i]);
            hm.put("stat",status[i]);
            aList.add(hm);
        }
        String[] from = {"txt","stat" };
        int[] to = { R.id.tv_item, R.id.tgl_status};
        SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), aList, R.layout.lv_layout, from, to);
        controlslv.setAdapter(adapter);



        controlslv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> lv, View view, int i, long l) {
                ListView lView = (ListView) lv;
                SimpleAdapter adapter = (SimpleAdapter) lView.getAdapter();
                HashMap<String,Object> hm = (HashMap) adapter.getItem(i);
                RelativeLayout rLayout = (RelativeLayout) view;
                ToggleButton tgl = (ToggleButton) rLayout.getChildAt(0);
                String strStatus = "";
                if(tgl.isChecked()){
                    tgl.setChecked(false);
                    //strStatus = "Off";
                    //status[i]=false;
                    publish(false);
                }else{
                    tgl.setChecked(true);
                    //strStatus = "On";
                    //status[i]=true;
                    publish(true);
                }
                //Toast.makeText(getBaseContext(), (String) hm.get("txt") + " : " + strStatus, Toast.LENGTH_SHORT).show();
            }
        });





    }

    private void publish(boolean state) {
        Log.d(TAG,"Publishing....");
        String topic = "/TRA000001X/rele";
        String payload = null;
        if (state){
            payload="1";
        }else {
            payload="0";
        }
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
