package fr.adracode.piano.keyboard.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record RawMappingConfig(
        Settings settings,
        List<SimpleMapping> simple,
        List<Toggle> toggles,
        Pedals pedals,
        List<MultiKey> multi) {

    public RawMappingConfig(Settings settings, List<SimpleMapping> simple, List<Toggle> toggles, Pedals pedals, List<MultiKey> multi){
        this.settings = settings;
        this.simple = Optional.ofNullable(simple).orElse(List.of());
        this.toggles = Optional.ofNullable(toggles).orElse(List.of());
        this.pedals = pedals;
        this.multi = Optional.ofNullable(multi).orElse(List.of());
    }

    public record Settings(
            int keyInterval,
            int toggleOnceBelow,
            int sustainRepeat,
            int sustainAfter
    ) { }

    public record SimpleMapping(int octave, List<Map<String, List<String>>> mapping) {
        public SimpleMapping(int octave, List<Map<String, List<String>>> mapping){
            this.octave = octave;
            this.mapping = Optional.of(mapping).orElse(List.of());
        }

        public List<SimpleKey> keys(){
            return mapping.stream().map(mapping -> new SimpleKey(octave,
                    mapping.keySet().stream().findFirst().orElseThrow(),
                    mapping.values().stream().findFirst().orElseThrow())).toList();
        }
    }

    public record SimpleKey(
            int octave,
            String tone,
            List<String> result) implements HasTriggers, HasResults {

        public SimpleKey(int octave, String tone, List<String> result){
            this.octave = octave;
            this.tone = tone;
            this.result = Optional.ofNullable(result).orElse(List.of());
        }

        @Override
        public String getUnicode(){
            return null;
        }

        @Override
        public List<String> getKeys(){
            return result;
        }

        @Override
        public List<Map<String, String>> getTriggers(){
            return List.of(Map.of(String.valueOf(octave), tone));
        }
    }

    public record MultiKey(
            List<Map<String, String>> trigger,
            String result,
            Set<String> with,
            List<String> keys) implements HasTriggers, HasResults {

        public MultiKey(List<Map<String, String>> trigger, String result, Set<String> with, List<String> keys){
            this.trigger = Optional.ofNullable(trigger).orElse(List.of());
            this.result = result;
            this.with = Optional.ofNullable(with).orElse(Set.of());
            this.keys = Optional.ofNullable(keys).orElse(List.of());
        }

        @Override
        public List<Map<String, String>> getTriggers(){
            return trigger;
        }

        @Override
        public String getUnicode(){
            return result;
        }

        @Override
        public List<String> getKeys(){
            return keys == null ? List.of() : keys;
        }
    }

    public record Toggle(
            Map<String, String> trigger,
            String toggle,
            String fallback) implements HasTriggers, HasResults {

        public Toggle(Map<String, String> trigger, String toggle, String fallback){
            this.trigger = Optional.ofNullable(trigger).orElse(Map.of());
            this.toggle = toggle;
            this.fallback = fallback;
        }

        @Override
        public List<Map<String, String>> getTriggers(){
            return List.of(trigger);
        }

        @Override
        public String getUnicode(){
            return toggle;
        }

        @Override
        public List<String> getKeys(){
            return List.of();
        }
    }

    public record Pedals(
            Pedal sustain
    ) { }

    public record Pedal(
            String toggle,
            Set<String> with,
            String result,
            List<String> keys,
            String fallback
    ) implements HasTriggers, HasResults {

        public Pedal(String toggle, Set<String> with, String result, List<String> keys, String fallback){
            this.toggle = toggle;
            this.with = Optional.ofNullable(with).orElse(Set.of());
            this.result = result;
            this.keys = Optional.ofNullable(keys).orElse(List.of());
            this.fallback = fallback;
        }

        @Override
        public String getUnicode(){
            return result;
        }

        @Override
        public List<String> getKeys(){
            return keys;
        }

        @Override
        public List<Map<String, String>> getTriggers(){
            return List.of();
        }
    }
}