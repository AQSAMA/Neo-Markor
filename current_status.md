# Current Status

- Date: 2026-02-22
- Repository: `/home/runner/work/Neo-Markor/Neo-Markor`
- Focus Area: File browser hierarchy + drag-and-drop milestone completed

## Working Architecture Snapshot

- **Storage pipeline (active):**
  - `FileRepositoryImpl` uses **SAF** (`DocumentFile`) to traverse the user-selected root directory.
  - File reads/writes are handled through **Okio** streams on `ContentResolver` URIs.
  - SAF move operations are available via `DocumentsContract.moveDocument(...)` for drag-and-drop relocation.
  - Root folder URI persistence is handled by `StoragePreferences` (DataStore-backed).
- **UI pipeline (active):**
  - `FileBrowserScreen` renders repository-driven `FileNode` trees.
  - Recursive tree rows keep URI-keyed expand/collapse state and support compose drag-and-drop source/target behavior.
  - Guardrail: drag-and-drop types come from `androidx.compose.ui.draganddrop` and require `ExperimentalFoundationApi` opt-in.
  - `EditorScreen` opens file URIs and uses repository read/write methods for note editing.
- **State pipeline (active):**
  - `FileBrowserViewModel` exposes `fileTree` and `directoryUri` as `StateFlow`.
  - Repository refresh events keep SAF tree state in sync after file operations.

## Immediate Next Step

- Move to the next backlog milestone:
  1. Note intelligence features (`[[wiki-links]]`, YAML frontmatter parsing, relative media embedding).
