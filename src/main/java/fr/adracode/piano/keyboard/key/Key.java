package fr.adracode.piano.keyboard.key;

import java.awt.event.KeyEvent;
import java.util.Optional;

public record Key(int octave, int tone) {

    public static long from(int octave, int tone){
        return ((long)octave << 32) | tone;
    }

    public static Key to(long key){
        return new Key((int)(key >> 32), (int)(key));
    }

    public static Optional<Integer> getKeyCode(String key){
        try {
            return Optional.of((int)KeyEvent.class.getField("VK_" + key).get(null));
        } catch(NoSuchFieldException | IllegalAccessException e){
            return Optional.empty();
        }
    }

}
