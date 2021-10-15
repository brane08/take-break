package brane08.fx.takebreak

import eu.hansolo.tilesfx.Tile
import eu.hansolo.tilesfx.Tile.SkinType
import eu.hansolo.tilesfx.TileBuilder
import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.*
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import javafx.util.Duration


class Countdown : VBox() {
    private val minutes: Tile = createTile("MINUTES", "0")
    private val seconds: Tile = createTile("SECONDS", "0")
    private val btnSkip = Button("Skip Break!")
    private lateinit var currentTimer: AnimationTimer

    init {
        this.background = Background(BackgroundFill(Tile.BACKGROUND.brighter(), CornerRadii.EMPTY, Insets.EMPTY))
        this.spacing = 10.0
        val upper = HBox(20.0, minutes, seconds)
        upper.padding = Insets(10.0)
        upper.alignment = Pos.CENTER
        this.children.add(upper)
        val controls = HBox(20.0, btnSkip)
        controls.padding = Insets(10.0)
        controls.alignment = Pos.CENTER
        this.children.add(controls)
        this.padding = Insets(10.0)
        btnSkip.onAction = EventHandler {
            stopTimer()
        }
    }

    fun startTimer(forSeconds: Int = 0, stage: Stage, cleanup: () -> Unit) {
        BreakWindow.LOG.info("Starting animation timer")
        updateTiles(forSeconds)
        stage.show()
        currentTimer = Animator(Duration.seconds(forSeconds.toDouble()), cleanup)
        currentTimer.start()
        BreakWindow.LOG.info("Started animation timer")
    }

    fun stopTimer() {
        currentTimer.stop()
    }

    private fun updateTiles(seconds: Int) {
        updateTiles(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }

    private fun updateTiles(m: Int, s: Int) {
        minutes.description = String.format("%02d", m)
        seconds.description = String.format("%02d", s)
    }

    private fun createTile(title: String, text: String): Tile {
        return TileBuilder.create().skinType(SkinType.CHARACTER)
            .prefSize(200.0, 200.0)
            .title(title)
            .titleAlignment(TextAlignment.CENTER)
            .description(text)
            .build()
    }

    private inner class Animator(
        var duration: Duration,
        var hide: () -> Unit
    ) : AnimationTimer() {

        var lastTimerCall: Long = System.nanoTime()

        override fun handle(now: Long) {
            if (now > lastTimerCall + 1000000000L) {
                duration = duration.subtract(Duration.seconds(1.0))
                val remainingSeconds = duration.toSeconds().toInt()
                val m = remainingSeconds / SECONDS_PER_MINUTE
                val s = remainingSeconds % SECONDS_PER_MINUTE
                if (m == 0 && s == 0) {
                    this.stop()
                }
                updateTiles(m, s)
                lastTimerCall = now
            }
        }

        override fun stop() {
            hide()
            BreakWindow.LOG.info("stopped animation timer")
            super.stop()
        }

    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60
    }
}