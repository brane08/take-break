package com.github.brane08.fx.takebreak.idle;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

public final class WindowsIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
        User32.INSTANCE.GetLastInputInfo(info);
        // GetTickCount wraps at ~49 days; mask to handle wrap-around correctly
        long idleMs = (Kernel32.INSTANCE.GetTickCount() - info.dwTime) & 0xFFFFFFFFL;
        return idleMs / 1000;
    }
}
