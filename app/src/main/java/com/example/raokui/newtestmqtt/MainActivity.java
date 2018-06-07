package com.example.raokui.newtestmqtt;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQQQQ";
    int i = 0;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    Toast.makeText(MainActivity.this, (String) msg.obj,
                            Toast.LENGTH_SHORT).show();

//                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                    Notification notification = new Notification(R.drawable.icon, "Mqtt即时推送", System.currentTimeMillis());
//                    notification.contentView = new RemoteViews("com.hxht.testmqttclient", R.layout.activity_notification);
//                    notification.contentView.setTextViewText(R.id.tv_desc, (String) msg.obj);
//                    notification.defaults = Notification.DEFAULT_SOUND;
//                    notification.flags = Notification.FLAG_AUTO_CANCEL;
//                    manager.notify(i++, notification);

                } else if (msg.what == 2) {
                    Log.d(TAG, "handleMessage: 连接成功");
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    try {
                        mqttClient.subscribe("myTopic", 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (msg.what == 3) {
                    Toast.makeText(MainActivity.this, "连接失败，系统正在重连", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "handleMessage: 连接失败，系统正在重连");
                }
            }
        };

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mqttClient.isConnected()) {
                    try {
                        mqttClient.publish("myTopic", "我是客户段1".getBytes(), 1, true);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        startc();
    }

    private ScheduledExecutorService scheduler;

    private void startc() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                if (!mqttClient.isConnected()) {
                    connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    private void connect() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mqttClient.connect(options);
                    Message msg = new Message();
                    msg.what = 2;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    private MqttClient mqttClient;
    private MqttConnectOptions options;

    private void init() {
        try {
            mqttClient = new MqttClient("tcp://192.168.0.48:61613", "clientId1", new MemoryPersistence());
            options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName("admin");
            options.setPassword("password".toCharArray());

            options.setConnectionTimeout(10);

            options.setKeepAliveInterval(20);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "connectionLost: 重连");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = topic + "---" + message.toString();
                    handler.sendMessage(msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "messageArrived: 推送后" + token.isComplete());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
