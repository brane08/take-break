package com.github.brane08.takebreak

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StartBreakAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) = BreakService.getInstance().startNow()
}

class ToggleAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) = BreakService.getInstance().togglePaused()
}

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) = BreakService.getInstance().ensureStarted()
}
