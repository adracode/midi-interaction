package fr.adracode.piano.keyboard.config;

import java.util.List;
import java.util.Map;

public record RawMappingConfig(
        Settings settings,
        Map<String, Map<String, String>> single,
        List<Toggle> toggles,
        Pedals pedals,
        List<MultiKey> multi) {

    public record Settings(
            int keyInterval,
            int toggleOnceBelow,
            int sustainRepeat,
            int sustainAfter
    ) { }

    public record MultiKey(
            List<Map<String, String>> trigger,
            String result,
            List<String> with,
            String key) { }

    public record Toggle(
            Map<String, String> trigger,
            String toggle) { }

    public record Pedals(
            Pedal sustain
    ) { }

    public record Pedal(
            String toggle,
            List<String> with,
            String result,
            String key
    ) { }
}