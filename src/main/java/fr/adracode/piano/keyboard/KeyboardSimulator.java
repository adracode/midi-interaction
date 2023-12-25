package fr.adracode.piano.keyboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.adracode.piano.keyboard.config.KeyboardSettings;
import fr.adracode.piano.keyboard.config.MappingParser;
import fr.adracode.piano.keyboard.config.RawMappingConfig;
import fr.adracode.piano.keyboard.os.OSKeyboard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static fr.adracode.piano.keyboard.KeyboardHand.OCTAVE_WITH_PEDALS;
import static fr.adracode.piano.keyboard.KeyboardHand.TONE;

public class KeyboardSimulator {
    private final KeyboardSettings settings;

    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final KeyboardMapping keyboardMapping;
    private final ToggleManager toggleManager;
    private final int[] toggleMasks;

    private final ScheduledFuture<?>[] timerTask = new ScheduledFuture[OCTAVE_WITH_PEDALS];

    public KeyboardSimulator(String mappingFile, OSKeyboard keyboard) throws IOException{
        File f = new File(mappingFile);
        MappingParser parser = new MappingParser(new YAMLMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .readValue(f, RawMappingConfig.class));

        settings = parser.getSettings();
        toggleMasks = parser.getToggleMasks(new int[OCTAVE_WITH_PEDALS]);
        toggleManager = new ToggleManager(keyboard, settings.sustainAfter(), parser.getToggleKeys(), toggleMasks);
        keyboardMapping = new KeyboardMapping(keyboard, parser.getKeys(), new KeyboardHand(settings.sustainAfter()),
                Arrays.stream(toggleMasks).map(mask -> ~mask).toArray(), toggleManager);

        System.out.println("Ready!");
    }

    public void handleKeyReleased(int data){
        int octave = data / TONE;
        if(isToggle(octave, data)){
            toggleManager.getHand().unregisterKey(octave, data % TONE);
        } else {
            keyboardMapping.getHand().unregisterKey(octave, data % TONE);
        }
    }

    private boolean isToggle(int octave, int tone){
        return ((1 << (TONE - (tone % TONE) - 1)) & toggleMasks[octave]) != 0;
    }

    public void handleKeyPressed(int data1, int data2){
        int octave = data1 / TONE;
        if(isToggle(octave, data1)){
            if(data2 < settings.toggleOnceBelow()){
                toggleManager.setOnce(octave);
            }
            toggleManager.getHand().registerKey(octave, data1 % TONE);
        } else {
            keyboardMapping.getHand().registerKey(octave, data1 % TONE);
        }
        if(!isSustain(octave)){
            //TODO: Si dans single, NE PEUT PAS être un premier trigger
            //TODO: boutons toggle exclusifs à ça
            timerTask[octave] = timer.scheduleAtFixedRate(() -> {
                try {
                    synchronized(this){
                        toggleManager.handle(octave);
                        keyboardMapping.handle(octave);
                        if(toggleManager.getHand().isEmpty(octave) && keyboardMapping.getHand().isEmpty(octave)){
                            timerTask[octave].cancel(true);
                            timerTask[octave] = null;
                        }
                    }
                } catch(RuntimeException e){
                    e.printStackTrace();
                }
            }, settings.keyInterval(), settings.sustainRepeat(), TimeUnit.MILLISECONDS);
        }
    }


    private boolean isSustain(int octave){
        return timerTask[octave] != null && !timerTask[octave].isCancelled();
    }
}
