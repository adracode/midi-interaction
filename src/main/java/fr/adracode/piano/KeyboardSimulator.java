package fr.adracode.piano;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.concurrent.*;

//TODO: generify handleHand
public class KeyboardSimulator implements MqttCallback {
    private static final int TIMEOUT = 50; //ms

    //TODO: config in files
    private static final boolean[] AUTHORIZED_KEYS = new boolean[128];

    static{
        AUTHORIZED_KEYS[60] = true;
        AUTHORIZED_KEYS[62] = true;
        AUTHORIZED_KEYS[64] = true;
        AUTHORIZED_KEYS[65] = true;
        AUTHORIZED_KEYS[67] = true;
        AUTHORIZED_KEYS[72] = true;
        AUTHORIZED_KEYS[74] = true;
        AUTHORIZED_KEYS[76] = true;
        AUTHORIZED_KEYS[77] = true;
        AUTHORIZED_KEYS[79] = true;
    }

    //TODO: config in files
    private static final int[] LEFT_MAPPING = new int[32];

    static{
        LEFT_MAPPING[1] = KeyEvent.VK_A;
        LEFT_MAPPING[2] = KeyEvent.VK_E;
        LEFT_MAPPING[4] = KeyEvent.VK_S;
        LEFT_MAPPING[8] = KeyEvent.VK_I;
        LEFT_MAPPING[16] = KeyEvent.VK_T;
        LEFT_MAPPING[3] = KeyEvent.VK_N;
        LEFT_MAPPING[6] = KeyEvent.VK_R;
        LEFT_MAPPING[12] = KeyEvent.VK_U;
        LEFT_MAPPING[24] = KeyEvent.VK_L;
        LEFT_MAPPING[5] = KeyEvent.VK_O;
        LEFT_MAPPING[9] = KeyEvent.VK_D;
        LEFT_MAPPING[17] = KeyEvent.VK_C;
        LEFT_MAPPING[10] = KeyEvent.VK_M;
        LEFT_MAPPING[18] = KeyEvent.VK_P;
        LEFT_MAPPING[20] = KeyEvent.VK_V;
        LEFT_MAPPING[7] = KeyEvent.VK_Q;
        LEFT_MAPPING[11] = KeyEvent.VK_F;
        LEFT_MAPPING[19] = KeyEvent.VK_B;
        LEFT_MAPPING[14] = KeyEvent.VK_G;
        LEFT_MAPPING[22] = KeyEvent.VK_H;
        LEFT_MAPPING[28] = KeyEvent.VK_J;
        LEFT_MAPPING[21] = KeyEvent.VK_X;
        LEFT_MAPPING[25] = KeyEvent.VK_Y;
        LEFT_MAPPING[26] = KeyEvent.VK_Z;
        LEFT_MAPPING[13] = KeyEvent.VK_W;
        LEFT_MAPPING[15] = KeyEvent.VK_K;
    }

    //TODO: config in files
    private static final int[] RIGHT_MAPPING = new int[32];

    static{
        RIGHT_MAPPING[16] = KeyEvent.VK_SPACE;
        RIGHT_MAPPING[8] = KeyEvent.VK_BACK_SPACE;
        RIGHT_MAPPING[28] = KeyEvent.VK_ENTER;
    }

    private final Robot robot = new Robot();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private int leftHand = 0;
    private int rightHand = 0;
    private boolean caps;
    private boolean leftEngaged;
    private boolean rightEngaged;
    private ScheduledFuture<?> timerTask;

    public KeyboardSimulator() throws AWTException{
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
                if(!AUTHORIZED_KEYS[data1]){
                    return;
                }
                if(data2 != 0){
                    if(data1 >= 60 && data1 <= 67){
                        handleLeftHand(data1);
                    } else if(data1 >= 72 && data1 <= 79){
                        handleRightHand(data1);
                    }
                    if(data2 >= 100){
                        caps = true;
                    }
                }
            }
            case ShortMessage.CONTROL_CHANGE -> {
                if(data1 == 64){
                    if(data2 != 0 && !isSustain()){
                        timerTask = timer.scheduleAtFixedRate(() -> {
                            operateKeyboard(getLeftHand(leftHand), caps);
                            operateKeyboard(getRightHand(rightHand), caps);
                        }, 0, TIMEOUT / 2, TimeUnit.MILLISECONDS);
                    } else if(isSustain()){
                        timerTask.cancel(true);
                        rightHand = 0;
                        leftHand = 0;
                    }
                }
            }
        }
    }

    private boolean isSustain(){
        return timerTask != null && !timerTask.isCancelled();
    }

    private void handleLeftHand(int data){
        if(!leftEngaged && !isSustain()){
            leftEngaged = true;
            CompletableFuture.delayedExecutor(TIMEOUT, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        operateKeyboard(getLeftHand(leftHand), caps);
                        leftEngaged = false;
                        leftHand = 0;
                    });
        }
        leftHand |= switch(data){
            case 60 -> 0b10000;
            case 62 -> 0b01000;
            case 64 -> 0b00100;
            case 65 -> 0b00010;
            case 67 -> 0b00001;
            default -> 0;
        };
    }

    private void handleRightHand(int data){
        if(!rightEngaged && !isSustain()){
            rightEngaged = true;
            CompletableFuture.delayedExecutor(TIMEOUT, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        operateKeyboard(getRightHand(rightHand), caps);
                        rightEngaged = false;
                        rightHand = 0;
                    });
        }
        rightHand |= switch(data){
            case 72 -> 0b10000;
            case 74 -> 0b01000;
            case 76 -> 0b00100;
            case 77 -> 0b00010;
            case 79 -> 0b00001;
            default -> 0;
        };
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
    }

    private int getLeftHand(int code){
        if(code < 0 || code >= LEFT_MAPPING.length){
            return -1;
        }
        return LEFT_MAPPING[code];
    }

    private int getRightHand(int code){
        if(code < 0 || code >= LEFT_MAPPING.length){
            return -1;
        }
        return RIGHT_MAPPING[code];
    }

    public void operateKeyboard(int charCode, boolean shift){
        if(charCode < 0){
            return;
        }
        if(shift){
            robot.keyPress(KeyEvent.VK_SHIFT);
        }
        try {
            robot.keyPress(charCode);
            robot.keyRelease(charCode);
        } catch(IllegalArgumentException ignored){
        }
        if(shift){
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }
}
