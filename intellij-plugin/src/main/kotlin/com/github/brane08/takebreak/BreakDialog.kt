package com.github.brane08.takebreak

import com.intellij.openapi.ui.DialogWrapper
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

class BreakDialog(seconds: Int, private val onEnd: () -> Unit) : DialogWrapper(true) {
    private var remaining = seconds
    private val label = JLabel(format(), SwingConstants.CENTER).apply { font = font.deriveFont(Font.BOLD, 48f) }
    private val timer = Timer(1000) {
        remaining -= 1
        if (remaining <= 0) close(OK_EXIT_CODE) else label.text = format()
    }

    init {
        title = "Time for a Break"
        setOKButtonText("Skip Break")
        init()
        timer.start()
    }

    override fun createCenterPanel(): JComponent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(16, 32, 16, 32)
        add(JLabel("Step away from the screen."))
        add(label)
    }

    override fun createActions() = arrayOf(okAction)

    override fun dispose() {
        timer.stop()
        super.dispose()
        onEnd()
    }

    private fun format() = "%02d:%02d".format(remaining / 60, remaining % 60)
}
