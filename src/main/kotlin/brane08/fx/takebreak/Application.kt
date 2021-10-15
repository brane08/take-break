package brane08.fx.takebreak

import javafx.application.Application


object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { ApplicationLock.releaseLock() }))
        ApplicationLock.tryToGetLock()
        Application.launch(BreakWindow::class.java)
    }
}