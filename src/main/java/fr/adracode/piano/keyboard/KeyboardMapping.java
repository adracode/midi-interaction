package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.os.OSKeyboard;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.Optional;

import static fr.adracode.piano.keyboard.KeyboardHand.OCTAVE_WITH_PEDALS;

public class KeyboardMapping extends Mapping<Key> implements HandleKey {

    private final ToggleManager toggleManager;
    private final KeyAction[] activeKey = new KeyAction[OCTAVE_WITH_PEDALS];

    public KeyboardMapping(OSKeyboard keyboard, Long2ObjectMap<Key> mapping, KeyboardHand hand, int[] masks, ToggleManager toggleManager){
        super(keyboard, mapping, hand, masks);
        this.toggleManager = toggleManager;
    }

    @Override
    public void handle(int octave){
        getCurrentKey(octave).ifPresent(key -> toggleManager.performAction(key, action -> {
            if(action.equals(activeKey[octave])){
                pressKeys(action);
            } else {
                pressKeys(action);
                releaseKeys(activeKey[octave]);
                activeKey[octave] = action;
            }
        }));
    }

    private Optional<Key> getCurrentKey(int octave){
        return hand.getHand(octave)
                .flatMap(hand1 -> Optional.ofNullable(mapping.get(Key.from(octave, hand1 & masks[octave]))));
    }
}
