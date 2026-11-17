package com.github.brane08.fx.takebreak;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public final class ApplicationLock {

    private static ServerSocket SOCKET = null;

    private ApplicationLock() {
    }

    public static void tryToGetLock() {
        try {
            SOCKET = new ServerSocket(14425, 10, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new RuntimeException("Application instance is running already");
        }
    }

    public static void releaseLock() {
        try {
            SOCKET.close();
        } catch (IOException e) {
            throw new RuntimeException("Application instance closing now");
        }
    }
}
