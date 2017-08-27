package com.tronstudios.mqtttest;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
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
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

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
    int stateScrollView=0;

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
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT, serial TEXT, activo BOOLEAN, fechacreado TEXT, lugar TEXT, server TEXT, port INTEGER, ultimaConexion TEXT, online BOOLEAN, signal INTEGER, state BOOLEAN);");
//        myDB.execSQL("INSERT INTO "
//                + "controles"
//                + " (serial, activo, fechacreado, lugar, server, port, ultimaConexion,online)"
//                + " VALUES ("+"'"+ "TRA000009X"+"'" +", '"+true+"'"+ ", "+"'"+datetime+"'"+", "+"'"+"COCINA"+"'"+", "+"'"+"138.197.20.62"+"'"+", "+"'"+1883+"','"+datetime+"','"+false+"');");

//        ContentValues cv = new ContentValues();
//        cv.put("lugar","BANO");
//        myDB.update("controles", cv, "id="+6, null);

        Log.d(TAG,"Reading DB...");
        c = myDB.rawQuery("SELECT * FROM controles", null);
        c.moveToFirst();
        if (c.getCount()>0){
            Log.d(TAG,"Populate array");
        }
        lugares=new String[c.getCount()];
        status=new Boolean[c.getCount()];
        int i=0;
        Log.d(TAG,"id"+" \t "+"serial"+" \t "+"activo"+" \t "+"fechacreado"+" \t\t "+"lugar"+" \t"+"online");
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
                String online=c.getString(c.getColumnIndex("online"));

                lugares[i]=lugar;
                Log.d(TAG,id+" \t "+serial+" \t "+activo+" \t "+fechacreado+" \t "+lugar+" \t "+online);

//                ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(i).findViewById(R.id.tgl_status);
//                ImageView imgv=(ImageView)controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
//                tglbtn.setVisibility(View.INVISIBLE);
//                imgv.setImageResource(R.drawable.nowifi);
                i++;
            }while(c.moveToNext());
        }

        myDB.close();
        startTimer();





        //prescalerCounter=4;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("nelson");
        options.setPassword("!irfz44n".toCharArray());
        final String clientId = MqttClient.generateClientId();//138.197.20.62
        client =new MqttAndroidClient(this.getApplicationContext(), "tcp://138.197.20.62:1883",clientId);
        Log.d(TAG,"clientId: "+clientId);
        try {
            IMqttToken token = client.connect(options);
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
                    Toast.makeText(getApplicationContext(),"Problema conectando a servidor!!!",Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onFailure: Something went wrong e.g. connection timeout or firewall problems");

                }
            });


            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG,"***************************************************************************");
                    Log.d(TAG,"Llego topic de: "+topic);
                    int startView=controlslv.getFirstVisiblePosition();
                    int endView=controlslv.getLastVisiblePosition();
                    Log.d(TAG,"controlslv.getCount(): "+controlslv.getCount());
                    Log.d(TAG,"controlslv.getFirstVisiblePosition(): "+startView);
                    Log.d(TAG,"controlslv.getLastVisiblePosition(): "+endView);
                    String payload;

                    String serialIncoming=null;
                    int ncontroles=0;
                    payload=new String(message.getPayload());
                    //Log.d(TAG,"on messageArrived: "+payload);
                    JSONObject jsonObject = new JSONObject(payload);
                    serialIncoming=jsonObject.getString("id");


                    Log.d(TAG,"serialIncoming: "+serialIncoming);
                    boolean estado=false;
                    String state=jsonObject.getString("state");
                    if (state.equals("1")){
                        estado=true;
                    }else {
                        estado=false;
                    }
                    int quality=jsonObject.getInt("quality");
                    Log.d(TAG,"state: "+state);
                    Log.d(TAG,"Guardando time de arrivo...");
                    String datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);



//                    String[] columns=c.getColumnNames();
//                    for (int i=0;i<columns.length;i++){
//                        Log.d(TAG,columns[i]);
//                    }


                    ContentValues data=new ContentValues();
                    data.put("ultimaConexion",datetime);
                    data.put("signal",quality);
                    data.put("state",estado);
                    myDB.update("controles",data,"serial='"+serialIncoming+"'",null);
                    myDB.close();
                    //Log.d(TAG,jsonObject.toString());
                    //Log.d(TAG,"id: "+jsonObject.getString("id"));

                    //Log.d(TAG,"rssi: "+jsonObject.getInt("rssi"));
//                    ncontroles=controlslv.getCount();
//                    //Log.d(TAG,"controlslv.getCount(): "+ncontroles);
//                    for (int i=0;i<ncontroles;i++){
//                        TextView txtV=(TextView)controlslv.getChildAt(i).findViewById(R.id.tv_item);
//                        //Log.d(TAG,txtV.getText().toString());
//                    }
                    Log.d(TAG,"Configurando estados de rows.....");
                    datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date dateNow;
                    //Log.d(TAG,"Datetime: "+datetime);
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    c.moveToFirst();
                    int i=0;
                    int j=0;
                    long diffMinutes=59;
                    long diffHours=24;
                    long diffSeconds=59;
                    if (c != null && c.getCount()>0) {
                        do {
                            //if (j>=startView && j<=endView){
                            //Log.d(TAG,"getCount(): "+c.getCount());
                            String id=c.getString(c.getColumnIndex("id"));
                            String serial=c.getString(c.getColumnIndex("serial"));
                            String lugar=c.getString(c.getColumnIndex("lugar"));
                            String ultimaconexion=c.getString(c.getColumnIndex("ultimaConexion"));
                            //Log.d(TAG,"Lugar: "+lugar);
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
                            ContentValues cv = new ContentValues();
                            cv.put("online",online);
                            myDB.update("controles", cv, "serial='"+serial+"'", null);
                            i++;
                        }while(c.moveToNext());
                    }



                    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    c.moveToFirst();
                    i=0;
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
                    Log.d(TAG,"I row: "+i);
                    if (i>=startView && i<=endView){
                        Log.d(TAG,"Control en posicion: "+i);

                        //Log.d(TAG,"RSSI: "+RSSI);

                        Log.d(TAG,"quality: "+quality);
                        ImageView imageView = (ImageView) controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
                        int RSSI=jsonObject.getInt("rssi");
                        if (quality>75){
                            imageView.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_64dp);
                        }else if (quality>50){
                            imageView.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_64dp);
                        }else if (quality>25){
                            imageView.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_64dp);
                        }else if (quality>10){
                            imageView.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_64dp);
                        }else if (quality<=10){
                            imageView.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_64dp);
                        }
                        ToggleButton tglBtn=(ToggleButton)controlslv.getChildAt(i).findViewById(R.id.tgl_status);
                        if (state.equals("1")){
                            //Log.d(TAG,"poniendo state en 1");
                            if (!tglBtn.isChecked()){
                                tglBtn.setChecked(true);
                            }
                        }else {
                            //Log.d(TAG,"poniendo state en 0");
                            if (tglBtn.isChecked()){
                                tglBtn.setChecked(false);
                            }
                        }
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

        controlslv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                stateScrollView=i;
                Log.d(TAG,"onScrollStateChanged: "+i);
                //int startView=controlslv.getFirstVisiblePosition();
                //int endView=controlslv.getLastVisiblePosition();
                //Log.d(TAG,"controlslv.getCount(): "+controlslv.getCount());
                //Log.d(TAG,"controlslv.getFirstVisiblePosition(): "+startView);
                //Log.d(TAG,"controlslv.getLastVisiblePosition(): "+endView);
                if (i==SCROLL_STATE_FLING){
                    Log.d(TAG,"SCROLL_STATE_FLING");
                }else if (i==SCROLL_STATE_IDLE){
                    Log.d(TAG,"SCROLL_STATE_IDLE");
                }else if (i==SCROLL_STATE_TOUCH_SCROLL) {
                    Log.d(TAG,"SCROLL_STATE_TOUCH_SCROLL");
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                Log.d(TAG,"onScroll: "+i+","+i1+","+i2);
                int startView=controlslv.getFirstVisiblePosition();
                int endView=controlslv.getLastVisiblePosition();
                Log.d(TAG,"controlslv.getCount(): "+controlslv.getCount());
                Log.d(TAG,"controlslv.getFirstVisiblePosition(): "+startView);
                Log.d(TAG,"controlslv.getLastVisiblePosition(): "+endView);
                myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                c = myDB.rawQuery("SELECT * FROM controles", null);
                c.moveToFirst();
                i=0;
                int j=0;
                if (c != null && c.getCount()>0) {
                    do {
                        j=i-startView;
                        if (i>=startView && i<=endView){
                            String online=c.getString(c.getColumnIndex("online"));
                            int quality=c.getInt(c.getColumnIndex("signal"));
                            //Log.d(TAG,"i: "+i+" online: "+online);
                            if (controlslv.getChildAt(j)!=null){
                                ImageView imgv=(ImageView)controlslv.getChildAt(j).findViewById(R.id.imageViewRssi);
                                ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(j).findViewById(R.id.tgl_status);
                                if (online.equals("0")){
                                    //tglbtn.setImageResource(R.drawable.wifi3);

                                    tglbtn.setVisibility(View.INVISIBLE);
                                    imgv.setImageResource(R.drawable.ic_signal_wifi_off_black_64dp);
                                    //tglbtn.setBackgroundDrawable(R.drawable.wifi);
                                }else {
                                    tglbtn.setVisibility(View.VISIBLE);
                                    ImageView imageView = (ImageView) controlslv.getChildAt(j).findViewById(R.id.imageViewRssi);
                                    if (quality>75){
                                        imageView.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_64dp);
                                    }else if (quality>50){
                                        imageView.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_64dp);
                                    }else if (quality>25){
                                        imageView.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_64dp);
                                    }else if (quality>10){
                                        imageView.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_64dp);
                                    }else if (quality<=10){
                                        imageView.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_64dp);
                                    }
                                }

                            }

                        }
                        i++;
                    }while(c.moveToNext());
                }
                myDB.close();

            }
        });


//        myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
//        c = myDB.rawQuery("SELECT * FROM controles", null);
//        c.moveToFirst();
//        if (c.getCount()>0){
//            Log.d(TAG,"Populate array");
//        }
//
//        i=1;
//        if (c != null && c.getCount()>0) {
//            do {
//                ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(i).findViewById(R.id.tgl_status);
//                ImageView imgv=(ImageView)controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
//                tglbtn.setVisibility(View.INVISIBLE);
//                imgv.setImageResource(R.drawable.nowifi);
//                i++;
//
//            }while(c.moveToNext());
//        }
//
//        myDB.close();


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"OnResume...");
        if (cTimer!=null){
            cTimer.cancel();
        }

        startTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        //getMenuInflater().inflate(R.menu.menu_main,menu);

        //return super.onCreateOptionsMenu(menu);
        return true;
    }

    private void publish(String serial, boolean state) {
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
                if (++prescalerCounter>5 && stateScrollView==SCROLL_STATE_IDLE){

                    prescalerCounter=0;
                    Log.d(TAG,"**************************************************************************************************************************************");
                    Log.d(TAG,"Timer....");
                    //Log.d(TAG,"Timer: onTick");
                    //Log.d(TAG,"Leyendo ultimos dates de conexion....");
                    String datetime= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date dateNow;
                    //Log.d(TAG,"Datetime: "+datetime);
                    myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
                    c = myDB.rawQuery("SELECT * FROM controles", null);
                    c.moveToFirst();
                    int i=0;
                    int j=0;
                    long diffMinutes=59;
                    long diffHours=24;
                    long diffSeconds=59;
                    if (c != null && c.getCount()>0) {
                        do {
                            //if (j>=startView && j<=endView){
                                //Log.d(TAG,"getCount(): "+c.getCount());
                                String id=c.getString(c.getColumnIndex("id"));
                                String serial=c.getString(c.getColumnIndex("serial"));
                                String lugar=c.getString(c.getColumnIndex("lugar"));
                                String ultimaconexion=c.getString(c.getColumnIndex("ultimaConexion"));
                                //Log.d(TAG,"Lugar: "+lugar);
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
                                ContentValues cv = new ContentValues();
                                cv.put("online",online);
                                myDB.update("controles", cv, "serial='"+serial+"'", null);

                                Log.d(TAG,"id: "+id+" \tLugar: "+lugar+" \tSerial: "+serial+" \tultimaConexion: "+ultimaconexion+" \tdiffMinutes: "+diffMinutes+" \t\tdiffHours: "+diffHours+" \tdiffSeconds: "+diffSeconds+" \tonline: "+online);


//                                //myDB = openOrCreateDatabase("controlesDB", MODE_PRIVATE, null);
//                                Cursor cc = myDB.rawQuery("SELECT * FROM controles", null);
//                                cc.moveToFirst();
//                                i=0;
//                                if (cc != null && cc.getCount()>0) {
//                                    do {
//                                        String serialr=cc.getString(cc.getColumnIndex("serial"));
//                                        if (serialr.equals(serial)){
//                                            break;
//                                        }
//                                        i++;
//                                    }while(cc.moveToNext());
//
//                                }
//                                i=i-startView;
//                                Log.d(TAG,"i: "+i);
//                                if (controlslv.getChildAt(i)!=null){
//                                    ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(i).findViewById(R.id.tgl_status);
//                                    ImageView imgv=(ImageView)controlslv.getChildAt(i).findViewById(R.id.imageViewRssi);
//                                    if (!online){
//                                        //tglbtn.setImageResource(R.drawable.wifi3);
//
//                                        tglbtn.setVisibility(View.INVISIBLE);
//                                        imgv.setImageResource(R.drawable.ic_signal_wifi_off_black_64dp);
//                                        //tglbtn.setBackgroundDrawable(R.drawable.wifi);
//                                    }else {
//                                        tglbtn.setVisibility(View.VISIBLE);
//                                    }
//
//                                }else {
//                                    Log.d(TAG,"i null: "+i);
//                                }
                            //}
                            j++;
                        }while(c.moveToNext());
                        //Log.d(TAG,"Salio...");
                        int startView=controlslv.getFirstVisiblePosition();
                        int endView=controlslv.getLastVisiblePosition();
                        Log.d(TAG,"controlslv.getCount(): "+controlslv.getCount());
                        Log.d(TAG,"controlslv.getFirstVisiblePosition(): "+startView);
                        Log.d(TAG,"controlslv.getLastVisiblePosition(): "+endView);
                        c = myDB.rawQuery("SELECT * FROM controles", null);
                        c.moveToFirst();
                        i=0;
                        if (c != null && c.getCount()>0) {
                            do {
                                j=i-startView;
                                if (i>=startView && i<=endView){
                                    String online=c.getString(c.getColumnIndex("online"));
                                    //Log.d(TAG,"i: "+i+" online: "+online);
                                    if (controlslv.getChildAt(j)!=null){
                                        ImageView imgv=(ImageView)controlslv.getChildAt(j).findViewById(R.id.imageViewRssi);
                                        ToggleButton tglbtn = (ToggleButton) controlslv.getChildAt(j).findViewById(R.id.tgl_status);
                                        if (online.equals("0")){
                                            //tglbtn.setImageResource(R.drawable.wifi3);

                                            tglbtn.setVisibility(View.INVISIBLE);
                                            imgv.setImageResource(R.drawable.ic_signal_wifi_off_black_64dp);
                                            //tglbtn.setBackgroundDrawable(R.drawable.wifi);
                                        }else {
                                            tglbtn.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }
                                i++;
                            }while(c.moveToNext());
                        }
                    }
                    myDB.close();
                }
            }
            public void onFinish() {
                //Log.d(TAG,"Timer: onFinish");
                cTimer.start();
            }
        };
        cTimer.start();
    }
}
