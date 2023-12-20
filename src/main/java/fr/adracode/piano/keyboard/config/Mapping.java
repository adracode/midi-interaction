package fr.adracode.piano.keyboard.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.adracode.piano.keyboard.key.*;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;

public class Mapping {

    private final KeyboardSettings settings;
    private final Long2IntMap singleMapping = new Long2IntArrayMap();
    private final Long2ObjectMap<KeyNode<KeyAction>> multiRoot = new Long2ObjectArrayMap<>();
    private final KeyNode<KeyAction> toggles = new KeyNode<>(0);
    private KeyNode<KeyAction> current;
    private long currentToggledKeys;

    public Mapping(String mappingFile) throws ParseException{
        try {
            File f = new File(mappingFile);
            RawMappingConfig rawMapping = new YAMLMapper()
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                    .readValue(f, RawMappingConfig.class);
            multiRoot.put(0, new KeyNode<>(0));

            settings = KeyboardSettings.fromRaw(rawMapping.settings());

            for(var octaveMapping : rawMapping.single().entrySet()){
                int octave = Integer.parseInt(octaveMapping.getKey());
                if(octave < 0 || octave >= OCTAVE){
                    continue;
                }
                for(var toneMapping : octaveMapping.getValue().entrySet()){
                    int tone = Integer.parseInt(toneMapping.getKey());
                    singleMapping.put(Key.from(octave, tone), Key.getKeyCode(toneMapping.getValue()).orElse(0).intValue());
                }
            }


            rawMapping.toggles().forEach(toggleKey -> ToggledKey.get(toggleKey.toggle()));
            for(RawMappingConfig.Toggle toggle : rawMapping.toggles()){
                buildTree(new RawMappingConfig.MultiKey(
                        List.of(toggle.trigger()),
                        toggle.toggle(),
                        null,
                        null
                ), this.toggles, t -> new KeyAction.Builder().toggle(t.result()));
            }

            List<RawMappingConfig.MultiKey> multiKeys = new ArrayList<>(rawMapping.multi());

            RawMappingConfig.Pedal sustain = rawMapping.pedals().sustain();
            Key fakeTrigger = Pedal.get("sustain").getFakeKey();

            if(sustain.toggle() != null){
                Pedal.get("sustain").asToggle();
                buildTree(new RawMappingConfig.MultiKey(
                        List.of(Map.of(String.valueOf(fakeTrigger.octave()), String.valueOf(fakeTrigger.tone()))),
                        rawMapping.pedals().sustain().toggle(),
                        null,
                        null
                ), this.toggles, t -> new KeyAction.Builder().toggle(t.result()));
            } else if(sustain.result() != null || sustain.key() != null){
                multiKeys.add(new RawMappingConfig.MultiKey(
                        List.of(Map.of(String.valueOf(fakeTrigger.octave()), String.valueOf(fakeTrigger.tone()))),
                        sustain.result(),
                        sustain.with(),
                        sustain.key()
                ));
            }

            for(RawMappingConfig.MultiKey multi : multiKeys){
                List<ToggledKey> toggledKeys = Optional.ofNullable(multi.with()).orElse(List.of()).stream()
                        .map(ToggledKey::get).toList();
                long toggleKeys = ToggledKey.of(toggledKeys);
                KeyNode<KeyAction> node = Optional.ofNullable(multiRoot.get(toggleKeys))
                        .orElseGet(() -> {
                            KeyNode<KeyAction> newTree = new KeyNode<>(toggleKeys);
                            multiRoot.put(toggleKeys, newTree);
                            return newTree;
                        });

                buildTree(multi, node, m -> new KeyAction.Builder().result(m.result()).keyCode(Key.getKeyCode(m.key()).orElse(null)));
            }
        } catch(NumberFormatException | IOException e){
            throw new ParseException("Couldn't parse mapping file: " + e);
        }
    }

    public KeyboardSettings getSettings(){
        return settings;
    }

    private void buildTree(RawMappingConfig.MultiKey multi, KeyNode<KeyAction> node, Function<RawMappingConfig.MultiKey, KeyAction.Builder> buildChild){
        var trigger = multi.trigger();
        for(int i = 0; i < trigger.size(); i++){
            var keys = trigger.get(i);
            var key = keys.entrySet().stream().findFirst().orElseThrow();
            int octave = Integer.parseInt(key.getKey());
            int tone = Integer.parseInt(key.getValue());
            long id = Key.from(octave, tone);

            var child = node.get(id);
            node = child.isPresent() ?
                    child.get() :
                    node.addChild(i == trigger.size() - 1 ?
                            new KeyLeaf<>(id, buildChild.apply(multi).build()) :
                            new KeyNode<>(id)
                    );
        }
    }

    public long getCurrentToggledKeys(){
        return currentToggledKeys;
    }

    public boolean toggle(long keyId){
        currentToggledKeys = ToggledKey.toggle(currentToggledKeys, keyId);
        return ToggledKey.isToggleOn(currentToggledKeys, keyId);
    }

    private KeyNode<KeyAction> getTree(){
        return Optional.ofNullable(multiRoot.get(currentToggledKeys)).orElse(multiRoot.get(0));
    }

    public Optional<Integer> getSingle(int octave, int tone){
        int key = singleMapping.get(Key.from(octave, tone));
        return key == 0 ? Optional.empty() : Optional.of(key);
    }

    //TODO: simplify
    public Optional<KeyAction> getMulti(int octave, int tone){
        if(tone == 0){
            return Optional.empty();
        }
        long key = Key.from(octave, tone);
        KeyNode<KeyAction> toggle = (current == null ? toggles : current).get(key).orElse(null);
        if(toggle != null){
            current = toggle;
            Optional<KeyAction> value = toggle.getValue();
            if(value.isPresent()){
                current = null;
            }
            return value;
        }

        current = (current == null ? getTree() : current).get(key).orElse(null);
        if(current == null){
            return Optional.empty();
        }
        Optional<KeyAction> result = current.getValue();
        if(result.isPresent()){
            current = null;
        }
        return result;
    }

}
