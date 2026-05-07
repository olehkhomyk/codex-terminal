package com.github.codexjb.actions

import com.github.codexjb.service.CodexTerminalService
import com.github.codexjb.util.FileRefFormatter
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager

class InsertCodexRefAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            e.project != null &&
                editor != null &&
                FileDocumentManager.getInstance().getFile(editor.document) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val vFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        val ref = FileRefFormatter.format(project, vFile)
        // Trailing space so the user can immediately type a question after the ref.
        CodexTerminalService.getInstance(project).insertText("$ref ")
    }
}
