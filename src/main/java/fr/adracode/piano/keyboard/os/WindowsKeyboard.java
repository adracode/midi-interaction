package fr.adracode.piano.keyboard.os;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import it.unimi.dsi.fastutil.ints.IntCollection;

import java.awt.*;

import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.*;

public class WindowsKeyboard implements OSKeyboard {

    private static final WinDef.DWORD KEY_DOWN = new WinDef.DWORD(KEYEVENTF_SCANCODE);
    private static final WinDef.DWORD KEY_UP = new WinDef.DWORD(KEYEVENTF_SCANCODE | KEYEVENTF_KEYUP);
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
        /*sendEvent(User32.INSTANCE.MapVirtualKeyEx(code, MAPVK_VK_TO_VSC,
                User32.INSTANCE.GetKeyboardLayout(0)), KEY_DOWN);*/
        robot.keyPress(code);
    }

    @Override
    public void releaseKey(int code){
        /*sendEvent(User32.INSTANCE.MapVirtualKeyEx(code, MAPVK_VK_TO_VSC,
                User32.INSTANCE.GetKeyboardLayout(0)), KEY_UP);*/
        robot.keyRelease(code);
    }

    public void sendUnicode(int unicode){
        sendEvent(unicode, UNICODE_DOWN);
        sendEvent(unicode, UNICODE_UP);

    }

    private void sendEvent(int code, WinDef.DWORD type){
        WinUser.INPUT input = new WinUser.INPUT();

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");

        input.input.ki.wScan = new WinDef.WORD(code);
        input.input.ki.dwFlags = type;
        input.input.ki.time = new WinDef.DWORD(0);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[])input.toArray(1), input.size());
    }


}
