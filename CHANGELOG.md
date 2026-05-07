# Changelog

## 1.0.2 — Better confirmation sounds and file-only references

### Changed

- File references now insert only `@path`, without adding a `#L...` line suffix from the caret or selection.

### Improved

- Sound notifications now track an active confirmation prompt instead of scanning stale terminal history.
- Confirmation sounds cover more approval, permission, continue, proceed, yes/no, and `y/n` prompt styles.
- While a confirmation is waiting, the plugin can remind with a gentle repeat beep.
- Beeping stops as soon as the terminal shows a confirmation response (`y`, `yes`, `no`, `1`, `approve`, `continue`, etc.) or output continues after the prompt.

## 1.0.1 — Terminal sync, safer file links, and optional sounds

### Added

- Optional sound notifications. Enable **Play sound when Codex needs attention or finishes responding** in `Settings → Tools → Codex [Beta]` to play the system sound when Codex appears to need confirmation, approval, input, or has settled after a response.

### Fixed

- File references now insert only the file path, without adding a `#L...` line suffix from the caret or selection.
- File references now use the actual open editor document. This fixes the shortcut linking the wrong file when two files have the same name in different folders.
- File links are now resolved relative to the configured Codex working directory, with absolute paths used for files outside it. This keeps references aligned with what Codex can actually resolve.
- Clicking the Codex sidebar icon now selects the existing `Codex` terminal tab instead of just opening the Terminal tool window on whichever tab was last active.
- If the `Codex` terminal tab is still open but the agent process has stopped, clicking the Codex icon now reruns the configured Codex command in that same tab.

## 1.0.0 — Initial release of Codex in Terminal

### Headline feature

- **Insert file references into Codex from the editor** (`Ctrl+Alt+K` / `Cmd+Alt+K`). Sends `@path#Lstart-Lend` for the current selection, or `@path#Lline` for the caret position, straight into the running Codex terminal — so Codex reads exactly the lines you pointed at with no manual copy/paste.

### Also included

- One-click launcher: click the Codex sidebar icon or press `Ctrl+Alt+Shift+C` / `Cmd+Alt+Shift+C` to open `codex` in a dedicated terminal tab.
- Paths are resolved relative to the project root, so references match Codex's own working directory.
- `Settings → Tools → Codex [Beta]` page with configurable command and working directory.
- Works on IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, Android Studio, and other JetBrains IDEs on platform build 253+.
