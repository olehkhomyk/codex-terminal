# Changelog

## 1.0.0 — Initial release

### Headline feature

- **Insert file references into Codex from the editor** (`Ctrl+Alt+K` / `Cmd+Alt+K`). Sends `@path#Lstart-Lend` for the current selection, or `@path#Lline` for the caret position, straight into the running Codex terminal — so Codex reads exactly the lines you pointed at with no manual copy/paste.

### Also included

- One-click launcher: click the Codex sidebar icon or press `Ctrl+Alt+Shift+C` / `Cmd+Alt+Shift+C` to open `codex` in a dedicated terminal tab.
- Paths are resolved relative to the project root, so references match Codex's own working directory.
- `Settings → Tools → Codex [Beta]` page with configurable command and working directory.
- Works on IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, Android Studio, and other JetBrains IDEs on platform build 253+.
