package fr.adracode.piano.keyboard.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import org.apache.commons.cli.ParseException;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;

public class MappingConfig {

    private final Long2IntMap singleMapping = new Long2IntArrayMap();
    private final KeyNode<String> multiRoot = new KeyNode<>(0, 0);
    private KeyNode<String> current;

    public MappingConfig(String mappingFile) throws ParseException{
        try {
            File f = new File(mappingFile);
            RawMappingConfig rawMapping = new YAMLMapper().readValue(f, RawMappingConfig.class);

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

            List<RawMappingConfig.MultiKey> multiKeys = rawMapping.multi();
            for(RawMappingConfig.MultiKey multi : multiKeys){
                KeyNode<String> node = multiRoot;
                var trigger = multi.trigger();
                for(int i = 0; i < trigger.size(); i++){
                    var keys = trigger.get(i);
                    var key = keys.entrySet().stream().findFirst().orElseThrow();
                    int octave = Integer.parseInt(key.getKey());
                    int tone = Integer.parseInt(key.getValue());

                    var child = node.get(octave, tone);
                    node = child.isPresent() ?
                            child.get() :
                            node.addChild(i == trigger.size() - 1 ?
                                    new KeyLeaf<>(octave, tone, multi.result()) :
                                    new KeyNode<>(octave, tone)
                            );
                }
            }

        } catch(NumberFormatException | IOException | NoSuchFieldException | IllegalAccessException e){
            throw new ParseException("Couldn't parse mapping file: " + e);
        }
    }

    public Optional<Integer> getSingle(int octave, int tone){
        int key = singleMapping.get(Key.from(octave, tone));
        return key == 0 ? Optional.empty() : Optional.of(key);
    }

    public Optional<String> getMulti(int octave, int tone){
        current = (current == null ? multiRoot : current).get(octave, tone).orElse(null);
        if(current == null){
            return Optional.empty();
        }
        Optional<String> result = current.getResult();
        if(result.isPresent()){
            current = null;
        }
        return result;
    }

}
