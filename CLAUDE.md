# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is a fresh Android Studio "Empty Activity" template (package `com.recipesaver.app`) that has not yet been built out. **`architecture.md` in the repo root is the design doc for the intended app ("Recipe Saver")** — read it before implementing features. It specifies the target architecture, package layout, data model, and a long list of concrete rules (below). Treat `architecture.md` as authoritative intent; treat the current source tree as the empty starting point.

**UI language: French only.** Every user-facing string is written in French — no English fallback, no `values-fr/` locale variants needed since French is the only supported locale for v1. Kotlin identifiers, composable names, and resource keys stay in English as normal; only the text users see is French.

## Commands

```bash
./gradlew build              # full build
./gradlew assembleDebug      # build debug APK
./gradlew test               # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest # instrumented tests (app/src/androidTest), needs device/emulator
./gradlew testDebugUnitTest --tests "com.recipesaver.app.SomeTest"  # single unit test
./gradlew lint               # Android Lint
```

No ktlint/detekt is configured yet despite being recommended in `architecture.md` — check before assuming either is set up.

## Architecture (per `architecture.md`)

Layered: **UI (Compose) → ViewModel → Repository → Data Sources**. The Repository is the single source of truth and hides storage details from the ViewModel/UI, so only it should change if a backend/sync is ever added.

Data is split by kind:
- **Room (SQLite)** — structured data only (recipe fields, image *file paths*). Never store image blobs in SQLite.
- **Internal file storage** (`context.filesDir`) — actual image binaries. Never use `cacheDir` for anything that must persist (OS can clear it).
- **DataStore (Preferences)** — small UI-only settings like the selected theme; does not belong in Room.

Intended package structure once code exists:
```
data/local/            Room DB, DAO, entities (Recipe, RecipeImage)
data/repository/       RecipeRepository
data/preferences/      ThemePreferences (DataStore)
data/files/            ImageStorageManager (save/load/delete image files)
ui/screens/            RecipeList, RecipeDetail, AddEditRecipe, Gallery, Settings
ui/viewmodel/          RecipeViewModel, ThemeViewModel
ui/theme/              Color.kt, Theme.kt, Type.kt (Material 3, RecipeSaverTheme)
```

Schema: `Recipe` (id, title, ingredients, steps, cookTimeMinutes?, servings?, tags?, createdAt) and `RecipeImage` (id, recipeId FK, filePath, position) — one-to-many for a multi-photo gallery per recipe.

Theming: multiple swappable Material 3 themes (`enum class AppTheme { CORAL, SAGE, NEUTRAL }`), selection persisted in DataStore, exposed as `StateFlow` from a `ThemeViewModel`, applied via a single top-level `RecipeSaverTheme(selectedTheme) { ... }` wrapping the app. Always use `MaterialTheme.colorScheme.*` — never hardcode colors/fonts in composables.

## Hard rules from architecture.md

- **Never use `fallbackToDestructiveMigration()`** on the Room database — always write explicit `Migration`s, since this app has no backend and losing local data is unrecoverable.
- **Package name and signing key must never change** across builds — Android must always treat a new APK as an update to the same app, or on-device data is lost. A release keystore should be generated once and reused for every build (not the auto-generated debug key).
- **Downsample/compress images before saving** to internal storage (e.g. cap ~1080px longest edge) — do not persist full-resolution camera photos.
- **All Room access must be off the main thread** — DAOs as `suspend fun` or returning `Flow<T>`, never synchronous queries.
- Use `LazyColumn`/`LazyRow` for the recipe list and gallery, never a plain `Column` with `.forEach`.
- Add LeakCanary as a **debug-only** dependency (image/gallery code is a common leak source).
- Explicitly out of scope for v1: backend/API, accounts/auth, multi-device sync, sharing between users.
