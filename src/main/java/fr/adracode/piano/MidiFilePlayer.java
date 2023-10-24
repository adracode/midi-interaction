package fr.adracode.piano;

import fr.adracode.piano.common.OneTimeStop;
import fr.adracode.piano.playlist.Playlist;
import fr.adracode.piano.playlist.PlaylistBuilder;
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
                .hasArgs()
                .desc("file(s) to play")
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
            player.readMidiFile(cmd.getOptionValues("file"),
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
    private Playlist playlist;
    
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
    
    public void readMidiFile(String[] files, boolean replay, long start, float tempoFactor){
        this.playlist = new PlaylistBuilder()
                .filenames(files)
                .loop(replay)
                .build();
        
        try {
            Map<String, List<MidiDevice>> midiInterface = getMidiDeviceInterfaces();
            devices.addAll(midiInterface.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
            MidiDevice out = midiInterface.get("out").get(0);
            out.open();
            
            //sendMidiEventsFromFile(out.getReceiver(), "setSnow.mid");
            
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
            sequencer.setTempoFactor(tempoFactor);
            
            do {
                if(stop){
                    return;
                }
                File currentlyPlaying = playlist.next();
                
                sequencer.setSequence(MidiSystem.getSequence(currentlyPlaying));
                sequencer.start();
                sequencer.setMicrosecondPosition(start);
                System.out.println("Currently playing " + currentlyPlaying.getName() + ", " +
                        "type Enter to play next music of playlist, or CTRL+C to stop the program.");
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
            } while(playlist.hasNext());
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
