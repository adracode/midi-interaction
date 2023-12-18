package fr.adracode.piano.keyboard.config;

public record KeyboardSettings(int keyInterval, int toggleOnceBelow, int sustainRepeat) {

    public static KeyboardSettings fromRaw(RawMappingConfig.Settings settings){
        return new KeyboardSettings(settings.keyInterval(), settings.toggleOnceBelow(), settings.sustainRepeat());
    }
}
