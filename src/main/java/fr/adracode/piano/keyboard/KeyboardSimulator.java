package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.key.ToggledKey;
import org.apache.commons.cli.ParseException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.*;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;
import static fr.adracode.piano.keyboard.KeyboardMapping.TONE;

public class KeyboardSimulator implements MqttCallback {

    private static final int TIMEOUT = 50; //ms
    private final Robot robot = new Robot();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final UnicodeKeyboard unicodeKeyboard = new UnicodeKeyboard();
    private final KeyboardMapping mapping;

    private boolean caps;
    private final boolean[] octaveEngaged = new boolean[OCTAVE];
    private ScheduledFuture<?> timerTask;
    private int lastKey;

    public KeyboardSimulator(String mappingFileName) throws AWTException, ParseException{
        mapping = new KeyboardMapping(mappingFileName);
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
                        }, 0, TIMEOUT / 2, TimeUnit.MILLISECONDS);
                    } else if(isSustain()){
                        timerTask.cancel(true);
                        for(int octave = 0; octave < OCTAVE; ++octave){
                            mapping.reset(octave);
                        }
                    }
                }
            }
        }
    }

    private void handleKeyPressed(int data1, int data2){
        int octave = data1 / TONE;
        mapping.registerKey(data1);
        if(data2 >= 100){
//            caps = true;
        }
        if(!octaveEngaged[octave]/* && !isSustain()*/){
            octaveEngaged[octave] = true;
            //TODO: Si dans single, NE PEUT PAS être un premier trigger
            //TODO: boutons toggle exclusifs à ça
            CompletableFuture.delayedExecutor(TIMEOUT, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        synchronized(this){
                            mapping.getCurrentKey(octave).ifPresent(key -> {
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
        key.getKeyCode().ifPresent(mapping.toggle(key) ? robot::keyPress : robot::keyRelease);
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
        mapping.reset(octave);
    }

    private void resetAll(){
        for(int i = 0; i < OCTAVE; ++i){
            reset(i);
        }
    }

}
