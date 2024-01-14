package fr.adracode.piano;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.util.function.BiConsumer;

public class MidiReader implements Runnable {

    private final BiConsumer<MidiMessage, Long> onMessage;
    private final MidiDevice device;
    
    public MidiReader(MidiDevice device, BiConsumer<MidiMessage, Long> onMessage){
        this.device = device;
        this.onMessage = onMessage;
    }

    public void run(){
        try {
            Receiver receiver = new Receiver() {
                public void send(MidiMessage message, long timeStamp){
                    onMessage.accept(message, timeStamp);
                }

                public void close(){
                }
            };

            device.getTransmitter().setReceiver(receiver);
        } catch(MidiUnavailableException e){
            System.out.println("Device " + device + " is not available");
            e.printStackTrace();
        }
    }
}
