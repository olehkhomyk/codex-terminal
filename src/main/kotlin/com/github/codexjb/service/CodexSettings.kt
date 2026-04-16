package com.github.codexjb.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "CodexSettings", storages = [Storage("codex.xml")])
class CodexSettings : PersistentStateComponent<CodexSettings.State> {

    data class State(
        var codexCommand: String = DEFAULT_COMMAND,
        /**
         * Working directory to start the Codex shell in.
         * Empty string falls back to the project's base path at launch time.
         */
        var workingDirectory: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(newState: State) {
        state = newState
    }

    companion object {
        const val DEFAULT_COMMAND: String = "codex"

        fun getInstance(project: Project): CodexSettings = project.service()
    }
}
