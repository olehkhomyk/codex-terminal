package com.github.codexjb.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.lang.ref.WeakReference

/**
 * Owns the per-project "Codex" terminal widget.
 *
 * - [launchOrFocus] creates the tab and runs the configured Codex command if no live tab exists;
 *   otherwise it focuses the existing tab without re-running the command.
 * - [insertText] writes raw text into the live tab's PTY **without** pressing Enter, so the user
 *   can append their own prompt before submitting.
 */
@Service(Service.Level.PROJECT)
class CodexTerminalService(private val project: Project) {

    private val log = Logger.getInstance(CodexTerminalService::class.java)
    private var widgetRef: WeakReference<TerminalWidget>? = null

    fun launchOrFocus(): TerminalWidget {
        val existing = cachedAlive()
        if (existing != null) {
            focusTerminalToolWindow()
            return existing
        }
        val widget = createNew()
        widgetRef = WeakReference(widget)
        return widget
    }

    fun insertText(text: String) {
        val widget = launchOrFocus()
        val connector = widget.ttyConnector
        if (connector == null) {
            log.warn(
                "Codex TTY connector is not ready; cannot insert text yet. " +
                    "Try again once the terminal has finished starting."
            )
            return
        }
        try {
            connector.write(text)
        } catch (t: Throwable) {
            log.warn("Failed to write to Codex terminal", t)
            return
        }
        focusTerminalToolWindow()
    }

    private fun cachedAlive(): TerminalWidget? {
        val widget = widgetRef?.get() ?: return null
        return if (isWidgetAlive(widget)) widget else null
    }

    private fun createNew(): TerminalWidget {
        val settings = CodexSettings.getInstance(project).state
        val workDir = settings.workingDirectory.ifBlank { project.basePath }
        val manager = TerminalToolWindowManager.getInstance(project)
        val widget = manager.createShellWidget(workDir, TAB_NAME, true, true)
        // Run the Codex CLI in the fresh tab. Pressing Enter is intentional.
        widget.sendCommandToExecute(settings.codexCommand)
        return widget
    }

    private fun focusTerminalToolWindow() {
        ToolWindowManager.getInstance(project)
            .getToolWindow(TERMINAL_TOOL_WINDOW_ID)
            ?.activate(null, true)
    }

    private fun isWidgetAlive(widget: TerminalWidget): Boolean {
        return try {
            !Disposer.isDisposed(widget)
        } catch (t: Throwable) {
            log.debug("Liveness check threw; treating widget as disposed", t)
            false
        }
    }

    companion object {
        const val TAB_NAME: String = "Codex"
        private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"
        fun getInstance(project: Project): CodexTerminalService = project.service()
    }
}
