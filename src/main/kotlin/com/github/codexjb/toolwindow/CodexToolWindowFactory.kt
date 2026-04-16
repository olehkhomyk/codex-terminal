package com.github.codexjb.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel

/**
 * The Codex tool window only exists to give us a sidebar stripe button.
 * Its content is intentionally empty — [com.github.codexjb.listener.CodexToolWindowListener]
 * intercepts every activation and instead launches Codex in the Terminal
 * tool window, then immediately hides this window.
 */
class CodexToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(JPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
