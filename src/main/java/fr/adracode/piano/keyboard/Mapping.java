package fr.adracode.piano.keyboard;


import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.os.OSKeyboard;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

public class Mapping<K extends Key> {

    protected final OSKeyboard keyboard;
    protected final KeyboardHand hand;
    protected final Long2ObjectMap<K> mapping;
    protected final int[] masks;

    public Mapping(OSKeyboard keyboard, Long2ObjectMap<K> mapping, KeyboardHand hand, int[] masks){
        this.keyboard = keyboard;
        this.hand = hand;
        this.mapping = mapping;
        this.masks = masks;
    }

    public KeyboardHand getHand(){
        return hand;
    }

    protected void pressKeys(KeyAction action){
        if(action == null){
            return;
        }
        action.getKeys().ifPresent(keyboard::pressKeys);
        action.getUnicode().ifPresent(keyboard::sendUnicode);
    }

    protected void releaseKeys(KeyAction action){
        if(action == null){
            return;
        }
        action.getKeys().ifPresent(keyboard::releaseKeys);
    }
}
