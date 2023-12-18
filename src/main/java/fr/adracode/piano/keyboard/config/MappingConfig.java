package fr.adracode.piano.keyboard.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.adracode.piano.keyboard.key.*;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.apache.commons.cli.ParseException;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;

public class MappingConfig {

    private final Long2IntMap singleMapping = new Long2IntArrayMap();
    private final Long2ObjectMap<KeyNode<KeyAction>> multiRoot = new Long2ObjectArrayMap<>();
    private final KeyNode<KeyAction> toggles = new KeyNode<>(0);
    private KeyNode<KeyAction> current;
    private long currentToggledKeys;

    public MappingConfig(String mappingFile) throws ParseException{
        try {
            File f = new File(mappingFile);
            RawMappingConfig rawMapping = new YAMLMapper()
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .readValue(f, RawMappingConfig.class);
            multiRoot.put(0, new KeyNode<>(0));

            for(var octaveMapping : rawMapping.single().entrySet()){
                int octave = Integer.parseInt(octaveMapping.getKey());
                if(octave < 0 || octave >= OCTAVE){
                    continue;
                }
                for(var toneMapping : octaveMapping.getValue().entrySet()){
                    int tone = Integer.parseInt(toneMapping.getKey());
                    singleMapping.put(Key.from(octave, tone), (int)KeyEvent.class.getField("VK_" + toneMapping.getValue()).get(null));
                }
            }

            rawMapping.toggles().forEach(toggleKey -> ToggledKey.get(toggleKey.result()));
            for(RawMappingConfig.MultiKey multi : rawMapping.toggles()){
                buildTree(multi, this.toggles, t -> new KeyAction.Builder().toggle(t.result()));
            }

            for(RawMappingConfig.MultiKey multi : rawMapping.multi()){
                List<ToggledKey> toggledKeys = Optional.ofNullable(multi.with()).orElse(List.of()).stream()
                        .map(ToggledKey::get).toList();
                long toggleKeys = ToggledKey.of(toggledKeys);
                KeyNode<KeyAction> node = Optional.ofNullable(multiRoot.get(toggleKeys))
                        .orElseGet(() -> {
                            KeyNode<KeyAction> newTree = new KeyNode<>(toggleKeys);
                            multiRoot.put(toggleKeys, newTree);
                            return newTree;
                        });

                buildTree(multi, node, m -> new KeyAction.Builder().result(m.result()).keyCode(m.keyCode()));
            }
            System.out.println(multiRoot);

        } catch(NumberFormatException | IOException | NoSuchFieldException | IllegalAccessException e){
            throw new ParseException("Couldn't parse mapping file: " + e);
        }
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

    public boolean toggle(ToggledKey key){
        currentToggledKeys = ToggledKey.toggle(currentToggledKeys, key);
        return ToggledKey.isToggleOn(currentToggledKeys, key);
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
