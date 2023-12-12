package fr.adracode.piano;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiRecorder {

    private final DeviceHandler deviceHandler;

    public MidiRecorder(DeviceHandler deviceHandler){
        this.deviceHandler = deviceHandler;
    }

    public void record(File output){
        try {
            MidiDevice device = deviceHandler.getMidiDeviceInterfaces().get("in").get(0);
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

            MidiSystem.write(seq, 0, output);
            System.out.println("Enregistrement terminé.");

            device.close();
            sequencer.close();
        } catch(MidiUnavailableException | IOException | InvalidMidiDataException e){
            e.printStackTrace();
        }
    }
}
