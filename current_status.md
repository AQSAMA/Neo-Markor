# Current Status

- Date: 2026-02-22
- Repository: `/home/runner/work/Neo-Markor/Neo-Markor`

## Working Architecture

**Stack:** MVVM + Clean Architecture + Repository pattern.  
**UI:** Jetpack Compose + Material 3.  
**Storage:** Okio for I/O + SAF/`ContentResolver` (scoped storage, Android 11+).  
**DI:** Koin. **Navigation:** Navigation Compose (typed routes via Base64-encoded SAF URIs).  
**Preferences:** DataStore. **Async:** Coroutines + StateFlow.

## Current Feature State

| Feature | Status |
|---|---|
| Build & AGP alignment (8.7.0) | ✅ Done |
| Package/namespace `com.aqsama.neomarkor` | ✅ Done |
| Main shell (M3 drawer + top bar + FAB) | ✅ Done |
| Triple-mode editor (Source/Preview/Reading) | ✅ Done |
| SAF workspace picker + DataStore persistence | ✅ Done |
| Okio file read/write/create | ✅ Done |
| Recursive expandable/collapsible file tree | ✅ Done |
| Drag-and-drop file/folder move (SAF `moveDocument`) | ✅ Done |
| Wiki-links, YAML frontmatter | 🔲 Next |
| Autosave / undo-redo | 🔲 Pending |
| Export (HTML/PDF) | 🔲 Pending |
| Theme customization | 🔲 Pending |

## Immediate Next Steps
1. Wiki-links `[[...]]` auto-complete and navigation in the editor.
2. YAML frontmatter parsing (title, tags, date) shown in dashboard.
3. Autosave with debounce in `EditorViewModel`.

## Key File Locations
- `ui/screen/FileBrowserScreen.kt` — tree + drag-and-drop
- `ui/screen/EditorScreen.kt` — triple-mode editor
- `data/repository/FileRepositoryImpl.kt` — Okio + SAF I/O
- `navigation/NavGraph.kt` — Base64-encoded URI routing
- `di/AppModule.kt` — Koin bindings
