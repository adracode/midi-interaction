package fr.adracode.piano;

import fr.adracode.piano.common.OneTimeStop;
import fr.adracode.piano.playlist.Playlist;
import fr.adracode.piano.playlist.PlaylistBuilder;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiFilePlayer {
    
    private boolean reRun = false;
    private boolean stop = false;
    private Sequencer sequencer;
    private Playlist playlist;

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
    }
    
    public void readMidiFile(MidiDevice device, String[] files, boolean replay, long start, float tempoFactor){
        this.playlist = new PlaylistBuilder()
                .filenames(files)
                .loop(replay)
                .build();

        try {
            //sendMidiEventsFromFile(device.getReceiver(), "setSnow.mid");

            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.getTransmitter().setReceiver(new Receiver() {
                public void send(MidiMessage message, long timeStamp){
                    try {
                        device.getReceiver().send(message, timeStamp);
                        if(message instanceof ShortMessage secondVoice){
                            if(isMidiNoteOnOffMessage(secondVoice)){
                                secondVoice.setMessage(secondVoice.getCommand(), secondVoice.getChannel() + 1, secondVoice.getData1(), secondVoice.getData2());
                                device.getReceiver().send(secondVoice, timeStamp);
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
