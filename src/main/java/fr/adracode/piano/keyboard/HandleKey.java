package fr.adracode.piano.keyboard;

@FunctionalInterface
public interface HandleKey {

    void handle(int octave);

}
