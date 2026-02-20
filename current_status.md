# Current Status

- Date: 2026-02-20
- Repository: `/home/runner/work/Neo-Markor/Neo-Markor`
- State: Fresh Compose starter template with default `MainActivity` greeting screen.

## What was reviewed
- Root/build files:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `app/build.gradle.kts`
- App code:
  - `app/src/main/java/com/example/kotlin_jp_compose_project/MainActivity.kt`
- Reference repositories under:
  - `reference_repositories/markor`
  - `reference_repositories/OpenNote-Compose`

## Baseline validation result
- Attempted baseline command:
  - `./gradlew --no-daemon lint testDebugUnitTest assembleDebug`
- Result:
  - Build failed early due unresolved Android Gradle Plugin version `9.0.0` from `gradle/libs.versions.toml`.
  - This must be corrected before any test APK can be generated.

## Immediate next step pending approval
- Proceed with Phase 1 minimal implementation:
  1. Fix build foundation (AGP/dependency alignment).
  2. Replace template UI with a thin Neo-Markor shell (dashboard + drawer scaffold).
  3. Add initial implementation docs for repeatable test APK generation in cloud workflow.

## Stack decisions captured in plan (v1)
- Architecture: MVVM + Clean Architecture + Repository.
- UI: Compose + Material 3 expressive.
- Navigation: type-safe Navigation Compose (stable track recommended first).
- DI: Koin.
- Editor: BasicTextField/TextFieldState + custom visual transformation.
- Reading mode: Markwon via AndroidView.
- Storage: Okio + SAF/ContentResolver for scoped storage.
- Settings: DataStore Preferences.
- Frontmatter: Kaml.
- Local media: Coil.
- Optional indexing cache: Room.
- Concurrency/state: Coroutines + StateFlow.

## Recommendation note
- Navigation Compose 3 is promising, but to minimize risk for first production iterations, use the stable Navigation Compose line with typed routes now and plan a controlled upgrade when Nav 3 is fully stable.
