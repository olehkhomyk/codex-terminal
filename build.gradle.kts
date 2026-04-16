plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.github.codexjb"
version = "1.0.0"

val pluginId = "com.github.codexjb"
val pluginName = "Codex Terminal"
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
            <p><b>Share editor selections with the <code>codex</code> CLI in one keystroke.</b></p>
            <p>The core feature: select any code in the editor, press a shortcut, and a precise
            file + line-range reference is typed straight into your running Codex session.
            No copy/paste, no re-typing paths, no re-explaining where the code lives.</p>
            <p>Example &mdash; selecting lines 12&ndash;34 of a file sends this to Codex:</p>
            <pre><code>@src/main/kotlin/com/github/codexjb/service/CodexTerminalService.kt#L12-L34</code></pre>
            <p>Codex resolves the reference automatically and reads the exact lines you pointed at.</p>
            <p>Additional features:</p>
            <ul>
              <li><b>One-click launch</b> &mdash; a sidebar icon (or <kbd>Ctrl/Cmd+Alt+Shift+C</kbd>) starts <code>codex</code> in a dedicated terminal tab.</li>
              <li><b>Selection &amp; caret aware</b> &mdash; single line <code>#L42</code>, range <code>#L12-L34</code>, no selection &rarr; current caret line.</li>
              <li><b>Project-root relative paths</b> &mdash; references match exactly what Codex sees as its working directory.</li>
              <li><b>Configurable command</b> &mdash; <code>codex</code>, <code>npx @openai/codex</code>, custom binaries, or WSL wrappers.</li>
            </ul>
            <p>Works across IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, and Android Studio.</p>
        """.trimIndent()
        changeNotes = """
            <p>Initial public release (1.0.0).</p>
            <ul>
              <li><b>Insert file references into Codex</b> (<kbd>Ctrl/Cmd+Alt+K</kbd>) &mdash; sends
                  <code>@path#Lstart-Lend</code> for the current selection, or <code>@path#Lline</code>
                  for the caret position, straight to the running Codex terminal.</li>
              <li>One-click launcher: click the Codex sidebar icon or press
                  <kbd>Ctrl/Cmd+Alt+Shift+C</kbd> to open <code>codex</code> in a dedicated terminal tab.</li>
              <li>Settings page under <b>Tools &rarr; Codex [Beta]</b> with configurable command
                  and working directory.</li>
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
