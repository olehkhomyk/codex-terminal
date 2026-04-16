package com.github.codexjb.listener

import com.github.codexjb.service.CodexTerminalService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

/**
 * Turns the "Codex" tool window stripe icon into a one-click launcher.
 *
 * Each time the user activates the Codex tool window, we:
 *  1. Hide the (otherwise empty) Codex panel right back.
 *  2. Open the Terminal tool window and run/focus the Codex CLI.
 *
 * This mirrors the Claude Code [Beta] plugin's behavior: clicking the sidebar
 * icon drops you straight into the CLI, with no intermediate UI.
 */
class CodexToolWindowListener(private val project: Project) : ToolWindowManagerListener {

    override fun toolWindowShown(toolWindow: ToolWindow) {
        if (toolWindow.id != TOOL_WINDOW_ID) return
        // Defer to avoid mutating tool-window state while a show() is in flight.
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                toolWindow.hide(null)
                CodexTerminalService.getInstance(project).launchOrFocus()
            }
        }
    }

    companion object {
        private const val TOOL_WINDOW_ID = "Codex"
    }
}
