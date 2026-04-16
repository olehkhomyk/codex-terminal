# Codex Terminal

**Share editor selections with the OpenAI [`codex`](https://www.npmjs.com/package/@openai/codex) CLI in one keystroke.**

The core feature: select any code in the editor, press a shortcut, and a precise file + line-range reference is typed straight into your running Codex session. No copy/paste, no re-typing paths, no re-explaining where the code lives.

Inspired by the official [Claude Code JetBrains plugin](https://code.claude.com/docs/en/jetbrains) — same UX, different backend.

## How it works

1. Open any project. Click the **Codex** icon in the right-hand sidebar (or press `Ctrl/Cmd+Alt+Shift+C`). A terminal tab named `Codex` opens and runs the CLI.
2. Switch to any editor. Select lines 12–34 of a file.
3. Press `Ctrl+Alt+K` (Windows/Linux) or `Cmd+Alt+K` (macOS).
4. The Codex terminal receives, at the cursor — without submitting:

   ```
   @src/main/kotlin/com/github/codexjb/service/CodexTerminalService.kt#L12-L34 
   ```

5. Type your question after the reference and press Enter. Codex resolves the range and reads exactly those lines.

### Reference format

| Editor state | What gets inserted |
|---|---|
| Selection spanning lines 12–34 | `@src/main/.../File.kt#L12-L34` |
| Caret on line 42, no selection | `@src/main/.../File.kt#L42` |
| Single-line selection on line 42 | `@src/main/.../File.kt#L42` |

Paths are relative to the project root, so they match Codex's working directory and resolve without extra context.

## Features

- **Insert file references from the editor** — the headline feature. `Ctrl+Alt+K` / `Cmd+Alt+K`, editor right-click menu, or bind your own shortcut.
- **One-click launch** — click the Codex sidebar icon or press `Ctrl+Alt+Shift+C` / `Cmd+Alt+Shift+C` to drop straight into the CLI.
- **Configurable command** — `Settings → Tools → Codex [Beta]`. Works with `codex`, `npx @openai/codex`, custom paths, or WSL wrappers like `wsl -d Ubuntu -- bash -lic "codex"`.

## Supported IDEs

IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, Android Studio, and any other JetBrains IDE based on platform build `253` (2025.3) or later.

## Installation

### From source

```sh
./gradlew buildPlugin
```

Output: `build/distributions/codex-for-jetbrains-1.0.0.zip`.

In your IDE: `Settings → Plugins → ⚙ → Install Plugin from Disk…` and select the zip. Restart the IDE.

### Prerequisites

Install the Codex CLI on your system:

```sh
npm install -g @openai/codex
```

Or configure the plugin to use `npx @openai/codex` (no global install required).

## Keybindings

Both shortcuts are rebindable under `Settings → Keymap → Plugins → Codex Terminal`.

| Action | Windows/Linux | macOS |
|---|---|---|
| Insert File Reference into Codex | `Ctrl+Alt+K` | `Cmd+Alt+K` |
| Launch Codex | `Ctrl+Alt+Shift+C` | `Cmd+Alt+Shift+C` |

## Settings

`Settings → Tools → Codex [Beta]`

| Setting | Default | Notes |
|---|---|---|
| Codex command | `codex` | Anything runnable in the integrated terminal shell. |
| Working directory | *(project root)* | Where the shell starts. |

## Known limitations

- **Reworked Terminal (experimental)** — if you've enabled JetBrains' new terminal via Registry, text injection may not work. Stay on the classic terminal for now.
- **Paths with spaces** — the unquoted `@path#Lx-Ly` format may misparse on some shells. File an issue if you hit this.

## Building

Requires JDK 17+. First build downloads ~1 GB of IDEA Community SDK.

```sh
./gradlew buildPlugin     # produces the installable zip
./gradlew runIde          # launches a sandbox IDE with the plugin installed
```

## License

MIT. See [LICENSE](LICENSE).
