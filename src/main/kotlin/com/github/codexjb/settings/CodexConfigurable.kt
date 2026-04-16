package com.github.codexjb.settings

import com.github.codexjb.service.CodexSettings
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

class CodexConfigurable(private val project: Project) : Configurable {

    private var panel: JPanel? = null
    private lateinit var commandField: JBTextField
    private lateinit var workDirField: TextFieldWithBrowseButton

    override fun getDisplayName(): String = "Codex [Beta]"

    override fun createComponent(): JComponent {
        commandField = JBTextField()
        commandField.emptyText.text = CodexSettings.DEFAULT_COMMAND

        workDirField = TextFieldWithBrowseButton()
        val folderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Codex Working Directory")
            .withDescription("Directory to start the Codex shell in. Leave blank to use the project root.")
        workDirField.addBrowseFolderListener(project, folderDescriptor)

        val hint = JBLabel(
            "<html>Examples: <code>codex</code>, <code>/usr/local/bin/codex</code>, " +
                "<code>npx @openai/codex</code>, " +
                "<code>wsl -d Ubuntu -- bash -lic \"codex\"</code></html>"
        )

        val built = FormBuilder.createFormBuilder()
            .addLabeledComponent("Codex command:", commandField, 1, false)
            .addComponentToRightColumn(hint)
            .addLabeledComponent("Working directory:", workDirField, 1, false)
            .addComponentToRightColumn(
                JBLabel("<html><i>Leave blank to use the project root.</i></html>")
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel

        built.border = JBUI.Borders.empty(10)
        panel = built
        reset()
        return built
    }

    override fun isModified(): Boolean {
        val state = CodexSettings.getInstance(project).state
        return commandField.text != state.codexCommand ||
            workDirField.text != state.workingDirectory
    }

    override fun apply() {
        val settings = CodexSettings.getInstance(project)
        val current = settings.state
        current.codexCommand = commandField.text.ifBlank { CodexSettings.DEFAULT_COMMAND }
        current.workingDirectory = workDirField.text.trim()
    }

    override fun reset() {
        val state = CodexSettings.getInstance(project).state
        commandField.text = state.codexCommand
        workDirField.text = state.workingDirectory
    }

    override fun disposeUIResources() {
        panel = null
    }
}
