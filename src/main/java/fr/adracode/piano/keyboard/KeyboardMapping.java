package fr.adracode.piano.keyboard;

import com.spencerwi.either.Either;
import fr.adracode.piano.keyboard.config.MappingConfig;
import org.apache.commons.cli.ParseException;

import java.util.Optional;

public class KeyboardMapping {

    public static final int OCTAVE = 11;
    public static final int TONE = 12;

    private final MappingConfig mapping;
    private final int[] hand = new int[OCTAVE];

    public KeyboardMapping(String mappingFile) throws ParseException{
        mapping = new MappingConfig(mappingFile);
    }

    public Either<Integer, String> getKey(int data){
        int octave = data / TONE;
        hand[octave] |= switch(data - octave * TONE){
            case 0 -> 0b100000000000;
            case 1 -> 0b010000000000;
            case 2 -> 0b001000000000;
            case 3 -> 0b000100000000;
            case 4 -> 0b000010000000;
            case 5 -> 0b000001000000;
            case 6 -> 0b000000100000;
            case 7 -> 0b000000010000;
            case 8 -> 0b000000001000;
            case 9 -> 0b000000000100;
            case 10 -> 0b000000000010;
            case 11 -> 0b000000000001;
            default -> 0;
        };
        return Either.either(
                () -> mapping.getSingle(octave, hand[octave]).orElse(null),
                () -> mapping.getMulti(octave, hand[octave]).orElse(null));
    }

    public void reset(int index){
        hand[index] = 0;
    }

    public Optional<Integer> getCurrentKey(int index){
        return mapping.getSingle(index, hand[index]);
    }
}
