package fr.adracode.piano.keyboard.config;

import fr.adracode.piano.keyboard.key.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingParser {

    private final RawMappingConfig rawConfig;
    private final List<RawMappingConfig.MultiKey> keys = new ArrayList<>();
    private final List<RawMappingConfig.Toggle> toggles = new ArrayList<>();

    public MappingParser(RawMappingConfig rawConfig){
        this.rawConfig = rawConfig;

        rawConfig.toggles().forEach(toggleKey -> {
            ToggleKey key = ToggleKey.get(toggleKey.toggle());
            ToggleKey.Fallback.of(toggleKey.fallback()).ifPresent(key::setFallback);
        });

        keys.addAll(rawConfig.multi());
        rawConfig.simple().forEach(simpleMapping ->
                keys.addAll(simpleMapping.keys().stream().map(simpleKey ->
                        new RawMappingConfig.MultiKey(simpleKey.getTriggers(), simpleKey.getUnicode(),
                                Set.of(), simpleKey.getKeys())).toList()));

        toggles.addAll(rawConfig.toggles());

        var sustain = rawConfig.pedals().sustain();
        Key fakeTrigger = Pedal.get("sustain").getFakeKey();

        if(sustain.result() != null || sustain.keys() != null){
            keys.add(new RawMappingConfig.MultiKey(
                    List.of(Map.of(String.valueOf(fakeTrigger.octave()), String.valueOf(fakeTrigger.tone()))),
                    sustain.result(),
                    sustain.with(),
                    sustain.keys()
            ));
        }
        if(sustain.toggle() != null){
            Pedal.get("sustain").asToggle();
            toggles.add(new RawMappingConfig.Toggle(
                    Map.of(String.valueOf(fakeTrigger.octave()), String.valueOf(fakeTrigger.tone())),
                    sustain.toggle(),
                    sustain.fallback()
            ));
        }
    }

    public KeyboardSettings getSettings(){
        return KeyboardSettings.fromRaw(rawConfig.settings());
    }

    public LongSet getToggleCombinations(){
        LongArraySet combinations = new LongArraySet(
                keys.stream().map(key -> ToggleKey.of(
                        key.with().stream().map(ToggleKey::get).toList())).collect(Collectors.toSet()));
        combinations.addAll(toggles.stream().map(toggle -> ToggleKey.get(toggle.toggle()).getId()).toList());
        return combinations;
    }

    public KeyNode<KeyAction> getTreeWith(Collection<String> toggles){
        return getTree(keys.stream()
                        .filter(multiKey -> toggles.containsAll(multiKey.with()) &&
                                multiKey.with().containsAll(toggles)).toList(),
                multiKey -> new KeyAction.Builder()
                        .keyCode(new IntArrayList(multiKey.getKeys().stream().map(key -> Key.getKeyCode(key).orElse(null))
                                .filter(Objects::nonNull).toList()))
                        .result(multiKey.getUnicode())
        );
    }

    public KeyNode<KeyAction> getTreeToggle(){
        return getTree(toggles,
                toggle -> new KeyAction.Builder()
                        .toggle(toggle.getUnicode())
        );
    }

    private <T extends HasTriggers & HasResults> KeyNode<KeyAction> getTree(
            List<T> list,
            Function<T, KeyAction.Builder> buildChild){
        KeyNode<KeyAction> root = new KeyNode<>(0);
        list.forEach(key -> {
            KeyNode<KeyAction> node = root;
            var triggers = key.getTriggers();
            for(int i = 0; i < triggers.size(); i++){
                //only the first because the map only contains one octave with its tone
                var trigger = triggers.get(i).entrySet().stream().findFirst().orElseThrow();
                long id = Key.from(
                        Integer.parseInt(trigger.getKey()), //octave
                        Integer.parseInt(trigger.getValue())); //node

                var child = node.get(id);
                node = child.isPresent() ?
                        child.get() :
                        node.addChild(i == triggers.size() - 1 ?
                                new KeyLeaf<>(id, buildChild.apply(key).build()) :
                                new KeyNode<>(id)
                        );
            }
        });
        return root;
    }
}
