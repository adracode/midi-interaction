package fr.adracode.piano;

import javax.sound.midi.*;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MidiReader implements Closeable, Runnable {

    private final DeviceHandler deviceHandler;
    private final BiConsumer<MidiMessage, Long> onMessage;
    private MidiDevice device;

    public MidiReader(DeviceHandler deviceHandler, BiConsumer<MidiMessage, Long> onMessage){
        this.deviceHandler = deviceHandler;
        this.onMessage = onMessage;
    }

    public void run(){
        Map<String, List<MidiDevice>> devices = deviceHandler.getMidiDeviceInterfaces();
        try {
            device = devices.get("in").get(0);
            device.open();
            Receiver receiver = new Receiver() {
                public void send(MidiMessage message, long timeStamp){
                    onMessage.accept(message, timeStamp);
                }

                public void close(){
                }
            };

            device.getTransmitter().setReceiver(receiver);
        } catch(MidiUnavailableException e){
            System.out.println("Device " + devices + " is not available :(");
            e.printStackTrace();
        }
    }

    @Override
    public void close(){
        device.close();
    }
}
