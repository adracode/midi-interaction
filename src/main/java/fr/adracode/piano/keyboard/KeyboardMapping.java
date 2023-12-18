package fr.adracode.piano.keyboard;

import com.spencerwi.either.Either;
import fr.adracode.piano.keyboard.config.Mapping;
import fr.adracode.piano.keyboard.key.KeyAction;

import java.util.Optional;

public class KeyboardMapping {

    public static final int OCTAVE = 11;
    public static final int TONE = 12;

    private final Mapping mapping;
    private final int[] hand = new int[OCTAVE];

    public KeyboardMapping(Mapping mappingConfig){
        this.mapping = mappingConfig;
    }

    public void registerKey(int data){
        int octave = data / TONE;
        //TODO: simplify (TONE - (data - octave * TONE) ...)
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
    }

    public void reset(int index){
        hand[index] = 0;
    }

    public Optional<Either<Integer, KeyAction>> getCurrentKey(int index){
        System.out.println(hand[index]);
        Either<Integer, KeyAction> result = Either.either(
                () -> mapping.getSingle(index, hand[index]).orElse(null),
                () -> mapping.getMulti(index, hand[index]).orElse(null));
        Optional<Either<Integer, KeyAction>> optionalResult = result.isLeft() && result.getLeft() == null || result.isRight() && result.getRight() == null ?
                Optional.empty() :
                Optional.of(result);

        return optionalResult;
    }
}
