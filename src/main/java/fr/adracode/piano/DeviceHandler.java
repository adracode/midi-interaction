package fr.adracode.piano;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DeviceHandler {
    
    public Get getMidiDeviceInterfaces(){
        return getMidiDeviceInterfaces(device -> true);
    }
    
    public Get getMidiDeviceInterfaces(String name){
        return getMidiDeviceInterfaces(device -> device.getName().equals(name));
    }
    
    public Get getMidiDeviceInterfaces(Predicate<MidiDevice.Info> filter){
        List<MidiDevice> in = new ArrayList<>();
        List<MidiDevice> out = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for(MidiDevice.Info info : infos){
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if(!filter.test(device.getDeviceInfo())){
                    continue;
                }
                if(device.getMaxTransmitters() != 0){
                    if(!device.getClass().toString().equals("class com.sun.media.sound.MidiInDevice")){
                        continue;
                    }
                    in.add(device);
                }
                if(device.getMaxReceivers() != 0){
                    if(!device.getClass().toString().equals("class com.sun.media.sound.MidiOutDevice")){
                        continue;
                    }
                    out.add(device);
                }
            } catch(MidiUnavailableException e){
                // Ignorer les périphériques MIDI non disponibles
            }
        }
        return new Get(in, out);
    }
    
    public record Get(List<MidiDevice> in, List<MidiDevice> out) {
        
        public Stream<MidiDevice> stream(){
            if(in.isEmpty() && out.isEmpty()){
                return Stream.empty();
            }
            if(in.isEmpty()){
                return out.stream();
            }
            if(out.isEmpty()){
                return in.stream();
            }
            return Stream.concat(in.stream(), out.stream());
        }
        
        public void forEach(Consumer<MidiDevice> consumer){
            in.forEach(consumer);
            out.forEach(consumer);
        }
        
        public Optional<MidiDevice> firstIn(){
            return in.isEmpty() ? Optional.empty() : Optional.of(in.get(0));
        }
        
        public Optional<MidiDevice> firstOut(){
            return out.isEmpty() ? Optional.empty() : Optional.of(out.get(0));
        }

    }

}
