package brane08.fx.takebreak

import eu.hansolo.tilesfx.Tile
import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.util.Duration
import java.net.URL
import java.util.*

class BreakController : Initializable {

    private val secondsPerMin = 60
    private val minuteProperty = SimpleStringProperty("00")
    private val secondProperty = SimpleStringProperty("00")
    private lateinit var currentTimer: AnimationTimer
    lateinit var hideCallback: Runnable

    @FXML
    lateinit var rootPane: VBox

    @FXML
    lateinit var minutes: Tile

    @FXML
    lateinit var seconds: Tile

    @FXML
    lateinit var btnSkip: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        rootPane.background = Background(BackgroundFill(Tile.BACKGROUND.brighter(), CornerRadii.EMPTY, Insets.EMPTY))
        minutes.descriptionProperty().bindBidirectional(minuteProperty)
        seconds.descriptionProperty().bindBidirectional(secondProperty)
    }

    fun stopTimer() {
        currentTimer.stop()
    }

    fun startTimer(timerFor: Int) {
        BreakApplication.LOG.info("Starting animation timer")
        updateTiles(timerFor)
        currentTimer = getAnimationTimer(timerFor.toDouble())
        currentTimer.start()
        BreakApplication.LOG.info("Started animation timer")
    }

    private fun getAnimationTimer(duration: Double): AnimationTimer {
        return object : AnimationTimer() {
            var timer = Duration.seconds(duration)
            var lastTimerCall: Long = System.nanoTime()

            override fun handle(now: Long) {
                if (now > lastTimerCall + 1000000000L) {
                    timer = timer.subtract(Duration.seconds(1.0))
                    val remainingSeconds = timer.toSeconds().toInt()
                    val m = remainingSeconds / secondsPerMin
                    val s = remainingSeconds % secondsPerMin
                    if (m == 0 && s == 0) {
                        this.stop()
                    }
                    updateTiles(m, s)
                    lastTimerCall = now
                }
            }

            override fun stop() {
                hideCallback.run()
                BreakApplication.LOG.info("stopped animation timer")
                super.stop()
            }
        }
    }

    private fun updateTiles(seconds: Int) {
        updateTiles(seconds / secondsPerMin, seconds % secondsPerMin)
    }

    private fun updateTiles(m: Int, s: Int) {
        minuteProperty.value = String.format("%02d", m)
        secondProperty.value = String.format("%02d", s)
    }

}