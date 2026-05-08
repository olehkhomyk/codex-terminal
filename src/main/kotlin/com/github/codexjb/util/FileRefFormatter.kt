package com.github.codexjb.util

import com.github.codexjb.service.CodexSettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/**
 * Formats an editor file reference for the Codex CLI.
 *
 * Paths are resolved relative to the configured Codex working directory (or project root
 * when the setting is blank), so Codex can resolve them directly.
 *
 * If the file lives outside that working directory (e.g. another module or an external
 * library), falls back to the absolute filesystem path.
 */
object FileRefFormatter {

    fun format(project: Project, vFile: VirtualFile, editor: Editor): String {
        val rel = relativeToCodexWorkingDirectory(project, vFile) ?: vFile.path
        val selection = editor.selectionModel
        if (!selection.hasSelection()) return "@$rel"

        val document = editor.document
        val start = document.getLineNumber(selection.selectionStart) + 1
        val rawEnd = document.getLineNumber(selection.selectionEnd) + 1
        // If the selection ends at the very start of a line, the visual last line is above.
        val end = if (
            selection.selectionEnd > 0 &&
            rawEnd > start &&
            document.getLineStartOffset(rawEnd - 1) == selection.selectionEnd
        ) rawEnd - 1 else rawEnd

        return if (start == end) "@$rel#L$start" else "@$rel#L$start-L$end"
    }

    private fun relativeToCodexWorkingDirectory(project: Project, vFile: VirtualFile): String? {
        val rootPath = CodexSettings.getInstance(project).state.workingDirectory
            .ifBlank { project.basePath ?: return null }
        val baseDir = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return null
        return VfsUtilCore.getRelativePath(vFile, baseDir, '/')
    }
}
