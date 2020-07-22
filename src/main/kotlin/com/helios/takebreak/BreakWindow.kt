package com.helios.takebreak

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.*
import java.lang.Exception
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.ImageIcon
import kotlin.system.exitProcess

class BreakWindow : Application() {

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private lateinit var defaultStage: Stage
    private val counter: AtomicInteger = AtomicInteger(0)
    private val countdown = Countdown()
    private val skipItem = MenuItem("Skip Break")
    private val monitorPool = Executors.newSingleThreadExecutor()

    private val cleanup = Runnable {
        scheduler.shutdownNow()
        monitorPool.shutdownNow()
    }

    private val task = Runnable {
        try {
            println(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            val addAndGet = counter.addAndGet(1)
            val displayTime = if ((addAndGet % 3) == 0) (BREAK) else SMALL_BREAK
            Platform.runLater {
                skipItem.isEnabled = true
                countdown.startTimer(displayTime, defaultStage, {
                    skipItem.isEnabled = false
                    defaultStage.hide()
                })
            }
        } catch (e: Exception) {
            LOG.error("", e)
        }
    }

    override fun start(stage: Stage?) {
        Platform.setImplicitExit(false)
        systemTray()
        stage?.let {
            defaultStage = it
            it.initStyle(StageStyle.UNDECORATED);
            it.isResizable = false
            it.isAlwaysOnTop = true;
            it.scene = Scene(countdown, WIDTH, HEIGHT)
            it.title = "Countdown"
            it.x = X;
            it.y = Y;
        }
        val future = monitorPool.submit(Runnable {
            try {
                while (true) {
                    if (scheduler.isShutdown)
                        break
                    val future = scheduler.schedule(task, SPACING, TimeUnit.MINUTES)
                    future.get()
                }
            } catch (e: InterruptedException) {
                LOG.info("Timer stopped")
            } catch (e: Exception) {
                LOG.error("", e)
            }
        })
    }

    override fun stop() {
        cleanup.run()
        super.stop()
    }

    fun systemTray() {
        if (SystemTray.isSupported()) {
            val tray = SystemTray.getSystemTray()
            val resource = this.javaClass.getResource("/coffee.png")
            LOG.info("{}", resource)
            val image = ImageIcon(resource).image
            val popup = PopupMenu()
            val trayIcon = TrayIcon(image, "Take Break", popup)

            skipItem.isEnabled = false
            skipItem.addActionListener {
                Platform.runLater { countdown.stopTimer() }
            }
            popup.add(skipItem)

            val exitItem = MenuItem("Exit")
            exitItem.addActionListener {
                cleanup.run()
                Platform.exit()
                tray.remove(trayIcon)
                exitProcess(0)
            }
            popup.add(exitItem)

            trayIcon.isImageAutoSize = true
            try {
                tray.add(trayIcon)
            } catch (e: AWTException) {
                LOG.error("Can't add to tray")
            }
        } else {
            LOG.error("Tray unavailable")
        }
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger("take-break")
        private const val SCALE_FACTOR = 70
        var WIDTH: Double = 0.0
        var HEIGHT: Double = 0.0
        var X = 0.0
        var Y = 0.0
        const val BREAK = 300
        const val SMALL_BREAK = 20
        const val SPACING = 25L
//        const val BREAK = 10
//        const val SMALL_BREAK = 5
//        const val SPACING = 10L

        init {
            val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
            WIDTH = (screenBounds.width * SCALE_FACTOR) / 100
            HEIGHT = (screenBounds.height * SCALE_FACTOR) / 100
            X = (screenBounds.width - WIDTH) / 2;
            Y = (screenBounds.height - HEIGHT) / 2;
        }
    }
}