package fr.adracode.piano;

import fr.adracode.piano.common.OneTimeStop;
import org.apache.commons.cli.*;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MidiFilePlayer {
    
    public static void main(String[] args) throws ParseException{
        Options options = new Options();
        
        options.addOption(Option.builder()
                .option("f")
                .longOpt("file")
                .hasArg(true)
                .desc("file to play")
                .build());
        
        options.addOption(Option.builder()
                .option("r")
                .longOpt("replay")
                .hasArg(false)
                .desc("replay file when stopped")
                .build());
        
        options.addOption(Option.builder()
                .option("s")
                .longOpt("start")
                .hasArg(true)
                .desc("start at microsecond")
                .type(Number.class)
                .build());
        
        options.addOption(Option.builder()
                .option("t")
                .longOpt("tempo")
                .hasArg(true)
                .desc("change tempo (<1 slower, >1 faster")
                .type(Number.class)
                .build());
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves its purpose
        
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e){
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            
            System.exit(1);
        }
        
        MidiFilePlayer player = new MidiFilePlayer();
        SignalHandler signalHandler = new SignalHandler() {
            public void handle(Signal signal){
                player.stop();
                System.exit(0);
            }
        };
        
        Signal.handle(new Signal("INT"), signalHandler);
    
        if(cmd.hasOption("file")){
            new MidiHandler().handle(player::reRun);
            player.readMidiFile(cmd.getOptionValue("file"),
                    cmd.hasOption("replay"),
                    cmd.hasOption("start") ? ((Number)(cmd.getParsedOptionValue("start"))).longValue() : 0,
                    cmd.hasOption("tempo") ? ((Number)(cmd.getParsedOptionValue("tempo"))).floatValue() : 1.0F);
        } else {
        }
    }
    
    private boolean reRun = false;
    private boolean stop = false;
    private Sequencer sequencer;
    private List<MidiDevice> devices = new ArrayList<>();
    
    public void reRun(){
        this.reRun = true;
    }
    
    public void stop(){
        this.stop = true;
        if(sequencer == null){
            return;
        }
        sequencer.stop();
        sequencer.close();
        for(MidiDevice device : devices){
            device.close();
        }
    }
    
    public void readMidiFile(String file, boolean replay, long start, float tempoFactor){
        try {
            Map<String, List<MidiDevice>> midiInterface = getMidiDeviceInterfaces();
            devices.addAll(midiInterface.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
            MidiDevice out = midiInterface.get("out").get(0);
            out.open();
            
            //sendMidiEventsFromFile(out.getReceiver(), "setSnow.mid");
            
            Sequence sequence = MidiSystem.getSequence(new File(file));
            
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.getTransmitter().setReceiver(new Receiver() {
                public void send(MidiMessage message, long timeStamp){
                    try {
                        out.getReceiver().send(message, timeStamp);
                        if(message instanceof ShortMessage){
                            ShortMessage secondVoice = (ShortMessage)message;
                            if(isMidiNoteOnOffMessage(secondVoice)){
                                secondVoice.setMessage(secondVoice.getCommand(), secondVoice.getChannel() + 1, secondVoice.getData1(), secondVoice.getData2());
                                out.getReceiver().send(secondVoice, timeStamp);
                            }
                        }
                    } catch(MidiUnavailableException | InvalidMidiDataException e){
                        e.printStackTrace();
                    }
                }
                
                public void close(){
                }
            });
            sequencer.setSequence(sequence);
            sequencer.setTempoFactor(tempoFactor);
            do {
                if(stop){
                    return;
                }
                sequencer.start();
                sequencer.setMicrosecondPosition(start);
                System.out.println("Lecture en cours... Appuyez sur Entrée pour arrêter / recommencer la lecture ou CTRL-C pour stopper.");
                try(OneTimeStop ignored1 = new OneTimeStop()
                        .setRunningCondition(() -> sequencer.isRunning() && !this.stop && !this.reRun)
                        .setStopOnInteraction(bufferedReader -> {
                            if(bufferedReader.ready()){
                                bufferedReader.readLine();
                                return true;
                            }
                            return false;
                        })
                        .block()) {
                } catch(Exception ignored){
                }
                
                if(this.reRun){
                    this.reRun = false;
                }
                
                // Fermer le lecteur MIDI et l'envoi MIDI
            } while(replay);
            this.stop();
            
        } catch(InvalidMidiDataException | IOException | MidiUnavailableException e){
            e.printStackTrace();
        }
    }
    
    public static boolean isMidiNoteOnOffMessage(ShortMessage message){
        int command = message.getCommand();
        // Vérifier si la commande est "note on" (9) ou "note off" (8)
        return command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF;
    }
    
    public static Map<String, List<MidiDevice>> getMidiDeviceInterfaces(){
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
    
    public static void sendMidiEventsFromFile(Receiver receiver, String filePath){
        try {
            Sequence sequence = MidiSystem.getSequence(new File(filePath));
            Track track = sequence.getTracks()[0];
            for(int i = 0; i < track.size(); i++){
                receiver.send(track.get(i).getMessage(), track.get(i).getTick());
            }
        } catch(InvalidMidiDataException | IOException e){
            e.printStackTrace();
        }
    }
    
    public static void sendMidiDataWithProgramChange(Receiver receiver, int instrument) throws InvalidMidiDataException{
        //receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 0, 123), -1);
        //receiver.send(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 19, 0), -1);
    }
    
    public static void recordMidiFromDevice(String file){
        try {
            MidiDevice device = getMidiDeviceInterfaces().get("in").get(0);
            Sequencer sequencer = MidiSystem.getSequencer();
            
            device.open();
            sequencer.open();
            
            device.getTransmitter().setReceiver(sequencer.getReceiver());
            
            Sequence sequence = new Sequence(Sequence.PPQ, 24);
            Track track = sequence.createTrack();
            
            sequencer.setSequence(sequence);
            sequencer.setTickPosition(0);
            sequencer.recordEnable(track, -1);
            sequencer.startRecording();
            
            System.out.println("Enregistrement en cours... Appuyez sur Entrée pour arrêter l'enregistrement.");
            System.in.read();
            
            sequencer.stopRecording();
            Sequence seq = sequencer.getSequence();
            
            MidiSystem.write(seq, 0, new File(file));
            System.out.println("Enregistrement terminé.");
            
            device.close();
            sequencer.close();
        } catch(MidiUnavailableException | IOException | InvalidMidiDataException e){
            e.printStackTrace();
        }
    }
    
}
