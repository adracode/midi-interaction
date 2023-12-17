package fr.adracode.piano.keyboard;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_UNICODE;

public class UnicodeKeyboard {

    private static final WinDef.DWORD KEY_DOWN = new WinDef.DWORD(KEYEVENTF_UNICODE);
    private static final WinDef.DWORD KEY_UP = new WinDef.DWORD(KEYEVENTF_UNICODE | KEYEVENTF_KEYUP);

    public void sendUnicode(int unicode){
        WinUser.INPUT input = new WinUser.INPUT();

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");

        input.input.ki.wScan = new WinDef.WORD(unicode);
        input.input.ki.dwFlags = KEY_DOWN;
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[])input.toArray(1), input.size());

        input.input.ki.dwFlags = KEY_UP;
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[])input.toArray(1), input.size());

    }

}
