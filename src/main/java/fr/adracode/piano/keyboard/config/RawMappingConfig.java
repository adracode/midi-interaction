package fr.adracode.piano.keyboard.config;

import java.util.List;
import java.util.Map;

public record RawMappingConfig(
        Settings settings,
        Map<String, Map<String, String>> single,
        List<MultiKey> toggles,
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
}