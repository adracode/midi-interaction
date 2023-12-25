package fr.adracode.piano.keyboard;

import java.util.Optional;
import java.util.stream.IntStream;

public class KeyboardHand {

    public static final int OCTAVE = 11;
    public static final int OCTAVE_WITH_PEDALS = OCTAVE + 1;
    public static final int TONE = 12;

    private final KeyDecoder[] decoders = new KeyDecoder[OCTAVE_WITH_PEDALS];

    public KeyboardHand(int sustainAfter){
        IntStream.range(0, OCTAVE_WITH_PEDALS).forEach(i -> decoders[i] = new KeyDecoder(sustainAfter));
    }

    public void registerKey(int octave, int tone){
        if(notInBound(octave)){
            return;
        }
        decoders[octave].registerKey(tone);
    }

    public void unregisterKey(int octave, int tone){
        if(notInBound(octave)){
            return;
        }
        decoders[octave].unregisterKey(tone);
    }

    public Optional<Integer> getHand(int octave){
        if(isEmpty(octave)){
            return Optional.empty();
        }
        return Optional.of(decoders[octave].getHand());
    }

    public boolean isEmpty(int octave){
        if(notInBound(octave)){
            return true;
        }
        return !decoders[octave].hasKey();
    }

    private boolean notInBound(int octave){
        return octave < 0 || octave >= OCTAVE_WITH_PEDALS;
    }

}
