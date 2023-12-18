package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.config.Mapping;
import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.key.ToggledKey;
import org.apache.commons.cli.ParseException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;
import static fr.adracode.piano.keyboard.KeyboardMapping.TONE;

public class KeyboardSimulator implements MqttCallback {

    private final Robot robot = new Robot();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final UnicodeKeyboard unicodeKeyboard = new UnicodeKeyboard();
    private final Mapping mapping;
    private final KeyboardMapping keyboardMapping;

    private final boolean[] octaveEngaged = new boolean[OCTAVE];
    private ScheduledFuture<?> timerTask;
    private int lastKey;
    private boolean togglePermanently;
    private long currentOnceToggledKeys;

    public KeyboardSimulator(String mappingFileName) throws AWTException, ParseException{
        mapping = new Mapping(mappingFileName);
        keyboardMapping = new KeyboardMapping(mapping);
    }

    @Override
    public void connectionLost(Throwable cause){
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
                }
            }
            case ShortMessage.CONTROL_CHANGE -> {
                if(data1 == 64){
                    if(data2 != 0 && !isSustain()){
                        timerTask = timer.scheduleAtFixedRate(() -> {
                            for(int octave = 0; octave < OCTAVE; ++octave){
                                operateKeyboard(lastKey);
                            }
                        }, 0, mapping.getSettings().sustainRepeat(), TimeUnit.MILLISECONDS);
                    } else if(isSustain()){
                        timerTask.cancel(true);
                        for(int octave = 0; octave < OCTAVE; ++octave){
                            keyboardMapping.reset(octave);
                        }
                    }
                }
            }
        }
    }

    private void handleKeyPressed(int data1, int data2){
        int octave = data1 / TONE;
        keyboardMapping.registerKey(data1);
        if(data2 >= mapping.getSettings().toggleOnceBelow()){
            togglePermanently = true;
        }
        if(!octaveEngaged[octave]/* && !isSustain()*/){
            octaveEngaged[octave] = true;
            //TODO: Si dans single, NE PEUT PAS être un premier trigger
            //TODO: boutons toggle exclusifs à ça
            CompletableFuture.delayedExecutor(mapping.getSettings().keyInterval(), TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        synchronized(this){
                            var currentKey = keyboardMapping.getCurrentKey(octave);
                            currentKey.ifPresent(key -> {
                                if(key.isLeft()){
                                    operateKeyboard(key.getLeft());
                                } else if(key.isRight()){
                                    KeyAction action = key.getRight();
                                    action.getToggle().ifPresent(this::toggle);
                                    action.getResult().ifPresent(r -> r.run(
                                            this::operateKeyboard,
                                            this::operateKeyboard
                                    ));
                                }
                            });
                            reset(octave);
                            togglePermanently = false;

                            //Reset once-toggles only if there is no toggle key
                            if(currentKey.map(o -> o.isLeft() || o.isRight() && o.getRight().getToggle().isEmpty()).orElse(true)){
                                long toggled = mapping.getCurrentToggledKeys();
                                LongStream.range(0, ToggledKey.getNextId()).forEach(i -> {
                                    long it = (long)Math.pow(2, i);
                                    if((it & currentOnceToggledKeys) != 0){
                                        ToggledKey.get(it).getKeyCode().ifPresent((toggled & it) == 0 ? robot::keyPress : robot::keyRelease);
                                    }
                                });
                                mapping.toggle(currentOnceToggledKeys);

                                currentOnceToggledKeys = 0;
                            }
                        }
                    });
        }
    }


    private boolean isSustain(){
        return timerTask != null && !timerTask.isCancelled();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
    }

    public void toggle(ToggledKey key){
        if(mapping.toggle(key.getId())){
            key.getKeyCode().ifPresent(robot::keyPress);
        } else {
            key.getKeyCode().ifPresent(robot::keyRelease);
        }
        if(!togglePermanently){
            currentOnceToggledKeys = ToggledKey.toggle(currentOnceToggledKeys, key.getId());
        }
    }

    public void operateKeyboard(String str){
        for(int i = 0; i < str.length(); ++i){
            unicodeKeyboard.sendUnicode(str.codePointAt(i));
        }
        resetAll();
    }

    public void operateKeyboard(int charCode){
        if(charCode <= 0){
            return;
        }
        try {
            robot.keyPress(charCode);
            robot.keyRelease(charCode);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void reset(int octave){
        octaveEngaged[octave] = false;
        keyboardMapping.reset(octave);
    }

    private void resetAll(){
        for(int i = 0; i < OCTAVE; ++i){
            reset(i);
        }
    }

}
