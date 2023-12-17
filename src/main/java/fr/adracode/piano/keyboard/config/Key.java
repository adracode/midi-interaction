package fr.adracode.piano.keyboard.config;

public record Key(int octave, int tone) {

    public static long from(int octave, int tone){
        return ((long)octave << 32) | tone;
    }

    public static Key to(long key){
        return new Key((int)(key >> 32), (int)(key));
    }

}
