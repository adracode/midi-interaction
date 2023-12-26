package fr.adracode.piano.keyboard.os;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import it.unimi.dsi.fastutil.ints.IntCollection;

import java.awt.*;

import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_UNICODE;

public class WindowsKeyboard implements OSKeyboard {

    private static final WinDef.DWORD KEY_DOWN = new WinDef.DWORD();
    private static final WinDef.DWORD KEY_UP = new WinDef.DWORD(KEYEVENTF_KEYUP);
    private static final WinDef.DWORD UNICODE_DOWN = new WinDef.DWORD(KEYEVENTF_UNICODE);
    private static final WinDef.DWORD UNICODE_UP = new WinDef.DWORD(KEYEVENTF_UNICODE | KEYEVENTF_KEYUP);

    private final Robot robot = new Robot();

    public WindowsKeyboard() throws AWTException{
    }

    @Override
    public void pressKeys(IntCollection keys){
        if(keys.isEmpty()){
            return;
        }
        try {
            keys.forEach(this::pressKey);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void releaseKeys(IntCollection keys){
        if(keys.isEmpty()){
            return;
        }
        try {
            keys.forEach(this::releaseKey);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void sendUnicode(String unicode){
        for(int i = 0; i < unicode.length(); ++i){
            sendUnicode(unicode.codePointAt(i));
        }
    }

    @Override
    public void pressKey(int code){
        if(code < 0){
            sendEvent(-code, KEY_DOWN);
        } else {
            robot.keyPress(code);
        }
    }

    @Override
    public void releaseKey(int code){
        if(code < 0){
            sendEvent(-code, KEY_UP);
        } else {
            robot.keyRelease(code);
        }
    }

    public void sendUnicode(int unicode){
        sendEvent(unicode, UNICODE_DOWN);
        sendEvent(unicode, UNICODE_UP);

    }

    private void sendEvent(int code, WinDef.DWORD type){
        WinUser.INPUT input = new WinUser.INPUT();

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        if((type.intValue() & KEYEVENTF_UNICODE) != 0){
            input.input.ki.wScan = new WinDef.WORD(code);
        } else {
            input.input.ki.wVk = new WinDef.WORD(code);
        }
        input.input.ki.dwFlags = type;
        input.input.ki.time = new WinDef.DWORD(0);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[])input.toArray(1), input.size());
    }


}
