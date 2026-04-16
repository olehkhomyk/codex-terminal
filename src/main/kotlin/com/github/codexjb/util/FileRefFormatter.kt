package com.github.codexjb.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.LocalFileSystem

/**
 * Formats an editor location as an `@path#Lstart-Lend` reference for the Codex CLI.
 *
 * Paths are resolved relative to the **project root** (so Codex, which runs with the
 * project root as its working directory, can resolve them directly). Line numbers
 * are 1-indexed and ranges use the `#Lstart-Lend` form (Codex requires the second `L`).
 *
 * If the file lives outside the project root (e.g. an external library), falls back
 * to the absolute filesystem path.
 */
object FileRefFormatter {

    fun format(project: Project, vFile: VirtualFile, editor: Editor): String {
        val rel = relativeToProjectRoot(project, vFile) ?: vFile.path
        val doc = editor.document
        val sel = editor.selectionModel
        return if (sel.hasSelection()) {
            val start = doc.getLineNumber(sel.selectionStart) + 1
            val rawEnd = doc.getLineNumber(sel.selectionEnd) + 1
            // If the selection ends at the very start of a line, the "visual" last line
            // is the one above (user selected through the newline).
            val end = if (
                sel.selectionEnd > 0 &&
                rawEnd > start &&
                doc.getLineStartOffset(rawEnd - 1) == sel.selectionEnd
            ) rawEnd - 1 else rawEnd
            if (start == end) "@$rel#L$start" else "@$rel#L$start-L$end"
        } else {
            val line = doc.getLineNumber(editor.caretModel.offset) + 1
            "@$rel#L$line"
        }
    }

    private fun relativeToProjectRoot(project: Project, vFile: VirtualFile): String? {
        val basePath = project.basePath ?: return null
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        return VfsUtilCore.getRelativePath(vFile, baseDir, '/')
    }
}
