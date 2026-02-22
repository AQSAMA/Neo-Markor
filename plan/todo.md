# Neo-Markor Implementation Plan

## Technology Stack Decisions (explicit)
- **Architecture:** MVVM + Clean Architecture + Repository pattern.
- **UI:** Jetpack Compose + Material 3 (M3 expressive styling tokens).
- **Navigation:** Type-safe Navigation Compose.  
  - **Recommendation:** use current stable `androidx.navigation:navigation-compose` with typed routes via Kotlin serialization while Navigation 3 matures.
- **DI:** Koin (lightweight, Kotlin-first, no annotation processor).
- **Editor core:** `BasicTextField` + `TextFieldState` + custom `VisualTransformation` for Source/Live Preview behavior.
- **Reading mode renderer:** Markwon via `AndroidView` bridge for robust Markdown (tables/task lists/images).
- **Filesystem:** Okio for fast I/O + SAF/`ContentResolver` integration for Android 11+ scoped storage custom roots.
- **Preferences:** Jetpack DataStore (Preferences).
- **Frontmatter parsing:** Kaml (YAML).
- **Image loading:** Coil (configured for relative local file paths).
- **Optional local index/cache:** Room (files remain source of truth; Room accelerates search/wiki-link/tag queries).
- **Async/state:** Kotlin Coroutines + `StateFlow`.

## Checklist
- [x] Audit current Kotlin/Compose starter app and reference repositories (Markor + Open Note) to map required features.
- [x] Confirm current build/test baseline and record blockers.
- [x] Explicitly define Neo-Markor target technology stack and library decisions.
- [x] Align app identity and foundation (application id/name, package naming cleanup, stable Gradle plugin/dependency versions).
- [x] Implement local-first filesystem workspace core (custom root directory picker, recursive tree model, file CRUD for plain-text formats).
- [x] Build main shell UI (M3 expressive top app bar + navigation drawer + dashboard surface) with fast note capture action.
- [x] Implement triple-mode editor foundation:
  - [x] Source mode (raw markdown editing with lightweight syntax highlighting).
  - [x] Live preview mode (progressive markdown rendering while editing).
  - [x] Reading mode (read-only rendered markdown with tables/task lists/images).
- [ ] Add note intelligence features (wiki-links `[[...]]`, YAML frontmatter parsing, relative media embedding).
- [ ] Add organization features (recent/pinned/daily notes).
- [x] Implement infinite folder depth tree navigation milestone (recursive tree UI with persistent expand/collapse state and visual indentation).
- [ ] Add background reliability features (autosave, undo/redo, format-aware editor handling for md/txt/json/yaml/todo.txt).
- [ ] Add export/share options (HTML + PDF).
- [ ] Add customization surface (theme mode, accent color, corner radius controls).
- [x] Add drag-and-drop file/folder move interactions with SAF-backed move operations.
- [x] Add tests incrementally for parser/repository/viewmodel/editor mode transitions.
- [x] Add repeatable test APK workflow for cloud-first iteration:
  - [x] Local command path (`./gradlew assembleDebug`) and output path documentation.
  - [x] PR/CI artifact build workflow to download installable APK each iteration.
- [ ] Final validation pass (targeted tests + assembleDebug + manual UI verification screenshots).

## Review
- Updated checklist state to keep file I/O/editor items explicitly marked complete.
- Added explicit near-term milestones for recursive tree UX and SAF-backed drag-and-drop moves.
- Implemented persistent recursive folder expansion state in `FileBrowserScreen` using URI-keyed state map.
- Added Compose drag-and-drop interactions for files/folders and SAF-backed physical move operation wiring (`FileBrowserViewModel` + `FileRepository.moveNode` + `FileRepositoryImpl`).
- Added focused unit tests for drag payload encoding/decoding and drop-target validation rules.
