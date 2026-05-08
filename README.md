# Codex in Terminal

**Share the current editor file or selection with the OpenAI [`codex`](https://www.npmjs.com/package/@openai/codex) CLI in one keystroke.**

The core feature: press a shortcut in the editor, and a precise file reference is typed straight into your running Codex session. If text is selected, the reference includes the selected line range. No copy/paste, no re-typing paths, no re-explaining where the file lives.

Inspired by the official [Claude Code JetBrains plugin](https://code.claude.com/docs/en/jetbrains) — same UX, different backend.

## How it works

1. Open any project. Click the **Codex** icon in the right-hand sidebar (or press `Ctrl/Cmd+Alt+Shift+C`). A terminal tab named `Codex` opens and runs the CLI.
2. Switch to any editor, optionally selecting the code you want to point at.
3. Press `Ctrl+Alt+K` (Windows/Linux) or `Cmd+Alt+K` (macOS).
4. The Codex terminal receives, at the cursor — without submitting:

   ```
   @src/main/kotlin/com/github/codexjb/service/CodexTerminalService.kt
   ```

5. Type your question after the reference and press Enter. Codex resolves the file, or the selected line range when a selection was present.

### Reference format

| Editor state | What gets inserted |
|---|---|
| No selection | `@src/main/.../File.kt` |
| Single-line selection | `@src/main/.../File.kt#L42` |
| Multi-line selection | `@src/main/.../File.kt#L25-L40` |

Paths are relative to the configured Codex working directory. If a file is outside that directory, the plugin inserts its absolute path so Codex does not confuse it with a same-named file elsewhere.

## Features

- **Insert file references from the editor** — the headline feature. `Ctrl+Alt+K` / `Cmd+Alt+K`, editor right-click menu, or bind your own shortcut.
- **One-click launch** — click the Codex sidebar icon or press `Ctrl+Alt+Shift+C` / `Cmd+Alt+Shift+C` to drop straight into the CLI.
- **Configurable command** — `Settings → Tools → Codex in Terminal`. Works with `codex`, `npx @openai/codex`, custom paths, or WSL wrappers like `wsl -d Ubuntu -- bash -lic "codex"`.
- **Optional attention sound** — play a system sound when Codex appears to need confirmation or has finished responding.

## Supported IDEs

IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, Android Studio, and any other JetBrains IDE based on platform build `253` (2025.3) or later.

## Installation

### From source

```sh
./gradlew buildPlugin
```

Output: `build/distributions/codex-in-terminal-1.0.3.zip`.

In your IDE: `Settings → Plugins → ⚙ → Install Plugin from Disk…` and select the zip. Restart the IDE.

### Prerequisites

Install the Codex CLI on your system:

```sh
npm install -g @openai/codex
```

Or configure the plugin to use `npx @openai/codex` (no global install required).

## Keybindings

Both shortcuts are rebindable under `Settings → Keymap → Plugins → Codex in Terminal`.

| Action | Windows/Linux | macOS |
|---|---|---|
| Insert File Reference into Codex | `Ctrl+Alt+K` | `Cmd+Alt+K` |
| Launch Codex | `Ctrl+Alt+Shift+C` | `Cmd+Alt+Shift+C` |

## Settings

`Settings → Tools → Codex in Terminal`

| Setting | Default | Notes |
|---|---|---|
| Codex command | `codex` | Anything runnable in the integrated terminal shell. |
| Working directory | *(project root)* | Where the shell starts. |
| Play sound when Codex needs attention or finishes responding | Off | Uses the system beep for confirmations, gentle reminders, and likely ready prompts. |

## Known limitations

- **Reworked Terminal (experimental)** — if you've enabled JetBrains' new terminal via Registry, text injection may not work. Stay on the classic terminal for now.
- **Paths with spaces** — the unquoted `@path` / `@path#Lx-Ly` format may misparse on some shells. File an issue if you hit this.

## Building

Requires JDK 17+. First build downloads ~1 GB of IDEA Community SDK.

```sh
./gradlew buildPlugin     # produces the installable zip
./gradlew runIde          # launches a sandbox IDE with the plugin installed
```

## License

MIT. See [LICENSE](LICENSE).
