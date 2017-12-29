package com.roogle.stetho.mqtt;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.lazy.library.logging.Logcat;
import com.roogle.simple.stetho.SimpleStetho;
import com.roogle.simple.stetho.common.Consumer;
import com.roogle.simple.stetho.common.ZipUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public final class StethoMqttClient {
    private static final String TAG = "StethoMqttClient";
    private MqttAndroidClient mqttAndroidClient;

    private final SimpleStetho simpleStetho;
    private final String serverUri;
    private final String clientId;
    private final String subscriptionTopic;
    private final String publishTopic;
    private final String userName;
    private final String password;
    private boolean isUsingZip = false;

    public StethoMqttClient(@NonNull final SimpleStetho simpleStetho, @NonNull String serverUri, @NonNull String clientId,
                            @NonNull String subscriptionTopic, @NonNull String publishTopic) {
        this.simpleStetho = simpleStetho;
        this.serverUri = serverUri;
        this.clientId = clientId;
        this.subscriptionTopic = subscriptionTopic;
        this.publishTopic = publishTopic;
        this.userName = null;
        this.password = null;
    }

    public StethoMqttClient(@NonNull final SimpleStetho simpleStetho, @NonNull String serverUri, @NonNull String clientId,
                            @NonNull String subscriptionTopic, @NonNull String publishTopic,
                            @NonNull String userName, @NonNull String password) {
        this.simpleStetho = simpleStetho;
        this.serverUri = serverUri;
        this.clientId = clientId;
        this.subscriptionTopic = subscriptionTopic;
        this.publishTopic = publishTopic;
        this.userName = userName;
        this.password = password;
    }

    public void connect() {
        mqttAndroidClient = new MqttAndroidClient(simpleStetho.getContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Logcat.i(TAG, "Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    Logcat.i(TAG, "Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Logcat.i(TAG, "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Logcat.i(TAG, "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        if (userName != null) {
            mqttConnectOptions.setUserName(userName);
        }
        if (password != null) {
            mqttConnectOptions.setPassword(password.toCharArray());
        }

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Logcat.i(TAG, "Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }

    public void disconnect() {
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToTopic() {
        try {
            mqttAndroidClient.unsubscribe(subscriptionTopic);
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Logcat.i(TAG, "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Logcat.i(TAG, "Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    String messageString = new String(message.getPayload()).trim();
                    Logcat.i(TAG, "Message: " + topic + " : " + messageString);

                    if (TextUtils.isEmpty(messageString)) {
                        return;
                    }
                    if (isUsingZip) {
                        messageString = ZipUtil.unCompressString(messageString);
                    }
                    simpleStetho.executeCommand(messageString, new Consumer<String>() {
                        @Override
                        public void apply(String msg) {
                            if (!TextUtils.isEmpty(msg)) {
                                publishMessage(msg);
                            }
                        }
                    });
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void publishMessage(String publishMessage) {
        if (TextUtils.isEmpty(publishMessage)) {
            return;
        }
        try {
            if (isUsingZip) {
                publishMessage = ZipUtil.compressString(publishMessage);
            }
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUsingZip(boolean usingZip) {
        isUsingZip = usingZip;
    }
}
