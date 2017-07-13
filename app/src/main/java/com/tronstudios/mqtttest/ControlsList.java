package com.tronstudios.mqtttest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
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
import org.w3c.dom.Text;

import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ControlsList extends AppCompatActivity {
    String[] lugares;
    Boolean[] status;
    String TAG="mqtt";
    ListView controlslv;
    ImageView imageViewRssi;
    MqttAndroidClient client;
    int secuencia;
    SQLiteDatabase myDB;
    Cursor c;
    Button btn_salir;
    CountDownTimer cTimer = null;
    int prescalerCounter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls_list);
        if(savedInstanceState!=null){
            //status = savedInstanceState.getBooleanArray("status");
        }
        controlslv= (ListView) findViewById(R.id.lv_controls);
        imageViewRssi=(ImageView)findViewById(R.id.imageViewRssi);
        btn_salir=(Button)findViewById(R.id.button_salir);
        btn_salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                System.exit(0);
            }
        });

        Log.d(TAG,"Creando DB y tabla");
        String datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
        Log.d(TAG,"Datetime: "+datetime);
        myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
        //myDB.execSQL("DROP TABLE IF EXISTS controles");//borramos tabla
        myDB.execSQL("CREATE TABLE IF NOT EXISTS "
                + "controles"
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT, serial TEXT, activo BOOLEAN, fechacreado TEXT, lugar TEXT, server TEXT, port INTEGER, ultimaConexion TEXT);");
//        myDB.execSQL("INSERT INTO "
//                + "controles"
//                + " (serial, activo, fechacreado, lugar, server, port, ultimaConexion)"
//                + " VALUES ("+"'"+ "TRA000002X"+"'" +", '"+true+"'"+ ", "+"'"+datetime+"'"+", "+"'"+"CUARTO"+"'"+", "+"'"+"138.197.20.62"+"'"+", "+"'"+1883+"','"+datetime+"');");

        Log.d(TAG,"Reading DB...");
        c = myDB.rawQuery("SELECT * FROM controles", null);
        c.moveToFirst();
        if (c.getCount()>0){
            Log.d(TAG,"Populate array");
        }
        lugares=new String[c.getCount()];
        status=new Boolean[c.getCount()];
        int i=0;
        if (c != null && c.getCount()>0) {
            do {
                int id=c.getInt(c.getColumnIndex("id"));
                String serial=c.getString(c.getColumnIndex("serial"));
                int activo=(c.getColumnIndex("activo"));
                if (activo==1){
                    status[i]=true;
                }else if (activo==1){
                    status[i]=false;
                }
                String fechacreado=c.getString(c.getColumnIndex("fechacreado"));
                String lugar=c.getString(c.getColumnIndex("lugar"));

                lugares[i]=lugar;


                Log.d(TAG,"id: "+id+" \t "+serial+" \t "+activo+" \t "+fechacreado+" \t "+lugar);
                i++;

            }while(c.moveToNext());
        }

        myDB.close();
        startTimer();

        final String clientId = MqttClient.generateClientId();//138.197.20.62
        client =new MqttAndroidClient(this.getApplicationContext(), "tcp://138.197.20.62:1883",clientId);
        Log.d(TAG,"clientId: "+clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess: "+"We are connected");
                    Log.d(TAG,"Subscribiendo controles....");
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    Log.d(TAG,"Encontrados: "+c.getCount()+" controles...");
                    c.moveToFirst();
                    if (c != null && c.getCount()>0) {
                        do {
                            String serial=c.getString(c.getColumnIndex("serial"));
                            Log.d(TAG,"Subsribiendo control: "+serial);
                            setSubscription(serial);
                        }while(c.moveToNext());


                    }











                    myDB.close();

                    //setSubscription();
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
                    Log.d(TAG,"Llego topic de: "+topic);
                    String payload;

                    String serialIncoming=null;
                    int ncontroles=0;
                    payload=new String(message.getPayload());
                    //Log.d(TAG,"on messageArrived: "+payload);
                    JSONObject jsonObject = new JSONObject(payload);
                    serialIncoming=jsonObject.getString("id");


                    Log.d(TAG,"serialIncoming: "+serialIncoming);
                    Log.d(TAG,"Guardando time de arrivo...");
                    String datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    String query="update controles set ultimaConexion='"+datetime+"' where serial='"+serialIncoming+"';";
                    //Log.d(TAG,"query: "+query);
                    myDB.execSQL(query);


                    myDB.close();
                    //Log.d(TAG,jsonObject.toString());
                    //Log.d(TAG,"id: "+jsonObject.getString("id"));
                    Log.d(TAG,"state: "+jsonObject.getString("state"));
                    //Log.d(TAG,"rssi: "+jsonObject.getInt("rssi"));
                    ncontroles=controlslv.getCount();
                    //Log.d(TAG,"controlslv.getCount(): "+ncontroles);
                    for (int i=0;i<ncontroles;i++){
                        TextView txtV=(TextView)controlslv.getChildAt(i).findViewById(R.id.tv_item);
                        //Log.d(TAG,txtV.getText().toString());
                    }
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    c.moveToFirst();
                    int i=0;
                    if (c != null && c.getCount()>0) {
                        do {
                            String serial=c.getString(c.getColumnIndex("serial"));
                            //Log.d(TAG,"Control: "+serial);
                            if (serial.equals(serialIncoming)){
                                break;
                            }
                            i++;
                        }while(c.moveToNext());


                    }
                    myDB.close();
                    //Log.d(TAG,"Control en posicion: "+i);
                    //Log.d(TAG,"RSSI: "+RSSI);
                    int quality=jsonObject.getInt("quality");
                    ImageView imageView = (ImageView) controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
                    int RSSI=jsonObject.getInt("rssi");
                    if (quality>75){
                        imageView.setImageResource(R.drawable.wifi4);
                    }else if (quality>50){
                        imageView.setImageResource(R.drawable.wifi3);
                    }else if (quality>25){
                        imageView.setImageResource(R.drawable.wifi2);
                    }else if (quality>10){
                        imageView.setImageResource(R.drawable.wifi1);
                    }


                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        }


        // Each row in the list stores lugares name and its status
        List<HashMap<String,Object>> aList = new ArrayList<HashMap<String,Object>>();
        for(i=0;i<lugares.length;i++){
            HashMap<String, Object> hm = new HashMap<String,Object>();
            hm.put("txt", lugares[i]);
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
                Log.d(TAG,"pulsado: "+i);
                ListView lView = (ListView) lv;
                SimpleAdapter adapter = (SimpleAdapter) lView.getAdapter();
                HashMap<String,Object> hm = (HashMap) adapter.getItem(i);
                RelativeLayout rLayout = (RelativeLayout) view;
                ToggleButton tgl = (ToggleButton) rLayout.getChildAt(0);
                TextView txtv=(TextView)rLayout.getChildAt(1);
                Log.d(TAG,"txtv text:"+txtv.getText().toString());
                String strStatus = "";
                Log.d(TAG,"Leyendo serial de control....");
                myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                c = myDB.rawQuery("SELECT serial FROM controles WHERE lugar='"+txtv.getText().toString()+"';", null);
                c.moveToFirst();
                String serial=null;
                if (c != null && c.getCount()>0) {
                    do {
                        serial=c.getString(c.getColumnIndex("serial"));
                        Log.d(TAG,"Serial leido: "+serial);

                    }while(c.moveToNext());
                }






                if(tgl.isChecked()){
                    tgl.setChecked(false);
                    //strStatus = "Off";
                    //status[i]=false;
                    publish(serial,false);
                }else{
                    tgl.setChecked(true);
                    //strStatus = "On";
                    //status[i]=true;
                    publish(serial,true);
                }
                //Toast.makeText(getBaseContext(), (String) hm.get("txt") + " : " + strStatus, Toast.LENGTH_SHORT).show();
                myDB.close();
            }
        });





    }

    private void publish(String serial,boolean state) {
        Log.d(TAG,"Publishing control: "+serial);
        //String topic = "/TRA000001X/rele";
        String topic="/"+serial+"/rele";
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

    public void setSubscription(String serial){
        String topic="/"+serial+"/"+"estado";
        Log.d(TAG,"Setting subscription of: "+topic);
        try {
            client.subscribe(topic,0);


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    void startTimer() {
        cTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (++prescalerCounter>5){
                    prescalerCounter=0;
                    //Log.d(TAG,"Timer: onTick");
                    Log.d(TAG,"Leyendo ultimos dates de conexion....");
                    String datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date dateNow;



                    //Log.d(TAG,"Datetime: "+datetime);
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    c.moveToFirst();
                    int i=0;
                    long diffMinutes=59;
                    long diffHours=24;
                    long diffSeconds=59;
                    if (c != null && c.getCount()>0) {
                        do {
                            String id=c.getString(c.getColumnIndex("id"));
                            String serial=c.getString(c.getColumnIndex("serial"));
                            String ultimaconexion=c.getString(c.getColumnIndex("ultimaConexion"));
                            diffMinutes=59;diffHours=24;
                            try {
                                dateNow=simpleDateFormat.parse(datetime);
                                long diff=dateNow.getTime()-simpleDateFormat.parse(ultimaconexion).getTime();
                                diffMinutes = diff / (60 * 1000) % 60;
                                diffSeconds = diff / 1000 % 60;
                                //Log.d(TAG,"diffMinutes: "+diffMinutes);
                                diffHours = diff / (60 * 60 * 1000);
                                //Log.d(TAG,"diffHours: "+diffHours);

                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            boolean online=false;
                            if (diffHours==0 && diffMinutes==0 && diffSeconds<10){
                                online=true;
                            }else {
                                online=false;
                            }
                            myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                            c = myDB.rawQuery("SELECT * FROM controles", null);
                            c.moveToFirst();
                            i=0;
                            if (c != null && c.getCount()>0) {
                                do {
                                    String serialr=c.getString(c.getColumnIndex("serial"));
                                    if (serialr.equals(serial)){
                                        break;
                                    }
                                    i++;
                                }while(c.moveToNext());

                            }
                            ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(i).findViewById(R.id.tgl_status);
                            ImageView imgv=(ImageView)controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
                            if (!online){
                                //tglbtn.setImageResource(R.drawable.wifi3);

                                tglbtn.setVisibility(View.INVISIBLE);
                                imgv.setImageResource(R.drawable.nowifi);
                                //tglbtn.setBackgroundDrawable(R.drawable.wifi);
                            }else {
                                tglbtn.setVisibility(View.VISIBLE);
                            }



                            myDB.close();

                            Log.d(TAG,"id: "+id+" Serial: "+serial+" ultimaConexion: "+ultimaconexion+" diffMinutes: "+diffMinutes+" diffHours: "+diffHours+" diffSeconds: "+diffSeconds+" online: "+online);
                            /*
                            myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                            c = myDB.rawQuery("SELECT * FROM controles", null);
                            c.moveToFirst();
                            int i=0;
                            if (c != null && c.getCount()>0) {
                                do {
                                    String serial=c.getString(c.getColumnIndex("serial"));
                                    //Log.d(TAG,"Control: "+serial);
                                    if (serial.equals(serialIncoming)){
                                        break;
                                    }
                                    i++;
                                }while(c.moveToNext());


                            }
                            myDB.close();
                            //Log.d(TAG,"Control en posicion: "+i);
                            //Log.d(TAG,"RSSI: "+RSSI);
                            int quality=jsonObject.getInt("quality");
                            ImageView imageView = (ImageView) controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
                            int RSSI=jsonObject.getInt("rssi");
                            if (quality>75){
                                imageView.setImageResource(R.drawable.wifi4);
                            }else if (quality>50){
                                imageView.setImageResource(R.drawable.wifi3);
                            }else if (quality>25){

                            */


                        }while(c.moveToNext());
                    }




                    myDB.close();
                }


            }
            public void onFinish() {
                Log.d(TAG,"Timer: onFinish");
                cTimer.start();
            }
        };
        cTimer.start();
    }
}
