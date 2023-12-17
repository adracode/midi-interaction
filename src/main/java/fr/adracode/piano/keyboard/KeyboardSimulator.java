package fr.adracode.piano.keyboard;

import com.spencerwi.either.Either;
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
    private static final boolean ALT_KEY_MODE = false;

    private final Robot robot = new Robot();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

    private final KeyboardMapping mapping;
    private boolean caps;
    private final boolean[] octaveEngaged = new boolean[OCTAVE];

    private ScheduledFuture<?> timerTask;
    private int lastKey;
    private final UnicodeKeyboard unicodeKeyboard = new UnicodeKeyboard();

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
                                operateKeyboard(lastKey, caps);
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
        Either<Integer, String> key = mapping.getKey(data1);
        if(data2 >= 100){
//            caps = true;
        }
        if(key.isLeft()){
            if(!octaveEngaged[octave] && !isSustain()){
                octaveEngaged[octave] = true;
                CompletableFuture.delayedExecutor(TIMEOUT, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            lastKey = mapping.getCurrentKey(octave).orElse(-1);
                            operateKeyboard(lastKey, caps);
                            reset(octave);
                        });
            }
        } else if(key.isRight()){
            operateKeyboard(key.getRight());
        }
    }


    private boolean isSustain(){
        return timerTask != null && !timerTask.isCancelled();
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
    }

    public void operateKeyboard(String str){
        for(int i = 0; i < str.length(); ++i){
            unicodeKeyboard.sendUnicode(str.codePointAt(i));
        }
        resetAll();
    }

    public void operateKeyboard(int charCode, boolean shift){
        if(charCode <= 0){
            return;
        }
        if(shift){
            robot.keyPress(KeyEvent.VK_SHIFT);
        }
        try {
            robot.keyPress(charCode);
            robot.keyRelease(charCode);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        if(shift){
            robot.keyRelease(KeyEvent.VK_SHIFT);
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
