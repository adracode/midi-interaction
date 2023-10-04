package fr.adracode.piano;

import javax.sound.midi.*;
import java.util.List;
import java.util.Map;

public class MidiHandler {
    
    public void handle(Runnable onSustain){
        Map<String, List<MidiDevice>> devices = MidiFilePlayer.getMidiDeviceInterfaces();
        try {
            MidiDevice.Info[] devs = MidiSystem.getMidiDeviceInfo();
            MidiDevice device = devices.get("in").get(0);
            device.open();
            
            Receiver receiver = new Receiver() {
                public void send(MidiMessage message, long timeStamp){
                    if(message instanceof ShortMessage){
                        ShortMessage sm = (ShortMessage)message;
                        if(sm.getCommand() == 176 && sm.getData1() == 64 && sm.getData2() == 0){
                            onSustain.run();
                        }
                    }
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
}