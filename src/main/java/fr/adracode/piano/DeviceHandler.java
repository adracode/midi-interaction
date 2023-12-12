package fr.adracode.piano;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceHandler {

    public Map<String, List<MidiDevice>> getMidiDeviceInterfaces(){
        Map<String, List<MidiDevice>> interfaces = new HashMap<>();
        interfaces.put("in", new ArrayList<>());
        interfaces.put("out", new ArrayList<>());
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for(MidiDevice.Info info : infos){
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if(device.getMaxTransmitters() != 0){
                    if(!device.getClass().toString().equals("class com.sun.media.sound.MidiInDevice")){
                        continue;
                    }
                    interfaces.get("in").add(device);
                }
                if(device.getMaxReceivers() != 0){
                    if(!device.getClass().toString().equals("class com.sun.media.sound.MidiOutDevice")){
                        continue;
                    }
                    interfaces.get("out").add(device);
                }
            } catch(MidiUnavailableException e){
                // Ignorer les périphériques MIDI non disponibles
            }
        }
        return interfaces;
    }

}
