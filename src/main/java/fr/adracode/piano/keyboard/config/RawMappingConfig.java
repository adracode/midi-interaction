package fr.adracode.piano.keyboard.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.List;
import java.util.Optional;

public record RawMappingConfig(
        Settings settings,
        List<Toggle> toggles,
        List<String> availablePedals,
        List<MappingUnit> keyboard) {

    public RawMappingConfig(Settings settings, List<Toggle> toggles, List<String> availablePedals, List<MappingUnit> keyboard){
        this.settings = settings;
        this.toggles = Optional.ofNullable(toggles).orElse(List.of());
        this.availablePedals = Optional.ofNullable(availablePedals).orElse(List.of());
        this.keyboard = Optional.ofNullable(keyboard).orElse(List.of());
    }

    public record Settings(
            int keyInterval,
            int toggleOnceBelow,
            int sustainRepeat,
            int sustainAfter
    ) { }

    public static final class Toggle {
        private String fallback;
        private String trigger;
        private String toggle;

        public Toggle(){
        }

        @JsonAnySetter
        public void setTrigger(String key, String toggle){
            trigger = key;
            this.toggle = toggle;
        }

        public void setFallback(String fallback){
            this.fallback = fallback;
        }

        public String fallback(){
            return fallback;
        }

        public String trigger(){
            return trigger;
        }

        public String toggle(){
            return toggle;
        }
    }

    public record MappingUnit(List<String> toggle, List<RawKey> mapping) { }

    public static final class RawKey {

        private String trigger;
        private List<String> result;

        public RawKey(){
        }

        @JsonAnySetter
        public void setTrigger(String key, List<String> result){
            trigger = key;
            this.result = Optional.ofNullable(result).orElse(List.of());
        }

        public void setTrigger(String trigger){
            this.trigger = trigger;
        }

        public void setResult(List<String> result){
            this.result = result;
        }

        public String trigger(){
            return trigger;
        }

        public List<String> result(){
            return result;
        }
    }
}