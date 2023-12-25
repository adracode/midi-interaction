package fr.adracode.piano.keyboard.os;

import it.unimi.dsi.fastutil.ints.IntCollection;

public interface OSKeyboard {

    void pressKey(int code);

    void releaseKey(int code);

    void sendUnicode(int unicode);

    void pressKeys(IntCollection codes);

    void releaseKeys(IntCollection codes);

    void sendUnicode(String unicode);

}
