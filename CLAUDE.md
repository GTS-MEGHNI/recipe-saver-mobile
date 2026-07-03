# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is the **Android app** (package `com.recipesaver.app`) for "Recipe Saver". **`architecture.md` in the repo root is the design doc for the intended app** — read it before implementing features.

### Sibling `api/` project

This repo (`recipes/mobile/`) has a sibling folder `recipes/api/` — the **owner-run HTTP backend** for the planned v2 API-backed model (see `architecture.md` §12). It is a **Laravel 12 / PHP 8.3** app (`laravel/recipes-api`) with its own `CLAUDE.md`; work on it from that directory, not this one. It already implements the §12.4 endpoint contract — `apiResource('recipes')` plus `recipes/{recipe}/cover` and nested `recipes.images` — authenticated by a single static key via an `api.key` middleware reading the `X-API-Key` header (env `RECIPE_API_KEY`, no accounts). The recipe shape mirrors this app's `Recipe` entity. The mobile app's `data/remote/` layer that consumes it is **not yet implemented**. It specifies the target architecture, package layout, data model, and a long list of concrete rules (below). Treat `architecture.md` as authoritative intent; treat the current source tree as the empty starting point.

**UI language: French only.** Every user-facing string is written in French — no English fallback, no `values-fr/` locale variants needed since French is the only supported locale for v1. Kotlin identifiers, composable names, and resource keys stay in English as normal; only the text users see is French.

## Commands

```bash
./gradlew ktlintCheck        # Kotlin style/lint (ktlint) — run before building
./gradlew compileDebugKotlin # type check / compile only
./gradlew ktlintFormat       # auto-fix ktlint violations
./gradlew build              # full build
./gradlew assembleDebug      # build debug APK
./gradlew assembleRelease    # build signed release APK (needs keystore/keystore.properties)
./gradlew installDebug       # install debug APK onto an already-connected device
./gradlew test               # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest # instrumented tests (app/src/androidTest), needs device/emulator
./gradlew testDebugUnitTest --tests "com.recipesaver.app.SomeTest"  # single unit test
./gradlew lint               # Android Lint
```

ktlint **is** configured (`org.jlleitschuh.gradle.ktlint`); detekt is not.

### Gradle configuration cache is disabled — keep it that way

`gradle.properties` sets `org.gradle.configuration-cache=false`. It was previously `true` and was
caught **serving stale outputs**: `assembleDebug`/`installDebug` reported `BUILD SUCCESSFUL` while
packaging/installing an hour-old APK that did not contain freshly added sources (a new screen was
missing from the dex, so a wired-up button silently did nothing). Builds must faithfully reflect the
current source, so the cache stays **off**. Do not re-enable it. If you ever suspect a stale build,
confirm the change is really in the APK — e.g.
`unzip -p app/build/outputs/apk/debug/app-debug.apk 'classes*.dex' | strings -a | grep -o 'ui/screens/[A-Za-z]*' | sort -u`
should list every screen you expect.

### Building an APK

All Gradle commands need `JAVA_HOME` pointed at Android Studio's bundled JDK first:

```bash
export JAVA_HOME=/snap/android-studio/current/jbr
```

- **Debug APK** (for testing; signed with the auto-generated debug key):
  ```bash
  ./gradlew assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`

- **Release APK** (for real installs that must persist data across updates; signed with the
  reused release keystore):
  ```bash
  ./gradlew assembleRelease
  ```
  Output: `app/build/outputs/apk/release/app-release.apk`

Release signing is applied only when `keystore/keystore.properties` exists (it points at
`keystore/recipesaver-release.jks`). Without it, `assembleRelease` produces an **unsigned** APK
that won't install. The keystore + passwords must never change — see the hard rules below.

### Always run quality + type checks before building

Before any `build` / `assembleDebug` / `installDebug`, run **both** and fix what they report:

1. `./gradlew ktlintCheck` — code style (use `ktlintFormat` to auto-fix, then re-check).
2. `./gradlew compileDebugKotlin` — type check / compilation.

Only build or install once both pass. This catches style and type errors cheaply, before the slower packaging steps.

### Do not run the emulator

**Never launch, boot, or manage the Android emulator** (`emulator -avd …`, starting an AVD, etc.) — the developer runs the emulator themselves. Claude's job ends at building and, when a device is **already connected** (`adb devices` shows one), `installDebug`. If no device is connected, stop and ask the developer to start their emulator rather than starting one.

**After building, always check `adb devices` and install directly if one is connected.** When a build/change is done and a device shows up in `~/Android/Sdk/platform-tools/adb devices`, go straight to `installDebug` (which builds + installs) without waiting to be asked — but **never run auto tests to verify** (see the auto-test rule below). If no device is connected, report the build result and ask the developer to connect/start their emulator.

**Device:** the developer's emulator is the AVD **`Pixel_6`** (SDK at `~/Android/Sdk`; `adb` lives in `~/Android/Sdk/platform-tools/`, `emulator` in `~/Android/Sdk/emulator/`). Launch with `~/Android/Sdk/emulator/emulator -avd Pixel_6` only when the developer explicitly asks; otherwise assume they start it themselves.

**Do not auto-test on the emulator.** After `installDebug`, do **not** drive or verify the app on the device yourself — no `adb shell am start`, `adb shell input tap/text/swipe`, `adb exec-out screencap`, or logcat-based UI checking to confirm a change works. The developer does all interactive testing and screenshotting. Report what you built and let them run it. Only interact with the running app this way if the developer **explicitly** asks you to (e.g. "take a screenshot", "tap through it").

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

**Planned v2 pivot (see `architecture.md` §12):** the app is moving to an **API-backed** model — a
single owner-run HTTP API becomes the source of truth, authenticated by a static `API_KEY`
(`X-API-Key` header, no accounts), so data survives phone loss/replacement. Only the data layer
(`data/remote/` + `RecipeRepository`) changes; UI/ViewModels stay untouched. Some v1 rules above are
relaxed or reversed once this lands — see §12.6. Not yet implemented; a few decisions still open.
