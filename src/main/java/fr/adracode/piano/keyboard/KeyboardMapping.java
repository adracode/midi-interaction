package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.config.Mapping;
import fr.adracode.piano.keyboard.config.MappingParser;
import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.key.KeyNode;
import fr.adracode.piano.keyboard.key.ToggleKey;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.Optional;

public class KeyboardMapping {

    public static final int OCTAVE = 11;
    public static final int TONE = 12;

    private final Long2ObjectMap<Mapping<KeyAction>> mapping = new Long2ObjectArrayMap<>();
    private final Mapping<KeyAction> toggleMapping;
    private final int[] hand = new int[OCTAVE];

    private long toggles;

    public KeyboardMapping(MappingParser parser){
        toggleMapping = new Mapping<>(parser.getTreeToggle());
        parser.getToggleCombinations().forEach(combination ->
                mapping.put(combination, new Mapping<>(parser.getTreeWith(
                        ToggleKey.from(combination).stream().map(ToggleKey::getLabel).toList()))));
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

    public boolean toggle(long toggleKey){
        toggles = ToggleKey.toggle(toggles, toggleKey);
        return ToggleKey.isToggleOn(toggles, toggleKey);
    }

    public Optional<KeyAction> getCurrentKey(int octave){
        if(octave < 0 || octave > OCTAVE || hand[octave] == 0){
            return Optional.empty();
        }
        long key = Key.from(octave, hand[octave]);
        return getMulti(toggleMapping, key)
                .or(() -> getMulti(Optional.ofNullable(mapping.get(toggles)).orElse(mapping.get(0)), key)
                        .or(() -> {
                            ToggleKey.Fallback fallback = ToggleKey.getFallback(toggles).orElse(null);
                            if(fallback == null){
                                return Optional.empty();
                            }
                            if(fallback == ToggleKey.Fallback.NORMAL){
                                return getMulti(mapping.get(0), key);
                            }
                            return Optional.empty();
                        }));
    }

    public long currentToggles(){
        return toggles;
    }

    private Optional<KeyAction> getMulti(Mapping<KeyAction> mapping, long key){
        var result = mapping.getNext(key);
        result.flatMap(KeyNode::getValue).ifPresent($ -> mapping.resetPath());
        return result.flatMap(KeyNode::getValue);
    }
}
