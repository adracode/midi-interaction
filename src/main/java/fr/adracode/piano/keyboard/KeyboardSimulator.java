package fr.adracode.piano.keyboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.adracode.piano.keyboard.config.KeyboardSettings;
import fr.adracode.piano.keyboard.config.MappingParser;
import fr.adracode.piano.keyboard.config.RawMappingConfig;
import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.Pedal;
import fr.adracode.piano.keyboard.key.ToggleKey;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.IntList;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;
import static fr.adracode.piano.keyboard.KeyboardMapping.TONE;

public class KeyboardSimulator implements MqttCallback {

    private final KeyboardSettings settings;

    private final Robot robot = new Robot();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final UnicodeKeyboard unicodeKeyboard = new UnicodeKeyboard();
    private final KeyboardMapping keyboardMapping;

    private final ScheduledFuture<?>[] timerTask = new ScheduledFuture[OCTAVE];

    private boolean togglePermanently;
    private long currentOnceToggledKeys;

    private final Int2LongMap[] currentKeyPressed = new Int2LongMap[OCTAVE];

    public KeyboardSimulator(String mappingFile) throws AWTException, IOException{
        File f = new File(mappingFile);
        MappingParser parser = new MappingParser(new YAMLMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .readValue(f, RawMappingConfig.class));
        keyboardMapping = new KeyboardMapping(parser);
        settings = parser.getSettings();
        IntStream.range(0, OCTAVE).forEach(i -> currentKeyPressed[i] = new Int2LongArrayMap());
    }

    @Override
    public void connectionLost(Throwable cause){
        cause.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message){
        String rawMessage = message.toString();
        String[] parts = rawMessage.split(",");
        int command = Integer.parseInt(parts[0].trim());
        int data1 = Integer.parseInt(parts[1].trim());
        int data2 = Integer.parseInt(parts[2].trim());
        switch(command){
            case ShortMessage.NOTE_ON -> {
                if(data2 != 0){
                    handleKeyPressed(data1, data2);
                } else {
                    handleKeyReleased(data1);
                }
            }
            case ShortMessage.CONTROL_CHANGE -> {
                if(data1 == 64){
                    Pedal sustain = Pedal.get("sustain");
                    if(sustain.isToggleMode() || data2 > 0){
                        Key.weightedBinaryStream(sustain.getFakeKey().tone())
                                .filter(wBit -> wBit > 0)
                                .forEach(wBit -> handleKeyPressed(
                                        sustain.getFakeKey().octave() * TONE + TONE - Integer.numberOfTrailingZeros(wBit) - 1,
                                        127,
                                        !sustain.isToggleMode()));
                    } else if(!sustain.isToggleMode() && data2 == 0){
                        Key.weightedBinaryStream(sustain.getFakeKey().tone())
                                .filter(wBit -> wBit > 0)
                                .forEach(wBit -> handleKeyReleased(
                                        sustain.getFakeKey().octave() * TONE + TONE - Integer.numberOfTrailingZeros(wBit) - 1));
                    }

                }
            }
        }
    }

    private void handleKeyReleased(int data){
        currentKeyPressed[data / TONE].remove(data);
    }

    private void handleKeyPressed(int data1, int data2){
        handleKeyPressed(data1, data2, true);
    }

    private void handleKeyPressed(int data1, int data2, boolean registerKey){
        if(registerKey){
            currentKeyPressed[data1 / TONE].put(data1, System.currentTimeMillis());
        }
        int octave = data1 / TONE;
        keyboardMapping.registerKey(data1);
        if(data2 >= settings.toggleOnceBelow()){
            togglePermanently = true;
        }
        if(!isSustain(octave)){
            //TODO: Si dans single, NE PEUT PAS être un premier trigger
            //TODO: boutons toggle exclusifs à ça
            timerTask[octave] = timer.scheduleAtFixedRate(() -> {
                try {
                    synchronized(this){

                        long sustainInterval = System.currentTimeMillis() - settings.sustainAfter();
                        currentKeyPressed[octave].int2LongEntrySet().stream()
                                .filter(entry -> entry.getLongValue() < sustainInterval)
                                .forEach(entry -> keyboardMapping.registerKey(entry.getIntKey()));

                        var currentKey = keyboardMapping.getCurrentKey(octave);
                        currentKey.ifPresentOrElse(key -> {
                            key.getToggle().ifPresent(this::toggle);
                            key.getKeys().ifPresent(this::operateKeyboard);
                            key.getUnicode().ifPresent(this::operateKeyboard);
                        }, () -> {
                            //TODO: Check for each octave
                            if(currentKeyPressed[octave].isEmpty()){
                                timerTask[octave].cancel(true);
                                timerTask[octave] = null;
                            }
                        });
                        reset(octave);
                        togglePermanently = false;

                        //Reset once-toggles only if there is no toggle keys
                        if(currentKey.map(keyAction -> keyAction.getToggle().isEmpty()).orElse(false)){
                            long toggled = keyboardMapping.currentToggles();
                            LongStream.range(0, ToggleKey.getNextId()).forEach(i -> {
                                long it = (long)Math.pow(2, i);
                                if((it & currentOnceToggledKeys) != 0){
                                    ToggleKey.get(it).getKeyCode().ifPresent((toggled & it) == 0 ? robot::keyPress : robot::keyRelease);
                                }
                            });
                            keyboardMapping.toggle(currentOnceToggledKeys);
                            currentOnceToggledKeys = 0;
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

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
    }

    public void toggle(ToggleKey key){
        if(keyboardMapping.toggle(key.getId())){
            key.getKeyCode().ifPresent(robot::keyPress);
        } else {
            key.getKeyCode().ifPresent(robot::keyRelease);
        }
        if(!togglePermanently){
            currentOnceToggledKeys = ToggleKey.toggle(currentOnceToggledKeys, key.getId());
        }
    }

    public void operateKeyboard(String str){
        for(int i = 0; i < str.length(); ++i){
            unicodeKeyboard.sendUnicode(str.codePointAt(i));
        }
    }

    public void operateKeyboard(IntList charCodes){
        if(charCodes.isEmpty()){
            return;
        }
        try {
            for(int i = 0; i < charCodes.size(); ++i){
                robot.keyPress(charCodes.getInt(i));
            }
            for(int i = charCodes.size() - 1; i > 0; --i){
                robot.keyRelease(charCodes.getInt(i));
            }
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void reset(int octave){
        keyboardMapping.reset(octave);
    }

}
