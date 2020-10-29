package brane08.fx.takebreak

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

object ApplicationLock {

    private lateinit var socket: ServerSocket

    fun tryToGetLock() {
        try {
            socket = ServerSocket(14425, 10, InetAddress.getLoopbackAddress());
        } catch (ex: IOException) {
            throw RuntimeException("Application instance is running already")
        }
    }

    fun releaseLock() {
        socket.close()
    }
}