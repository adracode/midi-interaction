package fr.adracode.piano;

import fr.adracode.piano.common.OneTimeStop;
import fr.adracode.piano.playlist.Playlist;
import fr.adracode.piano.playlist.PlaylistBuilder;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MidiFilePlayer {

    private final DeviceHandler deviceHandler;

    private boolean reRun = false;
    private boolean stop = false;
    private Sequencer sequencer;
    private List<MidiDevice> devices = new ArrayList<>();
    private Playlist playlist;

    public MidiFilePlayer(DeviceHandler deviceHandler){
        this.deviceHandler = deviceHandler;
    }

    public void reRun(){
        this.reRun = true;
    }

    public void stop(){
        this.stop = true;
    }

    public void shutdown(){
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
            Map<String, List<MidiDevice>> midiInterface = deviceHandler.getMidiDeviceInterfaces();
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
            } while(playlist.hasNext() && !stop);
            this.shutdown();

        } catch(InvalidMidiDataException | IOException | MidiUnavailableException e){
            e.printStackTrace();
        }
    }

    public static boolean isMidiNoteOnOffMessage(ShortMessage message){
        int command = message.getCommand();
        // Vérifier si la commande est "note on" (9) ou "note off" (8)
        return command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF;
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
}
