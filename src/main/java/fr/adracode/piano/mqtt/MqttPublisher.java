package fr.adracode.piano.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public class MqttPublisher implements Closeable {

    private final MqttClient client;

    public MqttPublisher(String broker, int port) throws MqttException{
        this(broker, port, UUID.randomUUID().toString());
    }

    public MqttPublisher(String host, int port, String clientId) throws MqttException{
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient("tcp://" + host + ":" +port, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        client.connect(connOpts);
    }

    public void publish(String topic, String content) throws MqttException{
        MqttMessage message = new MqttMessage(content.getBytes());
        client.publish(topic, message);
    }

    @Override
    public void close() throws IOException{
        try {
            client.disconnect();
            client.close();
        } catch(MqttException e){
            throw new IOException(e);
        }
    }
}
