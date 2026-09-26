# Tacca — native Android app — design

Date: 2026-09-26
Status: approved in conversation, pending written-spec review
Source mockup: `docs/mockup/*.dc.html` (copied from the "Tacca — app calibrazione macina"
design canvas). The mockup files are the visual source of truth for layout, sizes, colors and
icon geometry; this spec says what to build and where it deviates from or extends them.

## 1. Purpose and success criteria

Tacca remembers the grinder setting ("calibration") that works for a given
**coffee × brew method × grinder** combination, so the user can dial in instantly instead of
re-discovering the setting.

- Target: public release on Google Play in the future; first user is the author.
- Data lives only on the device, with manual JSON export/import to move it between phones.
- Multilingual from day one: English (default/fallback), Italian, Spanish, French, German.

The first version is done when:

1. Every screen in the mockup exists as a native Compose screen and matches the mockup's
   look (palette, type, radii, spacing, icons) in light theme.
2. The user can create, edit and delete coffees, methods, grinders and calibrations; all
   data survives app restarts.
3. Export produces a JSON file; importing it on a clean install restores identical data;
   a malformed file is rejected without touching existing data.
4. Accent color (4 options), theme (system/light/dark) and app language can be changed in
   Settings.
5. All UI strings exist in the 5 languages (lint `MissingTranslation` is an error).
6. `./gradlew test lint assembleRelease bundleRelease` passes; a signed release APK and AAB
   are produced.

## 2. Tech stack and architecture

- Kotlin, Jetpack Compose, Material 3 (custom color scheme; dynamic color **off**).
- minSdk 26, target/compile SDK = latest stable available when scaffolding.
- Single Gradle module `:app`, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`).
- Persistence: Room (with KSP), schema JSON exported to `app/schemas/` and committed.
- Preferences: Jetpack DataStore (Preferences).
- Serialization: kotlinx.serialization (export/import).
- Navigation: Navigation Compose (type-safe routes).
- Localization: Android resources + `AppCompatDelegate.setApplicationLocales`
  (AppCompat 1.6+, `autoStoreLocales` service for API < 33) + `localeConfig`
  (`res/xml/locales_config.xml`) so Android 13+ shows the per-app language setting.
- DI: manual. `TaccaApplication` owns an `AppContainer` (database, repositories,
  settings store, export/import service). ViewModels get dependencies through a
  `viewModelFactory { initializer { ... } }`.

Package layout (`it.matteocolombo.tacca`):

```
data/db/          Room entities, DAOs, TaccaDatabase, migrations
data/repo/        Repositories (CoffeeRepository, MethodRepository, GrinderRepository,
                  CalibrationRepository) – the only thing ViewModels talk to
data/settings/    SettingsStore (DataStore): accent, theme mode
data/backup/      Backup DTOs, BackupSerializer, BackupService (export/import)
domain/           Pure Kotlin: GrindSetting, SettingRules, DialGeometry, formatting
ui/theme/         Colors (light/dark), accents, typography, shapes, TaccaTheme
ui/icons/         Icon catalog (method icons, app icons) – vector drawables + enum
ui/components/    Shared composables: TaccaTopBar, BottomNav, Stepper, Chip, Card,
                  Dial, ConfirmDialog, EmptyState, SheetScaffold
ui/<screen>/      One package per screen: Screen composable + ViewModel + UiState
MainActivity      Single activity, edge-to-edge, hosts NavHost
```

Rule: `domain/` has no Android dependencies and holds all non-trivial logic (limits,
formatting inputs, dial math) so it is unit-testable.

## 3. Data model

### Entities (Room)

**Coffee** — `id: Long (PK, auto)`, `name: String`, `createdAt: Long`.
All descriptive data (origin, roaster, process…) goes into the name, by user decision.

**Method** — `id: Long`, `name: String?`, `builtInKey: String?`, `icon: MethodIcon`,
`tint: Tint`, `sortOrder: Int`.
- Built-in keys: `moka`, `espresso`, `v60`, `french`, `aero`.
- Display name = `name` if non-null, else the localized string for `builtInKey`.
  Renaming a built-in method stores `name`; the built-in key is kept (only for icon
  defaults / history, not for behavior).
- Seeded on first database creation (Room `onCreate` callback), in the mockup order:
  Moka, Espresso, V60, French press, AeroPress, with `name = null`.
- `MethodIcon` enum: `MOKA, ESPRESSO, V60, FRENCH_PRESS, AEROPRESS, DROP, CUP`
  (first five = mockup SVGs; DROP = the "Metodi" tab icon; CUP = new, same stroke style).
- `Tint` enum: `PEACH #F7DCCB, SAND #F2E4BC, SKY #D7E3EE, SAGE #DCE6D1, LILAC #E6DDEE`
  (mockup values; built-ins map moka→PEACH, espresso→SAND, v60→SKY, french→SAGE,
  aero→LILAC). Each tint has a dark-theme counterpart (section 6).

**Grinder** — `id: Long`, `name: String`, `multiTurn: Boolean`, `stepsPerTurn: Int?`
(nullable = unknown, "optional" in the mockup), `createdAt: Long`.
- For a single-turn grinder `stepsPerTurn` means "total steps".
- Display tint: rotates through `Tint` by list position; one shared grinder icon (the
  mockup's hand-grinder SVG).

**Calibration** — `id: Long`, `coffeeId`, `methodId`, `grinderId` (FKs, `ON DELETE CASCADE`),
`turns: Int` (0 for single-turn grinders), `steps: Int`, `notes: String` (default ""),
`updatedAt: Long`.
- Unique index on `(coffeeId, methodId, grinderId)`: **one value per combination,
  overwritten on edit** (no history).

### Setting rules (`domain/SettingRules`)

Given a grinder `(multiTurn, stepsPerTurn)`:

| Grinder | turns | steps range |
|---|---|---|
| multi-turn, N known | `0..∞` | `0..N-1` |
| multi-turn, N unknown | `0..∞` | `0..∞` |
| single-turn, N known | always 0, hidden | `0..N` |
| single-turn, N unknown | always 0, hidden | `0..∞` |

- Steppers clamp to these ranges. Switching grinder in the editor re-clamps the current
  values (as the mockup script does).
- `stepsPerTurn` when set must be `1..500`.
- If a grinder is edited so that existing calibrations fall outside the new ranges, those
  calibrations are **kept unchanged**; the grinder editor shows an inline warning with the
  count ("3 calibrations are outside the new range") before saving. Display of such a
  calibration still works (dial shows a full ring for overflow).
- If a grinder is switched from multi-turn to single-turn, existing `turns` values are kept
  and still displayed; the warning above counts calibrations with `turns > 0` too.

### Formatting

Localized plurals (`<plurals>`) for the summary:
- multi-turn with turns > 0: "2 turns + 12 steps" (IT "2 giri + 12 step").
- otherwise: "12 steps".
The compact list cells show the numbers with the mockup's small uppercase labels
("TURNS"/"STEPS", localized; IT "GIRI"/"STEP").

### Deletion

Deleting a coffee, method or grinder cascades to its calibrations. A confirmation dialog
always states the consequence with the count ("Delete 'V60'? 3 calibrations will be
deleted too."). Deleting a calibration asks for a simple confirmation.

## 4. Screens and navigation

Single activity, `NavHost`. Bottom navigation with 4 top-level destinations, each keeping
its own back stack/state (standard `saveState/restoreState`): **Calibrations** (start),
**Coffees**, **Methods**, **Grinders**. Bottom bar and FAB follow the mockup (88px nav
with pill indicator `#F4DCCD`, FAB 60×60 radius 22 in accent). Edge-to-edge with proper
insets; no fake status bar.

### 4.1 Calibrations (home) — `Main.dc.html`

- Header: small "tacca" brand mark in accent, H1 "Calibrations", right side: search
  button (44×44) and **settings** button (gear, same style) — settings is an addition.
- Search: tapping the search button turns the header into a search field (back arrow +
  text field + clear). Filters rows whose coffee, method or grinder name contains the
  query (case/diacritic-insensitive).
- Filter chips row: "All" + one chip per method (display name), in method `sortOrder`.
  Only methods that have at least one calibration are shown, plus the selected one.
- Optional coffee filter: when navigated from the Coffees tab, a dismissible chip
  "☕ <coffee name> ✕" appears before the method chips (icon = coffee bean SVG, no emoji).
- List grouped by coffee (coffee bean icon + name), rows as in the mockup: method icon on
  method tint, method name, grinder name, turns column (shown when the grinder is
  multi-turn — even at 0 — or when `turns > 0`) and steps in accent. Group order: most recently updated
  calibration first; rows inside a group by method `sortOrder`.
- Tap row → Detail. FAB → Calibration editor (new).
- Empty states:
  - No calibrations at all: illustration-free empty state (dial icon, title, one line,
    button "New calibration"). If there is no coffee or no grinder yet, the text says so
    and the editor handles inline creation (4.3).
  - Filters/search give no result: "No calibrations match" + "Clear filters".

### 4.2 Calibration detail — `Detail.dc.html`

- Top: back button, "Edit" button (→ editor prefilled).
- Coffee name (H1 28px), chips: method (icon + name on tint) and grinder (icon + name).
- Dial card: Compose `Canvas` dial, 260dp:
  - `N = stepsPerTurn` ticks (if unknown: 40 decorative ticks, none highlighted, no
    pointer); every 5th tick is major; ticks `0..steps` in accent, rest `#E3D6C8`;
    pointer dot at `steps/N`; one inner ring per turn (`r = 90 - k*9`, max 5 rings drawn,
    then a "+n" label). Geometry in `domain/DialGeometry` (pure, tested).
  - Center: "2 turns +" (only when turns > 0), big steps number in accent, "STEPS" label.
  - `contentDescription` = full formatted setting.
- Three step tiles as in the mockup: "Close to zero", "Turn N full turns" (hidden when
  turns = 0 → two tiles), "Add M steps" (highlighted).
- Notes card (only if notes non-empty).
- "Same coffee" section: other calibrations of the same coffee (method icon, method ·
  grinder, formatted setting); tap → that detail. Hidden when empty.

### 4.3 Calibration editor (new / edit) — `NewCalibration.dc.html`

- Title "New calibration" / "Edit calibration", close button.
- Sections (uppercase legends) as in the mockup: **Coffee** (radio-style list),
  **Method** (5-column grid of tinted tiles; wraps to more rows if more methods),
  **Grinder** (2-column grid, subtitle "Multi-turn · 40/turn" / "Single turn · 40 steps" /
  "... · steps unknown"), **Adjustment** (turns stepper — or the single-turn explanation
  text — and steps stepper with range hint), **Notes** (multiline text field, optional).
- Coffee list shows the 3 most recently used coffees + "Show all (n)" expanding the rest.
- Each of Coffee and Grinder sections ends with a "+ New" chip: coffee → the coffee sheet
  (4.4); grinder → grinder editor (4.6); on return the new item is selected.
- Screen is scrollable; the Save button is pinned at the bottom: "Save · 2 turns + 12
  steps". Disabled until coffee, method and grinder are chosen.
- New mode, if the chosen combination already exists: a line above Save
  "Replaces the current setting (1 turn + 30 steps)"; saving overwrites that record.
- Edit mode: changing the combination to one that exists elsewhere shows the same
  warning; on save the other record is replaced and the edited one moves to the new
  combination (one transaction). Edit mode also has a "Delete calibration" text button.
- After save: new → navigate to the detail (replacing the editor in the back stack);
  edit → pop back to the detail.
- Preselection when opened from home with a method/coffee filter active: those values
  are preselected. Default steps/turns for a fresh editor: 0/0.

### 4.4 Coffees — `Coffees.dc.html`

- H1 "Coffees", subtitle "n coffees" (plural), "+" button (accent) → coffee sheet (new).
- Search field filtering by name.
- Cards as in the mockup; subtitle "3 calibrations · V60, AeroPress, French press" or
  "No calibrations yet"; tint rotates by position.
- Tap → Calibrations tab with the coffee filter set. Long-press or trailing "⋮" button
  (44×44) → coffee sheet (edit) with Rename + Delete.
- **Coffee sheet** (modal bottom sheet, new UI in mockup style): title, name field
  (single line, autofocus, IME action Done), Save button in accent; in edit mode a
  "Delete coffee" text button (cascade confirmation).

### 4.5 Methods — `Methods.dc.html`

- H1 "Methods", subtitle "How you brew your coffee".
- 2-column grid of 150dp tiles on the method tint: icon in a 56×56 surface square, name,
  "n calibrations". Last tile: dashed "Add method".
- Tap → Calibrations tab with that method chip selected. Long-press or trailing "⋮" →
  method sheet (edit).
- **Method sheet**: name field (prefilled with the display name; for a built-in method,
  leaving the localized name unchanged keeps `name = null`), icon picker (7 icons, 44×44
  tiles, selected = dark border like the mockup method grid), tint picker (5 circles),
  Save; edit mode adds "Delete method" (cascade confirmation).

### 4.6 Grinders and grinder editor — `Grinders.dc.html`, `NewGrinder.dc.html`

- List as in the mockup (card: icon on tint, name, "n calibrations", chips "Multi-turn" +
  "40 steps per turn" / "Single turn" + "40 steps" / "Steps unknown"), then dashed
  "Add grinder".
- Tap card → grinder editor (edit). Dashed button → grinder editor (new).
- Grinder editor = mockup "New grinder" screen, reused for edit:
  name field, multi-turn switch with explanation, steps stepper ("Steps per turn" /
  "Total steps", optional: a "Not set" state reachable by stepping below 1 or via a
  "Don't know" text button; default 40 for a new grinder — the mockup script shows 30,
  40 matches the rest of the mockup data), preview card "Calibrations will read like
  this: 2 turns + 12 steps", Save button pinned at the bottom (the mockup omits it), in
  edit mode "Delete grinder", and the out-of-range warning (section 3).

### 4.7 Settings (new screen, mockup style)

Reached from the home header gear. Sections:
- **Accent color**: 4 circles `#B4552E` (terracotta, default), `#2F6B5A`, `#3D5A8A`,
  `#8A4B6E`; selected has a ring.
- **Theme**: segmented control System / Light / Dark.
- **Language**: row showing the current app language. API 33+: opens the system per-app
  language settings (`Settings.ACTION_APP_LOCALE_SETTINGS`). API < 33: in-app list
  (System default, English, Italiano, Español, Français, Deutsch) applied via
  `AppCompatDelegate.setApplicationLocales`.
- **Data**: "Export data" (SAF `CreateDocument("application/json")`, default name
  `tacca-backup-YYYY-MM-DD.json`), "Import data" (SAF `OpenDocument`, confirmation
  "This replaces all current data" before applying). Result shown in a snackbar.
- **About**: app version.

## 5. Export / import

JSON document:

```json
{
  "format": "tacca-backup",
  "version": 1,
  "exportedAt": "2026-09-26T10:00:00Z",
  "coffees":      [{"id": 1, "name": "...", "createdAt": 0}],
  "methods":      [{"id": 1, "name": null, "builtInKey": "moka", "icon": "MOKA", "tint": "PEACH", "sortOrder": 0}],
  "grinders":     [{"id": 1, "name": "...", "multiTurn": true, "stepsPerTurn": 40, "createdAt": 0}],
  "calibrations": [{"coffeeId": 1, "methodId": 1, "grinderId": 1, "turns": 2, "steps": 12, "notes": "", "updatedAt": 0}]
}
```

- Settings (accent/theme/language) are **not** exported.
- Import validates everything before writing: `format` and `version` (reject newer
  versions with a clear message), required fields, enum values, every calibration
  reference resolves, no duplicate combinations, non-negative numbers. Any failure →
  error snackbar, database untouched.
- Valid file → one Room transaction: delete all rows, insert all (IDs preserved).
- Android Auto Backup stays enabled (default rules) as a second safety net.

## 6. Visual system

Taken from the mockup (light theme):

| Token | Light | Dark |
|---|---|---|
| background | `#F6F0E8` | `#1C1410` |
| surface (cards, nav) | `#FFFCF8` | `#261C16` |
| outline / dividers | `#EADFD3` | `#3A2C23` |
| dashed border | `#D9C9B8` | `#4A392D` |
| inactive tick | `#E3D6C8` | `#4A392D` |
| text | `#2A1C13` | `#F3E9DF` |
| text muted | `#6F5B4C` | `#B9A594` |
| text on tint (subtitle) | `#5E4A3C` | `#D9C8B8` |
| nav pill / highlight | `#F4DCCD` | `#4A2E22` |
| tints PEACH/SAND/SKY/SAGE/LILAC | mockup values | `#4A3226`/`#463B22`/`#2A3844`/`#2F3A2A`/`#3A3044` |
| on-accent | `#FFFFFF` | `#FFFFFF` |

- Accent options as in 4.7. In dark theme the accent is used as-is for fills (white text
  on it); for accent-colored *text* on dark surfaces a lightened variant is derived
  (mix 35% with white) to keep ≥4.5:1. All pairs must meet WCAG AA: 4.5:1 for body text,
  3:1 for ≥24sp numbers.
- Typography: Bricolage Grotesque (700; display, numbers) and Figtree (400/500/600/700;
  body), bundled as OFL font files in `res/font` (license files committed). Sizes from
  the mockup: H1 34sp (tab screens) / 28sp (detail, editors), group titles 15sp/700, row
  title 15sp/600, secondary 13sp, legends 12sp/700 uppercase +0.07em, big dial number 76sp.
- Shapes: cards 22–28dp radius, buttons 16dp, chips full pill, method tiles 18dp (editor) /
  26dp (methods grid), icon squares 14–20dp — as in the mockup.
- Icons: the mockup's inline SVGs converted to vector drawables (stroke style, round caps,
  `currentColor` → tinted in Compose). Circles/ellipses converted to path data.
- App icon: adaptive icon — the brand mark (circle with notch at 12 o'clock) in white on
  terracotta; monochrome layer for themed icons.
- Touch targets ≥ 48dp; every icon-only button has a localized `contentDescription`.

## 7. Localization

- `values/strings.xml` = English (default and Play Store fallback). `values-it`,
  `values-es`, `values-fr`, `values-de` complete translations. Plurals via `<plurals>`.
- Built-in method names are string resources (`method_moka`, …). "Moka", "Espresso",
  "V60", "French press" and "AeroPress" are proper-ish names; translate only where a
  common local name exists (e.g. DE "French Press", FR "Cafetière à piston", ES "Prensa
  francesa").
- Translations are written by Claude and must be reviewed by native speakers before
  Play Store publication (noted in README).
- `android:localeConfig` + `res/xml/locales_config.xml` listing en, it, es, fr, de;
  `AppLocalesMetadataHolderService` with `autoStoreLocales=true` for API < 33.
- Lint: `MissingTranslation` and `ExtraTranslation` as errors.
- No hard-coded user-visible strings in Kotlin; formatting helpers take a `Resources`/
  `Context` or return resource IDs + args.

## 8. Error handling

- Room operations run in repositories on `Dispatchers.IO`; UI observes `Flow`s.
- Name fields: required after trim (Save disabled when blank). Duplicate names are
  allowed but show a non-blocking hint ("A coffee with this name already exists").
- Unique-combination handling is explicit in the repository (`upsertCalibration`), not
  left to a raw constraint exception.
- Export/import failures (I/O, validation) → snackbar with a specific localized message;
  no crash, no partial state.
- Room migrations: version 1 at release; `exportSchema = true`; future changes need a
  migration + migration test. No `fallbackToDestructiveMigration`.

## 9. Testing

All tests run on the JVM (no emulator available in the build environment):

- **Unit (JUnit)**: `SettingRules` (all rows of the table, re-clamping), formatting inputs,
  `DialGeometry`, backup serialization round trip and every validation rejection,
  ViewModels with fake repositories (editor: preselection, replace warning, save flows;
  grinder editor: out-of-range warning; home: filters and search).
- **Robolectric**: Room DAOs (unique combination upsert, cascade deletes, counts,
  seeding of built-in methods), `BackupService` import transaction on a real in-memory DB,
  Compose UI smoke tests for the main flow (create grinder → create coffee → create
  calibration → visible in home → detail shows the setting) and locale rendering
  (Italian strings appear under `it` config).
- **Lint** with warnings-as-errors for translation checks.
- Final verification: `./gradlew test lint assembleRelease bundleRelease`; the release APK
  is sent to the user for manual QA on a real phone.

## 10. Release setup

- `applicationId = "it.matteocolombo.tacca"`, `namespace` same, `versionCode 1`,
  `versionName "1.0.0"`.
- Release build: R8 minify + resource shrinking, required keep rules for
  kotlinx.serialization.
- Signing from `keystore.properties` (git-ignored) pointing to an upload keystore
  (git-ignored) generated locally; its location and the need to back it up are reported
  to the user. Debug builds sign with the default debug key.
- Outputs: release APK (for sideload QA) and AAB (for Play).
- Out of scope: Play Store listing, screenshots, privacy policy page (the app collects no
  data; a minimal policy will be needed at publication).

## 11. Out of scope (v1)

Cloud sync/accounts, calibration history, extra coffee fields, brew timers/recipes,
widgets, tablets-specific layouts (phone portrait first; landscape just scrolls),
iOS.
