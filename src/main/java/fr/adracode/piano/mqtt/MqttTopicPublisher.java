package fr.adracode.piano.mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.Closeable;
import java.io.IOException;

public class MqttTopicPublisher implements Closeable {

    private final MqttPublisher publisher;
    private final String topic;

    public MqttTopicPublisher(MqttPublisher publisher, String topic){
        this.publisher = publisher;
        this.topic = topic;
    }

    public void publish(String content) throws MqttException{
        publisher.publish(topic, content);
    }

    @Override
    public void close() throws IOException{
        publisher.close();
    }
}
