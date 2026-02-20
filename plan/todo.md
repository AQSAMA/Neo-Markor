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
- [ ] Align app identity and foundation (application id/name, package naming cleanup, stable Gradle plugin/dependency versions).
- [ ] Implement local-first filesystem workspace core (custom root directory picker, recursive tree model, file CRUD for plain-text formats).
- [ ] Build main shell UI (M3 expressive top app bar + navigation drawer + dashboard surface) with fast note capture action.
- [ ] Implement triple-mode editor foundation:
  - [ ] Source mode (raw markdown editing with lightweight syntax highlighting).
  - [ ] Live preview mode (progressive markdown rendering while editing).
  - [ ] Reading mode (read-only rendered markdown with tables/task lists/images).
- [ ] Add note intelligence features (wiki-links `[[...]]`, YAML frontmatter parsing, relative media embedding).
- [ ] Add organization features (recent/pinned/daily notes, infinite folder depth tree navigation).
- [ ] Add background reliability features (autosave, undo/redo, format-aware editor handling for md/txt/json/yaml/todo.txt).
- [ ] Add export/share options (HTML + PDF).
- [ ] Add customization surface (theme mode, accent color, corner radius controls).
- [ ] Add drag-and-drop file/folder move interactions.
- [ ] Add tests incrementally for parser/repository/viewmodel/editor mode transitions.
- [ ] Add repeatable test APK workflow for cloud-first iteration:
  - [ ] Local command path (`./gradlew assembleDebug`) and output path documentation.
  - [ ] PR/CI artifact build workflow to download installable APK each iteration.
- [ ] Final validation pass (targeted tests + assembleDebug + manual UI verification screenshots).

## Review
- Added an explicit technology stack section so implementation phases reference concrete architectural/library choices.
- Documented one key recommendation: prefer stable Navigation Compose typed-route setup now, then upgrade to Navigation 3 when stable for production.
