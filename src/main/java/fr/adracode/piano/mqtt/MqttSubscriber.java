package fr.adracode.piano.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public class MqttSubscriber implements Closeable {

    private final MqttClient client;

    public MqttSubscriber(String broker, int port, String topic, MqttCallback onReceive) throws MqttException{
        this(broker, port, UUID.randomUUID().toString(), topic, onReceive);
    }

    public MqttSubscriber(String host, int port, String clientId, String topic, MqttCallback onReceive) throws MqttException{
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient("tcp://" + host + ":" +port, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        client.connect(connOpts);
        client.setCallback(onReceive);
        client.subscribe(topic);
    }

    @Override
    public void close() throws IOException{
        try {
            client.disconnect();
            client.close();
        } catch(MqttException e){
            throw new RuntimeException(e);
        }
    }
}
