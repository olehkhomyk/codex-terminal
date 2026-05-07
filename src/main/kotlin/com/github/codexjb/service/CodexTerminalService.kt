package com.github.codexjb.service

import com.intellij.openapi.Disposable
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
import java.awt.Toolkit
import java.lang.ref.WeakReference
import javax.swing.Timer

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
class CodexTerminalService(private val project: Project) : Disposable {

    private val log = Logger.getInstance(CodexTerminalService::class.java)
    private var widgetRef: WeakReference<TerminalWidget>? = null
    private var lastCommandSentAtMillis: Long = 0L
    private var soundMonitorTimer: Timer? = null
    private var soundMonitorWidgetRef: WeakReference<TerminalWidget>? = null
    private var lastObservedTerminalTail: String = ""
    private var lastTerminalChangeAtMillis: Long = 0L
    private var lastSoundAtMillis: Long = 0L
    private var pendingReadySound: Boolean = false
    private var pendingReadyOutputChars: Int = 0
    private var lastAttentionFingerprint: String = ""
    private var activeAttentionFingerprint: String = ""
    private var lastAttentionReminderAtMillis: Long = 0L
    private var lastReadyFingerprint: String = ""

    fun launchOrFocus(): TerminalWidget {
        val existing = findCodexTab()
        if (existing != null) {
            widgetRef = WeakReference(existing.widget)
            ensureSoundMonitor(existing.widget)
            focusTerminalContent(existing.content)
            if (shouldStartCodex(existing.widget)) {
                runCodexCommand(existing.widget)
            }
            return existing.widget
        }
        val widget = createNew()
        widgetRef = WeakReference(widget)
        ensureSoundMonitor(widget)
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
            muteCurrentSoundState(widget)
        } catch (t: Throwable) {
            log.warn("Failed to write to Codex terminal", t)
            return
        }
        findCodexTab()?.content?.let { focusTerminalContent(it) } ?: focusTerminalToolWindow()
    }

    override fun dispose() {
        stopSoundMonitor()
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
        ensureSoundMonitor(widget)
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
            muteCurrentSoundState(widget)
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

    private fun ensureSoundMonitor(widget: TerminalWidget) {
        soundMonitorWidgetRef = WeakReference(widget)
        lastObservedTerminalTail = terminalTail(widget)
        lastAttentionFingerprint = attentionFingerprint(lastObservedTerminalTail)
        activeAttentionFingerprint = ""
        lastAttentionReminderAtMillis = 0L
        lastReadyFingerprint = readyFingerprint(lastObservedTerminalTail)
        pendingReadySound = false
        pendingReadyOutputChars = 0
        if (soundMonitorTimer != null) return

        soundMonitorTimer = Timer(SOUND_MONITOR_INTERVAL_MS) {
            checkTerminalForSoundNotification()
        }.also {
            it.initialDelay = SOUND_MONITOR_INTERVAL_MS
            it.start()
        }
    }

    private fun checkTerminalForSoundNotification() {
        val widget = soundMonitorWidgetRef?.get()
        if (widget == null || !isWidgetAlive(widget)) {
            stopSoundMonitor()
            return
        }

        if (!CodexSettings.getInstance(project).state.playNotificationSound) {
            lastObservedTerminalTail = terminalTail(widget)
            lastAttentionFingerprint = attentionFingerprint(lastObservedTerminalTail)
            activeAttentionFingerprint = ""
            lastAttentionReminderAtMillis = 0L
            lastReadyFingerprint = readyFingerprint(lastObservedTerminalTail)
            pendingReadySound = false
            pendingReadyOutputChars = 0
            return
        }

        val now = System.currentTimeMillis()
        val tail = terminalTail(widget)
        if (tail != lastObservedTerminalTail) {
            val addedText = addedTerminalText(lastObservedTerminalTail, tail)
            lastObservedTerminalTail = tail
            lastTerminalChangeAtMillis = now

            if (activeAttentionFingerprint.isNotBlank() && confirmsOrContinues(addedText)) {
                clearActiveAttention()
            }

            val attentionFingerprint = attentionFingerprint(addedText)
            if (attentionFingerprint.isNotBlank() && attentionFingerprint != activeAttentionFingerprint) {
                activeAttentionFingerprint = attentionFingerprint
                lastAttentionFingerprint = attentionFingerprint
                lastAttentionReminderAtMillis = now
                playSound(now)
                pendingReadySound = false
                pendingReadyOutputChars = 0
            } else {
                if (activeAttentionFingerprint.isNotBlank() && outputMovedPastAttention(addedText)) {
                    clearActiveAttention()
                }
                pendingReadyOutputChars += addedText.count { !it.isWhitespace() }
                pendingReadySound = pendingReadyOutputChars >= READY_SOUND_MIN_OUTPUT_CHARS
            }
            return
        }

        if (
            activeAttentionFingerprint.isNotBlank() &&
            now - lastAttentionReminderAtMillis >= ATTENTION_REMINDER_INTERVAL_MS
        ) {
            playSound(now)
            lastAttentionReminderAtMillis = now
        }

        if (
            pendingReadySound &&
            now - lastTerminalChangeAtMillis >= READY_SOUND_QUIET_MS &&
            looksReadyForNextPrompt(tail)
        ) {
            val readyFingerprint = readyFingerprint(tail)
            if (readyFingerprint.isNotBlank() && readyFingerprint != lastReadyFingerprint) {
                playSound(now)
                lastReadyFingerprint = readyFingerprint
            }
            pendingReadySound = false
            pendingReadyOutputChars = 0
        }
    }

    private fun terminalTail(widget: TerminalWidget): String {
        return try {
            widget.getText().toString().takeLast(TERMINAL_TAIL_CHARS)
        } catch (t: Throwable) {
            log.debug("Cannot read Codex terminal text for sound notification detection", t)
            ""
        }
    }

    private fun addedTerminalText(previousTail: String, currentTail: String): String {
        val commonPrefixLength = previousTail.commonPrefixWith(currentTail).length
        return currentTail.substring(commonPrefixLength)
    }

    private fun needsAttentionSound(tail: String): Boolean {
        return ATTENTION_PATTERNS.any { it.containsMatchIn(tail) }
    }

    private fun confirmsOrContinues(addedText: String): Boolean {
        return CONFIRMATION_RESPONSE_PATTERN.containsMatchIn(addedText) ||
            addedText.count { !it.isWhitespace() } >= ATTENTION_CLEAR_MIN_OUTPUT_CHARS
    }

    private fun outputMovedPastAttention(addedText: String): Boolean {
        return addedText.count { !it.isWhitespace() } >= ATTENTION_CLEAR_MIN_OUTPUT_CHARS
    }

    private fun looksReadyForNextPrompt(tail: String): Boolean {
        val lastLine = lastNonBlankLine(tail) ?: return false
        return READY_PATTERNS.any { it.containsMatchIn(lastLine) } ||
            PROMPT_LINE_PATTERN.matches(lastLine)
    }

    private fun attentionFingerprint(tail: String): String {
        val line = tail.lineSequence()
            .filter { it.isNotBlank() }
            .lastOrNull { needsAttentionSound(it) }
            ?: return ""
        return normalizeTerminalLine(line)
    }

    private fun readyFingerprint(tail: String): String {
        return lastNonBlankLine(tail)?.let { normalizeTerminalLine(it) }.orEmpty()
    }

    private fun lastNonBlankLine(text: String): String? {
        return text.lineSequence().lastOrNull { it.isNotBlank() }
    }

    private fun normalizeTerminalLine(line: String): String {
        return line.trim().replace(Regex("\\s+"), " ")
    }

    private fun muteCurrentSoundState(widget: TerminalWidget) {
        val tail = terminalTail(widget)
        lastObservedTerminalTail = tail
        lastAttentionFingerprint = attentionFingerprint(tail)
        clearActiveAttention()
        lastReadyFingerprint = readyFingerprint(tail)
        pendingReadySound = false
        pendingReadyOutputChars = 0
    }

    private fun playSound(now: Long) {
        if (now - lastSoundAtMillis < SOUND_MIN_GAP_MS) return
        try {
            Toolkit.getDefaultToolkit().beep()
            lastSoundAtMillis = now
        } catch (t: Throwable) {
            log.debug("Failed to play Codex notification sound", t)
        }
    }

    private fun clearActiveAttention() {
        activeAttentionFingerprint = ""
        lastAttentionReminderAtMillis = 0L
    }

    private fun stopSoundMonitor() {
        soundMonitorTimer?.stop()
        soundMonitorTimer = null
        soundMonitorWidgetRef = null
        lastObservedTerminalTail = ""
        pendingReadySound = false
        pendingReadyOutputChars = 0
        lastAttentionFingerprint = ""
        clearActiveAttention()
        lastReadyFingerprint = ""
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
        private const val SOUND_MONITOR_INTERVAL_MS: Int = 1_000
        private const val READY_SOUND_QUIET_MS: Long = 2_000L
        private const val READY_SOUND_MIN_OUTPUT_CHARS: Int = 160
        private const val ATTENTION_REMINDER_INTERVAL_MS: Long = 6_000L
        private const val ATTENTION_CLEAR_MIN_OUTPUT_CHARS: Int = 24
        private const val SOUND_MIN_GAP_MS: Long = 1_500L
        private const val TERMINAL_TAIL_CHARS: Int = 4_000
        private val CODEX_CONTENT_KEY: Key<Boolean> = Key.create("com.github.codexjb.codexTerminal")
        private val ATTENTION_PATTERNS = listOf(
            Regex("\\b(do you want to|would you like to|proceed\\?|continue\\?)", RegexOption.IGNORE_CASE),
            Regex("\\b(approve|approval|confirm|confirmation|permission|allow|authorize)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(waiting for|needs?)\\s+(approval|confirmation|input|permission)", RegexOption.IGNORE_CASE),
            Regex("\\b(yes/no|y/n|y/N|Y/n)\\b", RegexOption.IGNORE_CASE),
            Regex("[\\[(](?:y/n|y/N|Y/n|yes/no)[\\])]", RegexOption.IGNORE_CASE),
            Regex("\\b(choose|select)\\b.*\\b(yes|no|approve|allow|continue|proceed)\\b", RegexOption.IGNORE_CASE)
        )
        private val CONFIRMATION_RESPONSE_PATTERN = Regex(
            "(?im)(^|\\R)\\s*(?:y|yes|n|no|1|2|a|allow|approve|approved|confirm|confirmed|continue|proceed|ok)\\s*(?:\\R|$)"
        )
        private val READY_PATTERNS = listOf(
            Regex("\\b(done|completed|finished|ready|implemented)\\b", RegexOption.IGNORE_CASE),
            Regex("\\binvestigation\\s+(finished|complete|completed)\\b", RegexOption.IGNORE_CASE)
        )
        private val PROMPT_LINE_PATTERN = Regex("\\s*(›|>|codex[>›])\\s*")

        fun getInstance(project: Project): CodexTerminalService = project.service()
    }
}
