package brane08.fx.takebreak.tasks

import brane08.fx.takebreak.BreakApplication
import brane08.fx.takebreak.config.Injector
import brane08.fx.takebreak.domain.BreakConfig
import javafx.application.Platform
import javafx.stage.Stage
import java.awt.MenuItem
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

class BreakSchedule(
	private val counter: AtomicInteger,
	private val currentStage: Stage,
	private val skipItem: MenuItem,
	private val startTimer: (time: Int) -> Unit
) : Runnable {

	override fun run() {
		try {
			BreakApplication.LOG.info("{}", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
			val breakConfig: BreakConfig = Injector.get("breakConfig")
			val displayTime = breakConfig.getBreakTime(counter.addAndGet(1))
			Platform.runLater {
				skipItem.isEnabled = true
				currentStage.show()
				startTimer(displayTime)
			};
		} catch (e: InterruptedException) {
			BreakApplication.LOG.error("Interrupted the thread", e)
		} catch (e: Exception) {
			BreakApplication.LOG.error("Unhandled exception, check!!!", e)
		}
	}
}