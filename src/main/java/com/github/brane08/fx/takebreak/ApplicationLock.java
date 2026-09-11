package com.github.brane08.fx.takebreak;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public final class ApplicationLock {

    private static ServerSocket SOCKET = null;

    private ApplicationLock() {
    }

    // Port 14425 is an arbitrary loopback-only port used as a single-instance mutex.
    // It must never be changed without coordinating with existing installations.
    private static final int LOCK_PORT = 14425;

    public static void tryToGetLock() {
        try {
            SOCKET = new ServerSocket(LOCK_PORT, 10, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new RuntimeException("Application instance is running already");
        }
    }

    public static void releaseLock() {
        if (SOCKET == null) {
            return;
        }
        try {
            SOCKET.close();
        } catch (IOException e) {
            throw new RuntimeException("Application instance closing now");
        }
    }
}
