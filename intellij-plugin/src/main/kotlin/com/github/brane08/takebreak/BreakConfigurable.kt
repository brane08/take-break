package com.github.brane08.takebreak

import com.intellij.openapi.options.Configurable
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class BreakConfigurable : Configurable {
    private val fields = linkedMapOf(
        "Short Break Duration (seconds):" to spinner(10, 120),
        "Long Break Duration (seconds):" to spinner(60, 600),
        "Break Interval (seconds):" to spinner(60, 3600),
        "Advance Notice (seconds):" to spinner(0, 120),
        "Idle Threshold (seconds):" to spinner(60, 600),
    )
    private val spinners get() = fields.values.toList()

    override fun getDisplayName() = "Take Break"

    override fun createComponent(): JComponent {
        reset()
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        fields.forEach { (text, spinner) ->
            panel.add(JPanel().apply { add(JLabel(text)); add(spinner) })
        }
        return panel
    }

    override fun isModified(): Boolean = values() != BreakSettings.getInstance().state

    override fun apply() {
        BreakSettings.getInstance().loadState(values())
        BreakService.getInstance().reschedule()
    }

    override fun reset() {
        val s = BreakSettings.getInstance().state
        listOf(s.shortBreak, s.longBreak, s.interval, s.notice, s.idleThreshold)
            .forEachIndexed { i, v -> spinners[i].value = v }
    }

    private fun values() = spinners.map { it.value as Int }.let {
        BreakState(it[0], it[1], it[2], it[3], it[4])
    }

    private fun spinner(min: Int, max: Int) = JSpinner(SpinnerNumberModel(min, min, max, 1))
}
