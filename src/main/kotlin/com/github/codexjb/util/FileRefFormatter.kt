package com.github.codexjb.util

import com.github.codexjb.service.CodexSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/**
 * Formats a file as an `@path` reference for the Codex CLI.
 *
 * Paths are resolved relative to the configured Codex working directory (or project root
 * when the setting is blank), so Codex can resolve them directly.
 *
 * If the file lives outside that working directory (e.g. another module or an external
 * library), falls back to the absolute filesystem path.
 */
object FileRefFormatter {

    fun format(project: Project, vFile: VirtualFile): String {
        val rel = relativeToCodexWorkingDirectory(project, vFile) ?: vFile.path
        return "@$rel"
    }

    private fun relativeToCodexWorkingDirectory(project: Project, vFile: VirtualFile): String? {
        val rootPath = CodexSettings.getInstance(project).state.workingDirectory
            .ifBlank { project.basePath ?: return null }
        val baseDir = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return null
        return VfsUtilCore.getRelativePath(vFile, baseDir, '/')
    }
}
