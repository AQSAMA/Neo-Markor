# Current Status

- Date: 2026-02-22
- Repository: `/home/runner/work/Neo-Markor/Neo-Markor`
- State: Neo-Markor app is running on the current architecture with SAF + Okio integration for workspace file operations.

## Current working architecture snapshot
- Storage root selection is SAF-backed (`OpenDocumentTree`) and persisted through DataStore preferences.
- File operations are repository-driven:
  - Recursive tree scan from the persisted SAF root.
  - Okio read/write for note content.
  - New note creation inside the selected workspace.
- Dashboard currently hosts the navigation drawer and recent notes list.
- Standalone `FileBrowserScreen` still exists in navigation and is the last major UI path to be merged into the drawer.

## Immediate next step
- Implement the recursive file/folder tree directly inside the drawer side panel (global access), then add drawer drag-and-drop move support backed by SAF updates and remove standalone file-browser navigation.
