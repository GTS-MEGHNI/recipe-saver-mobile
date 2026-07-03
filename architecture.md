# Recipe Saver — Architecture Design

## 1. Overview

A local-only Android app for saving personal recipes, including text details and a photo gallery per recipe. No backend, no network dependency, no user accounts. Distributed as a sideloaded APK (not published to Play Store).

**Core principle:** Offline-first, single-device, single-user. All data lives in the app's private storage on the phone.

---

## 2. Goals & Constraints

- Kotlin + Jetpack Compose for UI.
- All data persists locally on-device — no server, no API.
- Data must survive app **updates** (new APK installs) without loss.
- Data does *not* need to survive app **uninstall** or be synced across devices (out of scope for v1).
- Package name and signing key must stay constant across builds so Android treats new APKs as updates, not fresh installs.

---

## 3. High-Level Architecture

Layered architecture (UI → ViewModel → Repository → Data Sources). This keeps UI code decoupled from *how* data is stored, so storage details can change later without rewriting screens.

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)            │
│  Screens: RecipeList, RecipeDetail,        │
│  AddEditRecipe, Gallery, Settings           │
└─────────────────┬───────────────────────────┘
                  │ observes state from
┌─────────────────▼───────────────────────────┐
│            ViewModel Layer                  │
│  RecipeViewModel — holds UI state,          │
│  exposes actions (add/edit/delete),          │
│  talks only to the Repository                │
└─────────────────┬───────────────────────────┘
                  │ calls
┌─────────────────▼───────────────────────────┐
│            Repository Layer                 │
│  RecipeRepository — single source of truth,  │
│  hides DB + file storage details from        │
│  ViewModel/UI                                │
└──────┬─────────────────────┬──────────────────┘
       │                     │
┌──────▼──────────┐   ┌──────▼──────────────────┐
│  Room Database    │   │   File Storage             │
│  (structured data: │   │  (actual image files,      │
│  title, ingredients,│  │  stored in app's internal   │
│  steps, tags, image │  │  files dir — context.filesDir)│
│  file paths)        │  │                             │
└─────────────────────┘   └─────────────────────────────┘
```

Why this matters: if a backend or sync feature is ever wanted later, only the Repository layer changes — UI and ViewModel code stays untouched.

---

## 4. Data Storage Strategy

### 4.1 Split: metadata vs. files

- **Room (SQLite)** stores structured/text data only: recipe fields and *file path references* to images.
- **Internal file storage** (`context.filesDir`) stores the actual image binaries.

**Never store image blobs inside SQLite** — it bloats the database, slows queries, and complicates migrations. Store a path/URI string instead.

**Never use `cacheDir` or "cache" flavored external storage** for images — the OS can clear these under storage pressure. Use `context.filesDir` (private, persistent, internal) for anything that must not be lost.

### 4.2 Suggested schema

**Recipe** (one row per recipe)
| Column | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| title | String | |
| ingredients | String or separate table | can start as newline-delimited text, normalize later if needed |
| steps | String | |
| cookTimeMinutes | Int? | optional |
| servings | Int? | optional |
| category/tags | String? | optional, simple text or comma-separated for v1 |
| createdAt | Long (timestamp) | |

**RecipeImage** (one-to-many: many images per recipe)
| Column | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| recipeId | Long (FK → Recipe.id) | |
| filePath | String | path within internal storage |
| position | Int | ordering within the gallery |

This one-to-many relationship is what enables a true "gallery" (multiple photos per recipe) rather than a single image column.

---

## 5. Package Structure

```
com.yourname.recipesaver/
├── data/
│   ├── local/
│   │   ├── RecipeDatabase.kt       (Room DB setup + migrations)
│   │   ├── RecipeDao.kt            (queries)
│   │   └── entities/
│   │       ├── Recipe.kt
│   │       └── RecipeImage.kt
│   ├── repository/
│   │   └── RecipeRepository.kt
│   ├── preferences/
│   │   └── ThemePreferences.kt     (DataStore — selected theme)
│   └── files/
│       └── ImageStorageManager.kt  (save/load/delete image files)
├── ui/
│   ├── screens/
│   │   └── SettingsScreen.kt        (theme changer UI, among other settings)
│   ├── viewmodel/
│   │   └── ThemeViewModel.kt        (exposes selected theme as StateFlow)
│   └── components/
└── MainActivity.kt
```

---

## 6. Data Persistence Across Updates

Android preserves an app's private storage across installs **automatically**, as long as:
1. **Package name** stays identical across every build.
2. **Signing key** stays identical across every build (see Section 7).

If either changes, Android treats the new APK as a different app — installing it either fails (if the old one is still present) or requires uninstalling first, which wipes all data.

### Room schema migrations

Any time the table structure changes between versions (new column, new table, renamed field), a proper Room `Migration` must be written — otherwise Room will crash on the version bump, or (if `fallbackToDestructiveMigration()` is used) silently **wipe the database**.

**Rule for this project: never use `fallbackToDestructiveMigration()`. Always write explicit migrations for schema changes.**

---

## 7. Signing Strategy

Every APK must be signed to install, store-published or not. Two options:

- **Debug key** — auto-generated by the build tooling, fine only if always building from the exact same, never-reset dev environment.
- **Release key (recommended)** — a keystore file generated once, reused for every build going forward. Provides a stable app identity independent of the dev machine's state.

**Decision for this project:** generate a personal release keystore once, sign all builds with it, and back up the `.jks` file + password somewhere durable (password manager / encrypted storage). Losing this file means future "updates" can't overwrite the old install — only fresh installs, with data loss.

---

## 8. Explicitly Out of Scope (v1)

- Backend / API server.
- User accounts or authentication.
- Multi-device sync.
- Sharing recipes between users.
- Cross-device backup beyond what Android's built-in Auto Backup provides (optional future addition, no server required).

These aren't excluded because they're bad ideas — they're excluded because nothing in the current requirements needs them, and the Repository layer leaves room to add them later without a rewrite.

---

## 9. Theming

UI-layer concern only — independent of the data architecture above, safe to build in parallel with the Room/repository work.

Jetpack Compose uses **Material 3** theming, scaffolded as a small set of files rather than a custom-built component:

```
ui/theme/
├── Color.kt       (color palette — primary, secondary, background, surface, error, etc.)
├── Theme.kt        (wraps the app, applies color scheme + typography)
└── Type.kt         (font families and text styles — headline, body, label)
```

The whole app is wrapped in a single `RecipeSaverTheme { ... }` composable (defined in `Theme.kt`). Every screen inherits colors and fonts from there — changing a value in `Color.kt` or `Type.kt` updates the whole app, with no hardcoded colors/fonts scattered across individual screens.

**What it covers:**
- **Colors** — `primary`, `onPrimary`, `secondary`, `onSecondary`, `background`, `surface`, `error`, etc., following Material 3's naming scheme so built-in components (buttons, text fields, cards) pick the right color automatically.
- **Typography** — a `Typography` object with named styles (`headlineLarge`, `bodyMedium`, `labelSmall`, etc.), each with font family, size, and weight defined once.
- **Dark mode** — define a light color scheme and a dark color scheme; the app switches automatically based on device settings, no per-screen logic needed.
- **Dynamic color** (Android 12+, optional) — Material 3 can derive the palette from the user's wallpaper. Nice-to-have, not required.

**Rule for this project:** define the palette and typography in `ui/theme/` before building out screens, and always reference theme values (`MaterialTheme.colorScheme.primary`, etc.) rather than hardcoded colors/fonts in composables.

### 9.1 Suggested palettes (concrete values)

Three theme options to implement, each following the primary / secondary / background / text structure used by Material 3's `ColorScheme`.

**Coral & amber** — warm, appetite-triggering, classic food-app feel
| Role | Hex |
|---|---|
| Primary | `#D85A30` |
| Secondary | `#EF9F27` |
| Background | `#FAECE7` |
| Text | `#2C2C2A` |

**Sage & cream** — calm, natural, wellness-leaning
| Role | Hex |
|---|---|
| Primary | `#3B6D11` |
| Secondary | `#97C459` |
| Background | `#EAF3DE` |
| Text | `#2C2C2A` |

**Warm neutral** — minimal, lets food photos stand out (recommended default)
| Role | Hex |
|---|---|
| Primary | `#993C1D` |
| Secondary | `#5F5E5A` |
| Background | `#F1EFE8` |
| Text | `#2C2C2A` |

Each of these should be defined as a full Material 3 `ColorScheme` in `Color.kt` (deriving `onPrimary`, `onSecondary`, `surface`, `error`, etc. from these anchor values — Material Theme Builder or Android Studio's theme tooling can generate the full scheme from a single seed color per palette). Dark-mode variants can be generated the same way if dark mode support is desired.

### 9.2 Multi-theme support (swappable themes)

Rather than a single hardcoded color scheme, the app supports multiple named themes the user can switch between at runtime (e.g. Coral, Sage, Neutral).

**Structure:**
- Define `enum class AppTheme { CORAL, SAGE, NEUTRAL }` mapping to the three palettes in Section 9.1.
- Each enum value maps to its own `ColorScheme` (optionally with light + dark variants).
- `Typography` (`Type.kt`) stays shared across themes — no need to vary fonts per theme.
- `RecipeSaverTheme(selectedTheme: AppTheme) { ... }` selects the right `ColorScheme` and applies it via `MaterialTheme`, wrapping the whole app.

**Persisting the selection:**
The selected theme is a user preference, not app data — it does not belong in Room alongside recipes. Use **Jetpack DataStore (Preferences)**, a lightweight key-value store suited for simple settings (no tables, no migrations required).

```
data/
├── local/                        (Room — recipes, images)
├── preferences/
│   └── ThemePreferences.kt       (DataStore — selected theme)
└── repository/
```

**Runtime switching:**
The selected theme is exposed as a `StateFlow` (e.g. via a small `ThemeViewModel`), observed by the top-level `RecipeSaverTheme` composable. Because Compose is reactive, updating the stored preference triggers an automatic recomposition — the whole UI re-themes immediately, no restart required.

**UI entry point — Settings screen with theme changer:**

A dedicated `SettingsScreen`, reachable from the recipe list (e.g. a gear icon in the top app bar). Contents:
- A "Theme" section listing each `AppTheme` option as a tappable card/swatch showing that theme's primary/secondary/background colors (same visual format as the palette previews already agreed on).
- The currently active theme is visually marked (e.g. a checkmark or highlighted border) so the user always knows what's selected.
- Tapping an option:
  1. Immediately updates the in-memory `StateFlow`, so the whole app (including the Settings screen itself) re-themes live — the user sees the result before committing to anything.
  2. Persists the choice to DataStore in the background, so it survives app restarts.
- No separate "Save" or "Apply" button needed — selection *is* the action, consistent with how most Android settings screens behave (instant apply, no confirm step).

This keeps the feature small in scope: one new screen, one new ViewModel (or an addition to an existing app-level ViewModel), and the DataStore/enum plumbing already described above.

**Scope note:** this is a small, self-contained addition (a few files, one new dependency) that can be built alongside the core theming setup, or deferred to a post-v1 polish pass without affecting the rest of the architecture.

---

## 10. Performance & Code Quality

Practices worth setting up early — cheap now, expensive to retrofit once the codebase grows.

### 10.1 Static analysis & style

- **Android Lint** (built into Gradle, `./gradlew lint`) — catches Android-specific issues (missing permissions, deprecated APIs, resource problems) for free, no setup needed.
- **ktlint** or **detekt** — Kotlin style/quality linters. Pick one; ktlint is simpler (formatting-focused), detekt is more thorough (complexity, code smells). Either catches issues like unused imports, overly long functions, and inconsistent formatting before they pile up.
- **Rule for this project:** ask Claude Code to run lint/static analysis after generating each major feature, not just at the end — catching issues in small batches is far easier than a giant cleanup pass later.

### 10.2 Testing

- **Unit tests** (JUnit) for anything with logic worth verifying — Repository methods, DAOs, ViewModel state transitions. Doesn't need to be exhaustive for a personal app, but core data operations (add/edit/delete recipe) are worth covering so a refactor doesn't silently break saving your data.
- **Room** supports an in-memory test database, so DAO tests don't touch real files.
- Compose UI tests are optional for v1 — nice to have, not essential for a single-developer personal app.

### 10.3 Performance practices specific to this app

- **Lists**: always use `LazyColumn`/`LazyRow` (never a plain `Column` with `.forEach`) for the recipe list and image gallery — non-lazy lists render every item upfront regardless of what's visible, which gets slow fast once there are many recipes/photos.
- **Image loading**: Coil (already planned) handles memory/disk caching automatically — don't hand-roll bitmap loading or caching logic.
- **Downsample images before storage**: photos from a modern phone camera can be several MB each; resize/compress on save (e.g. cap at ~1080px longest edge) rather than storing full-resolution originals. This keeps the app's storage footprint and gallery scroll performance reasonable. Worth flagging explicitly to Claude Code — it's an easy step to skip.
- **Database access off the main thread**: Room + Kotlin Coroutines/Flow handles this by default when DAOs are written as `suspend fun` or return `Flow<T>` — never call Room queries synchronously on the main thread (Room will actually throw a runtime error if you try, which is a useful guardrail).
- **Avoid unnecessary recomposition**: keep state as narrowly scoped as possible (e.g. don't hoist state higher than it needs to be), and use `remember`/`derivedStateOf` appropriately. Not critical to obsess over in v1, but worth a pass if any screen feels janky.

### 10.4 Memory & leak checks

- **LeakCanary** — a drop-in debug-only library that automatically detects memory leaks (e.g. an Activity or Bitmap that isn't garbage collected) and shows a notification with details. Very low effort to add, high value for catching image-handling leaks specifically, which are one of the most common leak sources in gallery-style apps.

### 10.5 Build variants

- Keep **debug** and **release** build types distinct in Gradle (already implied by the signing setup in Section 7). Debug builds can enable extra logging/LeakCanary; release builds should have these stripped out and enable code shrinking (R8/ProGuard) to reduce APK size — safe to enable by default for a single-module app like this, but worth testing the release build still works after enabling it (shrinking occasionally strips something reflectively-referenced).

### 10.6 Suggested guardrails to give Claude Code explicitly

- Never use `fallbackToDestructiveMigration()` (already noted in Section 6).
- Always downsample/compress images before saving to internal storage.
- Always perform Room operations via `suspend fun` / `Flow`, never on the main thread.
- Run lint after each major feature addition, not just at project end.
- Add LeakCanary as a debug-only dependency from the start.

---

## 11. Build Order (Recommended)

1. Define theming (`ui/theme/` — colors, typography) alongside initial project scaffolding.
2. Room database + entities (Recipe only, no images) — get add/list/view/delete working end-to-end.
3. Add single-image picking + storage per recipe (Android Photo Picker API, no runtime permission needed).
4. Expand to multi-image gallery (RecipeImage table, list UI).
5. Polish: search/filter, tags/categories, edit flow.
6. Add multi-theme support (DataStore preference + Settings screen) — optional, can be done anytime after step 1.
7. Set up release keystore + signing config before doing any "real" builds meant to persist across updates.
8. Add lint/static analysis, basic unit tests, and LeakCanary — ideally woven in throughout rather than bolted on at the end.
