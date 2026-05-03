package com.github.codexjb.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.lang.ref.WeakReference

/**
 * Owns the per-project "Codex" terminal widget.
 *
 * - [launchOrFocus] creates the tab and runs the configured Codex command if no live tab exists,
 *   focuses the existing Codex tab if it is still running, or restarts Codex in that tab when
 *   the terminal is still open but the foreground command has exited.
 * - [insertText] writes raw text into the live tab's PTY **without** pressing Enter, so the user
 *   can append their own prompt before submitting.
 */
@Service(Service.Level.PROJECT)
class CodexTerminalService(private val project: Project) {

    private val log = Logger.getInstance(CodexTerminalService::class.java)
    private var widgetRef: WeakReference<TerminalWidget>? = null
    private var lastCommandSentAtMillis: Long = 0L

    fun launchOrFocus(): TerminalWidget {
        val existing = findCodexTab()
        if (existing != null) {
            widgetRef = WeakReference(existing.widget)
            focusTerminalContent(existing.content)
            if (shouldStartCodex(existing.widget)) {
                runCodexCommand(existing.widget)
            }
            return existing.widget
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
        findCodexTab()?.content?.let { focusTerminalContent(it) } ?: focusTerminalToolWindow()
    }

    private fun findCodexTab(): TerminalTab? {
        val manager = TerminalToolWindowManager.getInstance(project)
        val cached = cachedAlive()
        if (cached != null) {
            val content = manager.getContainer(cached)?.content
            if (content != null && content.isValid) {
                markCodexContent(content)
                return TerminalTab(content, cached)
            }
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TERMINAL_TOOL_WINDOW_ID)
            ?: return null
        return toolWindow.contentManager.contents
            .asSequence()
            .filter { isCodexContent(it) }
            .mapNotNull { content ->
                val widget = TerminalToolWindowManager.findWidgetByContent(content)
                if (widget != null && isWidgetAlive(widget)) TerminalTab(content, widget) else null
            }
            .firstOrNull()
    }

    private fun createNew(): TerminalWidget {
        val settings = CodexSettings.getInstance(project).state
        val workDir = settings.workingDirectory.ifBlank { project.basePath }
        val manager = TerminalToolWindowManager.getInstance(project)
        val widget = manager.createShellWidget(workDir, TAB_NAME, true, true)
        manager.getContainer(widget)?.content?.let { markCodexContent(it) }
        // Run the Codex CLI in the fresh tab. Pressing Enter is intentional.
        runCodexCommand(widget)
        return widget
    }

    private fun focusTerminalContent(content: Content) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TERMINAL_TOOL_WINDOW_ID)
            ?: return
        val selectContent = Runnable {
            val manager = content.manager ?: toolWindow.contentManager
            if (content.isValid && manager.getIndexOfContent(content) >= 0) {
                manager.setSelectedContent(content, true)
                manager.requestFocus(content, true)
            }
        }
        if (toolWindow.isActive) {
            selectContent.run()
        } else {
            toolWindow.activate(selectContent, true)
        }
    }

    private fun focusTerminalToolWindow() {
        ToolWindowManager.getInstance(project)
            .getToolWindow(TERMINAL_TOOL_WINDOW_ID)
            ?.activate(null, true)
    }

    private fun shouldStartCodex(widget: TerminalWidget): Boolean {
        if (System.currentTimeMillis() - lastCommandSentAtMillis < COMMAND_START_GRACE_MS) return false
        val hasRunningCommand = hasRunningCommand(widget)
        return hasRunningCommand == false
    }

    private fun runCodexCommand(widget: TerminalWidget) {
        val command = CodexSettings.getInstance(project).state.codexCommand
            .ifBlank { CodexSettings.DEFAULT_COMMAND }
        try {
            widget.sendCommandToExecute(command)
            lastCommandSentAtMillis = System.currentTimeMillis()
        } catch (t: Throwable) {
            log.warn("Failed to start Codex command in terminal", t)
        }
    }

    private fun hasRunningCommand(widget: TerminalWidget): Boolean? {
        val shellWidget = ShellTerminalWidget.asShellJediTermWidget(widget) ?: return null
        return try {
            shellWidget.hasRunningCommands()
        } catch (t: Throwable) {
            log.debug("Cannot determine terminal command state; leaving existing tab untouched", t)
            null
        }
    }

    private fun cachedAlive(): TerminalWidget? {
        val widget = widgetRef?.get() ?: return null
        return if (isWidgetAlive(widget)) widget else null
    }

    private fun isWidgetAlive(widget: TerminalWidget): Boolean {
        return try {
            !Disposer.isDisposed(widget)
        } catch (t: Throwable) {
            log.debug("Liveness check threw; treating widget as disposed", t)
            false
        }
    }

    private fun isCodexContent(content: Content): Boolean {
        return content.getUserData(CODEX_CONTENT_KEY) == true ||
            content.displayName == TAB_NAME ||
            content.tabName == TAB_NAME ||
            content.toolwindowTitle == TAB_NAME
    }

    private fun markCodexContent(content: Content) {
        content.putUserData(CODEX_CONTENT_KEY, true)
    }

    private data class TerminalTab(val content: Content, val widget: TerminalWidget)

    companion object {
        const val TAB_NAME: String = "Codex"
        private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"
        private const val COMMAND_START_GRACE_MS: Long = 2_000L
        private val CODEX_CONTENT_KEY: Key<Boolean> = Key.create("com.github.codexjb.codexTerminal")

        fun getInstance(project: Project): CodexTerminalService = project.service()
    }
}
