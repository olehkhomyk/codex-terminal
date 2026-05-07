plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.github.codexjb"
version = "1.0.2"

val pluginId = "com.github.codexjb"
val pluginName = "Codex in Terminal"
val pluginVersion = project.version.toString()
val platformVersion = providers.gradleProperty("platformVersion").orElse("2025.3.3")
val pluginSinceBuild = providers.gradleProperty("pluginSinceBuild").orElse("253")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(platformVersion.get(), useInstaller = false)
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = pluginId
        name = pluginName
        version = pluginVersion
        description = """
            <p><b>Share the current editor file with the <code>codex</code> CLI in one keystroke.</b></p>
            <p>The core feature: press a shortcut in the editor, and a precise file reference is typed
            straight into your running Codex session. No copy/paste, no re-typing paths,
            no re-explaining where the file lives.</p>
            <p>Example:</p>
            <pre><code>@src/main/kotlin/com/github/codexjb/service/CodexTerminalService.kt</code></pre>
            <p>Codex resolves the reference automatically from the inserted path.</p>
            <p>Additional features:</p>
            <ul>
              <li><b>One-click launch</b> &mdash; a sidebar icon (or <kbd>Ctrl/Cmd+Alt+Shift+C</kbd>) starts <code>codex</code> in a dedicated terminal tab.</li>
              <li><b>Editor file references</b> &mdash; inserts the current editor file path without adding a line suffix.</li>
              <li><b>Working-directory relative paths</b> &mdash; references match exactly what Codex sees as its working directory, with absolute paths for files outside it.</li>
              <li><b>Optional attention sound</b> &mdash; play a system sound when Codex appears to need confirmation or has finished responding.</li>
              <li><b>Configurable command</b> &mdash; <code>codex</code>, <code>npx @openai/codex</code>, custom binaries, or WSL wrappers.</li>
            </ul>
            <p>Works across IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, and Android Studio.</p>
        """.trimIndent()
        changeNotes = """
            <p><b>1.0.2 &mdash; better confirmation sounds and file-only references</b></p>
            <ul>
              <li><b>Improved confirmation sounds</b>: sound notifications now track active confirmation prompts, remind while a prompt is waiting, and stop as soon as the terminal shows an answer or continued output.</li>
              <li><b>Improved confirmation detection</b>: catches more approval/permission/yes-no style prompts without re-triggering from stale terminal history.</li>
              <li><b>Changed file references</b>: the insert shortcut now sends only <code>@path</code>, without adding line numbers from the caret or selection.</li>
              <li><b>Fixed file reference insertion</b>: the shortcut now uses the actual open editor document, so same-named files in different folders no longer resolve to the wrong file.</li>
              <li><b>Fixed Codex terminal focus</b>: clicking the Codex icon now selects the existing Codex terminal tab instead of merely opening whatever Terminal tab was active.</li>
              <li><b>Fixed stopped agent recovery</b>: if the Codex tab is still open but the agent process has exited, clicking the icon reruns the configured Codex command in that tab.</li>
              <li><b>Improved path resolution</b>: file links are relative to the configured Codex working directory, with absolute paths for files outside it.</li>
              <li><b>Added optional sound notifications</b>: enable a system sound when Codex appears to need confirmation or has finished responding.</li>
            </ul>
        """.trimIndent()
        ideaVersion {
            sinceBuild = pluginSinceBuild.get()
        }
        vendor {
            name = "Oleh Khomyk"
            email = "oleh.khomyk@gmail.com"
        }
    }
    instrumentCode = false
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(21)
}
