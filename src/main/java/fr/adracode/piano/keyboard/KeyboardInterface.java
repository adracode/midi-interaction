package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.Pedal;
import fr.adracode.piano.keyboard.os.OSKeyboard;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.sound.midi.ShortMessage;
import java.io.IOException;

import static fr.adracode.piano.keyboard.KeyboardHand.TONE;

public class KeyboardInterface implements MqttCallback {

    private final KeyboardSimulator keyboardSimulator;

    public KeyboardInterface(String mappingFile, OSKeyboard keyboard) throws IOException{
        keyboardSimulator = new KeyboardSimulator(mappingFile, keyboard);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message){
        String rawMessage = message.toString();
        String[] parts = rawMessage.split(",");
        int command = Integer.parseInt(parts[0].trim());
        int data1 = Integer.parseInt(parts[1].trim());
        int data2 = Integer.parseInt(parts[2].trim());
        switch(command){
            case ShortMessage.NOTE_ON -> {
                if(data2 != 0){
                    keyboardSimulator.handleKeyPressed(data1, data2);
                } else {
                    keyboardSimulator.handleKeyReleased(data1);
                }
            }
            case ShortMessage.CONTROL_CHANGE -> {
                if(data1 == 64){
                    Pedal sustain = Pedal.init("sustain");
                    if(sustain.isToggleMode() || data2 > 0){
                        Key.weightedBinaryStream(sustain.getFakeKey().getTone())
                                .filter(wBit -> wBit > 0)
                                .forEach(wBit -> keyboardSimulator.handleKeyPressed(
                                        sustain.getFakeKey().getOctave() * TONE + TONE - Integer.numberOfTrailingZeros(wBit) - 1,
                                        127
                                ));
                    } else if(!sustain.isToggleMode() && data2 == 0){
                        Key.weightedBinaryStream(sustain.getFakeKey().getTone())
                                .filter(wBit -> wBit > 0)
                                .forEach(wBit -> keyboardSimulator.handleKeyReleased(
                                        sustain.getFakeKey().getOctave() * TONE + TONE - Integer.numberOfTrailingZeros(wBit) - 1));
                    }

                }
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause){
        cause.printStackTrace();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
    }
}
