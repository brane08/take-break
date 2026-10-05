package com.github.brane08.takebreak

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** All durations are in seconds, matching the desktop app. */
data class BreakState(
    var shortBreak: Int = 60,
    var longBreak: Int = 300,
    var interval: Int = 1080,
    var notice: Int = 30,
    var idleThreshold: Int = 300,
)

@State(name = "TakeBreakSettings", storages = [Storage("take-break.xml")])
class BreakSettings : PersistentStateComponent<BreakState> {
    private var state = BreakState()

    override fun getState(): BreakState = state
    override fun loadState(s: BreakState) { state = s }

    /** Mirrors BreakConfig.normalized(): interval outlasts the longest break, notice precedes the break. */
    fun normalized(): BreakState {
        val interval = maxOf(state.interval, maxOf(state.longBreak, state.shortBreak) + 1)
        return state.copy(interval = interval, notice = minOf(state.notice, interval - 1))
    }

    /** Every third break is a long one. */
    fun breakSeconds(instance: Int): Int = if (instance % 3 == 0) state.longBreak else state.shortBreak

    companion object {
        fun getInstance(): BreakSettings = ApplicationManager.getApplication().getService(BreakSettings::class.java)
    }
}
