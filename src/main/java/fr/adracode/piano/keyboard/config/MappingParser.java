package fr.adracode.piano.keyboard.config;

import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.Pedal;
import fr.adracode.piano.keyboard.key.ToggleKey;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingParser {

    private final RawMappingConfig rawConfig;
    private final Long2ObjectMap<Key> keys = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ToggleKey> toggleKeys;
    private final Map<String, ToggleKey> toggleKeysByLabel;

    public MappingParser(RawMappingConfig rawConfig){
        this.rawConfig = rawConfig;

        rawConfig.availablePedals().forEach(Pedal::init);
        toggleKeys = initToggleKeys();
        toggleKeys.put(ToggleKey.NO_TOGGLE.getKey(), ToggleKey.NO_TOGGLE);
        toggleKeysByLabel = toggleKeys.values().stream().collect(Collectors.toMap(ToggleKey::getLabel, Function.identity()));

    }

    public int[] getToggleMasks(int[] masks){
        for(ToggleKey value : toggleKeys.values()){
            masks[value.getOctave()] |= value.getTone();
        }
        return masks;
    }

    public KeyboardSettings getSettings(){
        return KeyboardSettings.fromRaw(rawConfig.settings());
    }

    public Long2ObjectMap<Key> getKeys(){
        rawConfig.keyboard().forEach(mappingUnit -> {
            long togglesId = ToggleKey.of(mappingUnit.toggle().stream().map(toggleKeysByLabel::get).toList());
            mappingUnit.mapping().forEach(rawKey -> {
                Key key = parseTrigger(rawKey.trigger());

                rawKey.result().forEach(result ->
                        Key.getKeyCode(result).ifPresentOrElse(
                                keyCode -> key.addResult(keyCode, togglesId),
                                () -> key.addResult(result, togglesId)));
                keys.put(key.getKey(), key);
            });
        });
        return keys;
    }

    public Long2ObjectMap<ToggleKey> getToggleKeys(){
        return toggleKeys;
    }

    private Long2ObjectMap<ToggleKey> initToggleKeys(){
        Long2ObjectMap<ToggleKey> keys = new Long2ObjectOpenHashMap<>();
        rawConfig.toggles().forEach(toggle -> {
            Key key = parseTrigger(toggle.trigger());
            ToggleKey toggleKey = new ToggleKey(key.getOctave(), key.getTone(), toggle.toggle());
            ToggleKey.Fallback.of(toggle.fallback()).ifPresent(toggleKey::setFallback);
            keys.put(toggleKey.getKey(), toggleKey);
        });

        return keys;
    }

    private Key parseTrigger(String trigger){
        String[] rawTrigger = trigger.split("/");
        if(rawTrigger.length == 1){
            return Pedal.get(rawTrigger[0]).orElseThrow().getFakeKey();
        }
        int octave = Integer.parseInt(rawTrigger[0]);
        int tone = Integer.parseInt(rawTrigger[1]);
        return Optional.ofNullable(keys.get(Key.from(octave, tone))).orElseGet(() -> new Key(octave, tone));
    }

}
