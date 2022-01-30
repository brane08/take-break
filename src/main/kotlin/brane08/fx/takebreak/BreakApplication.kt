package brane08.fx.takebreak

import brane08.fx.takebreak.config.Injector
import brane08.fx.takebreak.domain.BreakConfig
import brane08.fx.takebreak.tasks.BreakSchedule
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class BreakApplication : Application() {

	private val scheduler = Executors.newSingleThreadScheduledExecutor()
	private val monitorPool = Executors.newSingleThreadExecutor()
	private val skipItem = MenuItem("Skip Break")
	private val counter: AtomicInteger = AtomicInteger(0)
	private lateinit var defaultStage: Stage

	private val cleanup = Runnable {
		scheduler.shutdownNow()
		monitorPool.shutdownNow()
	}

	private val hideCallback = Runnable {
		skipItem.isEnabled = false
		defaultStage.hide()
	}

	override fun start(rootStage: Stage) {
		this.defaultStage = rootStage
		LOG.info("starting app")
		val loader = FXMLLoader(this.javaClass.getResource("/views/main.fxml"))
		val parent = loader.load<Parent>()
		val controller = loader.getController<BreakController>()
		controller.hideCallback = hideCallback
		initStage(rootStage, parent)
		systemTray(controller)
		val breakConfig: BreakConfig = Injector.get(BreakConfig::class.java)
		monitorPool.submit {
			try {
				while (true) {
					if (scheduler.isShutdown)
						break
					val future = scheduler.schedule(
						BreakSchedule(counter, rootStage, skipItem, controller::startTimer),
						breakConfig.spacing.toLong(),
						TimeUnit.MINUTES
					)
					future.get()
				}
			} catch (e: InterruptedException) {
				LOG.info("Timer stopped")
			} catch (e: Exception) {
				LOG.error("", e)
			}
		}
	}

	private fun initStage(rootStage: Stage, parent: Parent?) {
		Platform.setImplicitExit(false)
		rootStage.scene = Scene(parent, WIDTH, HEIGHT)
		rootStage.initStyle(StageStyle.UNDECORATED);
		rootStage.isResizable = false
		rootStage.isAlwaysOnTop = true
		rootStage.x = X
		rootStage.y = Y
	}

	override fun stop() {
		cleanup.run()
		super.stop()
	}

	fun systemTray(controller: BreakController) {
		if (SystemTray.isSupported()) {
			val tray = SystemTray.getSystemTray()
			val image = ImageIO.read(this.javaClass.getResource("/coffee.png"))
			val popup = PopupMenu()
			val trayIcon = TrayIcon(image, "Take Break", popup)

			skipItem.isEnabled = false
			skipItem.addActionListener {
				Platform.runLater { controller.stopTimer() }
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
				Taskbar.getTaskbar().setIconImage(image)
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

		init {
			val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
			WIDTH = (screenBounds.width * SCALE_FACTOR) / 100
			HEIGHT = (screenBounds.height * SCALE_FACTOR) / 100
			X = (screenBounds.width - WIDTH) / 2;
			Y = (screenBounds.height - HEIGHT) / 2;
		}

		@JvmStatic
		fun main(args: Array<String>) {
			Runtime.getRuntime().addShutdownHook(Thread { ApplicationLock.releaseLock() })
			ApplicationLock.tryToGetLock()
			launch(BreakApplication::class.java)
		}
	}
}