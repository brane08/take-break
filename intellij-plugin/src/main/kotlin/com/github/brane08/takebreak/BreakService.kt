package com.github.brane08.takebreak

import com.intellij.ide.IdeEventQueue
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BreakService : Disposable {
    private var tick: ScheduledFuture<*>? = null
    private var notice: ScheduledFuture<*>? = null
    private var counter = 0
    private var dialog: BreakDialog? = null
    private var started = false
    var paused = false
        private set

    /** Called per project open; only the first call starts the app-wide schedule. */
    @Synchronized
    fun ensureStarted() {
        if (started) return
        started = true
        reschedule()
    }

    @Synchronized
    fun reschedule() {
        cancel()
        if (paused) return
        val cfg = BreakSettings.getInstance().normalized()
        val pool = AppExecutorUtil.getAppScheduledExecutorService()
        tick = pool.scheduleWithFixedDelay({ onTick() }, cfg.interval.toLong(), cfg.interval.toLong(), TimeUnit.SECONDS)
        scheduleNotice(cfg)
    }

    @Synchronized
    fun togglePaused() {
        paused = !paused
        reschedule()
    }

    fun startNow() = show(force = true)

    private fun onTick() {
        scheduleNotice(BreakSettings.getInstance().normalized())
        show(force = false)
    }

    @Synchronized
    private fun scheduleNotice(cfg: BreakState) {
        notice?.cancel(false)
        if (cfg.notice <= 0) return
        notice = AppExecutorUtil.getAppScheduledExecutorService().schedule({
            NotificationGroupManager.getInstance().getNotificationGroup("Take Break")
                .createNotification("Your break starts soon", NotificationType.INFORMATION).notify(null)
        }, (cfg.interval - cfg.notice).toLong(), TimeUnit.SECONDS)
    }

    private fun show(force: Boolean) {
        ApplicationManager.getApplication().invokeLater({
            val cfg = BreakSettings.getInstance().normalized()
            if (dialog != null) return@invokeLater // drop the tick while a break is showing
            val idleSeconds = IdeEventQueue.getInstance().idleTime / 1000
            if (!force && idleSeconds >= cfg.idleThreshold) return@invokeLater // user already away
            counter += 1
            dialog = BreakDialog(BreakSettings.getInstance().breakSeconds(counter)) { dialog = null }.also { it.show() }
        }, ModalityState.any())
    }

    private fun cancel() {
        tick?.cancel(false)
        notice?.cancel(false)
        tick = null
        notice = null
    }

    override fun dispose() = cancel()

    companion object {
        fun getInstance(): BreakService = ApplicationManager.getApplication().getService(BreakService::class.java)
    }
}
