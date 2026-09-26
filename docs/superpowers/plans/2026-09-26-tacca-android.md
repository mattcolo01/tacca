# Tacca Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build "Tacca", a native Android app (Kotlin + Jetpack Compose) that stores the grinder setting for every coffee × brew method × grinder combination, faithfully reproducing the mockup in `docs/mockup/`, multilingual (en/it/es/fr/de), local-only with JSON export/import, ready for a future Play Store release.

**Architecture:** Single Gradle module `:app`. Pure-Kotlin `domain/` (rules, dial math, models) → Room `data/db/` → repository interfaces in `data/repo/` (Room implementations) → one ViewModel + stateless Screen composable per screen in `ui/<screen>/`, wired by Navigation Compose in a single `AppCompatActivity`. Manual DI through `AppContainer` owned by `TaccaApplication`.

**Tech Stack:** AGP 9.4.1 (built-in Kotlin), Gradle 9.8.0, Kotlin 2.4.20 (Compose compiler + serialization plugins), KSP 2.3.12, Compose BOM 2026.09.00 (Material 3), Navigation Compose 2.10.2 (type-safe routes), Room 2.8.5, DataStore Preferences 1.2.1, AppCompat 1.8.0 (per-app locales), kotlinx.serialization 1.11.0, JUnit 4 + Robolectric 4.17 + Compose UI test.

**Spec:** `docs/superpowers/specs/2026-09-26-tacca-android-design.md` (read it before starting any task). Visual source of truth: `docs/mockup/*.dc.html` (exact colors, sizes, radii, spacing, SVG icon geometry).

## Global Constraints

- `applicationId` and `namespace`: `it.matteocolombo.tacca`; `versionCode 1`, `versionName "1.0.0"`.
- `minSdk 26`; `compileSdk` = API 37 minor 2 (`compileSdk { version = release(37) { minorApiLevel = 2 } }`); `targetSdk 36`.
- Java/Kotlin toolchain 21 (`jvmToolchain(21)`, `JavaVersion.VERSION_21`). Robolectric runs at `sdk=35` (`app/src/test/resources/robolectric.properties`) — SDK 36 sandboxes fail on this JDK.
- Do NOT apply `org.jetbrains.kotlin.android` (AGP 9 has built-in Kotlin). Plugins: `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`, `com.google.devtools.ksp`, `androidx.room`.
- Everything in code and git is English, ASCII only in identifiers and comments. User-visible text only in `res/values*/strings.xml`. Never hard-code a user-visible string in Kotlin.
- Languages: `values/` = English (default), `values-it`, `values-es`, `values-fr`, `values-de`; lint `MissingTranslation`/`ExtraTranslation` are errors.
- Dynamic color OFF. Colors only from `TaccaTheme.colors` (never literal hex in screens).
- Touch targets ≥ 48dp (44dp visual boxes get `Modifier.minimumInteractiveComponentSize()`), every icon-only button has a `contentDescription` from resources.
- Calibrations: exactly one per (coffeeId, methodId, grinderId); editing overwrites (no history).
- Deleting a coffee/method/grinder cascades to its calibrations after a confirmation that states the count.
- Export/import JSON: `format = "tacca-backup"`, `version = 1`; import validates fully before writing and replaces all data in one transaction; settings are not exported.
- Build commands always via a login shell so `ANDROID_HOME` is set: `bash -l -c './gradlew ...'` from the repo root `/home/matteo/p/tmp/tacca`.
- Commit after every task (English messages, trailer `Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>`; never a `Claude-Session:` line).

## Review Focus

1. **Filters pointing at deleted data** — the home coffee/method filter references an id that was deleted (or replaced by an import): the filter must silently reset, never leave an empty list with no visible chip. Pinned by `HomeViewModelTest.filterOnDeletedCoffeeIsIgnored` (Task 15).
2. **Double-tapping Save** in sheets/editors must not create duplicates. Pinned by `CoffeesViewModelTest.doubleSaveCreatesOneCoffee` (Task 11) and the `saving` guard in the calibration editor (`CalibrationEditorViewModelTest.doubleSaveCreatesOneCalibration`, Task 13).
3. **Editing a calibration that is out of range for its grinder** (grinder changed after saving) must show the stored values unchanged until the user touches a stepper. Pinned by `CalibrationEditorViewModelTest.outOfRangeValuesAreNotClampedOnLoad` (Task 13).
4. **Grinder with unknown steps per turn** — editor steppers unbounded, dial decorative with no pointer, detail tiles still correct. Pinned by `DialGeometryTest.unknownStepsPerTurnIsDecorative` (Task 2) and `DetailScreenTest.unknownStepsPerTurnShowsSetting` (Task 14).
5. **Very long coffee names** (the mockup's are long) must wrap/ellipsize without hiding the setting numbers. Pinned by `HomeScreenTest.longCoffeeNameKeepsSettingVisible` (Task 15).

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/*
app/build.gradle.kts, app/proguard-rules.pro, app/lint.xml, app/schemas/ (Room, generated, committed)
app/src/main/AndroidManifest.xml
app/src/main/java/it/matteocolombo/tacca/
  TaccaApplication.kt            Application; owns AppContainer
  AppContainer.kt                manual DI: database, repositories, settings, backup, documents, homeFilter
  MainActivity.kt                AppCompatActivity, edge-to-edge, theme from settings, hosts TaccaApp
  domain/
    GrindSetting.kt              GrindSetting, GrinderSpec
    SettingRules.kt              limits, clamping, range checks
    DialGeometry.kt              pure dial math
    Catalog.kt                   MethodIcon, Tint, BuiltInMethod
    Models.kt                    Coffee, Method, Grinder, Calibration
    AppSettings.kt               ThemeMode, AccentOption, AppSettings
    TextMatch.kt                 diacritic/case-insensitive search
  data/db/
    Entities.kt                  Room entities
    CalibrationRow.kt            joined projection
    Daos.kt                      CoffeeDao, MethodDao, GrinderDao, CalibrationDao
    Mappers.kt                   entity/row -> domain
    TaccaDatabase.kt             database + seed callback
  data/repo/
    Repositories.kt              interfaces + SaveCalibration + ExistingCalibration
    RoomCoffeeRepository.kt, RoomMethodRepository.kt, RoomGrinderRepository.kt, RoomCalibrationRepository.kt
    HomeFilterStore.kt           shared home filter state
  data/backup/
    BackupModels.kt              DTOs
    BackupCodec.kt               encode/decode/validate, BackupError, BackupException
    BackupService.kt             export/import against the database
    DocumentStreams.kt           ContentResolver abstraction
  data/settings/
    SettingsRepository.kt        DataStore-backed accent/theme
  ui/theme/  Colors.kt, Type.kt, Theme.kt
  ui/icons/  TaccaIcons.kt       MethodIcon -> drawable
  ui/format/ Labels.kt           setting/method/grinder/tint labels from resources
  ui/navigation/ Routes.kt, TopLevelDestination.kt, TaccaApp.kt
  ui/components/ Buttons.kt, Cards.kt, Chips.kt, Stepper.kt, Dial.kt, Dialogs.kt, EmptyState.kt, Fields.kt, Headers.kt, BottomBar.kt
  ui/grinders/     GrindersViewModel.kt, GrindersScreen.kt
  ui/grindereditor/GrinderEditorViewModel.kt, GrinderEditorScreen.kt
  ui/coffees/      CoffeesViewModel.kt, CoffeesScreen.kt, CoffeeSheet.kt
  ui/methods/      MethodsViewModel.kt, MethodsScreen.kt, MethodSheet.kt
  ui/editor/       CalibrationEditorViewModel.kt, CalibrationEditorScreen.kt
  ui/detail/       DetailViewModel.kt, DetailScreen.kt
  ui/home/         HomeViewModel.kt, HomeScreen.kt
  ui/settings/     SettingsViewModel.kt, SettingsScreen.kt, AppLanguage.kt
app/src/main/res/ drawable/ic_*.xml, font/*.ttf, values*/strings.xml, values/colors.xml, values-night/colors.xml, values/themes.xml, xml/locales_config.xml, mipmap-anydpi-v26/*
app/src/test/java/it/matteocolombo/tacca/ ... mirrors main; testutil/ (TestDb.kt, MainDispatcherRule.kt)
third_party/fonts/  OFL license texts
README.md
```

### Shared test utilities (created in Task 4, used from then on)

- `testutil/TestDb.kt`: `fun testDatabase(): TaccaDatabase` (in-memory, seeded) and `class TestRepos(db)` exposing Room repositories with a controllable clock.
- `testutil/MainDispatcherRule.kt`: sets `Dispatchers.Main` to an `UnconfinedTestDispatcher`.

ViewModel tests run under Robolectric (`@RunWith(AndroidJUnit4::class)`) against an in-memory Room database with the real repositories (higher fidelity than fakes; the spec's "fake repositories" intent — isolated, fast, deterministic — is kept). Room emits on its own executor, so tests await state with `flow.first { predicate }` instead of `advanceUntilIdle()`.

---

### Task 1: Project scaffold and toolchain

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `local.properties` (git-ignored)
- Create: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/lint.xml`
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/xml/locales_config.xml`
- Create: `app/src/main/java/it/matteocolombo/tacca/TaccaApplication.kt`, `app/src/main/java/it/matteocolombo/tacca/MainActivity.kt`
- Create: `app/src/test/resources/robolectric.properties`
- Test: `app/src/test/java/it/matteocolombo/tacca/AppLaunchTest.kt`

**Interfaces:**
- Produces: a building project; `TaccaApplication` (class, registered in manifest); `MainActivity : AppCompatActivity`; theme `Theme.Tacca`; string `app_name`.

- [ ] **Step 1: Verify the SDK environment**

The Android SDK is installed at `$HOME/android-sdk` and `ANDROID_HOME` is exported by `/etc/sandbox-persistent.sh`.

Run: `bash -l -c 'echo $ANDROID_HOME && ls $ANDROID_HOME'`
Expected: `/home/agent/android-sdk` and a listing containing `cmdline-tools platforms`. If missing, install: download `https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip`, unzip to `$HOME/android-sdk/cmdline-tools/latest`, accept licenses with `yes | sdkmanager --licenses`, and add the two export lines (`ANDROID_HOME`, `PATH`) to `/etc/sandbox-persistent.sh`. AGP downloads missing platforms/build-tools automatically on first build.

- [ ] **Step 2: Gradle wrapper**

Copy the wrapper from the verified probe project: `cp -r /home/matteo/.claude/jobs/3847e06c/tmp/probe/{gradlew,gradlew.bat,gradle} .` then delete `gradle/libs.versions.toml` from the copy if present (it is rewritten below). If the probe directory no longer exists, create a throwaway template with `bash -l -c 'android create --name=Tmp --application-id=tmp.tmp --min-sdk=26 -o /home/matteo/.claude/jobs/3847e06c/tmp/tmpl'` and copy its `gradlew`, `gradlew.bat`, `gradle/wrapper/`.

Make `gradle/wrapper/gradle-wrapper.properties` exactly:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Root build files**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Tacca"
include(":app")
```

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.20"
ksp = "2.3.12"
coreKtx = "1.19.1"
lifecycle = "2.11.0"
activity = "1.13.0"
composeBom = "2026.09.00"
navigation = "2.10.2"
room = "2.8.5"
datastore = "1.2.1"
appcompat = "1.8.0"
serialization = "1.11.0"
coroutines = "1.11.0"
junit = "4.13.2"
robolectric = "4.17"
androidxTestCore = "1.7.0"
androidxTestExt = "1.3.0"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-foundation = { module = "androidx.compose.foundation:foundation" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-navigation-testing = { module = "androidx.navigation:navigation-testing", version.ref = "navigation" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTestCore" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
```

`local.properties` (git-ignored, already in `.gitignore`): `sdk.dir=/home/agent/android-sdk`

- [ ] **Step 4: App module build file**

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "it.matteocolombo.tacca"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        applicationId = "it.matteocolombo.tacca"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
    androidResources { localeFilters += listOf("en", "it", "es", "fr", "de") }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint {
        abortOnError = true
        lintConfig = file("lint.xml")
    }
}

kotlin { jvmToolchain(21) }
room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
}
```

`app/proguard-rules.pro`:

```proguard
# kotlinx.serialization: keep generated serializers of our @Serializable classes
# (backup DTOs and type-safe navigation routes).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class it.matteocolombo.tacca.**$$serializer { *; }
-keepclassmembers class it.matteocolombo.tacca.** {
    *** Companion;
}
-keepclasseswithmembers class it.matteocolombo.tacca.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

`app/lint.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<lint>
    <issue id="MissingTranslation" severity="error" />
    <issue id="ExtraTranslation" severity="error" />
    <issue id="HardcodedText" severity="error" />
    <!-- Versions are pinned deliberately in the version catalog. -->
    <issue id="GradleDependency" severity="ignore" />
    <issue id="NewerVersionAvailable" severity="ignore" />
    <issue id="AndroidGradlePluginVersion" severity="ignore" />
</lint>
```

- [ ] **Step 5: Manifest and base resources**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".TaccaApplication"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:localeConfig="@xml/locales_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tacca">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Persists the per-app language on API < 33 (AppCompat). -->
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <meta-data
                android:name="autoStoreLocales"
                android:value="true" />
        </service>
    </application>

</manifest>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Tacca" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/window_background</item>
    </style>
</resources>
```

`app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window_background">#F6F0E8</color>
</resources>
```

`app/src/main/res/values-night/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window_background">#1C1410</color>
</resources>
```

`app/src/main/res/values/strings.xml` (the full string set comes in Task 8):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name" translatable="false">Tacca</string>
</resources>
```

`app/src/main/res/xml/locales_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="it" />
    <locale android:name="es" />
    <locale android:name="fr" />
    <locale android:name="de" />
</locale-config>
```

`app/src/test/resources/robolectric.properties`:

```properties
sdk=35
```

- [ ] **Step 6: Write the failing launch test**

`app/src/test/java/it/matteocolombo/tacca/AppLaunchTest.kt`:

```kotlin
package it.matteocolombo.tacca

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Temporary smoke test; replaced by AppFlowTest in Task 17.
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndShowsAppName() {
        compose.onNodeWithText("Tacca").assertIsDisplayed()
    }
}
```

- [ ] **Step 7: Run it to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*AppLaunchTest*"'`
Expected: compilation FAILS (`Unresolved reference 'MainActivity'`).

- [ ] **Step 8: Minimal application and activity**

`app/src/main/java/it/matteocolombo/tacca/TaccaApplication.kt`:

```kotlin
package it.matteocolombo.tacca

import android.app.Application

class TaccaApplication : Application()
```

`app/src/main/java/it/matteocolombo/tacca/MainActivity.kt`:

```kotlin
package it.matteocolombo.tacca

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Replaced by the real app shell in Task 17.
        setContent { Text(stringResource(R.string.app_name)) }
    }
}
```

- [ ] **Step 9: Run the test and a debug build**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*AppLaunchTest*" assembleDebug'`
Expected: `BUILD SUCCESSFUL`, 1 test passed. (First run downloads Gradle, dependencies and platform 37.2: several minutes.)

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "Scaffold Tacca Android project

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Domain model, setting rules, dial geometry, text matching

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/domain/GrindSetting.kt`, `SettingRules.kt`, `DialGeometry.kt`, `Catalog.kt`, `Models.kt`, `AppSettings.kt`, `TextMatch.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/domain/SettingRulesTest.kt`, `DialGeometryTest.kt`, `TextMatchTest.kt`

**Interfaces:**
- Produces (all in package `it.matteocolombo.tacca.domain`, no Android imports):
  - `data class GrindSetting(val turns: Int, val steps: Int)` with `GrindSetting.ZERO`
  - `data class GrinderSpec(val multiTurn: Boolean, val stepsPerTurn: Int?)`
  - `object SettingRules { MIN_STEPS_PER_TURN=1; MAX_STEPS_PER_TURN=500; DEFAULT_STEPS_PER_TURN=40; fun maxSteps(spec): Int?; fun clamp(setting, spec): GrindSetting; fun isInRange(setting, spec): Boolean; fun showsTurns(setting, spec): Boolean; fun isValidStepsPerTurn(n: Int?): Boolean }`
  - `data class DialPoint(x: Float, y: Float)`, `data class DialTick(inner, outer, major, active)`, `data class DialModel(ticks, pointer: DialPoint?, ringRadii: List<Float>, hiddenTurns: Int, outerRadius: Float)`, `object DialGeometry { DECORATIVE_TICKS=40; MAX_RINGS=5; fun build(size: Float, setting: GrindSetting, stepsPerTurn: Int?): DialModel }`
  - `enum class MethodIcon { MOKA, ESPRESSO, V60, FRENCH_PRESS, AEROPRESS, DROP, CUP }`
  - `enum class Tint { PEACH, SAND, SKY, SAGE, LILAC }` with `Tint.rotating(index: Int): Tint`
  - `enum class BuiltInMethod(val key: String, val icon: MethodIcon, val tint: Tint)` with `BuiltInMethod.fromKey(key: String?): BuiltInMethod?`
  - `data class Coffee(id: Long, name: String, createdAt: Long)`, `data class Method(id: Long, customName: String?, builtIn: BuiltInMethod?, icon: MethodIcon, tint: Tint, sortOrder: Int)`, `data class Grinder(id: Long, name: String, spec: GrinderSpec, createdAt: Long)`, `data class Calibration(id: Long, coffee: Coffee, method: Method, grinder: Grinder, setting: GrindSetting, notes: String, updatedAt: Long)`
  - `enum class ThemeMode { SYSTEM, LIGHT, DARK }`, `enum class AccentOption(val argb: Long) { TERRACOTTA, PINE, INDIGO, PLUM }`, `data class AppSettings(accent = TERRACOTTA, themeMode = SYSTEM)`
  - `object TextMatch { fun normalize(s: String): String; fun matches(query: String, vararg fields: String): Boolean }`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/domain/SettingRulesTest.kt`:

```kotlin
package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingRulesTest {
    private val multi40 = GrinderSpec(multiTurn = true, stepsPerTurn = 40)
    private val multiUnknown = GrinderSpec(multiTurn = true, stepsPerTurn = null)
    private val single40 = GrinderSpec(multiTurn = false, stepsPerTurn = 40)
    private val singleUnknown = GrinderSpec(multiTurn = false, stepsPerTurn = null)

    @Test fun maxStepsFollowsTheTable() {
        assertEquals(39, SettingRules.maxSteps(multi40))
        assertNull(SettingRules.maxSteps(multiUnknown))
        assertEquals(40, SettingRules.maxSteps(single40))
        assertNull(SettingRules.maxSteps(singleUnknown))
    }

    @Test fun clampMultiTurnKeepsTurnsAndCapsSteps() {
        assertEquals(GrindSetting(3, 39), SettingRules.clamp(GrindSetting(3, 55), multi40))
        assertEquals(GrindSetting(0, 0), SettingRules.clamp(GrindSetting(-1, -4), multi40))
    }

    @Test fun clampSingleTurnForcesZeroTurns() {
        assertEquals(GrindSetting(0, 40), SettingRules.clamp(GrindSetting(2, 90), single40))
    }

    @Test fun clampUnknownIsUnbounded() {
        assertEquals(GrindSetting(7, 999), SettingRules.clamp(GrindSetting(7, 999), multiUnknown))
        assertEquals(GrindSetting(0, 999), SettingRules.clamp(GrindSetting(7, 999), singleUnknown))
    }

    @Test fun isInRange() {
        assertTrue(SettingRules.isInRange(GrindSetting(2, 12), multi40))
        assertFalse(SettingRules.isInRange(GrindSetting(2, 40), multi40))
        assertFalse(SettingRules.isInRange(GrindSetting(1, 5), single40))
        assertTrue(SettingRules.isInRange(GrindSetting(0, 40), single40))
    }

    @Test fun showsTurns() {
        assertTrue(SettingRules.showsTurns(GrindSetting(0, 3), multi40))
        assertFalse(SettingRules.showsTurns(GrindSetting(0, 3), single40))
        assertTrue(SettingRules.showsTurns(GrindSetting(1, 3), single40))
    }

    @Test fun validStepsPerTurn() {
        assertTrue(SettingRules.isValidStepsPerTurn(null))
        assertTrue(SettingRules.isValidStepsPerTurn(1))
        assertTrue(SettingRules.isValidStepsPerTurn(500))
        assertFalse(SettingRules.isValidStepsPerTurn(0))
        assertFalse(SettingRules.isValidStepsPerTurn(501))
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/domain/DialGeometryTest.kt`:

```kotlin
package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialGeometryTest {
    private val eps = 0.01f

    @Test fun knownStepsPerTurnMatchesMockup() {
        val m = DialGeometry.build(260f, GrindSetting(2, 12), 40)
        assertEquals(40, m.ticks.size)
        assertEquals(13, m.ticks.count { it.active }) // ticks 0..12 inclusive
        assertEquals(130f, m.ticks[0].outer.x, eps)
        assertEquals(12f, m.ticks[0].outer.y, eps)
        assertTrue(m.ticks[0].major)
        assertEquals(false, m.ticks[1].major)
        val p = m.pointer!!
        assertEquals(242.22f, p.x, eps)
        assertEquals(166.46f, p.y, eps)
        assertEquals(listOf(90f, 81f), m.ringRadii)
        assertEquals(0, m.hiddenTurns)
    }

    @Test fun scalesWithSize() {
        val m = DialGeometry.build(520f, GrindSetting(0, 0), 40)
        assertEquals(236f, m.outerRadius, eps)
        assertEquals(24f, m.ticks[0].outer.y, eps)
    }

    @Test fun unknownStepsPerTurnIsDecorative() {
        val m = DialGeometry.build(260f, GrindSetting(1, 17), null)
        assertEquals(DialGeometry.DECORATIVE_TICKS, m.ticks.size)
        assertEquals(0, m.ticks.count { it.active })
        assertNull(m.pointer)
        assertEquals(1, m.ringRadii.size)
    }

    @Test fun ringsAreCappedAndOverflowCounted() {
        val m = DialGeometry.build(260f, GrindSetting(7, 0), 40)
        assertEquals(DialGeometry.MAX_RINGS, m.ringRadii.size)
        assertEquals(2, m.hiddenTurns)
    }

    @Test fun outOfRangeStepsFillTheDial() {
        val m = DialGeometry.build(260f, GrindSetting(0, 45), 40)
        assertEquals(40, m.ticks.count { it.active })
        assertEquals(130f, m.pointer!!.x, eps)
        assertEquals(12f, m.pointer!!.y, eps)
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/domain/TextMatchTest.kt`:

```kotlin
package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchTest {
    @Test fun normalizeStripsDiacriticsAndCase() {
        assertEquals("finca el paraiso", TextMatch.normalize("  Finca El Paraíso "))
        assertEquals("cafe", TextMatch.normalize("CAFÉ"))
    }

    @Test fun matchesAnyField() {
        assertTrue(TextMatch.matches("paraiso", "Colombia Huila Finca El Paraíso", "V60"))
        assertTrue(TextMatch.matches("v6", "Brasile", "V60"))
        assertFalse(TextMatch.matches("moka", "Brasile", "V60"))
    }

    @Test fun blankQueryMatchesEverything() {
        assertTrue(TextMatch.matches("   ", "anything"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.domain.*"'`
Expected: compilation FAILS (unresolved `GrinderSpec`, `DialGeometry`, `TextMatch`).

- [ ] **Step 3: Implement the domain files**

`domain/GrindSetting.kt`:

```kotlin
package it.matteocolombo.tacca.domain

/** A grinder setting as read on the dial: full turns from zero, then steps. */
data class GrindSetting(val turns: Int, val steps: Int) {
    companion object {
        val ZERO = GrindSetting(0, 0)
    }
}

/**
 * How a grinder dial is organised. For a single-turn grinder [stepsPerTurn] is the
 * total number of steps. `null` means the user does not know it.
 */
data class GrinderSpec(val multiTurn: Boolean, val stepsPerTurn: Int?)
```

`domain/SettingRules.kt`:

```kotlin
package it.matteocolombo.tacca.domain

object SettingRules {
    const val MIN_STEPS_PER_TURN = 1
    const val MAX_STEPS_PER_TURN = 500
    const val DEFAULT_STEPS_PER_TURN = 40

    /** Highest allowed steps value, or null when unbounded. */
    fun maxSteps(spec: GrinderSpec): Int? {
        val n = spec.stepsPerTurn ?: return null
        return if (spec.multiTurn) n - 1 else n
    }

    fun clamp(setting: GrindSetting, spec: GrinderSpec): GrindSetting {
        val turns = if (spec.multiTurn) setting.turns.coerceAtLeast(0) else 0
        val steps = setting.steps.coerceIn(0, maxSteps(spec) ?: Int.MAX_VALUE)
        return GrindSetting(turns, steps)
    }

    fun isInRange(setting: GrindSetting, spec: GrinderSpec): Boolean = clamp(setting, spec) == setting

    /** Whether the turns value is shown for this setting on this grinder. */
    fun showsTurns(setting: GrindSetting, spec: GrinderSpec): Boolean = spec.multiTurn || setting.turns > 0

    fun isValidStepsPerTurn(n: Int?): Boolean = n == null || n in MIN_STEPS_PER_TURN..MAX_STEPS_PER_TURN
}
```

`domain/DialGeometry.kt`:

```kotlin
package it.matteocolombo.tacca.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DialPoint(val x: Float, val y: Float)

data class DialTick(val inner: DialPoint, val outer: DialPoint, val major: Boolean, val active: Boolean)

data class DialModel(
    val ticks: List<DialTick>,
    /** Null when steps per turn are unknown. */
    val pointer: DialPoint?,
    val ringRadii: List<Float>,
    /** Turns beyond [DialGeometry.MAX_RINGS] that are not drawn as rings. */
    val hiddenTurns: Int,
    val outerRadius: Float,
)

/** Geometry of the mockup dial (designed on a 260 px square), scaled to [size]. */
object DialGeometry {
    const val DECORATIVE_TICKS = 40
    const val MAX_RINGS = 5
    private const val REFERENCE_SIZE = 260f

    fun build(size: Float, setting: GrindSetting, stepsPerTurn: Int?): DialModel {
        val scale = size / REFERENCE_SIZE
        val center = size / 2f
        val known = stepsPerTurn != null
        val count = stepsPerTurn ?: DECORATIVE_TICKS
        val outer = 118f * scale
        val ticks = (0 until count).map { i ->
            val a = angle(i.toFloat() / count)
            val major = i % 5 == 0
            val inner = (if (major) 102f else 108f) * scale
            DialTick(point(center, inner, a), point(center, outer, a), major, known && i <= setting.steps)
        }
        val pointer = if (known) {
            point(center, outer, angle((setting.steps.toFloat() / count).coerceIn(0f, 1f)))
        } else {
            null
        }
        val drawnRings = setting.turns.coerceIn(0, MAX_RINGS)
        val rings = (0 until drawnRings).map { k -> (90f - k * 9f) * scale }
        return DialModel(ticks, pointer, rings, (setting.turns - MAX_RINGS).coerceAtLeast(0), outer)
    }

    /** 0 = twelve o'clock, clockwise. */
    private fun angle(fraction: Float): Float = (fraction * 2.0 * PI - PI / 2).toFloat()

    private fun point(center: Float, radius: Float, angle: Float) =
        DialPoint(center + cos(angle) * radius, center + sin(angle) * radius)
}
```

`domain/Catalog.kt`:

```kotlin
package it.matteocolombo.tacca.domain

enum class MethodIcon { MOKA, ESPRESSO, V60, FRENCH_PRESS, AEROPRESS, DROP, CUP }

enum class Tint {
    PEACH, SAND, SKY, SAGE, LILAC;

    companion object {
        fun rotating(index: Int): Tint = entries[Math.floorMod(index, entries.size)]
    }
}

/** Methods seeded on first launch. The key is stable and stored in the database. */
enum class BuiltInMethod(val key: String, val icon: MethodIcon, val tint: Tint) {
    MOKA("moka", MethodIcon.MOKA, Tint.PEACH),
    ESPRESSO("espresso", MethodIcon.ESPRESSO, Tint.SAND),
    V60("v60", MethodIcon.V60, Tint.SKY),
    FRENCH_PRESS("french", MethodIcon.FRENCH_PRESS, Tint.SAGE),
    AEROPRESS("aero", MethodIcon.AEROPRESS, Tint.LILAC);

    companion object {
        fun fromKey(key: String?): BuiltInMethod? = entries.firstOrNull { it.key == key }
    }
}
```

`domain/Models.kt`:

```kotlin
package it.matteocolombo.tacca.domain

data class Coffee(val id: Long, val name: String, val createdAt: Long)

/** Display name: [customName] if set, otherwise the localized name of [builtIn]. */
data class Method(
    val id: Long,
    val customName: String?,
    val builtIn: BuiltInMethod?,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

data class Grinder(val id: Long, val name: String, val spec: GrinderSpec, val createdAt: Long)

data class Calibration(
    val id: Long,
    val coffee: Coffee,
    val method: Method,
    val grinder: Grinder,
    val setting: GrindSetting,
    val notes: String,
    val updatedAt: Long,
)
```

`domain/AppSettings.kt`:

```kotlin
package it.matteocolombo.tacca.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentOption(val argb: Long) {
    TERRACOTTA(0xFFB4552E),
    PINE(0xFF2F6B5A),
    INDIGO(0xFF3D5A8A),
    PLUM(0xFF8A4B6E),
}

data class AppSettings(
    val accent: AccentOption = AccentOption.TERRACOTTA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
```

`domain/TextMatch.kt`:

```kotlin
package it.matteocolombo.tacca.domain

import java.text.Normalizer
import java.util.Locale

/** Case- and diacritic-insensitive "contains" matching for search fields. */
object TextMatch {
    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT).trim()

    fun matches(query: String, vararg fields: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        return fields.any { normalize(it).contains(q) }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.domain.*"'`
Expected: `BUILD SUCCESSFUL`, all domain tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add domain model, setting rules and dial geometry

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Room database

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/data/db/Entities.kt`, `CalibrationRow.kt`, `Daos.kt`, `Mappers.kt`, `TaccaDatabase.kt`
- Create (generated by build, commit it): `app/schemas/it.matteocolombo.tacca.data.db.TaccaDatabase/1.json`
- Test: `app/src/test/java/it/matteocolombo/tacca/data/db/DaoTest.kt`

**Interfaces:**
- Consumes: domain types from Task 2.
- Produces:
  - Entities `CoffeeEntity(id, name, createdAt)`, `MethodEntity(id, name: String?, builtInKey: String?, icon: MethodIcon, tint: Tint, sortOrder: Int)`, `GrinderEntity(id, name, multiTurn, stepsPerTurn: Int?, createdAt)`, `CalibrationEntity(id, coffeeId, methodId, grinderId, turns, steps, notes, updatedAt)`; table names `coffee`, `method`, `grinder`, `calibration`.
  - DAOs (see code) incl. `CalibrationDao.observeRows(): Flow<List<CalibrationRow>>`, `observeRow(id): Flow<CalibrationRow?>`, `findByCombination(...)`, `getAll()`, `insertAll()`, `deleteAll()` on every DAO.
  - `TaccaDatabase.build(context)`, `TaccaDatabase.inMemory(context)`; both seed the 5 built-in methods on creation.
  - Mappers: `CoffeeEntity.toModel()`, `MethodEntity.toModel()`, `GrinderEntity.toModel()`, `CalibrationRow.toModel()`.

- [ ] **Step 1: Write the failing DAO tests**

`app/src/test/java/it/matteocolombo/tacca/data/db/DaoTest.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: TaccaDatabase

    @Before fun setUp() {
        db = TaccaDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After fun tearDown() = db.close()

    private suspend fun seedOne(): CalibrationEntity {
        val coffeeId = db.coffeeDao().insert(CoffeeEntity(name = "Brasile", createdAt = 1))
        val grinderId = db.grinderDao().insert(GrinderEntity(name = "JX-Pro", multiTurn = true, stepsPerTurn = 40, createdAt = 1))
        val methodId = db.methodDao().getAll().first().id
        val c = CalibrationEntity(coffeeId = coffeeId, methodId = methodId, grinderId = grinderId, turns = 2, steps = 12, notes = "", updatedAt = 5)
        return c.copy(id = db.calibrationDao().insert(c))
    }

    @Test fun seedsBuiltInMethodsInOrder() = runTest {
        val methods = db.methodDao().getAll()
        assertEquals(BuiltInMethod.entries.map { it.key }, methods.map { it.builtInKey })
        assertEquals(listOf(0, 1, 2, 3, 4), methods.map { it.sortOrder })
        assertEquals(MethodIcon.MOKA, methods[0].icon)
        assertEquals(Tint.PEACH, methods[0].tint)
        assertNull(methods[0].name)
    }

    @Test fun combinationIsUnique() = runTest {
        val c = seedOne()
        try {
            db.calibrationDao().insert(c.copy(id = 0))
            fail("expected unique constraint violation")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test fun deletingCoffeeCascades() = runTest {
        val c = seedOne()
        db.coffeeDao().delete(c.coffeeId)
        assertEquals(0, db.calibrationDao().getAll().size)
    }

    @Test fun deletingGrinderAndMethodCascade() = runTest {
        val c = seedOne()
        db.methodDao().delete(c.methodId)
        assertEquals(0, db.calibrationDao().getAll().size)
        val c2 = seedOne()
        db.grinderDao().delete(c2.grinderId)
        assertEquals(0, db.calibrationDao().getAll().size)
    }

    @Test fun rowsJoinAllFields() = runTest {
        val c = seedOne()
        val row = db.calibrationDao().observeRows().first().single()
        assertEquals("Brasile", row.coffeeName)
        assertEquals("moka", row.methodBuiltInKey)
        assertEquals("JX-Pro", row.grinderName)
        assertEquals(40, row.grinderStepsPerTurn)
        val model = row.toModel()
        assertEquals(GrindSetting(2, 12), model.setting)
        assertEquals(BuiltInMethod.MOKA, model.method.builtIn)
        assertEquals(c.id, db.calibrationDao().observeRow(c.id).first()!!.id)
    }

    @Test fun countsAndLookups() = runTest {
        val c = seedOne()
        assertEquals(1, db.calibrationDao().countForCoffee(c.coffeeId))
        assertEquals(1, db.calibrationDao().countForMethod(c.methodId))
        assertEquals(1, db.calibrationDao().countForGrinder(c.grinderId))
        assertEquals(c.id, db.calibrationDao().findByCombination(c.coffeeId, c.methodId, c.grinderId)!!.id)
        assertEquals(4, db.methodDao().maxSortOrder())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*DaoTest*"'`
Expected: compilation FAILS (unresolved `TaccaDatabase`).

- [ ] **Step 3: Implement entities, row, DAOs, mappers, database**

`data/db/Entities.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint

@Entity(tableName = "coffee")
data class CoffeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "method")
data class MethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null for a built-in method that keeps its localized name. */
    val name: String?,
    val builtInKey: String?,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

@Entity(tableName = "grinder")
data class GrinderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val multiTurn: Boolean,
    val stepsPerTurn: Int?,
    val createdAt: Long,
)

@Entity(
    tableName = "calibration",
    foreignKeys = [
        ForeignKey(entity = CoffeeEntity::class, parentColumns = ["id"], childColumns = ["coffeeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MethodEntity::class, parentColumns = ["id"], childColumns = ["methodId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GrinderEntity::class, parentColumns = ["id"], childColumns = ["grinderId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["coffeeId", "methodId", "grinderId"], unique = true),
        Index(value = ["methodId"]),
        Index(value = ["grinderId"]),
    ],
)
data class CalibrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coffeeId: Long,
    val methodId: Long,
    val grinderId: Long,
    val turns: Int,
    val steps: Int,
    val notes: String = "",
    val updatedAt: Long,
)
```

`data/db/CalibrationRow.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint

/** A calibration joined with its coffee, method and grinder. */
data class CalibrationRow(
    val id: Long,
    val coffeeId: Long,
    val coffeeName: String,
    val coffeeCreatedAt: Long,
    val methodId: Long,
    val methodName: String?,
    val methodBuiltInKey: String?,
    val methodIcon: MethodIcon,
    val methodTint: Tint,
    val methodSortOrder: Int,
    val grinderId: Long,
    val grinderName: String,
    val grinderMultiTurn: Boolean,
    val grinderStepsPerTurn: Int?,
    val grinderCreatedAt: Long,
    val turns: Int,
    val steps: Int,
    val notes: String,
    val updatedAt: Long,
)

internal const val CALIBRATION_ROW_SELECT = """
    SELECT c.id, c.coffeeId, co.name AS coffeeName, co.createdAt AS coffeeCreatedAt,
           c.methodId, m.name AS methodName, m.builtInKey AS methodBuiltInKey, m.icon AS methodIcon,
           m.tint AS methodTint, m.sortOrder AS methodSortOrder,
           c.grinderId, g.name AS grinderName, g.multiTurn AS grinderMultiTurn,
           g.stepsPerTurn AS grinderStepsPerTurn, g.createdAt AS grinderCreatedAt,
           c.turns, c.steps, c.notes, c.updatedAt
    FROM calibration c
    JOIN coffee co ON co.id = c.coffeeId
    JOIN method m ON m.id = c.methodId
    JOIN grinder g ON g.id = c.grinderId
"""
```

`data/db/Daos.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Query("SELECT * FROM coffee ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<CoffeeEntity>>

    @Query("SELECT * FROM coffee ORDER BY id")
    suspend fun getAll(): List<CoffeeEntity>

    @Query("SELECT * FROM coffee WHERE id = :id")
    suspend fun get(id: Long): CoffeeEntity?

    @Insert suspend fun insert(entity: CoffeeEntity): Long

    @Insert suspend fun insertAll(entities: List<CoffeeEntity>)

    @Query("UPDATE coffee SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM coffee WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM coffee")
    suspend fun deleteAll()
}

@Dao
interface MethodDao {
    @Query("SELECT * FROM method ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<MethodEntity>>

    @Query("SELECT * FROM method ORDER BY sortOrder, id")
    suspend fun getAll(): List<MethodEntity>

    @Query("SELECT * FROM method WHERE id = :id")
    suspend fun get(id: Long): MethodEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM method")
    suspend fun maxSortOrder(): Int

    @Insert suspend fun insert(entity: MethodEntity): Long

    @Insert suspend fun insertAll(entities: List<MethodEntity>)

    @Update suspend fun update(entity: MethodEntity)

    @Query("DELETE FROM method WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM method")
    suspend fun deleteAll()
}

@Dao
interface GrinderDao {
    @Query("SELECT * FROM grinder ORDER BY createdAt, id")
    fun observeAll(): Flow<List<GrinderEntity>>

    @Query("SELECT * FROM grinder ORDER BY id")
    suspend fun getAll(): List<GrinderEntity>

    @Query("SELECT * FROM grinder WHERE id = :id")
    suspend fun get(id: Long): GrinderEntity?

    @Insert suspend fun insert(entity: GrinderEntity): Long

    @Insert suspend fun insertAll(entities: List<GrinderEntity>)

    @Update suspend fun update(entity: GrinderEntity)

    @Query("DELETE FROM grinder WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM grinder")
    suspend fun deleteAll()
}

@Dao
interface CalibrationDao {
    @Query("$CALIBRATION_ROW_SELECT ORDER BY c.updatedAt DESC, c.id DESC")
    fun observeRows(): Flow<List<CalibrationRow>>

    @Query("$CALIBRATION_ROW_SELECT WHERE c.id = :id")
    fun observeRow(id: Long): Flow<CalibrationRow?>

    @Query("SELECT * FROM calibration WHERE coffeeId = :coffeeId AND methodId = :methodId AND grinderId = :grinderId")
    suspend fun findByCombination(coffeeId: Long, methodId: Long, grinderId: Long): CalibrationEntity?

    @Query("SELECT * FROM calibration WHERE id = :id")
    suspend fun get(id: Long): CalibrationEntity?

    @Query("SELECT * FROM calibration ORDER BY id")
    suspend fun getAll(): List<CalibrationEntity>

    @Query("SELECT * FROM calibration WHERE grinderId = :grinderId")
    suspend fun getForGrinder(grinderId: Long): List<CalibrationEntity>

    @Query("SELECT COUNT(*) FROM calibration WHERE coffeeId = :id")
    suspend fun countForCoffee(id: Long): Int

    @Query("SELECT COUNT(*) FROM calibration WHERE methodId = :id")
    suspend fun countForMethod(id: Long): Int

    @Query("SELECT COUNT(*) FROM calibration WHERE grinderId = :id")
    suspend fun countForGrinder(id: Long): Int

    @Insert suspend fun insert(entity: CalibrationEntity): Long

    @Insert suspend fun insertAll(entities: List<CalibrationEntity>)

    @Update suspend fun update(entity: CalibrationEntity)

    @Query("DELETE FROM calibration WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM calibration")
    suspend fun deleteAll()
}
```

If KSP rejects the string template in `@Query`, inline the SELECT text in both queries (annotation arguments must be compile-time constants; `const val` concatenation normally works).

`data/db/Mappers.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method

fun CoffeeEntity.toModel() = Coffee(id, name, createdAt)

fun MethodEntity.toModel() = Method(id, name, BuiltInMethod.fromKey(builtInKey), icon, tint, sortOrder)

fun GrinderEntity.toModel() = Grinder(id, name, GrinderSpec(multiTurn, stepsPerTurn), createdAt)

fun CalibrationRow.toModel() = Calibration(
    id = id,
    coffee = Coffee(coffeeId, coffeeName, coffeeCreatedAt),
    method = Method(methodId, methodName, BuiltInMethod.fromKey(methodBuiltInKey), methodIcon, methodTint, methodSortOrder),
    grinder = Grinder(grinderId, grinderName, GrinderSpec(grinderMultiTurn, grinderStepsPerTurn), grinderCreatedAt),
    setting = GrindSetting(turns, steps),
    notes = notes,
    updatedAt = updatedAt,
)
```

`data/db/TaccaDatabase.kt`:

```kotlin
package it.matteocolombo.tacca.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import it.matteocolombo.tacca.domain.BuiltInMethod

@Database(
    entities = [CoffeeEntity::class, MethodEntity::class, GrinderEntity::class, CalibrationEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TaccaDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun methodDao(): MethodDao
    abstract fun grinderDao(): GrinderDao
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        const val FILE_NAME = "tacca.db"

        fun build(context: Context): TaccaDatabase =
            Room.databaseBuilder(context.applicationContext, TaccaDatabase::class.java, FILE_NAME)
                .addCallback(SeedBuiltInMethods)
                .build()

        fun inMemory(context: Context): TaccaDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, TaccaDatabase::class.java)
                .addCallback(SeedBuiltInMethods)
                .allowMainThreadQueries()
                .build()
    }
}

/** Inserts the five built-in methods, in mockup order, when the database is created. */
internal object SeedBuiltInMethods : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        BuiltInMethod.entries.forEachIndexed { index, method ->
            db.execSQL(
                "INSERT INTO method (name, builtInKey, icon, tint, sortOrder) VALUES (NULL, ?, ?, ?, ?)",
                arrayOf<Any>(method.key, method.icon.name, method.tint.name, index),
            )
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*DaoTest*"'`
Expected: PASS (6 tests). `app/schemas/it.matteocolombo.tacca.data.db.TaccaDatabase/1.json` now exists.

- [ ] **Step 5: Commit** (include `app/schemas/`)

```bash
git add -A
git commit -m "Add Room database with entities, DAOs and built-in method seeding

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Repositories, home filter store, app container, test utilities

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/data/repo/Repositories.kt`, `RoomCoffeeRepository.kt`, `RoomMethodRepository.kt`, `RoomGrinderRepository.kt`, `RoomCalibrationRepository.kt`, `HomeFilterStore.kt`
- Create: `app/src/main/java/it/matteocolombo/tacca/AppContainer.kt`
- Modify: `app/src/main/java/it/matteocolombo/tacca/TaccaApplication.kt`
- Create: `app/src/test/java/it/matteocolombo/tacca/testutil/TestDb.kt`, `MainDispatcherRule.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/data/repo/RepositoryTest.kt`

**Interfaces:**
- Consumes: Task 3 DAOs/mappers, Task 2 domain.
- Produces (package `it.matteocolombo.tacca.data.repo`):

```kotlin
interface CoffeeRepository {
    fun observeCoffees(): Flow<List<Coffee>>          // ordered by name
    suspend fun get(id: Long): Coffee?
    suspend fun add(name: String): Long               // trims; blank -> IllegalArgumentException
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
}
interface MethodRepository {
    fun observeMethods(): Flow<List<Method>>          // ordered by sortOrder
    suspend fun get(id: Long): Method?
    suspend fun add(name: String, icon: MethodIcon, tint: Tint): Long   // sortOrder = max + 1
    suspend fun update(id: Long, customName: String?, icon: MethodIcon, tint: Tint) // customName null keeps built-in name
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
}
interface GrinderRepository {
    fun observeGrinders(): Flow<List<Grinder>>        // ordered by createdAt
    suspend fun get(id: Long): Grinder?
    suspend fun add(name: String, spec: GrinderSpec): Long
    suspend fun update(id: Long, name: String, spec: GrinderSpec)
    suspend fun delete(id: Long)
    suspend fun calibrationCount(id: Long): Int
    suspend fun countOutOfRange(id: Long, spec: GrinderSpec): Int
}
data class SaveCalibration(val editingId: Long?, val coffeeId: Long, val methodId: Long, val grinderId: Long, val setting: GrindSetting, val notes: String)
data class ExistingCalibration(val id: Long, val setting: GrindSetting)
interface CalibrationRepository {
    fun observeAll(): Flow<List<Calibration>>         // updatedAt desc
    fun observe(id: Long): Flow<Calibration?>
    suspend fun findExisting(coffeeId: Long, methodId: Long, grinderId: Long): ExistingCalibration?
    suspend fun save(request: SaveCalibration): Long  // insert / replace / move, one transaction; returns saved id
    suspend fun delete(id: Long)
}
data class HomeFilter(val methodId: Long? = null, val coffeeId: Long? = null, val query: String = "")
class HomeFilterStore { val filter: StateFlow<HomeFilter>; fun update(transform: (HomeFilter) -> HomeFilter); fun showCoffee(id: Long); fun showMethod(id: Long) }
```

  - `class AppContainer(context: Context)` with `database`, `coffees`, `methods`, `grinders`, `calibrations`, `homeFilter` (later tasks add `backup`, `documents`, `settings`).
  - `TaccaApplication.container: AppContainer`.
  - Test utils: `fun testDatabase(): TaccaDatabase`; `class TestRepos(val db: TaccaDatabase, var now: Long = 1_000L)` with `coffees`, `methods`, `grinders`, `calibrations`, `homeFilter`, and `suspend fun builtInMethod(m: BuiltInMethod): Method`; `class MainDispatcherRule`.

- [ ] **Step 1: Test utilities**

`app/src/test/java/it/matteocolombo/tacca/testutil/MainDispatcherRule.kt`:

```kotlin
package it.matteocolombo.tacca.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

`app/src/test/java/it/matteocolombo/tacca/testutil/TestDb.kt`:

```kotlin
package it.matteocolombo.tacca.testutil

import androidx.test.core.app.ApplicationProvider
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.RoomCalibrationRepository
import it.matteocolombo.tacca.data.repo.RoomCoffeeRepository
import it.matteocolombo.tacca.data.repo.RoomGrinderRepository
import it.matteocolombo.tacca.data.repo.RoomMethodRepository
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Method
import kotlinx.coroutines.flow.first

fun testDatabase(): TaccaDatabase = TaccaDatabase.inMemory(ApplicationProvider.getApplicationContext())

/** Real Room repositories over an in-memory database, with a controllable clock. */
class TestRepos(val db: TaccaDatabase = testDatabase(), var now: Long = 1_000L) {
    private val clock: () -> Long = { now++ }
    val coffees = RoomCoffeeRepository(db, clock)
    val methods = RoomMethodRepository(db)
    val grinders = RoomGrinderRepository(db, clock)
    val calibrations = RoomCalibrationRepository(db, clock)
    val homeFilter = HomeFilterStore()

    suspend fun builtInMethod(m: BuiltInMethod): Method = methods.observeMethods().first().first { it.builtIn == m }
}
```

- [ ] **Step 2: Write the failing repository tests**

`app/src/test/java/it/matteocolombo/tacca/data/repo/RepositoryTest.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private suspend fun ids(): Triple<Long, Long, Long> {
        val coffee = r.coffees.add("Brasile")
        val method = r.builtInMethod(BuiltInMethod.MOKA).id
        val grinder = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        return Triple(coffee, method, grinder)
    }

    @Test fun addTrimsNames() = runTest {
        val id = r.coffees.add("  Etiopia  ")
        assertEquals("Etiopia", r.coffees.get(id)!!.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNameIsRejected() = runTest {
        r.coffees.add("   ")
    }

    @Test fun saveNewInserts() = runTest {
        val (c, m, g) = ids()
        val id = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), " dose 18g "))
        val saved = r.calibrations.observe(id).first()!!
        assertEquals(GrindSetting(1, 30), saved.setting)
        assertEquals("dose 18g", saved.notes)
    }

    @Test fun saveNewOnExistingCombinationReplacesIt() = runTest {
        val (c, m, g) = ids()
        val first = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), ""))
        val second = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(2, 5), ""))
        assertEquals(first, second)
        val all = r.calibrations.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(GrindSetting(2, 5), all.single().setting)
    }

    @Test fun editMovingOntoExistingCombinationDeletesTheOther() = runTest {
        val (c, m, g) = ids()
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val a = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 1), ""))
        r.calibrations.save(SaveCalibration(null, c, v60, g, GrindSetting(3, 3), ""))
        val moved = r.calibrations.save(SaveCalibration(a, c, v60, g, GrindSetting(2, 2), ""))
        assertEquals(a, moved)
        val all = r.calibrations.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(v60, all.single().method.id)
        assertEquals(GrindSetting(2, 2), all.single().setting)
    }

    @Test fun findExisting() = runTest {
        val (c, m, g) = ids()
        assertNull(r.calibrations.findExisting(c, m, g))
        val id = r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(0, 7), ""))
        assertEquals(ExistingCalibration(id, GrindSetting(0, 7)), r.calibrations.findExisting(c, m, g))
    }

    @Test fun countOutOfRange() = runTest {
        val (c, m, g) = ids()
        r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 30), ""))
        assertEquals(0, r.grinders.countOutOfRange(g, GrinderSpec(true, 40)))
        assertEquals(1, r.grinders.countOutOfRange(g, GrinderSpec(true, 20)))
        assertEquals(1, r.grinders.countOutOfRange(g, GrinderSpec(false, 40))) // turns > 0 on single-turn
        assertEquals(0, r.grinders.countOutOfRange(g, GrinderSpec(true, null)))
    }

    @Test fun methodAddAppendsAndUpdateKeepsBuiltIn() = runTest {
        val id = r.methods.add("Chemex", MethodIcon.DROP, Tint.SKY)
        assertEquals(5, r.methods.get(id)!!.sortOrder)
        val moka = r.builtInMethod(BuiltInMethod.MOKA)
        r.methods.update(moka.id, "Bialetti", MethodIcon.MOKA, Tint.SAND)
        val updated = r.methods.get(moka.id)!!
        assertEquals("Bialetti", updated.customName)
        assertEquals(BuiltInMethod.MOKA, updated.builtIn)
        assertEquals(Tint.SAND, updated.tint)
    }

    @Test fun deleteCascadesAndCounts() = runTest {
        val (c, m, g) = ids()
        r.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(1, 1), ""))
        assertEquals(1, r.coffees.calibrationCount(c))
        assertEquals(1, r.methods.calibrationCount(m))
        assertEquals(1, r.grinders.calibrationCount(g))
        r.grinders.delete(g)
        assertEquals(0, r.calibrations.observeAll().first().size)
    }

    @Test fun homeFilterStore() {
        val s = HomeFilterStore()
        s.update { it.copy(query = "x") }
        s.showCoffee(3)
        assertEquals(HomeFilter(coffeeId = 3), s.filter.value)
        s.showMethod(2)
        assertEquals(HomeFilter(methodId = 2), s.filter.value)
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*RepositoryTest*"'`
Expected: compilation FAILS (unresolved repository classes).

- [ ] **Step 4: Implement repositories, filter store, container**

`data/repo/Repositories.kt`: put exactly the interface/data-class block from **Interfaces** above, with imports:

```kotlin
package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.Flow

// (interfaces CoffeeRepository, MethodRepository, GrinderRepository, CalibrationRepository,
//  data classes SaveCalibration, ExistingCalibration exactly as listed in the Interfaces block)

/** Trims [name] and rejects blank values. */
internal fun cleanName(name: String): String = name.trim().also { require(it.isNotEmpty()) { "Name must not be blank" } }
```

`data/repo/RoomCoffeeRepository.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.data.db.CoffeeEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Coffee
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCoffeeRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : CoffeeRepository {
    private val dao = db.coffeeDao()

    override fun observeCoffees(): Flow<List<Coffee>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Coffee? = dao.get(id)?.toModel()
    override suspend fun add(name: String): Long = dao.insert(CoffeeEntity(name = cleanName(name), createdAt = clock()))
    override suspend fun rename(id: Long, name: String) = dao.rename(id, cleanName(name))
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForCoffee(id)
}
```

`data/repo/RoomMethodRepository.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.MethodEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMethodRepository(private val db: TaccaDatabase) : MethodRepository {
    private val dao = db.methodDao()

    override fun observeMethods(): Flow<List<Method>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Method? = dao.get(id)?.toModel()

    override suspend fun add(name: String, icon: MethodIcon, tint: Tint): Long = db.withTransaction {
        dao.insert(MethodEntity(name = cleanName(name), builtInKey = null, icon = icon, tint = tint, sortOrder = dao.maxSortOrder() + 1))
    }

    override suspend fun update(id: Long, customName: String?, icon: MethodIcon, tint: Tint) {
        val current = checkNotNull(dao.get(id)) { "Method $id not found" }
        val name = customName?.let(::cleanName)
        require(name != null || current.builtInKey != null) { "A custom method needs a name" }
        dao.update(current.copy(name = name, icon = icon, tint = tint))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForMethod(id)
}
```

`data/repo/RoomGrinderRepository.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import it.matteocolombo.tacca.data.db.GrinderEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGrinderRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : GrinderRepository {
    private val dao = db.grinderDao()

    override fun observeGrinders(): Flow<List<Grinder>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun get(id: Long): Grinder? = dao.get(id)?.toModel()

    override suspend fun add(name: String, spec: GrinderSpec): Long {
        require(SettingRules.isValidStepsPerTurn(spec.stepsPerTurn))
        return dao.insert(GrinderEntity(name = cleanName(name), multiTurn = spec.multiTurn, stepsPerTurn = spec.stepsPerTurn, createdAt = clock()))
    }

    override suspend fun update(id: Long, name: String, spec: GrinderSpec) {
        require(SettingRules.isValidStepsPerTurn(spec.stepsPerTurn))
        val current = checkNotNull(dao.get(id)) { "Grinder $id not found" }
        dao.update(current.copy(name = cleanName(name), multiTurn = spec.multiTurn, stepsPerTurn = spec.stepsPerTurn))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun calibrationCount(id: Long): Int = db.calibrationDao().countForGrinder(id)

    override suspend fun countOutOfRange(id: Long, spec: GrinderSpec): Int =
        db.calibrationDao().getForGrinder(id).count { !SettingRules.isInRange(GrindSetting(it.turns, it.steps), spec) }
}
```

`data/repo/RoomCalibrationRepository.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.CalibrationEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.db.toModel
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.GrindSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCalibrationRepository(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : CalibrationRepository {
    private val dao = db.calibrationDao()

    override fun observeAll(): Flow<List<Calibration>> = dao.observeRows().map { rows -> rows.map { it.toModel() } }
    override fun observe(id: Long): Flow<Calibration?> = dao.observeRow(id).map { it?.toModel() }

    override suspend fun findExisting(coffeeId: Long, methodId: Long, grinderId: Long): ExistingCalibration? =
        dao.findByCombination(coffeeId, methodId, grinderId)?.let { ExistingCalibration(it.id, GrindSetting(it.turns, it.steps)) }

    override suspend fun save(request: SaveCalibration): Long = db.withTransaction {
        val now = clock()
        val notes = request.notes.trim()
        val existing = dao.findByCombination(request.coffeeId, request.methodId, request.grinderId)
        if (request.editingId == null) {
            if (existing != null) {
                dao.update(existing.copy(turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now))
                existing.id
            } else {
                dao.insert(
                    CalibrationEntity(
                        coffeeId = request.coffeeId, methodId = request.methodId, grinderId = request.grinderId,
                        turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now,
                    ),
                )
            }
        } else {
            val editing = checkNotNull(dao.get(request.editingId)) { "Calibration ${request.editingId} not found" }
            if (existing != null && existing.id != editing.id) dao.delete(existing.id)
            dao.update(
                editing.copy(
                    coffeeId = request.coffeeId, methodId = request.methodId, grinderId = request.grinderId,
                    turns = request.setting.turns, steps = request.setting.steps, notes = notes, updatedAt = now,
                ),
            )
            editing.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
```

`data/repo/HomeFilterStore.kt`:

```kotlin
package it.matteocolombo.tacca.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeFilter(val methodId: Long? = null, val coffeeId: Long? = null, val query: String = "")

/** Home screen filter, shared so that the Coffees and Methods tabs can open a filtered home. */
class HomeFilterStore {
    private val state = MutableStateFlow(HomeFilter())
    val filter: StateFlow<HomeFilter> = state.asStateFlow()

    fun update(transform: (HomeFilter) -> HomeFilter) = state.update(transform)
    fun showCoffee(id: Long) = state.update { HomeFilter(coffeeId = id) }
    fun showMethod(id: Long) = state.update { HomeFilter(methodId = id) }
}
```

`AppContainer.kt`:

```kotlin
package it.matteocolombo.tacca

import android.content.Context
import it.matteocolombo.tacca.data.db.TaccaDatabase
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.data.repo.RoomCalibrationRepository
import it.matteocolombo.tacca.data.repo.RoomCoffeeRepository
import it.matteocolombo.tacca.data.repo.RoomGrinderRepository
import it.matteocolombo.tacca.data.repo.RoomMethodRepository

/** Manual dependency container, one per process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TaccaDatabase by lazy { TaccaDatabase.build(appContext) }
    val coffees: CoffeeRepository by lazy { RoomCoffeeRepository(database) }
    val methods: MethodRepository by lazy { RoomMethodRepository(database) }
    val grinders: GrinderRepository by lazy { RoomGrinderRepository(database) }
    val calibrations: CalibrationRepository by lazy { RoomCalibrationRepository(database) }
    val homeFilter = HomeFilterStore()
}
```

`TaccaApplication.kt`:

```kotlin
package it.matteocolombo.tacca

import android.app.Application

class TaccaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `bash -l -c './gradlew testDebugUnitTest'`
Expected: PASS (all tests so far).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Add repositories, home filter store and app container

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Backup export/import

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/data/backup/BackupModels.kt`, `BackupCodec.kt`, `BackupService.kt`, `DocumentStreams.kt`
- Modify: `app/src/main/java/it/matteocolombo/tacca/AppContainer.kt` (add `backup`, `documents`)
- Test: `app/src/test/java/it/matteocolombo/tacca/data/backup/BackupCodecTest.kt`, `BackupServiceTest.kt`

**Interfaces:**
- Consumes: `TaccaDatabase` + DAOs (Task 3), `SettingRules`, `BuiltInMethod` (Task 2).
- Produces (package `it.matteocolombo.tacca.data.backup`):
  - `@Serializable data class BackupFile(format, version, exportedAt, coffees, methods, grinders, calibrations)` + DTOs `CoffeeDto`, `MethodDto`, `GrinderDto`, `CalibrationDto`
  - `enum class BackupError { NOT_A_BACKUP, NEWER_VERSION, INVALID_DATA, IO }`, `class BackupException(val error: BackupError, cause: Throwable? = null)`
  - `object BackupCodec { FORMAT = "tacca-backup"; VERSION = 1; fun encode(file: BackupFile): String; fun decode(text: String): BackupFile /* throws BackupException */ }`
  - `class BackupService(db: TaccaDatabase, clock: () -> Long = System::currentTimeMillis) { suspend fun createBackup(): BackupFile; suspend fun restore(file: BackupFile); suspend fun export(out: OutputStream); suspend fun import(input: InputStream) }` — `export`/`import` throw `BackupException`
  - `interface DocumentStreams { fun openInput(uri: Uri): InputStream?; fun openOutput(uri: Uri): OutputStream? }`, `class ContentResolverDocumentStreams(resolver: ContentResolver)`
  - `AppContainer.backup: BackupService`, `AppContainer.documents: DocumentStreams`

- [ ] **Step 1: Write the failing codec tests**

`app/src/test/java/it/matteocolombo/tacca/data/backup/BackupCodecTest.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val valid = BackupFile(
        format = BackupCodec.FORMAT,
        version = 1,
        exportedAt = "2026-09-26T10:00:00Z",
        coffees = listOf(CoffeeDto(1, "Brasile", 10)),
        methods = listOf(MethodDto(1, null, "moka", MethodIcon.MOKA, Tint.PEACH, 0), MethodDto(2, "Chemex", null, MethodIcon.DROP, Tint.SKY, 1)),
        grinders = listOf(GrinderDto(1, "JX-Pro", true, 40, 10)),
        calibrations = listOf(CalibrationDto(1, 1, 1, 2, 12, "", 20)),
    )

    private fun expectError(expected: BackupError, text: String) {
        try {
            BackupCodec.decode(text)
            fail("expected $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.error)
        }
    }

    private fun invalid(file: BackupFile) = expectError(BackupError.INVALID_DATA, BackupCodec.encode(file))

    @Test fun roundTrip() = assertEquals(valid, BackupCodec.decode(BackupCodec.encode(valid)))

    @Test fun ignoresUnknownKeys() {
        val text = BackupCodec.encode(valid).replaceFirst("{", "{\"extra\": 1,")
        assertEquals(valid, BackupCodec.decode(text))
    }

    @Test fun notJson() = expectError(BackupError.NOT_A_BACKUP, "hello")
    @Test fun wrongFormat() = expectError(BackupError.NOT_A_BACKUP, """{"format":"other","version":1}""")
    @Test fun jsonArray() = expectError(BackupError.NOT_A_BACKUP, "[1,2]")
    @Test fun newerVersion() = expectError(BackupError.NEWER_VERSION, BackupCodec.encode(valid.copy(version = 2)))
    @Test fun missingFields() = expectError(BackupError.INVALID_DATA, """{"format":"tacca-backup","version":1}""")
    @Test fun unknownEnum() = expectError(BackupError.INVALID_DATA, BackupCodec.encode(valid).replace("\"PEACH\"", "\"NEON\""))
    @Test fun blankCoffeeName() = invalid(valid.copy(coffees = listOf(CoffeeDto(1, " ", 10))))
    @Test fun duplicateIds() = invalid(valid.copy(coffees = listOf(CoffeeDto(1, "a", 1), CoffeeDto(1, "b", 1))))
    @Test fun danglingReference() = invalid(valid.copy(calibrations = listOf(CalibrationDto(9, 1, 1, 0, 0, "", 1))))
    @Test fun negativeValues() = invalid(valid.copy(calibrations = listOf(CalibrationDto(1, 1, 1, -1, 0, "", 1))))
    @Test fun duplicateCombination() = invalid(valid.copy(calibrations = valid.calibrations + valid.calibrations))
    @Test fun badStepsPerTurn() = invalid(valid.copy(grinders = listOf(GrinderDto(1, "g", true, 0, 1))))
    @Test fun customMethodWithoutName() = invalid(valid.copy(methods = listOf(MethodDto(1, null, null, MethodIcon.CUP, Tint.SAND, 0))))
    @Test fun unknownBuiltInKey() = invalid(valid.copy(methods = listOf(MethodDto(1, null, "siphon", MethodIcon.CUP, Tint.SAND, 0))))
}
```

- [ ] **Step 2: Write the failing service tests**

`app/src/test/java/it/matteocolombo/tacca/data/backup/BackupServiceTest.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupServiceTest {
    private val source = TestRepos()
    private val target = TestRepos()

    @After fun tearDown() {
        source.db.close()
        target.db.close()
    }

    private suspend fun populate() {
        val c = source.coffees.add("Etiopia — lavato")
        val m = source.builtInMethod(BuiltInMethod.V60).id
        val g = source.grinders.add("JX-Pro", GrinderSpec(true, 40))
        source.methods.add("Chemex", MethodIcon.CUP, Tint.LILAC)
        source.calibrations.save(SaveCalibration(null, c, m, g, GrindSetting(2, 12), "18g"))
    }

    @Test fun exportThenImportRestoresIdenticalData() = runTest {
        populate()
        target.coffees.add("to be replaced")
        val out = ByteArrayOutputStream()
        BackupService(source.db).export(out)
        BackupService(target.db).import(ByteArrayInputStream(out.toByteArray()))

        assertEquals(source.db.coffeeDao().getAll(), target.db.coffeeDao().getAll())
        assertEquals(source.db.methodDao().getAll(), target.db.methodDao().getAll())
        assertEquals(source.db.grinderDao().getAll(), target.db.grinderDao().getAll())
        // Calibration ids are not part of the backup format; compare everything else.
        assertEquals(
            source.db.calibrationDao().getAll().map { it.copy(id = 0) },
            target.db.calibrationDao().getAll().map { it.copy(id = 0) },
        )
    }

    @Test fun invalidImportLeavesDataUntouched() = runTest {
        target.coffees.add("keep me")
        try {
            BackupService(target.db).import(ByteArrayInputStream("{\"format\":\"tacca-backup\",\"version\":1}".toByteArray()))
            fail("expected BackupException")
        } catch (e: BackupException) {
            assertEquals(BackupError.INVALID_DATA, e.error)
        }
        assertEquals(listOf("keep me"), target.db.coffeeDao().getAll().map { it.name })
        assertEquals(5, target.db.methodDao().getAll().size)
    }

    @Test fun exportUsesFormatAndVersion() = runTest {
        val file = BackupService(source.db, clock = { 0L }).createBackup()
        assertEquals("tacca-backup", file.format)
        assertEquals(1, file.version)
        assertEquals("1970-01-01T00:00:00Z", file.exportedAt)
        assertEquals(5, file.methods.size)
    }
}
```

- [ ] **Step 3: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*Backup*"'`
Expected: compilation FAILS.

- [ ] **Step 4: Implement**

`data/backup/BackupModels.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val coffees: List<CoffeeDto>,
    val methods: List<MethodDto>,
    val grinders: List<GrinderDto>,
    val calibrations: List<CalibrationDto>,
)

@Serializable
data class CoffeeDto(val id: Long, val name: String, val createdAt: Long)

@Serializable
data class MethodDto(
    val id: Long,
    val name: String? = null,
    val builtInKey: String? = null,
    val icon: MethodIcon,
    val tint: Tint,
    val sortOrder: Int,
)

@Serializable
data class GrinderDto(val id: Long, val name: String, val multiTurn: Boolean, val stepsPerTurn: Int? = null, val createdAt: Long)

@Serializable
data class CalibrationDto(
    val coffeeId: Long,
    val methodId: Long,
    val grinderId: Long,
    val turns: Int,
    val steps: Int,
    val notes: String = "",
    val updatedAt: Long,
)
```

`data/backup/BackupCodec.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.SettingRules
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

enum class BackupError { NOT_A_BACKUP, NEWER_VERSION, INVALID_DATA, IO }

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(error.name, cause)

object BackupCodec {
    const val FORMAT = "tacca-backup"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** Parses and fully validates [text]. Throws [BackupException] on any problem. */
    fun decode(text: String): BackupFile {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw BackupException(BackupError.NOT_A_BACKUP, e)
        }
        val obj = root as? JsonObject ?: throw BackupException(BackupError.NOT_A_BACKUP)
        if ((obj["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) throw BackupException(BackupError.NOT_A_BACKUP)
        val version = (obj["version"] as? JsonPrimitive)?.intOrNull ?: throw BackupException(BackupError.INVALID_DATA)
        if (version > VERSION) throw BackupException(BackupError.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupError.INVALID_DATA)
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), obj)
        } catch (e: SerializationException) {
            throw BackupException(BackupError.INVALID_DATA, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.INVALID_DATA, e)
        }
        validate(file)
        return file
    }

    private fun validate(f: BackupFile) {
        fun ensure(ok: Boolean) {
            if (!ok) throw BackupException(BackupError.INVALID_DATA)
        }
        val coffeeIds = f.coffees.map { it.id }.toSet()
        val methodIds = f.methods.map { it.id }.toSet()
        val grinderIds = f.grinders.map { it.id }.toSet()
        ensure(coffeeIds.size == f.coffees.size && methodIds.size == f.methods.size && grinderIds.size == f.grinders.size)
        f.coffees.forEach { ensure(it.name.isNotBlank()) }
        f.methods.forEach {
            ensure(it.name == null || it.name.isNotBlank())
            ensure(it.builtInKey == null || BuiltInMethod.fromKey(it.builtInKey) != null)
            ensure(it.name != null || it.builtInKey != null)
        }
        f.grinders.forEach {
            ensure(it.name.isNotBlank())
            ensure(SettingRules.isValidStepsPerTurn(it.stepsPerTurn))
        }
        f.calibrations.forEach {
            ensure(it.coffeeId in coffeeIds && it.methodId in methodIds && it.grinderId in grinderIds)
            ensure(it.turns >= 0 && it.steps >= 0)
        }
        ensure(f.calibrations.map { Triple(it.coffeeId, it.methodId, it.grinderId) }.toSet().size == f.calibrations.size)
    }
}
```

`data/backup/BackupService.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import androidx.room.withTransaction
import it.matteocolombo.tacca.data.db.CalibrationEntity
import it.matteocolombo.tacca.data.db.CoffeeEntity
import it.matteocolombo.tacca.data.db.GrinderEntity
import it.matteocolombo.tacca.data.db.MethodEntity
import it.matteocolombo.tacca.data.db.TaccaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class BackupService(
    private val db: TaccaDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun createBackup(): BackupFile = db.withTransaction {
        BackupFile(
            format = BackupCodec.FORMAT,
            version = BackupCodec.VERSION,
            exportedAt = Instant.ofEpochMilli(clock()).toString(),
            coffees = db.coffeeDao().getAll().map { CoffeeDto(it.id, it.name, it.createdAt) },
            methods = db.methodDao().getAll().map { MethodDto(it.id, it.name, it.builtInKey, it.icon, it.tint, it.sortOrder) },
            grinders = db.grinderDao().getAll().map { GrinderDto(it.id, it.name, it.multiTurn, it.stepsPerTurn, it.createdAt) },
            calibrations = db.calibrationDao().getAll().map {
                CalibrationDto(it.coffeeId, it.methodId, it.grinderId, it.turns, it.steps, it.notes, it.updatedAt)
            },
        )
    }

    /** Replaces all data with [file] in one transaction. [file] must come from [BackupCodec.decode]. */
    suspend fun restore(file: BackupFile) = db.withTransaction {
        db.calibrationDao().deleteAll()
        db.coffeeDao().deleteAll()
        db.methodDao().deleteAll()
        db.grinderDao().deleteAll()
        db.coffeeDao().insertAll(file.coffees.map { CoffeeEntity(it.id, it.name, it.createdAt) })
        db.methodDao().insertAll(file.methods.map { MethodEntity(it.id, it.name, it.builtInKey, it.icon, it.tint, it.sortOrder) })
        db.grinderDao().insertAll(file.grinders.map { GrinderEntity(it.id, it.name, it.multiTurn, it.stepsPerTurn, it.createdAt) })
        db.calibrationDao().insertAll(
            file.calibrations.map {
                CalibrationEntity(coffeeId = it.coffeeId, methodId = it.methodId, grinderId = it.grinderId, turns = it.turns, steps = it.steps, notes = it.notes, updatedAt = it.updatedAt)
            },
        )
    }

    suspend fun export(out: OutputStream) {
        val text = BackupCodec.encode(createBackup())
        withContext(Dispatchers.IO) {
            try {
                out.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            } catch (e: IOException) {
                throw BackupException(BackupError.IO, e)
            }
        }
    }

    suspend fun import(input: InputStream) {
        val text = withContext(Dispatchers.IO) {
            try {
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: IOException) {
                throw BackupException(BackupError.IO, e)
            }
        }
        restore(BackupCodec.decode(text))
    }
}
```

Note: calibration ids are not part of the backup format (spec section 5), so restored calibrations get fresh auto-generated ids; coffee, method and grinder ids are preserved.

`data/backup/DocumentStreams.kt`:

```kotlin
package it.matteocolombo.tacca.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/** Opens user-picked documents; abstracted so ViewModels can be tested without a ContentResolver. */
interface DocumentStreams {
    fun openInput(uri: Uri): InputStream?
    fun openOutput(uri: Uri): OutputStream?
}

class ContentResolverDocumentStreams(private val resolver: ContentResolver) : DocumentStreams {
    override fun openInput(uri: Uri): InputStream? = resolver.openInputStream(uri)
    override fun openOutput(uri: Uri): OutputStream? = resolver.openOutputStream(uri, "wt")
}
```

In `AppContainer` add:

```kotlin
    val backup: BackupService by lazy { BackupService(database) }
    val documents: DocumentStreams by lazy { ContentResolverDocumentStreams(appContext.contentResolver) }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*Backup*"'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Add JSON backup export and validated import

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Settings store, theme, fonts

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/data/settings/SettingsRepository.kt`
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/theme/Colors.kt`, `Type.kt`, `Theme.kt`
- Create: `app/src/main/res/font/bricolage_grotesque.ttf`, `app/src/main/res/font/figtree.ttf`, `third_party/fonts/OFL-BricolageGrotesque.txt`, `third_party/fonts/OFL-Figtree.txt`
- Modify: `app/src/main/java/it/matteocolombo/tacca/AppContainer.kt` (add `settings`)
- Test: `app/src/test/java/it/matteocolombo/tacca/data/settings/SettingsRepositoryTest.kt`, `app/src/test/java/it/matteocolombo/tacca/ui/theme/ThemeContrastTest.kt`

**Interfaces:**
- Consumes: `AppSettings`, `AccentOption`, `ThemeMode`, `Tint` (Task 2).
- Produces:
  - `interface SettingsRepository { val settings: Flow<AppSettings>; suspend fun setAccent(accent: AccentOption); suspend fun setThemeMode(mode: ThemeMode) }`, `class DataStoreSettingsRepository(store: DataStore<Preferences>)`, `val Context.settingsDataStore: DataStore<Preferences>`
  - `AppContainer.settings: SettingsRepository`
  - `@Immutable data class TaccaColors(isDark, background, surface, outline, dashed, tickInactive, text, textMuted, textOnTint, highlight, accent, accentText, onAccent, danger, tints: Map<Tint, Color>)` with `fun tint(t: Tint): Color`; `fun taccaColors(dark: Boolean, accent: AccentOption): TaccaColors`
  - `val Bricolage: FontFamily`, `val Figtree: FontFamily`, `object TaccaType { brand, h1, h1Small, cardTitle, numberXL, numberL, numberM, numberS, title, rowTitle, body, bodySmall, label, legend, unit, nav, button }`
  - `val LocalTaccaColors`, `object TaccaTheme { val colors: TaccaColors }` (composable getter), `@Composable fun TaccaTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit)`

- [ ] **Step 1: Download fonts (OFL)**

```bash
mkdir -p app/src/main/res/font third_party/fonts
curl -sSfL -o app/src/main/res/font/bricolage_grotesque.ttf "https://github.com/google/fonts/raw/main/ofl/bricolagegrotesque/BricolageGrotesque%5Bopsz,wdth,wght%5D.ttf"
curl -sSfL -o app/src/main/res/font/figtree.ttf "https://github.com/google/fonts/raw/main/ofl/figtree/Figtree%5Bwght%5D.ttf"
curl -sSfL -o third_party/fonts/OFL-BricolageGrotesque.txt "https://github.com/google/fonts/raw/main/ofl/bricolagegrotesque/OFL.txt"
curl -sSfL -o third_party/fonts/OFL-Figtree.txt "https://github.com/google/fonts/raw/main/ofl/figtree/OFL.txt"
ls -l app/src/main/res/font
```

Expected: `bricolage_grotesque.ttf` ≈ 400 KB, `figtree.ttf` ≈ 60 KB.

- [ ] **Step 2: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/theme/ThemeContrastTest.kt` (plain JVM test — Compose `Color` math needs no Android):

```kotlin
package it.matteocolombo.tacca.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb).toDouble() / minOf(la, lb).toDouble()
    }

    private fun check(name: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$name contrast $ratio < $min", ratio >= min)
    }

    @Test fun allPairsMeetWcagAa() {
        for (dark in listOf(false, true)) for (accent in AccentOption.entries) {
            val c = taccaColors(dark, accent)
            val tag = "dark=$dark accent=$accent"
            check("$tag text/background", c.text, c.background, 4.5)
            check("$tag text/surface", c.text, c.surface, 4.5)
            check("$tag muted/background", c.textMuted, c.background, 4.5)
            check("$tag muted/surface", c.textMuted, c.surface, 4.5)
            check("$tag onAccent/accent", c.onAccent, c.accent, 4.5)
            check("$tag accentText/surface", c.accentText, c.surface, 4.5)
            check("$tag text/highlight", c.text, c.highlight, 4.5)
            check("$tag danger/surface", c.danger, c.surface, 4.5)
            for (t in Tint.entries) {
                check("$tag text/$t", c.text, c.tint(t), 4.5)
                check("$tag textOnTint/$t", c.textOnTint, c.tint(t), 4.5)
            }
        }
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/data/settings/SettingsRepositoryTest.kt`:

```kotlin
package it.matteocolombo.tacca.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun defaultsThenPersistsChanges() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") }
        val repo = DataStoreSettingsRepository(store)
        assertEquals(AppSettings(), repo.settings.first())
        repo.setAccent(AccentOption.PLUM)
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(AppSettings(AccentOption.PLUM, ThemeMode.DARK), repo.settings.first())
    }
}
```

- [ ] **Step 3: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*ThemeContrastTest*" --tests "*SettingsRepositoryTest*"'`
Expected: compilation FAILS.

- [ ] **Step 4: Implement settings repository**

`data/settings/SettingsRepository.kt`:

```kotlin
package it.matteocolombo.tacca.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setAccent(accent: AccentOption)
    suspend fun setThemeMode(mode: ThemeMode)
}

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            AppSettings(
                accent = p[ACCENT]?.let { v -> AccentOption.entries.firstOrNull { it.name == v } } ?: AccentOption.TERRACOTTA,
                themeMode = p[THEME_MODE]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
            )
        }

    override suspend fun setAccent(accent: AccentOption) {
        store.edit { it[ACCENT] = accent.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val ACCENT = stringPreferencesKey("accent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
```

In `AppContainer` add:

```kotlin
    val settings: SettingsRepository by lazy { DataStoreSettingsRepository(appContext.settingsDataStore) }
```

- [ ] **Step 5: Implement colors, type, theme**

`ui/theme/Colors.kt`:

```kotlin
package it.matteocolombo.tacca.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.Tint

/** Palette from the mockup (light) and its espresso-toned dark counterpart (spec section 6). */
@Immutable
data class TaccaColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val outline: Color,
    val dashed: Color,
    val tickInactive: Color,
    val text: Color,
    val textMuted: Color,
    val textOnTint: Color,
    val highlight: Color,
    /** Fills (FAB, primary button, active ticks). */
    val accent: Color,
    /** Accent used for text and numbers on surfaces. */
    val accentText: Color,
    val onAccent: Color,
    val danger: Color,
    private val tints: Map<Tint, Color>,
) {
    fun tint(t: Tint): Color = tints.getValue(t)
}

private val lightTints = mapOf(
    Tint.PEACH to Color(0xFFF7DCCB),
    Tint.SAND to Color(0xFFF2E4BC),
    Tint.SKY to Color(0xFFD7E3EE),
    Tint.SAGE to Color(0xFFDCE6D1),
    Tint.LILAC to Color(0xFFE6DDEE),
)

private val darkTints = mapOf(
    Tint.PEACH to Color(0xFF4A3226),
    Tint.SAND to Color(0xFF463B22),
    Tint.SKY to Color(0xFF2A3844),
    Tint.SAGE to Color(0xFF2F3A2A),
    Tint.LILAC to Color(0xFF3A3044),
)

fun taccaColors(dark: Boolean, accent: AccentOption): TaccaColors {
    val accentColor = Color(accent.argb)
    return if (!dark) {
        TaccaColors(
            isDark = false,
            background = Color(0xFFF6F0E8),
            surface = Color(0xFFFFFCF8),
            outline = Color(0xFFEADFD3),
            dashed = Color(0xFFD9C9B8),
            tickInactive = Color(0xFFE3D6C8),
            text = Color(0xFF2A1C13),
            textMuted = Color(0xFF6F5B4C),
            textOnTint = Color(0xFF5E4A3C),
            highlight = Color(0xFFF4DCCD),
            accent = accentColor,
            accentText = accentColor,
            onAccent = Color.White,
            danger = Color(0xFFB3261E),
            tints = lightTints,
        )
    } else {
        TaccaColors(
            isDark = true,
            background = Color(0xFF1C1410),
            surface = Color(0xFF261C16),
            outline = Color(0xFF3A2C23),
            dashed = Color(0xFF4A392D),
            tickInactive = Color(0xFF4A392D),
            text = Color(0xFFF3E9DF),
            textMuted = Color(0xFFB9A594),
            textOnTint = Color(0xFFD9C8B8),
            highlight = Color(0xFF4A2E22),
            accent = accentColor,
            accentText = lerp(accentColor, Color.White, 0.35f),
            onAccent = Color.White,
            danger = Color(0xFFFFB4AB),
            tints = darkTints,
        )
    }
}
```

If `ThemeContrastTest` reports a failing pair, adjust only the dark-theme value named in the message (lighten text-like colors / darken fills) until it passes; never change a light-theme mockup value — if a light pair fails, report it instead of changing it.

`ui/theme/Type.kt`:

```kotlin
package it.matteocolombo.tacca.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R

private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Bricolage = FontFamily(
    variable(R.font.bricolage_grotesque, 500),
    variable(R.font.bricolage_grotesque, 700),
)

val Figtree = FontFamily(
    variable(R.font.figtree, 400),
    variable(R.font.figtree, 500),
    variable(R.font.figtree, 600),
    variable(R.font.figtree, 700),
)

/** Text styles measured from the mockup. Colors are applied at the call site. */
object TaccaType {
    val brand = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val h1 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em)
    val h1Small = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em)
    val cardTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    val numberXL = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 72.sp, letterSpacing = (-0.03).em)
    val numberL = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 34.sp)
    val numberM = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 24.sp)
    val numberS = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
    val title = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
    val rowTitle = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp)
    val body = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
    val label = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val legend = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.07.em)
    val unit = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.06.em)
    val nav = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
    val button = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
}

internal val MaterialTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Bricolage),
        headlineLarge = base.headlineLarge.copy(fontFamily = Bricolage),
        headlineMedium = base.headlineMedium.copy(fontFamily = Bricolage),
        headlineSmall = base.headlineSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = Figtree),
        bodyMedium = base.bodyMedium.copy(fontFamily = Figtree),
        bodySmall = base.bodySmall.copy(fontFamily = Figtree),
        labelLarge = base.labelLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = Figtree),
        labelSmall = base.labelSmall.copy(fontFamily = Figtree),
    )
}
```

`ui/theme/Theme.kt`:

```kotlin
package it.matteocolombo.tacca.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode

val LocalTaccaColors = staticCompositionLocalOf { taccaColors(dark = false, accent = AccentOption.TERRACOTTA) }

object TaccaTheme {
    val colors: TaccaColors
        @Composable @ReadOnlyComposable get() = LocalTaccaColors.current
}

@Composable
fun TaccaTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(dark, settings.accent) { taccaColors(dark, settings.accent) }
    CompositionLocalProvider(LocalTaccaColors provides colors) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = MaterialTypography, content = content)
    }
}

private fun TaccaColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = highlight,
        onPrimaryContainer = text,
        secondary = accent,
        onSecondary = onAccent,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = background,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = background,
        inverseSurface = text,
        inverseOnSurface = surface,
        outline = outline,
        outlineVariant = outline,
        error = danger,
    )
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*ThemeContrastTest*" --tests "*SettingsRepositoryTest*"'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add settings store, Tacca theme and bundled fonts

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Icons

**Files:**
- Create: `app/src/main/res/drawable/ic_*.xml` (one per row of the table below)
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/icons/TaccaIcons.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/icons/TaccaIconsTest.kt`

**Interfaces:**
- Consumes: `MethodIcon` (Task 2).
- Produces: drawables `R.drawable.ic_brand, ic_tab_calibrations, ic_search, ic_settings, ic_bean, ic_drop, ic_grinder, ic_multi_turn, ic_method_moka, ic_method_espresso, ic_method_v60, ic_method_french_press, ic_method_aeropress, ic_method_cup, ic_add, ic_minus, ic_close, ic_back, ic_chevron_right, ic_edit, ic_more, ic_check`; `@DrawableRes fun MethodIcon.drawableRes(): Int`.

All icons are 24×24 stroke icons converted from the mockup SVGs (`docs/mockup/*.dc.html`), drawn in black and tinted in Compose. Template for every file (one `<path>` per path string in the table; same stroke width for all paths in a file):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#00000000"
        android:pathData="PATH"
        android:strokeWidth="WIDTH"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

| File | Stroke | Paths (each quoted string = one `<path>`) |
|---|---|---|
| `ic_brand` | 2.2 | `M3.5,12a8.5,8.5 0,1 0,17 0a8.5,8.5 0,1 0,-17 0` · `M12,3.5V7` |
| `ic_tab_calibrations` | 1.8 | `M3.5,12a8.5,8.5 0,1 0,17 0a8.5,8.5 0,1 0,-17 0` · `M12,3.5V6` · `M12,12l3.5,-3.5` |
| `ic_search` | 1.9 | `M4.5,11a6.5,6.5 0,1 0,13 0a6.5,6.5 0,1 0,-13 0` · `M20,20l-4.3,-4.3` |
| `ic_settings` | 1.9 | (Lucide "settings", ISC license — add an XML comment saying so) `M12.22,2h-0.44a2,2 0,0 0,-2 2v0.18a2,2 0,0 1,-1 1.73l-0.43,0.25a2,2 0,0 1,-2 0l-0.15,-0.08a2,2 0,0 0,-2.73 0.73l-0.22,0.38a2,2 0,0 0,0.73 2.73l0.15,0.1a2,2 0,0 1,1 1.72v0.51a2,2 0,0 1,-1 1.74l-0.15,0.09a2,2 0,0 0,-0.73 2.73l0.22,0.38a2,2 0,0 0,2.73 0.73l0.15,-0.08a2,2 0,0 1,2 0l0.43,0.25a2,2 0,0 1,1 1.73V20a2,2 0,0 0,2 2h0.44a2,2 0,0 0,2 -2v-0.18a2,2 0,0 1,1 -1.73l0.43,-0.25a2,2 0,0 1,2 0l0.15,0.08a2,2 0,0 0,2.73 -0.73l0.22,-0.39a2,2 0,0 0,-0.73 -2.73l-0.15,-0.08a2,2 0,0 1,-1 -1.74v-0.5a2,2 0,0 1,1 -1.74l0.15,-0.09a2,2 0,0 0,0.73 -2.73l-0.22,-0.38a2,2 0,0 0,-2.73 -0.73l-0.15,0.08a2,2 0,0 1,-2 0l-0.43,-0.25a2,2 0,0 1,-1 -1.73V4a2,2 0,0 0,-2 -2z` · `M9,12a3,3 0,1 0,6 0a3,3 0,1 0,-6 0` |
| `ic_bean` | 1.8 | ellipse inside `<group android:rotation="35" android:pivotX="12" android:pivotY="12">`: `M6,12a6,8.5 0,1 0,12 0a6,8.5 0,1 0,-12 0` · outside the group: `M8.6,17.4c2,-2.2 1.6,-4.6 3.4,-6s3,-3.4 3.4,-5.8` |
| `ic_drop` | 1.8 | `M12,3c3,4.2 6,7.4 6,11a6,6 0,0 1,-12 0c0,-3.6 3,-6.8 6,-11z` |
| `ic_grinder` | 1.8 | `M8,11h8v9a1,1 0,0 1,-1 1H9a1,1 0,0 1,-1 -1z` · `M7,11h10` · `M12,11V6h5` · `M17,6a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0` · `M8,15h8` |
| `ic_multi_turn` | 2.2 | `M20,12a8,8 0,1 1,-2.3 -5.7` · `M20,4v4h-4` |
| `ic_method_moka` | 1.7 | `M7,3h10l-2,8H9z` · `M9,13h6l2,8H7z` · `M8.5,12h7` · `M17,5c2.5,0 3,4 0.5,5` |
| `ic_method_espresso` | 1.7 | `M5,9h11v4a5,5 0,0 1,-5 5h-1a5,5 0,0 1,-5 -5z` · `M16,10h1.5a2.5,2.5 0,0 1,0 5H16` · `M3,21h16` · `M9,3v3M12.5,3v3` |
| `ic_method_v60` | 1.7 | `M3,6h18` · `M5,6l4.5,9h5L19,6` · `M10,15v3M14,15v3` · `M7,18h10` · `M8,21h8` |
| `ic_method_french_press` | 1.7 | `M6,8h10v12a1,1 0,0 1,-1 1H7a1,1 0,0 1,-1 -1z` · `M5,8h12` · `M11,3v9` · `M9,3h4` · `M6,12h10` · `M16,11h2a1,1 0,0 1,1 1v4a1,1 0,0 1,-1 1h-2` |
| `ic_method_aeropress` | 1.7 | `M8,3h8v15H8z` · `M6,3h12` · `M8,8h8` · `M6,21h12` · `M10,18v3M14,18v3` |
| `ic_method_cup` | 1.7 | `M4,8h13v7a4,4 0,0 1,-4 4H8a4,4 0,0 1,-4 -4z` · `M17,10h1.5a2.5,2.5 0,0 1,0 5H17` · `M8,2.5c-0.6,1 0.6,2 0,3` · `M12,2.5c-0.6,1 0.6,2 0,3` |
| `ic_add` | 2.4 | `M12,5v14M5,12h14` |
| `ic_minus` | 2.4 | `M6,12h12` |
| `ic_close` | 2.2 | `M6,6l12,12M18,6L6,18` |
| `ic_back` | 2.0 | `M15,5l-7,7 7,7` |
| `ic_chevron_right` | 2.0 | `M9,5l7,7 -7,7` |
| `ic_edit` | 2.0 | `M4,20h4L19,9l-4,-4L4,16z` · `M13.5,6.5l4,4` |
| `ic_more` | 3.0 | `M12,5h0.01M12,12h0.01M12,19h0.01` |
| `ic_check` | 2.6 | `M5,12.5l4.5,4.5L19,7.5` |

- [ ] **Step 1: Write the failing test**

`app/src/test/java/it/matteocolombo/tacca/ui/icons/TaccaIconsTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.icons

import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.MethodIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaccaIconsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun everyMethodIconHasADistinctDrawable() {
        val ids = MethodIcon.entries.map { it.drawableRes() }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }

    @Test fun uiIconsInflate() {
        listOf(
            R.drawable.ic_brand, R.drawable.ic_tab_calibrations, R.drawable.ic_search, R.drawable.ic_settings,
            R.drawable.ic_bean, R.drawable.ic_grinder, R.drawable.ic_multi_turn, R.drawable.ic_add, R.drawable.ic_minus,
            R.drawable.ic_close, R.drawable.ic_back, R.drawable.ic_chevron_right, R.drawable.ic_edit, R.drawable.ic_more,
            R.drawable.ic_check,
        ).forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*TaccaIconsTest*"'`
Expected: compilation FAILS (`Unresolved reference 'ic_brand'` / `drawableRes`).

- [ ] **Step 3: Create the 22 drawables from the table, then the mapping**

`ui/icons/TaccaIcons.kt`:

```kotlin
package it.matteocolombo.tacca.ui.icons

import androidx.annotation.DrawableRes
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.MethodIcon

@DrawableRes
fun MethodIcon.drawableRes(): Int = when (this) {
    MethodIcon.MOKA -> R.drawable.ic_method_moka
    MethodIcon.ESPRESSO -> R.drawable.ic_method_espresso
    MethodIcon.V60 -> R.drawable.ic_method_v60
    MethodIcon.FRENCH_PRESS -> R.drawable.ic_method_french_press
    MethodIcon.AEROPRESS -> R.drawable.ic_method_aeropress
    MethodIcon.DROP -> R.drawable.ic_drop
    MethodIcon.CUP -> R.drawable.ic_method_cup
}
```

- [ ] **Step 4: Run tests and resource compilation**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*TaccaIconsTest*" assembleDebug'`
Expected: PASS; no AAPT errors about `pathData`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add vector icons converted from the mockup

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: Strings in five languages and label helpers

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (full English set)
- Create: `app/src/main/res/values-it/strings.xml`, `values-es/strings.xml`, `values-fr/strings.xml`, `values-de/strings.xml`
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/format/Labels.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/format/LabelsTest.kt`

**Interfaces:**
- Consumes: domain types (Task 2), `BackupError` (Task 5).
- Produces: every string/plural resource below (later tasks use only these names), and in `it.matteocolombo.tacca.ui.format`:

```kotlin
fun settingText(res: Resources, setting: GrindSetting): String          // "2 turns + 12 steps" / "12 steps"
@Composable fun settingText(setting: GrindSetting): String
@StringRes fun BuiltInMethod.nameRes(): Int
fun methodName(res: Resources, method: Method): String
@Composable fun methodName(method: Method): String
fun grinderKindText(res: Resources, spec: GrinderSpec): String           // "Multi-turn · 40/turn"
@Composable fun grinderKindText(spec: GrinderSpec): String
@StringRes fun MethodIcon.labelRes(): Int
@StringRes fun Tint.labelRes(): Int
@StringRes fun AccentOption.labelRes(): Int
@StringRes fun ThemeMode.labelRes(): Int
@StringRes fun BackupError.messageRes(): Int
```

- [ ] **Step 1: Write the failing test**

`app/src/test/java/it/matteocolombo/tacca/ui/format/LabelsTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.format

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LabelsTest {
    private val res: Resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val moka = Method(1, null, BuiltInMethod.MOKA, MethodIcon.MOKA, Tint.PEACH, 0)

    @Test fun englishSettings() {
        assertEquals("2 turns + 12 steps", settingText(res, GrindSetting(2, 12)))
        assertEquals("1 turn + 1 step", settingText(res, GrindSetting(1, 1)))
        assertEquals("12 steps", settingText(res, GrindSetting(0, 12)))
        assertEquals("0 steps", settingText(res, GrindSetting.ZERO))
    }

    @Test @Config(qualifiers = "it")
    fun italianSettings() {
        assertEquals("2 giri + 12 step", settingText(res, GrindSetting(2, 12)))
        assertEquals("1 giro + 1 step", settingText(res, GrindSetting(1, 1)))
    }

    @Test @Config(qualifiers = "de")
    fun germanSettings() {
        assertEquals("2 Umdrehungen + 12 Klicks", settingText(res, GrindSetting(2, 12)))
    }

    @Test fun methodNames() {
        assertEquals("Moka", methodName(res, moka))
        assertEquals("Bialetti", methodName(res, moka.copy(customName = "Bialetti")))
        assertEquals("French press", methodName(res, moka.copy(builtIn = BuiltInMethod.FRENCH_PRESS)))
    }

    @Test @Config(qualifiers = "fr")
    fun frenchBuiltInName() {
        assertEquals("Cafetière à piston", methodName(res, moka.copy(builtIn = BuiltInMethod.FRENCH_PRESS)))
    }

    @Test fun grinderKinds() {
        assertEquals("Multi-turn · 40/turn", grinderKindText(res, GrinderSpec(true, 40)))
        assertEquals("Single turn · 40 steps", grinderKindText(res, GrinderSpec(false, 40)))
        assertEquals("Multi-turn · steps unknown", grinderKindText(res, GrinderSpec(true, null)))
        assertEquals("Single turn · steps unknown", grinderKindText(res, GrinderSpec(false, null)))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*LabelsTest*"'`
Expected: compilation FAILS.

- [ ] **Step 3: English strings**

Replace `app/src/main/res/values/strings.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name" translatable="false">Tacca</string>
    <string name="brand_name" translatable="false">tacca</string>

    <!-- Navigation -->
    <string name="nav_sections">Sections</string>
    <string name="nav_calibrations">Calibrations</string>
    <string name="nav_coffees">Coffees</string>
    <string name="nav_methods">Methods</string>
    <string name="nav_grinders">Grinders</string>

    <!-- Common actions -->
    <string name="action_save">Save</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_delete">Delete</string>
    <string name="action_close">Close</string>
    <string name="action_back">Back</string>
    <string name="action_edit">Edit</string>
    <string name="action_search">Search</string>
    <string name="action_clear_search">Clear search</string>
    <string name="action_settings">Settings</string>
    <string name="action_more">More options</string>
    <string name="action_new">New</string>
    <string name="action_show_all">Show all (%1$d)</string>

    <!-- Setting units -->
    <plurals name="turns_count">
        <item quantity="one">%d turn</item>
        <item quantity="other">%d turns</item>
    </plurals>
    <plurals name="steps_count">
        <item quantity="one">%d step</item>
        <item quantity="other">%d steps</item>
    </plurals>
    <string name="setting_turns_and_steps">%1$s + %2$s</string>
    <string name="unit_turns">Turns</string>
    <string name="unit_steps">Steps</string>
    <plurals name="calibrations_count">
        <item quantity="one">%d calibration</item>
        <item quantity="other">%d calibrations</item>
    </plurals>
    <string name="calibrations_none">No calibrations yet</string>
    <plurals name="coffees_count">
        <item quantity="one">%d coffee</item>
        <item quantity="other">%d coffees</item>
    </plurals>

    <!-- Built-in methods and icons -->
    <string name="method_moka">Moka</string>
    <string name="method_espresso">Espresso</string>
    <string name="method_v60">V60</string>
    <string name="method_french_press">French press</string>
    <string name="method_aeropress">AeroPress</string>
    <string name="icon_drop">Drop</string>
    <string name="icon_cup">Cup</string>

    <!-- Tints -->
    <string name="tint_peach">Peach</string>
    <string name="tint_sand">Sand</string>
    <string name="tint_sky">Sky</string>
    <string name="tint_sage">Sage</string>
    <string name="tint_lilac">Lilac</string>

    <!-- Home -->
    <string name="home_title">Calibrations</string>
    <string name="home_filter_all">All</string>
    <string name="home_search_hint">Search coffee, method, grinder</string>
    <string name="home_new_calibration">New calibration</string>
    <string name="home_remove_coffee_filter">Remove coffee filter</string>
    <string name="home_empty_title">No calibrations yet</string>
    <string name="home_empty_body">Save the setting that works for a coffee, a brew method and a grinder.</string>
    <string name="home_empty_setup">You\'ll add your first coffee and grinder along the way.</string>
    <string name="home_no_results">No calibrations match</string>
    <string name="home_clear_filters">Clear filters</string>

    <!-- Detail -->
    <string name="detail_step_zero">Close to zero</string>
    <plurals name="detail_step_turns">
        <item quantity="one">Turn %d full turn</item>
        <item quantity="other">Turn %d full turns</item>
    </plurals>
    <plurals name="detail_step_steps">
        <item quantity="one">Add %d step</item>
        <item quantity="other">Add %d steps</item>
    </plurals>
    <plurals name="detail_turns_plus">
        <item quantity="one">%d turn +</item>
        <item quantity="other">%d turns +</item>
    </plurals>
    <string name="detail_more_rings" translatable="false">+%1$d</string>
    <string name="detail_notes">Notes</string>
    <string name="detail_same_coffee">Same coffee</string>
    <string name="detail_not_found">This calibration no longer exists.</string>
    <string name="detail_dial_description">Dial: %1$s</string>

    <!-- Calibration editor -->
    <string name="editor_title_new">New calibration</string>
    <string name="editor_title_edit">Edit calibration</string>
    <string name="editor_section_coffee">Coffee</string>
    <string name="editor_section_method">Method</string>
    <string name="editor_section_grinder">Grinder</string>
    <string name="editor_section_adjustment">Adjustment</string>
    <string name="editor_section_notes">Notes</string>
    <string name="editor_notes_hint">Dose, time, yield…</string>
    <string name="editor_turns">Turns</string>
    <string name="editor_optional">optional</string>
    <string name="editor_single_turn_info">This grinder adjusts within a single turn: steps are enough.</string>
    <string name="editor_steps">Steps</string>
    <string name="editor_steps_range">%1$d–%2$d</string>
    <string name="editor_steps_range_after_turns">%1$d–%2$d after turns</string>
    <string name="editor_steps_unbounded">no limit</string>
    <string name="editor_fewer_turns">Fewer turns</string>
    <string name="editor_more_turns">More turns</string>
    <string name="editor_fewer_steps">Fewer steps</string>
    <string name="editor_more_steps">More steps</string>
    <string name="editor_save_summary">Save · %1$s</string>
    <string name="editor_replaces">Replaces the current setting (%1$s)</string>
    <string name="editor_delete">Delete calibration</string>
    <string name="editor_delete_confirm">Delete this calibration?</string>
    <string name="editor_no_coffee">Add a coffee to continue</string>
    <string name="editor_no_grinder">Add a grinder to continue</string>

    <!-- Grinder kinds (editor subtitles) -->
    <string name="grinder_kind_multi">Multi-turn · %1$d/turn</string>
    <string name="grinder_kind_multi_unknown">Multi-turn · steps unknown</string>
    <string name="grinder_kind_single">Single turn · %1$s</string>
    <string name="grinder_kind_single_unknown">Single turn · steps unknown</string>

    <!-- Coffees -->
    <string name="coffees_title">Coffees</string>
    <string name="coffees_add">Add coffee</string>
    <string name="coffees_search_label">Search a coffee</string>
    <string name="coffees_search_hint">Search by name…</string>
    <string name="coffees_empty">No coffees yet. Add the first one.</string>
    <string name="coffees_no_results">No coffees match</string>
    <string name="coffee_subtitle">%1$s · %2$s</string>
    <string name="coffee_sheet_title_new">New coffee</string>
    <string name="coffee_sheet_title_edit">Edit coffee</string>
    <string name="coffee_name_label">Name</string>
    <string name="coffee_name_hint">e.g. Ethiopia Yirgacheffe — washed</string>
    <string name="coffee_duplicate_hint">A coffee with this name already exists</string>
    <string name="coffee_delete">Delete coffee</string>

    <!-- Methods -->
    <string name="methods_title">Methods</string>
    <string name="methods_subtitle">How you brew your coffee</string>
    <string name="methods_add">Add method</string>
    <string name="method_sheet_title_new">New method</string>
    <string name="method_sheet_title_edit">Edit method</string>
    <string name="method_name_label">Name</string>
    <string name="method_name_hint">e.g. Chemex</string>
    <string name="method_icon_label">Icon</string>
    <string name="method_tint_label">Color</string>
    <string name="method_duplicate_hint">A method with this name already exists</string>
    <string name="method_delete">Delete method</string>

    <!-- Grinders -->
    <string name="grinders_title">Grinders</string>
    <string name="grinders_subtitle">Your coffee grinders</string>
    <string name="grinders_add">Add grinder</string>
    <string name="grinder_chip_multi">Multi-turn</string>
    <string name="grinder_chip_single">Single turn</string>
    <plurals name="grinder_chip_per_turn">
        <item quantity="one">%d step per turn</item>
        <item quantity="other">%d steps per turn</item>
    </plurals>
    <string name="grinder_chip_unknown">Steps unknown</string>
    <string name="grinder_editor_title_new">New grinder</string>
    <string name="grinder_editor_title_edit">Edit grinder</string>
    <string name="grinder_name_label">Name</string>
    <string name="grinder_name_hint">e.g. Grandma\'s grinder</string>
    <string name="grinder_duplicate_hint">A grinder with this name already exists</string>
    <string name="grinder_multi_title">Adjusts over several turns</string>
    <string name="grinder_multi_body">Turn it on if the dial makes more than one full turn from fine to coarse.</string>
    <string name="grinder_steps_per_turn">Steps per turn</string>
    <string name="grinder_steps_total">Total steps</string>
    <string name="grinder_steps_optional">Optional · helps check values</string>
    <string name="grinder_steps_not_set">Not set</string>
    <string name="grinder_steps_unknown_action">I don\'t know</string>
    <string name="grinder_preview_label">Calibrations will read like this</string>
    <plurals name="grinder_out_of_range">
        <item quantity="one">%d calibration is outside the new range</item>
        <item quantity="other">%d calibrations are outside the new range</item>
    </plurals>
    <string name="grinder_delete">Delete grinder</string>

    <!-- Confirmations -->
    <string name="confirm_delete_title">Delete “%1$s”?</string>
    <plurals name="confirm_delete_cascade_body">
        <item quantity="one">%d calibration will be deleted too.</item>
        <item quantity="other">%d calibrations will be deleted too.</item>
    </plurals>
    <string name="confirm_delete_no_cascade">Nothing else will be deleted.</string>

    <!-- Settings -->
    <string name="settings_title">Settings</string>
    <string name="settings_accent">Accent color</string>
    <string name="accent_terracotta">Terracotta</string>
    <string name="accent_pine">Pine</string>
    <string name="accent_indigo">Indigo</string>
    <string name="accent_plum">Plum</string>
    <string name="settings_theme">Theme</string>
    <string name="theme_system">System</string>
    <string name="theme_light">Light</string>
    <string name="theme_dark">Dark</string>
    <string name="settings_language">Language</string>
    <string name="language_system">System default</string>
    <string name="language_en" translatable="false">English</string>
    <string name="language_it" translatable="false">Italiano</string>
    <string name="language_es" translatable="false">Español</string>
    <string name="language_fr" translatable="false">Français</string>
    <string name="language_de" translatable="false">Deutsch</string>
    <string name="settings_data">Data</string>
    <string name="settings_export">Export data</string>
    <string name="settings_export_body">Save all coffees, methods, grinders and calibrations to a file.</string>
    <string name="settings_import">Import data</string>
    <string name="settings_import_body">Replace all data with a backup file.</string>
    <string name="import_confirm_title">Replace all data?</string>
    <string name="import_confirm_body">Everything currently in Tacca will be replaced by the contents of the file.</string>
    <string name="import_confirm_action">Replace</string>
    <string name="export_done">Data exported</string>
    <string name="import_done">Data imported</string>
    <string name="backup_error_io">Couldn\'t read or write the file</string>
    <string name="backup_error_not_a_backup">This file is not a Tacca backup</string>
    <string name="backup_error_newer">This backup comes from a newer version of Tacca. Update the app and try again.</string>
    <string name="backup_error_invalid">The backup file is damaged or incomplete</string>
    <string name="backup_file_name" translatable="false">tacca-backup-%1$s.json</string>
    <string name="settings_about">About</string>
    <string name="settings_version">Version %1$s</string>
</resources>
```

- [ ] **Step 4: Italian strings**

`app/src/main/res/values-it/strings.xml` — same keys (omit `translatable="false"` ones), values:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="nav_sections">Sezioni</string>
    <string name="nav_calibrations">Calibrazioni</string>
    <string name="nav_coffees">Caffè</string>
    <string name="nav_methods">Metodi</string>
    <string name="nav_grinders">Macine</string>

    <string name="action_save">Salva</string>
    <string name="action_cancel">Annulla</string>
    <string name="action_delete">Elimina</string>
    <string name="action_close">Chiudi</string>
    <string name="action_back">Indietro</string>
    <string name="action_edit">Modifica</string>
    <string name="action_search">Cerca</string>
    <string name="action_clear_search">Cancella ricerca</string>
    <string name="action_settings">Impostazioni</string>
    <string name="action_more">Altre opzioni</string>
    <string name="action_new">Nuovo</string>
    <string name="action_show_all">Mostra tutti (%1$d)</string>

    <plurals name="turns_count">
        <item quantity="one">%d giro</item>
        <item quantity="many">%d giri</item>
        <item quantity="other">%d giri</item>
    </plurals>
    <plurals name="steps_count">
        <item quantity="one">%d step</item>
        <item quantity="many">%d step</item>
        <item quantity="other">%d step</item>
    </plurals>
    <string name="setting_turns_and_steps">%1$s + %2$s</string>
    <string name="unit_turns">Giri</string>
    <string name="unit_steps">Step</string>
    <plurals name="calibrations_count">
        <item quantity="one">%d calibrazione</item>
        <item quantity="many">%d calibrazioni</item>
        <item quantity="other">%d calibrazioni</item>
    </plurals>
    <string name="calibrations_none">Ancora nessuna calibrazione</string>
    <plurals name="coffees_count">
        <item quantity="one">%d caffè in dispensa</item>
        <item quantity="many">%d caffè in dispensa</item>
        <item quantity="other">%d caffè in dispensa</item>
    </plurals>

    <string name="method_moka">Moka</string>
    <string name="method_espresso">Espresso</string>
    <string name="method_v60">V60</string>
    <string name="method_french_press">French press</string>
    <string name="method_aeropress">AeroPress</string>
    <string name="icon_drop">Goccia</string>
    <string name="icon_cup">Tazza</string>

    <string name="tint_peach">Pesca</string>
    <string name="tint_sand">Sabbia</string>
    <string name="tint_sky">Cielo</string>
    <string name="tint_sage">Salvia</string>
    <string name="tint_lilac">Lilla</string>

    <string name="home_title">Calibrazioni</string>
    <string name="home_filter_all">Tutti</string>
    <string name="home_search_hint">Cerca caffè, metodo, macina</string>
    <string name="home_new_calibration">Nuova calibrazione</string>
    <string name="home_remove_coffee_filter">Rimuovi filtro caffè</string>
    <string name="home_empty_title">Ancora nessuna calibrazione</string>
    <string name="home_empty_body">Salva la regolazione giusta per un caffè, un metodo e una macina.</string>
    <string name="home_empty_setup">Aggiungerai il primo caffè e la prima macina strada facendo.</string>
    <string name="home_no_results">Nessuna calibrazione corrisponde</string>
    <string name="home_clear_filters">Azzera filtri</string>

    <string name="detail_step_zero">Chiudi fino a zero</string>
    <plurals name="detail_step_turns">
        <item quantity="one">Ruota %d giro completo</item>
        <item quantity="many">Ruota %d giri completi</item>
        <item quantity="other">Ruota %d giri completi</item>
    </plurals>
    <plurals name="detail_step_steps">
        <item quantity="one">Aggiungi %d step</item>
        <item quantity="many">Aggiungi %d step</item>
        <item quantity="other">Aggiungi %d step</item>
    </plurals>
    <plurals name="detail_turns_plus">
        <item quantity="one">%d giro +</item>
        <item quantity="many">%d giri +</item>
        <item quantity="other">%d giri +</item>
    </plurals>
    <string name="detail_notes">Note</string>
    <string name="detail_same_coffee">Stesso caffè</string>
    <string name="detail_not_found">Questa calibrazione non esiste più.</string>
    <string name="detail_dial_description">Quadrante: %1$s</string>

    <string name="editor_title_new">Nuova calibrazione</string>
    <string name="editor_title_edit">Modifica calibrazione</string>
    <string name="editor_section_coffee">Caffè</string>
    <string name="editor_section_method">Metodo</string>
    <string name="editor_section_grinder">Macina</string>
    <string name="editor_section_adjustment">Regolazione</string>
    <string name="editor_section_notes">Note</string>
    <string name="editor_notes_hint">Dose, tempo, resa…</string>
    <string name="editor_turns">Giri</string>
    <string name="editor_optional">opzionale</string>
    <string name="editor_single_turn_info">Questa macina si regola entro un solo giro: bastano gli step.</string>
    <string name="editor_steps">Step</string>
    <string name="editor_steps_range">%1$d–%2$d</string>
    <string name="editor_steps_range_after_turns">%1$d–%2$d dopo i giri</string>
    <string name="editor_steps_unbounded">senza limite</string>
    <string name="editor_fewer_turns">Meno giri</string>
    <string name="editor_more_turns">Più giri</string>
    <string name="editor_fewer_steps">Meno step</string>
    <string name="editor_more_steps">Più step</string>
    <string name="editor_save_summary">Salva · %1$s</string>
    <string name="editor_replaces">Sostituisce la regolazione attuale (%1$s)</string>
    <string name="editor_delete">Elimina calibrazione</string>
    <string name="editor_delete_confirm">Eliminare questa calibrazione?</string>
    <string name="editor_no_coffee">Aggiungi un caffè per continuare</string>
    <string name="editor_no_grinder">Aggiungi una macina per continuare</string>

    <string name="grinder_kind_multi">Multi-giro · %1$d/giro</string>
    <string name="grinder_kind_multi_unknown">Multi-giro · step sconosciuti</string>
    <string name="grinder_kind_single">Giro singolo · %1$s</string>
    <string name="grinder_kind_single_unknown">Giro singolo · step sconosciuti</string>

    <string name="coffees_title">Caffè</string>
    <string name="coffees_add">Aggiungi caffè</string>
    <string name="coffees_search_label">Cerca un caffè</string>
    <string name="coffees_search_hint">Cerca per nome, origine…</string>
    <string name="coffees_empty">Ancora nessun caffè. Aggiungi il primo.</string>
    <string name="coffees_no_results">Nessun caffè corrisponde</string>
    <string name="coffee_subtitle">%1$s · %2$s</string>
    <string name="coffee_sheet_title_new">Nuovo caffè</string>
    <string name="coffee_sheet_title_edit">Modifica caffè</string>
    <string name="coffee_name_label">Nome</string>
    <string name="coffee_name_hint">Es. Etiopia Yirgacheffe — lavato</string>
    <string name="coffee_duplicate_hint">Esiste già un caffè con questo nome</string>
    <string name="coffee_delete">Elimina caffè</string>

    <string name="methods_title">Metodi</string>
    <string name="methods_subtitle">Come prepari il tuo caffè</string>
    <string name="methods_add">Aggiungi metodo</string>
    <string name="method_sheet_title_new">Nuovo metodo</string>
    <string name="method_sheet_title_edit">Modifica metodo</string>
    <string name="method_name_label">Nome</string>
    <string name="method_name_hint">Es. Chemex</string>
    <string name="method_icon_label">Icona</string>
    <string name="method_tint_label">Colore</string>
    <string name="method_duplicate_hint">Esiste già un metodo con questo nome</string>
    <string name="method_delete">Elimina metodo</string>

    <string name="grinders_title">Macine</string>
    <string name="grinders_subtitle">I tuoi macinacaffè</string>
    <string name="grinders_add">Aggiungi macina</string>
    <string name="grinder_chip_multi">Multi-giro</string>
    <string name="grinder_chip_single">Giro singolo</string>
    <plurals name="grinder_chip_per_turn">
        <item quantity="one">%d step per giro</item>
        <item quantity="many">%d step per giro</item>
        <item quantity="other">%d step per giro</item>
    </plurals>
    <string name="grinder_chip_unknown">Step sconosciuti</string>
    <string name="grinder_editor_title_new">Nuova macina</string>
    <string name="grinder_editor_title_edit">Modifica macina</string>
    <string name="grinder_name_label">Nome</string>
    <string name="grinder_name_hint">Es. il macinino della nonna</string>
    <string name="grinder_duplicate_hint">Esiste già una macina con questo nome</string>
    <string name="grinder_multi_title">Si regola con più giri</string>
    <string name="grinder_multi_body">Attivalo se la ghiera fa più di un giro completo tra fine e grosso.</string>
    <string name="grinder_steps_per_turn">Step per giro</string>
    <string name="grinder_steps_total">Step totali</string>
    <string name="grinder_steps_optional">Opzionale · aiuta a controllare i valori</string>
    <string name="grinder_steps_not_set">Non impostato</string>
    <string name="grinder_steps_unknown_action">Non lo so</string>
    <string name="grinder_preview_label">Le calibrazioni si leggeranno così</string>
    <plurals name="grinder_out_of_range">
        <item quantity="one">%d calibrazione è fuori dal nuovo intervallo</item>
        <item quantity="many">%d calibrazioni sono fuori dal nuovo intervallo</item>
        <item quantity="other">%d calibrazioni sono fuori dal nuovo intervallo</item>
    </plurals>
    <string name="grinder_delete">Elimina macina</string>

    <string name="confirm_delete_title">Eliminare “%1$s”?</string>
    <plurals name="confirm_delete_cascade_body">
        <item quantity="one">Verrà eliminata anche %d calibrazione.</item>
        <item quantity="many">Verranno eliminate anche %d calibrazioni.</item>
        <item quantity="other">Verranno eliminate anche %d calibrazioni.</item>
    </plurals>
    <string name="confirm_delete_no_cascade">Nient\'altro verrà eliminato.</string>

    <string name="settings_title">Impostazioni</string>
    <string name="settings_accent">Colore principale</string>
    <string name="accent_terracotta">Terracotta</string>
    <string name="accent_pine">Pino</string>
    <string name="accent_indigo">Indaco</string>
    <string name="accent_plum">Prugna</string>
    <string name="settings_theme">Tema</string>
    <string name="theme_system">Sistema</string>
    <string name="theme_light">Chiaro</string>
    <string name="theme_dark">Scuro</string>
    <string name="settings_language">Lingua</string>
    <string name="language_system">Predefinita di sistema</string>
    <string name="settings_data">Dati</string>
    <string name="settings_export">Esporta dati</string>
    <string name="settings_export_body">Salva caffè, metodi, macine e calibrazioni in un file.</string>
    <string name="settings_import">Importa dati</string>
    <string name="settings_import_body">Sostituisci tutti i dati con un file di backup.</string>
    <string name="import_confirm_title">Sostituire tutti i dati?</string>
    <string name="import_confirm_body">Tutto ciò che c\'è ora in Tacca sarà sostituito dal contenuto del file.</string>
    <string name="import_confirm_action">Sostituisci</string>
    <string name="export_done">Dati esportati</string>
    <string name="import_done">Dati importati</string>
    <string name="backup_error_io">Impossibile leggere o scrivere il file</string>
    <string name="backup_error_not_a_backup">Questo file non è un backup di Tacca</string>
    <string name="backup_error_newer">Questo backup viene da una versione più recente di Tacca. Aggiorna l\'app e riprova.</string>
    <string name="backup_error_invalid">Il file di backup è danneggiato o incompleto</string>
    <string name="settings_about">Informazioni</string>
    <string name="settings_version">Versione %1$s</string>
</resources>
```

- [ ] **Step 5: Spanish, French, German strings**

Create `values-es`, `values-fr`, `values-de` `strings.xml` with every translatable key of `values/strings.xml`, translated from the English text. Use this glossary consistently (it is part of the spec for these languages):

| Concept | es | fr | de |
|---|---|---|---|
| step (unit) | clic / clics | cran / crans | Klick / Klicks |
| turn (unit) | vuelta / vueltas | tour / tours | Umdrehung / Umdrehungen |
| calibration | calibración | réglage | Einstellung |
| grinder | molinillo | moulin | Mühle |
| coffee | café | café | Kaffee |
| method (brew) | método | méthode | Zubereitung |
| multi-turn | Varias vueltas | Multi-tours | Mehrere Umdrehungen |
| single turn | Una vuelta | Un seul tour | Eine Umdrehung |
| French press (built-in) | Prensa francesa | Cafetière à piston | French Press |
| Moka / Espresso / V60 / AeroPress | unchanged | unchanged (Espresso → Expresso) | unchanged |
| accent colors | Terracota, Pino, Índigo, Ciruela | Terre cuite, Pin, Indigo, Prune | Terrakotta, Kiefer, Indigo, Pflaume |

Rules: `setting_turns_and_steps` stays `%1$s + %2$s`; plurals: `es` and `fr` need `one`, `many`, `other` (use the `other` text for `many`); `de` needs `one`, `other`. Keep placeholders (`%1$s`, `%1$d`, `%d`) exactly. Escape apostrophes (`\'`), especially in French (`l\'app`). German `settingText(2,12)` must produce `2 Umdrehungen + 12 Klicks` (asserted in the test).

- [ ] **Step 6: Label helpers**

`ui/format/Labels.kt`:

```kotlin
package it.matteocolombo.tacca.ui.format

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.domain.Tint

fun settingText(res: Resources, setting: GrindSetting): String {
    val steps = res.getQuantityString(R.plurals.steps_count, setting.steps, setting.steps)
    if (setting.turns <= 0) return steps
    val turns = res.getQuantityString(R.plurals.turns_count, setting.turns, setting.turns)
    return res.getString(R.string.setting_turns_and_steps, turns, steps)
}

@Composable @ReadOnlyComposable
fun settingText(setting: GrindSetting): String = settingText(LocalResources.current, setting)

@StringRes
fun BuiltInMethod.nameRes(): Int = when (this) {
    BuiltInMethod.MOKA -> R.string.method_moka
    BuiltInMethod.ESPRESSO -> R.string.method_espresso
    BuiltInMethod.V60 -> R.string.method_v60
    BuiltInMethod.FRENCH_PRESS -> R.string.method_french_press
    BuiltInMethod.AEROPRESS -> R.string.method_aeropress
}

fun methodName(res: Resources, method: Method): String =
    method.customName ?: method.builtIn?.let { res.getString(it.nameRes()) }.orEmpty()

@Composable @ReadOnlyComposable
fun methodName(method: Method): String = methodName(LocalResources.current, method)

fun grinderKindText(res: Resources, spec: GrinderSpec): String {
    val n = spec.stepsPerTurn
    return when {
        spec.multiTurn && n != null -> res.getString(R.string.grinder_kind_multi, n)
        spec.multiTurn -> res.getString(R.string.grinder_kind_multi_unknown)
        n != null -> res.getString(R.string.grinder_kind_single, res.getQuantityString(R.plurals.steps_count, n, n))
        else -> res.getString(R.string.grinder_kind_single_unknown)
    }
}

@Composable @ReadOnlyComposable
fun grinderKindText(spec: GrinderSpec): String = grinderKindText(LocalResources.current, spec)

@StringRes
fun MethodIcon.labelRes(): Int = when (this) {
    MethodIcon.MOKA -> R.string.method_moka
    MethodIcon.ESPRESSO -> R.string.method_espresso
    MethodIcon.V60 -> R.string.method_v60
    MethodIcon.FRENCH_PRESS -> R.string.method_french_press
    MethodIcon.AEROPRESS -> R.string.method_aeropress
    MethodIcon.DROP -> R.string.icon_drop
    MethodIcon.CUP -> R.string.icon_cup
}

@StringRes
fun Tint.labelRes(): Int = when (this) {
    Tint.PEACH -> R.string.tint_peach
    Tint.SAND -> R.string.tint_sand
    Tint.SKY -> R.string.tint_sky
    Tint.SAGE -> R.string.tint_sage
    Tint.LILAC -> R.string.tint_lilac
}

@StringRes
fun AccentOption.labelRes(): Int = when (this) {
    AccentOption.TERRACOTTA -> R.string.accent_terracotta
    AccentOption.PINE -> R.string.accent_pine
    AccentOption.INDIGO -> R.string.accent_indigo
    AccentOption.PLUM -> R.string.accent_plum
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
fun BackupError.messageRes(): Int = when (this) {
    BackupError.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
    BackupError.NEWER_VERSION -> R.string.backup_error_newer
    BackupError.INVALID_DATA -> R.string.backup_error_invalid
    BackupError.IO -> R.string.backup_error_io
}
```

If `LocalResources` is unresolved in this Compose version, use `LocalContext.current.resources` and drop `@ReadOnlyComposable`.

- [ ] **Step 7: Run tests and lint**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*LabelsTest*" lintDebug'`
Expected: tests PASS; lint reports no `MissingTranslation`, `ExtraTranslation`, `MissingQuantity`, `StringFormatInvalid` errors. (Other lint warnings about unused resources are expected at this stage.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Add UI strings in five languages and label helpers

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: Routes and shared UI components

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/navigation/Routes.kt`, `TopLevelDestination.kt`
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/components/Buttons.kt`, `Cards.kt`, `Chips.kt`, `Stepper.kt`, `Dial.kt`, `Dialogs.kt`, `EmptyState.kt`, `Fields.kt`, `Headers.kt`, `BottomBar.kt`, `Layout.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/components/ComponentsTest.kt`

**Interfaces:**
- Consumes: theme (Task 6), icons (Task 7), strings/labels (Task 8), `DialGeometry` (Task 2).
- Produces:

```kotlin
// ui/navigation/Routes.kt
const val NEW_ID = -1L
const val NEW_GRINDER_RESULT_KEY = "newGrinderId"
@Serializable data object HomeRoute
@Serializable data object CoffeesRoute
@Serializable data object MethodsRoute
@Serializable data object GrindersRoute
@Serializable data object SettingsRoute
@Serializable data class DetailRoute(val calibrationId: Long)
@Serializable data class EditorRoute(val calibrationId: Long = NEW_ID, val coffeeId: Long = NEW_ID, val methodId: Long = NEW_ID)
@Serializable data class GrinderEditorRoute(val grinderId: Long = NEW_ID)
// SavedStateHandle keys = property names: "calibrationId", "coffeeId", "methodId", "grinderId"

// ui/navigation/TopLevelDestination.kt
enum class TopLevelDestination(@StringRes val label: Int, @DrawableRes val icon: Int, val route: Any) { CALIBRATIONS, COFFEES, METHODS, GRINDERS }

// ui/components
@Composable fun SquareIconButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false)
@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
@Composable fun DangerTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun DashedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, radius: Dp = 24.dp, vertical: Boolean = false)
@Composable fun SurfaceCard(modifier: Modifier = Modifier, radius: Dp = 22.dp, content: @Composable ColumnScope.() -> Unit)
@Composable fun GroupedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)
@Composable fun TintedIconBox(@DrawableRes icon: Int, background: Color, modifier: Modifier = Modifier, boxSize: Dp = 44.dp, iconSize: Dp = 24.dp, radius: Dp = 14.dp)
@Composable fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes leadingIcon: Int? = null, @DrawableRes trailingIcon: Int? = null)
@Composable fun InfoChip(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, background: Color = TaccaTheme.colors.background)
@Composable fun Stepper(value: String, onDecrement: () -> Unit, onIncrement: () -> Unit, decrementLabel: String, incrementLabel: String, modifier: Modifier = Modifier, canDecrement: Boolean = true, canIncrement: Boolean = true, valueStyle: TextStyle = TaccaType.numberL, valueColor: Color = TaccaTheme.colors.text)
@Composable fun Dial(setting: GrindSetting, stepsPerTurn: Int?, modifier: Modifier = Modifier, size: Dp = 260.dp)
@Composable fun ConfirmDialog(title: String, text: String?, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = true)
@Composable fun cascadeDeleteText(count: Int): String
@Composable fun TaccaBottomSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit)
@Composable fun EmptyState(@DrawableRes icon: Int, title: String, body: String?, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)
@Composable fun TaccaTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1, imeAction: ImeAction = ImeAction.Done, onImeAction: () -> Unit = {}, supportingText: String? = null, fieldModifier: Modifier = Modifier)
@Composable fun topLevelContentPadding(): PaddingValues   // 20dp sides, status bar + 20dp top, 24dp bottom
@Composable fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, label: String, modifier: Modifier = Modifier)
@Composable fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, titleStyle: TextStyle = TaccaType.h1, top: @Composable (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})
@Composable fun Legend(text: String, modifier: Modifier = Modifier)
@Composable fun BrandMark(modifier: Modifier = Modifier)
@Composable fun TaccaBottomBar(selected: TopLevelDestination?, onSelect: (TopLevelDestination) -> Unit, modifier: Modifier = Modifier)
```

Test tags used by later tests: `Stepper` buttons are found by their content descriptions; `TaccaTextField` exposes the label as the node's content description so tests can do `onNodeWithContentDescription("Name")`.

- [ ] **Step 1: Write the failing component tests**

`app/src/test/java/it/matteocolombo/tacca/ui/components/ComponentsTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.ui.navigation.TopLevelDestination
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stepperIncrementsAndDisablesAtBound() {
        compose.setContent {
            TaccaTheme {
                var v by remember { mutableIntStateOf(0) }
                Stepper(
                    value = v.toString(), onDecrement = { v-- }, onIncrement = { v++ },
                    decrementLabel = "Fewer", incrementLabel = "More", canDecrement = v > 0,
                )
            }
        }
        compose.onNodeWithContentDescription("Fewer").assertIsNotEnabled()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("1").assertIsDisplayed()
    }

    @Test fun dialDescribesTheSetting() {
        compose.setContent { TaccaTheme { Dial(GrindSetting(2, 12), stepsPerTurn = 40) } }
        compose.onNodeWithContentDescription("Dial: 2 turns + 12 steps").assertIsDisplayed()
    }

    @Test fun bottomBarSelectsAndCallsBack() {
        var picked: TopLevelDestination? = null
        compose.setContent {
            TaccaTheme {
                var sel by remember { mutableStateOf(TopLevelDestination.CALIBRATIONS) }
                TaccaBottomBar(selected = sel, onSelect = { sel = it; picked = it })
            }
        }
        compose.onNodeWithText("Calibrations").assertIsSelected()
        compose.onNodeWithText("Grinders").performClick()
        compose.onNodeWithText("Grinders").assertIsSelected()
        assertEquals(TopLevelDestination.GRINDERS, picked)
    }

    @Test fun confirmDialogInvokesConfirm() {
        var confirmed = false
        compose.setContent {
            TaccaTheme { ConfirmDialog("Delete “V60”?", "3 calibrations will be deleted too.", "Delete", { confirmed = true }, {}) }
        }
        compose.onNodeWithText("Delete").performClick()
        assertEquals(true, confirmed)
    }
}
```

The bottom bar's `selectable` item merges icon+label semantics, so `onNodeWithText("Grinders")` targets the item. If `assertIsSelected` cannot find merged semantics, use `onNode(hasText("Grinders") and isSelectable())`.

- [ ] **Step 2: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*ComponentsTest*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Routes and top-level destinations**

`ui/navigation/Routes.kt`:

```kotlin
package it.matteocolombo.tacca.ui.navigation

import kotlinx.serialization.Serializable

/** Sentinel for "no id" in route arguments (nullable Long arguments are avoided on purpose). */
const val NEW_ID = -1L

/** SavedStateHandle key through which the grinder editor returns a newly created grinder id. */
const val NEW_GRINDER_RESULT_KEY = "newGrinderId"

@Serializable data object HomeRoute
@Serializable data object CoffeesRoute
@Serializable data object MethodsRoute
@Serializable data object GrindersRoute
@Serializable data object SettingsRoute
@Serializable data class DetailRoute(val calibrationId: Long)
@Serializable data class EditorRoute(val calibrationId: Long = NEW_ID, val coffeeId: Long = NEW_ID, val methodId: Long = NEW_ID)
@Serializable data class GrinderEditorRoute(val grinderId: Long = NEW_ID)

/** Reads a Long route argument, mapping [NEW_ID] and absence to null. */
fun androidx.lifecycle.SavedStateHandle.idArg(key: String): Long? = get<Long>(key)?.takeIf { it != NEW_ID }
```

`ui/navigation/TopLevelDestination.kt`:

```kotlin
package it.matteocolombo.tacca.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.matteocolombo.tacca.R

enum class TopLevelDestination(@StringRes val label: Int, @DrawableRes val icon: Int, val route: Any) {
    CALIBRATIONS(R.string.nav_calibrations, R.drawable.ic_tab_calibrations, HomeRoute),
    COFFEES(R.string.nav_coffees, R.drawable.ic_bean, CoffeesRoute),
    METHODS(R.string.nav_methods, R.drawable.ic_drop, MethodsRoute),
    GRINDERS(R.string.nav_grinders, R.drawable.ic_grinder, GrindersRoute),
}
```

- [ ] **Step 4: Components**

`ui/components/Buttons.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/** 44dp rounded-square icon button of the mockup, inside a 48dp touch target. */
@Composable
fun SquareIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .then(if (filled) Modifier.background(c.accent) else Modifier.background(c.surface).border(1.dp, c.outline, shape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = if (filled) c.onAccent else c.text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = TaccaTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.onAccent,
            disabledContainerColor = c.outline,
            disabledContentColor = c.textMuted,
        ),
    ) { Text(text, style = TaccaType.button) }
}

@Composable
fun DangerTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = TaccaType.label, color = TaccaTheme.colors.danger)
    }
}

/** Dashed "add" tile of the mockup (Grinders list, Methods grid). */
@Composable
fun DashedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, radius: Dp = 24.dp, vertical: Boolean = false) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(radius)
    val content: @Composable () -> Unit = {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
        Text(text, style = TaccaType.title, color = c.textMuted)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = c.dashed,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                    cornerRadius = CornerRadius(radius.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}
```

`ui/components/Cards.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, radius: Dp = 22.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(modifier.clip(shape).background(c.surface).border(1.dp, c.outline, shape), content = content)
}

/** Rows separated by 1dp hairlines inside one rounded container (mockup home groups). Rows must set their own surface background. */
@Composable
fun GroupedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape).background(c.outline).border(1.dp, c.outline, shape),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        content = content,
    )
}

@Composable
fun TintedIconBox(
    @DrawableRes icon: Int,
    background: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    radius: Dp = 14.dp,
) {
    Box(modifier.size(boxSize).clip(RoundedCornerShape(radius)).background(background), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = TaccaTheme.colors.text, modifier = Modifier.size(iconSize))
    }
}
```

`ui/components/Chips.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/** Home filter chip: 38dp pill, dark when selected. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    val c = TaccaTheme.colors
    val bg = if (selected) c.text else c.surface
    val fg = if (selected) c.surface else c.text
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) c.text else c.outline, CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leadingIcon?.let { Icon(painterResource(it), null, tint = fg, modifier = Modifier.size(16.dp)) }
        Text(label, style = TaccaType.label, color = fg, maxLines = 1)
        trailingIcon?.let { Icon(painterResource(it), null, tint = fg, modifier = Modifier.size(14.dp)) }
    }
}

/** Small 30dp informational pill (grinder cards, detail header). */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    background: Color = TaccaTheme.colors.background,
) {
    Row(
        modifier = modifier.height(30.dp).clip(CircleShape).background(background).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(painterResource(it), null, tint = TaccaTheme.colors.text, modifier = Modifier.size(14.dp)) }
        Text(text, style = TaccaType.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = TaccaTheme.colors.text, maxLines = 1)
    }
}
```

`ui/components/Stepper.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun Stepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementLabel: String,
    incrementLabel: String,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
    valueStyle: TextStyle = TaccaType.numberL,
    valueColor: Color = TaccaTheme.colors.text,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        StepButton(R.drawable.ic_minus, decrementLabel, canDecrement, onDecrement)
        Text(value, style = valueStyle, color = valueColor, maxLines = 1)
        StepButton(R.drawable.ic_add, incrementLabel, canIncrement, onIncrement)
    }
}

@Composable
private fun StepButton(@DrawableRes icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.background).alpha(if (enabled) 1f else 0.4f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
        }
    }
}
```

`ui/components/Dial.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.DialGeometry
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun Dial(setting: GrindSetting, stepsPerTurn: Int?, modifier: Modifier = Modifier, size: Dp = 260.dp) {
    val c = TaccaTheme.colors
    val description = stringResource(R.string.detail_dial_description, settingText(setting))
    val hiddenTurns = (setting.turns - DialGeometry.MAX_RINGS).coerceAtLeast(0)
    Box(
        modifier.size(size).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val side = this.size.minDimension
            val s = side / 260f
            val model = DialGeometry.build(side, setting, stepsPerTurn)
            val center = Offset(side / 2f, side / 2f)
            model.ringRadii.forEach { r -> drawCircle(c.highlight, radius = r, center = center, style = Stroke(5f * s)) }
            model.ticks.forEach { t ->
                drawLine(
                    color = if (t.active) c.accent else c.tickInactive,
                    start = Offset(t.inner.x, t.inner.y),
                    end = Offset(t.outer.x, t.outer.y),
                    strokeWidth = (if (t.major) 3f else 2f) * s,
                    cap = StrokeCap.Round,
                )
            }
            model.pointer?.let { p ->
                val o = Offset(p.x, p.y)
                drawCircle(c.accent, radius = 8f * s, center = o)
                drawCircle(c.surface, radius = 8f * s, center = o, style = Stroke(3f * s))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hiddenTurns > 0) Text(stringResource(R.string.detail_more_rings, hiddenTurns), style = TaccaType.bodySmall, color = c.textMuted)
            if (setting.turns > 0) {
                Text(pluralStringResource(R.plurals.detail_turns_plus, setting.turns, setting.turns), style = TaccaType.label.copy(fontSize = 15.sp), color = c.textMuted)
            }
            Text(setting.steps.toString(), style = TaccaType.numberXL, color = c.accentText)
            Text(stringResource(R.string.unit_steps).uppercase(), style = TaccaType.unit.copy(fontSize = 12.sp), color = c.textMuted)
        }
    }
}
```

`ui/components/Dialogs.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val c = TaccaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, style = TaccaType.label, color = if (destructive) c.danger else c.accentText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), style = TaccaType.label, color = c.text) }
        },
        title = { Text(title, style = TaccaType.title.copy(fontSize = 18.sp, lineHeight = 23.sp), color = c.text) },
        text = text?.let { { Text(it, style = TaccaType.body, color = c.textMuted) } },
        containerColor = c.surface,
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun cascadeDeleteText(count: Int): String =
    if (count == 0) stringResource(R.string.confirm_delete_no_cascade)
    else pluralStringResource(R.plurals.confirm_delete_cascade_body, count, count)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaccaBottomSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = TaccaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = TaccaType.h1Small.copy(fontSize = 24.sp), color = c.text)
            content()
        }
    }
}
```

`ui/components/EmptyState.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = TaccaTheme.colors
    Column(
        modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(c.highlight), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = c.accentText, modifier = Modifier.size(30.dp))
        }
        Text(title, style = TaccaType.title.copy(fontSize = 17.sp), color = c.text, textAlign = TextAlign.Center)
        body?.let { Text(it, style = TaccaType.body.copy(fontSize = 14.sp), color = c.textMuted, textAlign = TextAlign.Center) }
        if (actionLabel != null && onAction != null) {
            PrimaryButton(actionLabel, onAction, Modifier.widthIn(max = 280.dp))
        }
    }
}
```

`ui/components/Fields.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

/** Labelled text field in mockup style (label above, 16dp radius, surface fill). */
@Composable
fun TaccaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    supportingText: String? = null,
    fieldModifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = { Text(placeholder, style = TaccaType.body.copy(fontSize = 16.sp), color = c.textMuted) },
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(16.dp),
            textStyle = TaccaType.body.copy(fontSize = 16.sp, color = c.text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            supportingText = supportingText?.let { { Text(it, style = TaccaType.bodySmall, color = c.textMuted) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                cursorColor = c.accent,
            ),
        )
    }
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, label: String, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.surface).border(1.dp, c.outline, shape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = TaccaType.body, color = c.textMuted, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TaccaType.body.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            )
        }
    }
}
```

`ui/components/Headers.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = TaccaType.h1,
    top: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = TaccaTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            top?.invoke()
            Text(title, style = titleStyle, color = c.text, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = TaccaType.body.copy(fontSize = TaccaType.label.fontSize), color = c.textMuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun Legend(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = TaccaType.legend, color = TaccaTheme.colors.textMuted, modifier = modifier.padding(start = 4.dp))
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(R.drawable.ic_brand), contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.brand_name), style = TaccaType.brand, color = c.accentText)
    }
}
```

`ui/components/BottomBar.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.ui.navigation.TopLevelDestination
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun TaccaBottomBar(selected: TopLevelDestination?, onSelect: (TopLevelDestination) -> Unit, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = c.outline)
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 12.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopLevelDestination.entries.forEach { dest ->
                val isSelected = dest == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(66.dp)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(dest) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier.size(width = 52.dp, height = 30.dp).clip(CircleShape).background(if (isSelected) c.highlight else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(dest.icon), contentDescription = null, tint = if (isSelected) c.text else c.textMuted, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        stringResource(dest.label),
                        style = TaccaType.nav.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold),
                        color = if (isSelected) c.text else c.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
```

`ui/components/Layout.kt`:

```kotlin
package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Content padding for the four tab screens: mockup side gutters plus the status bar inset. */
@Composable
fun topLevelContentPadding(): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return PaddingValues(start = 20.dp, end = 20.dp, top = top + 20.dp, bottom = 24.dp)
}
```

- [ ] **Step 5: Run tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*ComponentsTest*"'`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Add navigation routes and shared UI components

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

## Screen tasks — shared conventions (read before Tasks 10–16)

- Each screen package has a **ViewModel** (with `companion object { val Factory: ViewModelProvider.Factory }` built with `viewModelFactory { initializer { ... } }`, reading `(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container` and `createSavedStateHandle()`), a stateful **`XEntry`** composable (named `...Entry` so it never clashes with the `...Route` navigation objects) (gets the VM via `viewModel(factory = XViewModel.Factory)`, collects `uiState` with `collectAsStateWithLifecycle()`, turns one-shot state such as `savedId`/`deleted` into navigation callbacks with `LaunchedEffect`), and a stateless **`XScreen`** composable (pure function of state + lambdas, the thing UI tests render).
- Top-level screens (Home, Coffees, Methods, Grinders) take a `modifier` that the nav shell uses to add the bottom-bar padding; they add the status-bar inset themselves with `topLevelContentPadding()` (from `ui/components/Layout.kt`, Task 9). Full-screen routes (Detail, editors, Settings) use `Modifier.fillMaxSize().background(c.background).statusBarsPadding()` and put pinned buttons in a bottom area with `navigationBarsPadding().imePadding()`.
- Visual values (paddings, radii, font sizes, colors) come from the matching mockup file in `docs/mockup/` — follow it; the snippets below already encode them.
- ViewModel tests: `@RunWith(AndroidJUnit4::class)`, `@get:Rule val main = MainDispatcherRule()`, `val r = TestRepos()` (close `r.db` in `@After`), build the VM directly with repositories from `r`, await with `vm.uiState.first { predicate }`.

---

### Task 10: Grinders list and grinder editor

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/grinders/GrindersViewModel.kt`, `GrindersScreen.kt`
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/grindereditor/GrinderEditorViewModel.kt`, `GrinderEditorScreen.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/grinders/GrindersViewModelTest.kt`, `app/src/test/java/it/matteocolombo/tacca/ui/grindereditor/GrinderEditorViewModelTest.kt`, `GrinderEditorScreenTest.kt`

**Interfaces:**
- Consumes: `GrinderRepository`, `CalibrationRepository`, `SettingRules`, `TextMatch`, `Tint.rotating`, components, labels, `idArg`.
- Produces:
  - `GrindersViewModel(grinders, calibrations)`, `data class GrinderItem(grinder: Grinder, calibrationCount: Int, tint: Tint)`, `data class GrindersUiState(loading, items)`
  - `@Composable fun GrindersEntry(onGrinderClick: (Long) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier)`
  - `GrinderEditorViewModel(savedStateHandle, grinders)` reading arg `"grinderId"`; `data class GrinderEditorUiState(...)` (see code)
  - `@Composable fun GrinderEditorEntry(onClose: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit)`

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/it/matteocolombo/tacca/ui/grinders/GrindersViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grinders

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrindersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    @Test fun listsGrindersWithCountsAndRotatingTints() = runTest {
        val jx = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        r.grinders.add("Encore", GrinderSpec(false, 40))
        val coffee = r.coffees.add("Brasile")
        r.calibrations.save(SaveCalibration(null, coffee, r.builtInMethod(BuiltInMethod.MOKA).id, jx, GrindSetting(1, 2), ""))

        val vm = GrindersViewModel(r.grinders, r.calibrations)
        val state = vm.uiState.first { !it.loading && it.items.size == 2 && it.items[0].calibrationCount == 1 }
        assertEquals(listOf("JX-Pro", "Encore"), state.items.map { it.grinder.name })
        assertEquals(listOf(1, 0), state.items.map { it.calibrationCount })
        assertEquals(listOf(Tint.PEACH, Tint.SAND), state.items.map { it.tint })
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/grindereditor/GrinderEditorViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grindereditor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrinderEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm(id: Long? = null) = GrinderEditorViewModel(SavedStateHandle(id?.let { mapOf("grinderId" to it) } ?: emptyMap()), r.grinders)

    @Test fun newGrinderDefaults() = runTest {
        val s = vm().uiState.first { it.loaded }
        assertTrue(s.isNew)
        assertTrue(s.multiTurn)
        assertEquals(SettingRules.DEFAULT_STEPS_PER_TURN, s.stepsPerTurn)
        assertEquals(GrindSetting(2, 12), s.preview)
    }

    @Test fun saveNewCreatesGrinder() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.onNameChange("  JX-Pro ")
        vm.onMultiTurnChange(false)
        vm.save()
        val id = vm.uiState.first { it.savedId != null }.savedId!!
        val g = r.grinders.get(id)!!
        assertEquals("JX-Pro", g.name)
        assertEquals(GrinderSpec(false, 40), g.spec)
    }

    @Test fun editLoadsAndWarnsAboutOutOfRange() = runTest {
        val g = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val c = r.coffees.add("Brasile")
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(1, 30), ""))
        val vm = vm(g)
        val loaded = vm.uiState.first { it.loaded }
        assertEquals("JX-Pro", loaded.name)
        assertEquals(1, loaded.calibrationCount)
        vm.onMultiTurnChange(false)
        assertEquals(1, vm.uiState.first { it.outOfRangeCount == 1 }.outOfRangeCount)
        vm.onMultiTurnChange(true)
        assertEquals(0, vm.uiState.first { it.outOfRangeCount == 0 }.outOfRangeCount)
    }

    @Test fun stepsGoToUnknownBelowOneAndBackToDefault() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        repeat(40) { vm.decrementSteps() }
        assertNull(vm.uiState.value.stepsPerTurn)
        vm.incrementSteps()
        assertEquals(40, vm.uiState.value.stepsPerTurn)
        vm.setStepsUnknown()
        assertNull(vm.uiState.value.stepsPerTurn)
    }

    @Test fun flagsDuplicateNames() = runTest {
        r.grinders.add("Encore", GrinderSpec(false, 40))
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.onNameChange("encore ")
        assertTrue(vm.uiState.value.isDuplicateName)
    }

    @Test fun deleteRemovesGrinder() = runTest {
        val g = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val vm = vm(g)
        vm.uiState.first { it.loaded }
        vm.delete()
        vm.uiState.first { it.deleted }
        assertNull(r.grinders.get(g))
    }
}
```

- [ ] **Step 2: Write the failing screen test**

`app/src/test/java/it/matteocolombo/tacca/ui/grindereditor/GrinderEditorScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grindereditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrinderEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: GrinderEditorUiState) = compose.setContent {
        TaccaTheme { GrinderEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}) }
    }

    @Test fun singleTurnShowsTotalStepsAndPreview() {
        show(GrinderEditorUiState(loaded = true, name = "Encore", multiTurn = false, stepsPerTurn = 40))
        compose.onNodeWithText("Total steps").assertIsDisplayed()
        compose.onNodeWithText("12 steps").assertIsDisplayed()
    }

    @Test fun blankNameDisablesSave() {
        show(GrinderEditorUiState(loaded = true, name = " "))
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun showsOutOfRangeWarningAndDelete() {
        show(GrinderEditorUiState(loaded = true, isNew = false, name = "JX", outOfRangeCount = 2))
        compose.onNodeWithText("2 calibrations are outside the new range").assertIsDisplayed()
        compose.onNodeWithText("Delete grinder").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.grinders.*" --tests "it.matteocolombo.tacca.ui.grindereditor.*"'`
Expected: compilation FAILS.

- [ ] **Step 4: Implement the grinders list**

`ui/grinders/GrindersViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grinders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GrinderItem(val grinder: Grinder, val calibrationCount: Int, val tint: Tint)

data class GrindersUiState(val loading: Boolean = true, val items: List<GrinderItem> = emptyList())

class GrindersViewModel(grinders: GrinderRepository, calibrations: CalibrationRepository) : ViewModel() {
    val uiState: StateFlow<GrindersUiState> =
        combine(grinders.observeGrinders(), calibrations.observeAll()) { list, cals ->
            val counts = cals.groupingBy { it.grinder.id }.eachCount()
            GrindersUiState(
                loading = false,
                items = list.mapIndexed { i, g -> GrinderItem(g, counts[g.id] ?: 0, Tint.rotating(i)) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrindersUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                GrindersViewModel(c.grinders, c.calibrations)
            }
        }
    }
}
```

`ui/grinders/GrindersScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grinders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.components.DashedButton
import it.matteocolombo.tacca.ui.components.InfoChip
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun GrindersEntry(onGrinderClick: (Long) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val vm: GrindersViewModel = viewModel(factory = GrindersViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    GrindersScreen(state, onGrinderClick, onAddClick, modifier)
}

@Composable
fun GrindersScreen(state: GrindersUiState, onGrinderClick: (Long) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    LazyColumn(
        modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.grinders_title),
                subtitle = stringResource(R.string.grinders_subtitle),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.items, key = { it.grinder.id }) { item -> GrinderCard(item, onClick = { onGrinderClick(item.grinder.id) }) }
        item { DashedButton(stringResource(R.string.grinders_add), onAddClick, Modifier.fillMaxWidth().height(64.dp)) }
    }
}

@Composable
private fun GrinderCard(item: GrinderItem, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    val spec = item.grinder.spec
    SurfaceCard(Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 24.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TintedIconBox(R.drawable.ic_grinder, c.tint(item.tint), boxSize = 52.dp, iconSize = 28.dp, radius = 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.grinder.name, style = TaccaType.cardTitle, color = c.text)
                    Text(
                        if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                        else pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                        style = TaccaType.bodySmall, color = c.textMuted,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = spec.stepsPerTurn
                if (spec.multiTurn) {
                    InfoChip(stringResource(R.string.grinder_chip_multi), icon = R.drawable.ic_multi_turn)
                    InfoChip(if (n != null) pluralStringResource(R.plurals.grinder_chip_per_turn, n, n) else stringResource(R.string.grinder_chip_unknown))
                } else {
                    InfoChip(stringResource(R.string.grinder_chip_single))
                    InfoChip(if (n != null) pluralStringResource(R.plurals.steps_count, n, n) else stringResource(R.string.grinder_chip_unknown))
                }
            }
        }
    }
}
```

- [ ] **Step 5: Implement the grinder editor**

`ui/grindereditor/GrinderEditorViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grindereditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GrinderEditorUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val multiTurn: Boolean = true,
    val stepsPerTurn: Int? = SettingRules.DEFAULT_STEPS_PER_TURN,
    /** Normalized names of the other grinders. */
    val existingNames: Set<String> = emptySet(),
    val outOfRangeCount: Int = 0,
    val calibrationCount: Int = 0,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val deleted: Boolean = false,
) {
    val spec: GrinderSpec get() = GrinderSpec(multiTurn, stepsPerTurn)
    val canSave: Boolean get() = loaded && !saving && name.isNotBlank()
    val isDuplicateName: Boolean get() = name.isNotBlank() && TextMatch.normalize(name) in existingNames
    val preview: GrindSetting get() = SettingRules.clamp(GrindSetting(2, 12), spec)
}

class GrinderEditorViewModel(savedStateHandle: SavedStateHandle, private val grinders: GrinderRepository) : ViewModel() {
    private val grinderId: Long? = savedStateHandle.idArg("grinderId")
    private val state = MutableStateFlow(GrinderEditorUiState(isNew = grinderId == null))
    val uiState: StateFlow<GrinderEditorUiState> = state.asStateFlow()
    private var rangeJob: Job? = null

    init {
        viewModelScope.launch {
            val all = grinders.observeGrinders().first()
            val existing = grinderId?.let { id -> all.firstOrNull { it.id == id } }
            val count = grinderId?.let { grinders.calibrationCount(it) } ?: 0
            state.update { s ->
                s.copy(
                    loaded = true,
                    name = existing?.name ?: s.name,
                    multiTurn = existing?.spec?.multiTurn ?: s.multiTurn,
                    stepsPerTurn = if (existing != null) existing.spec.stepsPerTurn else s.stepsPerTurn,
                    existingNames = all.filter { it.id != grinderId }.map { TextMatch.normalize(it.name) }.toSet(),
                    calibrationCount = count,
                )
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value) }

    fun onMultiTurnChange(value: Boolean) {
        state.update { it.copy(multiTurn = value) }
        refreshOutOfRange()
    }

    fun incrementSteps() {
        state.update { s ->
            s.copy(stepsPerTurn = s.stepsPerTurn?.let { (it + 1).coerceAtMost(SettingRules.MAX_STEPS_PER_TURN) } ?: SettingRules.DEFAULT_STEPS_PER_TURN)
        }
        refreshOutOfRange()
    }

    /** Stepping below the minimum means "unknown". */
    fun decrementSteps() {
        state.update { s -> s.copy(stepsPerTurn = s.stepsPerTurn?.let { if (it <= SettingRules.MIN_STEPS_PER_TURN) null else it - 1 }) }
        refreshOutOfRange()
    }

    fun setStepsUnknown() {
        state.update { it.copy(stepsPerTurn = null) }
        refreshOutOfRange()
    }

    private fun refreshOutOfRange() {
        val id = grinderId ?: return
        val spec = state.value.spec
        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            val n = grinders.countOutOfRange(id, spec)
            state.update { it.copy(outOfRangeCount = n) }
        }
    }

    fun save() {
        val s = state.value
        if (!s.canSave) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = if (grinderId == null) {
                grinders.add(s.name, s.spec)
            } else {
                grinders.update(grinderId, s.name, s.spec)
                grinderId
            }
            state.update { it.copy(saving = false, savedId = id) }
        }
    }

    fun delete() {
        val id = grinderId ?: return
        viewModelScope.launch {
            grinders.delete(id)
            state.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                GrinderEditorViewModel(createSavedStateHandle(), c.grinders)
            }
        }
    }
}
```

`ui/grindereditor/GrinderEditorScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.grindereditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.Stepper
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun GrinderEditorEntry(onClose: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val vm: GrinderEditorViewModel = viewModel(factory = GrinderEditorViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    GrinderEditorScreen(
        state = state,
        onClose = onClose,
        onNameChange = vm::onNameChange,
        onMultiTurnChange = vm::onMultiTurnChange,
        onStepsDown = vm::decrementSteps,
        onStepsUp = vm::incrementSteps,
        onStepsUnknown = vm::setStepsUnknown,
        onSave = vm::save,
        onDelete = vm::delete,
    )
}

@Composable
fun GrinderEditorScreen(
    state: GrinderEditorUiState,
    onClose: () -> Unit,
    onNameChange: (String) -> Unit,
    onMultiTurnChange: (Boolean) -> Unit,
    onStepsDown: () -> Unit,
    onStepsUp: () -> Unit,
    onStepsUnknown: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (state.isNew) R.string.grinder_editor_title_new else R.string.grinder_editor_title_edit),
                titleStyle = TaccaType.h1Small,
            ) { SquareIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose) }

            TaccaTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.grinder_name_label),
                placeholder = stringResource(R.string.grinder_name_hint),
                supportingText = if (state.isDuplicateName) stringResource(R.string.grinder_duplicate_hint) else null,
            )

            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val multiTitle = stringResource(R.string.grinder_multi_title)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(multiTitle, style = TaccaType.title, color = c.text)
                            Text(stringResource(R.string.grinder_multi_body), style = TaccaType.bodySmall, color = c.textMuted)
                        }
                        Switch(
                            checked = state.multiTurn,
                            onCheckedChange = onMultiTurnChange,
                            modifier = Modifier.semantics { contentDescription = multiTitle },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent, checkedThumbColor = Color.White,
                                uncheckedTrackColor = c.dashed, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = c.outline)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                stringResource(if (state.multiTurn) R.string.grinder_steps_per_turn else R.string.grinder_steps_total),
                                style = TaccaType.title, color = c.text,
                            )
                            Text(stringResource(R.string.grinder_steps_optional), style = TaccaType.bodySmall, color = c.textMuted)
                            if (state.stepsPerTurn != null) {
                                TextButton(onClick = onStepsUnknown, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                    Text(stringResource(R.string.grinder_steps_unknown_action), style = TaccaType.label, color = c.accentText)
                                }
                            }
                        }
                        Stepper(
                            value = state.stepsPerTurn?.toString() ?: stringResource(R.string.grinder_steps_not_set),
                            onDecrement = onStepsDown,
                            onIncrement = onStepsUp,
                            decrementLabel = stringResource(R.string.editor_fewer_steps),
                            incrementLabel = stringResource(R.string.editor_more_steps),
                            canDecrement = state.stepsPerTurn != null,
                            valueStyle = if (state.stepsPerTurn != null) TaccaType.numberM.copy(fontSize = 26.sp) else TaccaType.label,
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }

            if (state.outOfRangeCount > 0) {
                Text(
                    pluralStringResource(R.plurals.grinder_out_of_range, state.outOfRangeCount, state.outOfRangeCount),
                    style = TaccaType.label, color = c.danger,
                )
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.highlight).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(c.surface), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_tab_calibrations), contentDescription = null, tint = c.accentText, modifier = Modifier.size(22.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.grinder_preview_label).uppercase(), style = TaccaType.legend, color = c.textOnTint)
                    Text(settingText(state.preview), style = TaccaType.cardTitle, color = c.text)
                }
            }

            if (!state.isNew) DangerTextButton(stringResource(R.string.grinder_delete), { confirmDelete = true })
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp).navigationBarsPadding().imePadding()) {
            PrimaryButton(stringResource(R.string.action_save), onSave, enabled = state.canSave)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, state.name),
            text = cascadeDeleteText(state.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
```

- [ ] **Step 6: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.grinders.*" --tests "it.matteocolombo.tacca.ui.grindereditor.*"'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add grinders list and grinder editor

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 11: Coffees list and coffee sheet

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/coffees/CoffeesViewModel.kt`, `CoffeesScreen.kt`, `CoffeeSheet.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/coffees/CoffeesViewModelTest.kt`, `CoffeesScreenTest.kt`

**Interfaces:**
- Consumes: `CoffeeRepository`, `CalibrationRepository`, `HomeFilterStore`, components, labels.
- Produces:
  - `CoffeesViewModel(coffees, calibrations, homeFilter)`; `data class CoffeeItem(coffee, calibrationCount, methods: List<Method>, tint: Tint)`; `sealed interface CoffeeSheetState { data object New; data class Edit(coffee: Coffee, calibrationCount: Int) }`; `data class CoffeesUiState(loading, query, totalCount, items, sheet, existingNames, saving)`
  - `@Composable fun CoffeesEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier)`
  - **Reused by Task 13:** `@Composable fun CoffeeSheet(title: String, initialName: String, existingNames: Set<String>, onSave: (String) -> Unit, onDismiss: () -> Unit, saving: Boolean = false, onDelete: (() -> Unit)? = null)`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/coffees/CoffeesViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.coffees

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoffeesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = CoffeesViewModel(r.coffees, r.calibrations, r.homeFilter)

    @Test fun itemsCarryCountsAndMethods() = runTest {
        val c = r.coffees.add("Etiopia")
        r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.V60).id, g, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, c, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(1, 30), ""))
        val s = vm().uiState.first { it.items.size == 2 && it.items.any { i -> i.calibrationCount == 2 } }
        val etiopia = s.items.first { it.coffee.name == "Etiopia" }
        assertEquals(listOf(BuiltInMethod.MOKA, BuiltInMethod.V60), etiopia.methods.map { it.builtIn })
        assertEquals(2, s.totalCount)
    }

    @Test fun queryIgnoresDiacritics() = runTest {
        r.coffees.add("Colombia Finca El Paraíso")
        r.coffees.add("Brasile")
        val vm = vm()
        vm.onQueryChange("paraiso")
        val s = vm.uiState.first { it.query == "paraiso" && !it.loading }
        assertEquals(listOf("Colombia Finca El Paraíso"), s.items.map { it.coffee.name })
    }

    @Test fun doubleSaveCreatesOneCoffee() = runTest {
        val vm = vm()
        vm.uiState.first { !it.loading }
        vm.openNew()
        vm.save("Kenya")
        vm.save("Kenya")
        vm.uiState.first { it.sheet == null && !it.saving && it.totalCount >= 1 }
        assertEquals(1, r.coffees.observeCoffees().first().size)
    }

    @Test fun editRenamesAndDeleteCascades() = runTest {
        val id = r.coffees.add("Kenia")
        val vm = vm()
        val coffee = vm.uiState.first { it.items.isNotEmpty() }.items.single().coffee
        vm.openEdit(coffee)
        vm.uiState.first { it.sheet is CoffeeSheetState.Edit }
        vm.save("Kenya AA")
        vm.uiState.first { it.sheet == null && it.items.singleOrNull()?.coffee?.name == "Kenya AA" }
        vm.openEdit(r.coffees.get(id)!!)
        vm.uiState.first { it.sheet is CoffeeSheetState.Edit }
        vm.delete()
        vm.uiState.first { it.totalCount == 0 }
        assertNull(r.coffees.get(id))
    }

    @Test fun showOnHomeSetsFilter() {
        vm().showOnHome(7)
        assertEquals(HomeFilter(coffeeId = 7), r.homeFilter.filter.value)
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/coffees/CoffeesScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.coffees

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoffeesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyStateOffersAdd() {
        compose.setContent { TaccaTheme { CoffeesScreen(CoffeesUiState(loading = false), {}, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("No coffees yet. Add the first one.").assertIsDisplayed()
    }

    @Test fun sheetShowsDuplicateHint() {
        compose.setContent {
            TaccaTheme { CoffeeSheet("New coffee", "", setOf("brasile"), onSave = {}, onDismiss = {}) }
        }
        compose.onNodeWithContentDescription("Name").performTextInput("Brasile")
        compose.onNodeWithText("A coffee with this name already exists").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.coffees.*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Implement**

`ui/coffees/CoffeesViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.coffees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CoffeeItem(val coffee: Coffee, val calibrationCount: Int, val methods: List<Method>, val tint: Tint)

sealed interface CoffeeSheetState {
    data object New : CoffeeSheetState
    data class Edit(val coffee: Coffee, val calibrationCount: Int) : CoffeeSheetState
}

data class CoffeesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val totalCount: Int = 0,
    val items: List<CoffeeItem> = emptyList(),
    val sheet: CoffeeSheetState? = null,
    val existingNames: Set<String> = emptySet(),
    val saving: Boolean = false,
)

class CoffeesViewModel(
    private val coffees: CoffeeRepository,
    calibrations: CalibrationRepository,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sheet = MutableStateFlow<CoffeeSheetState?>(null)
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<CoffeesUiState> =
        combine(coffees.observeCoffees(), calibrations.observeAll(), query, sheet, saving) { list, cals, q, sh, sv ->
            val byCoffee = cals.groupBy { it.coffee.id }
            val items = list.mapIndexed { i, coffee ->
                val own = byCoffee[coffee.id].orEmpty()
                CoffeeItem(coffee, own.size, own.map { it.method }.distinctBy { it.id }.sortedBy { it.sortOrder }, Tint.rotating(i))
            }.filter { TextMatch.matches(q, it.coffee.name) }
            CoffeesUiState(false, q, list.size, items, sh, list.map { TextMatch.normalize(it.name) }.toSet(), sv)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoffeesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openNew() {
        sheet.value = CoffeeSheetState.New
    }

    fun openEdit(coffee: Coffee) {
        viewModelScope.launch { sheet.value = CoffeeSheetState.Edit(coffee, coffees.calibrationCount(coffee.id)) }
    }

    fun dismissSheet() {
        sheet.value = null
    }

    fun save(name: String) {
        if (saving.value || name.isBlank()) return
        saving.value = true
        viewModelScope.launch {
            try {
                when (val s = sheet.value) {
                    is CoffeeSheetState.Edit -> coffees.rename(s.coffee.id, name)
                    else -> coffees.add(name)
                }
                sheet.value = null
            } finally {
                saving.value = false
            }
        }
    }

    fun delete() {
        val s = sheet.value as? CoffeeSheetState.Edit ?: return
        viewModelScope.launch {
            coffees.delete(s.coffee.id)
            sheet.value = null
        }
    }

    fun showOnHome(coffeeId: Long) = homeFilter.showCoffee(coffeeId)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                CoffeesViewModel(c.coffees, c.calibrations, c.homeFilter)
            }
        }
    }
}
```

`ui/coffees/CoffeeSheet.kt`:

```kotlin
package it.matteocolombo.tacca.ui.coffees

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.TaccaBottomSheet
import it.matteocolombo.tacca.ui.components.TaccaTextField

/** Bottom sheet to create or rename a coffee. Also used inline by the calibration editor. */
@Composable
fun CoffeeSheet(
    title: String,
    initialName: String,
    existingNames: Set<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val normalized = TextMatch.normalize(name)
    val duplicate = normalized.isNotEmpty() && normalized != TextMatch.normalize(initialName) && normalized in existingNames
    TaccaBottomSheet(onDismiss = onDismiss, title = title) {
        TaccaTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.coffee_name_label),
            placeholder = stringResource(R.string.coffee_name_hint),
            fieldModifier = Modifier.focusRequester(focus),
            onImeAction = { if (name.isNotBlank() && !saving) onSave(name) },
            supportingText = if (duplicate) stringResource(R.string.coffee_duplicate_hint) else null,
        )
        PrimaryButton(stringResource(R.string.action_save), { onSave(name) }, enabled = name.isNotBlank() && !saving)
        if (onDelete != null) DangerTextButton(stringResource(R.string.coffee_delete), onDelete)
    }
}
```

`ui/coffees/CoffeesScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.coffees

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SearchField
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun CoffeesEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier) {
    val vm: CoffeesViewModel = viewModel(factory = CoffeesViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    CoffeesScreen(
        state = state,
        onQueryChange = vm::onQueryChange,
        onAdd = vm::openNew,
        onCoffeeClick = { vm.showOnHome(it.id); onShowOnHome() },
        onCoffeeEdit = vm::openEdit,
        onSave = vm::save,
        onDelete = vm::delete,
        onDismissSheet = vm::dismissSheet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoffeesScreen(
    state: CoffeesUiState,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCoffeeClick: (Coffee) -> Unit,
    onCoffeeEdit: (Coffee) -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.coffees_title),
                subtitle = pluralStringResource(R.plurals.coffees_count, state.totalCount, state.totalCount),
            ) { SquareIconButton(R.drawable.ic_add, stringResource(R.string.coffees_add), onAdd, filled = true) }
        }
        if (state.totalCount > 0) {
            item {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    placeholder = stringResource(R.string.coffees_search_hint),
                    label = stringResource(R.string.coffees_search_label),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
        when {
            !state.loading && state.totalCount == 0 -> item {
                EmptyState(R.drawable.ic_bean, stringResource(R.string.coffees_empty), null, actionLabel = stringResource(R.string.coffees_add), onAction = onAdd)
            }
            !state.loading && state.items.isEmpty() -> item {
                Text(stringResource(R.string.coffees_no_results), style = TaccaType.body, color = c.textMuted, modifier = Modifier.padding(8.dp))
            }
        }
        items(state.items, key = { it.coffee.id }) { item ->
            SurfaceCard(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .combinedClickable(onClick = { onCoffeeClick(item.coffee) }, onLongClick = { onCoffeeEdit(item.coffee) }),
            ) {
                Row(
                    Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TintedIconBox(R.drawable.ic_bean, c.tint(item.tint), boxSize = 48.dp, iconSize = 24.dp, radius = 16.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.coffee.name, style = TaccaType.title, color = c.text)
                        Text(
                            if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                            else stringResource(
                                R.string.coffee_subtitle,
                                pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                                item.methods.map { methodName(it) }.joinToString(", "),
                            ),
                            style = TaccaType.bodySmall, color = c.textMuted,
                        )
                    }
                    SquareIconButton(R.drawable.ic_more, stringResource(R.string.action_more), { onCoffeeEdit(item.coffee) })
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        CoffeeSheetState.New -> CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_new), initialName = "", existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving,
        )
        is CoffeeSheetState.Edit -> CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_edit), initialName = sheet.coffee.name, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving, onDelete = { confirmDelete = true },
        )
        null -> Unit
    }
    val editing = state.sheet as? CoffeeSheetState.Edit
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, editing.coffee.name),
            text = cascadeDeleteText(editing.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.coffees.*"'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add coffees list and coffee sheet

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: Methods grid and method sheet

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/methods/MethodsViewModel.kt`, `MethodsScreen.kt`, `MethodSheet.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/methods/MethodsViewModelTest.kt`, `MethodSheetTest.kt`

**Interfaces:**
- Consumes: `MethodRepository`, `CalibrationRepository`, `HomeFilterStore`, `drawableRes()`, labels.
- Produces:
  - `MethodsViewModel(methods, calibrations, homeFilter)`; `data class MethodItem(method: Method, calibrationCount: Int)`; `sealed interface MethodSheetState { data object New; data class Edit(method: Method, calibrationCount: Int) }`; `data class MethodsUiState(loading, items, sheet, existingNames, saving)`; `fun save(customName: String?, icon: MethodIcon, tint: Tint)`
  - `fun customNameFor(input: String, builtIn: BuiltInMethod?, localizedBuiltInName: String?): String?` — returns null when a built-in method keeps its localized name
  - `@Composable fun MethodsEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/methods/MethodsViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.methods

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MethodsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = MethodsViewModel(r.methods, r.calibrations, r.homeFilter)

    @Test fun listsBuiltInsInOrder() = runTest {
        val s = vm().uiState.first { !it.loading }
        assertEquals(BuiltInMethod.entries.toList(), s.items.map { it.method.builtIn })
    }

    @Test fun addsCustomMethodAtTheEnd() = runTest {
        val vm = vm()
        vm.uiState.first { !it.loading }
        vm.openNew()
        vm.save("Chemex", MethodIcon.DROP, Tint.SKY)
        val s = vm.uiState.first { it.items.size == 6 && it.sheet == null }
        assertEquals("Chemex", s.items.last().method.customName)
    }

    @Test fun editKeepsBuiltInWhenNameIsNull() = runTest {
        val vm = vm()
        val moka = vm.uiState.first { !it.loading }.items.first().method
        vm.openEdit(moka)
        vm.uiState.first { it.sheet is MethodSheetState.Edit }
        vm.save(null, MethodIcon.MOKA, Tint.LILAC)
        val updated = vm.uiState.first { it.items.first().method.tint == Tint.LILAC }.items.first().method
        assertNull(updated.customName)
        assertEquals(BuiltInMethod.MOKA, updated.builtIn)
    }

    @Test fun showOnHome() {
        vm().showOnHome(3)
        assertEquals(HomeFilter(methodId = 3), r.homeFilter.filter.value)
    }

    @Test fun customNameForKeepsBuiltInName() {
        assertNull(customNameFor("Moka ", BuiltInMethod.MOKA, "Moka"))
        assertEquals("Bialetti", customNameFor("Bialetti", BuiltInMethod.MOKA, "Moka"))
        assertEquals("Chemex", customNameFor(" Chemex ", null, null))
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/methods/MethodSheetTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.methods

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MethodSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun picksIconAndTint() {
        var result: Triple<String, MethodIcon, Tint>? = null
        compose.setContent {
            TaccaTheme { MethodSheet("New method", method = null, existingNames = emptySet(), onSave = { n, i, t -> result = Triple(n, i, t) }, onDismiss = {}) }
        }
        compose.onNodeWithContentDescription("Name").performTextInput("Chemex")
        compose.onNodeWithContentDescription("Drop").performClick()
        compose.onNodeWithContentDescription("Lilac").performClick()
        compose.onNodeWithText("Save").performClick()
        assertEquals(Triple("Chemex", MethodIcon.DROP, Tint.LILAC), result)
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.methods.*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Implement**

`ui/methods/MethodsViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MethodItem(val method: Method, val calibrationCount: Int)

sealed interface MethodSheetState {
    data object New : MethodSheetState
    data class Edit(val method: Method, val calibrationCount: Int) : MethodSheetState
}

data class MethodsUiState(
    val loading: Boolean = true,
    val items: List<MethodItem> = emptyList(),
    val sheet: MethodSheetState? = null,
    /** Normalized custom names (built-in display names are checked in the sheet). */
    val existingNames: Set<String> = emptySet(),
    val saving: Boolean = false,
)

/** Null when [input] equals the localized name of [builtIn], so the method keeps following the app language. */
fun customNameFor(input: String, builtIn: BuiltInMethod?, localizedBuiltInName: String?): String? {
    val trimmed = input.trim()
    return if (builtIn != null && trimmed == localizedBuiltInName?.trim()) null else trimmed
}

class MethodsViewModel(
    private val methods: MethodRepository,
    calibrations: CalibrationRepository,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val sheet = MutableStateFlow<MethodSheetState?>(null)
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<MethodsUiState> =
        combine(methods.observeMethods(), calibrations.observeAll(), sheet, saving) { list, cals, sh, sv ->
            val counts = cals.groupingBy { it.method.id }.eachCount()
            MethodsUiState(
                loading = false,
                items = list.map { MethodItem(it, counts[it.id] ?: 0) },
                sheet = sh,
                existingNames = list.mapNotNull { it.customName }.map(TextMatch::normalize).toSet(),
                saving = sv,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MethodsUiState())

    fun openNew() {
        sheet.value = MethodSheetState.New
    }

    fun openEdit(method: Method) {
        viewModelScope.launch { sheet.value = MethodSheetState.Edit(method, methods.calibrationCount(method.id)) }
    }

    fun dismissSheet() {
        sheet.value = null
    }

    fun save(customName: String?, icon: MethodIcon, tint: Tint) {
        if (saving.value) return
        val current = sheet.value
        if (current !is MethodSheetState.Edit && customName.isNullOrBlank()) return
        saving.value = true
        viewModelScope.launch {
            try {
                if (current is MethodSheetState.Edit) methods.update(current.method.id, customName, icon, tint)
                else methods.add(customName!!, icon, tint)
                sheet.value = null
            } finally {
                saving.value = false
            }
        }
    }

    fun delete() {
        val s = sheet.value as? MethodSheetState.Edit ?: return
        viewModelScope.launch {
            methods.delete(s.method.id)
            sheet.value = null
        }
    }

    fun showOnHome(methodId: Long) = homeFilter.showMethod(methodId)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                MethodsViewModel(c.methods, c.calibrations, c.homeFilter)
            }
        }
    }
}
```

`ui/methods/MethodSheet.kt`:

```kotlin
package it.matteocolombo.tacca.ui.methods

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.TaccaBottomSheet
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.format.labelRes
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MethodSheet(
    title: String,
    method: Method?,
    existingNames: Set<String>,
    onSave: (name: String, icon: MethodIcon, tint: Tint) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    val c = TaccaTheme.colors
    val initialName = method?.let { methodName(it) }.orEmpty()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(method?.icon ?: MethodIcon.CUP) }
    var tint by rememberSaveable { mutableStateOf(method?.tint ?: Tint.SAND) }
    val normalized = TextMatch.normalize(name)
    val duplicate = normalized.isNotEmpty() && normalized != TextMatch.normalize(initialName) && normalized in existingNames

    TaccaBottomSheet(onDismiss = onDismiss, title = title) {
        TaccaTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.method_name_label),
            placeholder = stringResource(R.string.method_name_hint),
            supportingText = if (duplicate) stringResource(R.string.method_duplicate_hint) else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend(stringResource(R.string.method_icon_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodIcon.entries.forEach { option ->
                    val selected = option == icon
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.tint(tint))
                            .border(1.5.dp, if (selected) c.text else Color.Transparent, RoundedCornerShape(14.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { icon = option })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(option.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(26.dp)) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend(stringResource(R.string.method_tint_label))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tint.entries.forEach { option ->
                    val selected = option == tint
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .border(2.dp, if (selected) c.text else Color.Transparent, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { tint = option })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(38.dp).clip(CircleShape).background(c.tint(option))) }
                }
            }
        }
        PrimaryButton(stringResource(R.string.action_save), { onSave(name, icon, tint) }, enabled = name.isNotBlank() && !saving)
        if (onDelete != null) DangerTextButton(stringResource(R.string.method_delete), onDelete)
    }
}
```

`ui/methods/MethodsScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.methods

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DashedButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.cascadeDeleteText
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.nameRes
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun MethodsEntry(onShowOnHome: () -> Unit, modifier: Modifier = Modifier) {
    val vm: MethodsViewModel = viewModel(factory = MethodsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val res = LocalResources.current
    MethodsScreen(
        state = state,
        onMethodClick = { vm.showOnHome(it.id); onShowOnHome() },
        onMethodEdit = vm::openEdit,
        onAdd = vm::openNew,
        onSave = { name, icon, tint ->
            val builtIn = (state.sheet as? MethodSheetState.Edit)?.method?.builtIn
            vm.save(customNameFor(name, builtIn, builtIn?.let { res.getString(it.nameRes()) }), icon, tint)
        },
        onDelete = vm::delete,
        onDismissSheet = vm::dismissSheet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MethodsScreen(
    state: MethodsUiState,
    onMethodClick: (Method) -> Unit,
    onMethodEdit: (Method) -> Unit,
    onAdd: () -> Unit,
    onSave: (String, MethodIcon, Tint) -> Unit,
    onDelete: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TaccaTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().background(c.background),
        contentPadding = topLevelContentPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            ScreenHeader(
                title = stringResource(R.string.methods_title),
                subtitle = stringResource(R.string.methods_subtitle),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.items, key = { it.method.id }) { item ->
            val shape = RoundedCornerShape(26.dp)
            Column(
                Modifier.height(150.dp).clip(shape).background(c.tint(item.method.tint))
                    .combinedClickable(onClick = { onMethodClick(item.method) }, onLongClick = { onMethodEdit(item.method) })
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(c.surface), contentAlignment = Alignment.Center) {
                        Icon(painterResource(item.method.icon.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(32.dp))
                    }
                    SquareIconButton(R.drawable.ic_more, stringResource(R.string.action_more), { onMethodEdit(item.method) })
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(methodName(item.method), style = TaccaType.cardTitle, color = c.text, maxLines = 1)
                    Text(
                        if (item.calibrationCount == 0) stringResource(R.string.calibrations_none)
                        else pluralStringResource(R.plurals.calibrations_count, item.calibrationCount, item.calibrationCount),
                        style = TaccaType.bodySmall, color = c.textOnTint,
                    )
                }
            }
        }
        item {
            DashedButton(stringResource(R.string.methods_add), onAdd, Modifier.height(150.dp), radius = 26.dp, vertical = true)
        }
    }

    when (val sheet = state.sheet) {
        MethodSheetState.New -> MethodSheet(
            stringResource(R.string.method_sheet_title_new), method = null, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving,
        )
        is MethodSheetState.Edit -> MethodSheet(
            stringResource(R.string.method_sheet_title_edit), method = sheet.method, existingNames = state.existingNames,
            onSave = onSave, onDismiss = onDismissSheet, saving = state.saving, onDelete = { confirmDelete = true },
        )
        null -> Unit
    }
    val editing = state.sheet as? MethodSheetState.Edit
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title, methodName(editing.method)),
            text = cascadeDeleteText(editing.calibrationCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.methods.*"'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add methods grid and method sheet

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 13: Calibration editor

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/editor/CalibrationEditorViewModel.kt`, `CalibrationEditorScreen.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/editor/CalibrationEditorViewModelTest.kt`, `CalibrationEditorScreenTest.kt`

**Interfaces:**
- Consumes: all four repositories, `SaveCalibration`, `SettingRules`, `TextMatch`, `CoffeeSheet` (Task 11), `idArg`, `NEW_GRINDER_RESULT_KEY`, components, labels, `drawableRes()`.
- Produces:
  - `CalibrationEditorViewModel(savedStateHandle, coffees, methods, grinders, calibrations)` reading args `"calibrationId"`, `"coffeeId"`, `"methodId"` and observing `NEW_GRINDER_RESULT_KEY`
  - `data class EditorUiState(...)` (see code) with derived `grinder`, `canSave`, `visibleCoffees`, `hiddenCoffeeCount`, `maxSteps`, `turnsEnabled`, `canIncrementSteps`
  - `data class EditorActions(...)` bundle of callbacks for the stateless screen
  - `@Composable fun CalibrationEditorEntry(onClose: () -> Unit, onNewGrinder: () -> Unit, onSaved: (id: Long, wasEdit: Boolean) -> Unit, onDeleted: () -> Unit)`

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/it/matteocolombo/tacca/ui/editor/CalibrationEditorViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import it.matteocolombo.tacca.ui.navigation.NEW_GRINDER_RESULT_KEY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm(args: Map<String, Any?> = emptyMap(), handle: SavedStateHandle = SavedStateHandle(args)) =
        CalibrationEditorViewModel(handle, r.coffees, r.methods, r.grinders, r.calibrations)

    @Test fun presetsAndSingleGrinderArePreselected() = runTest {
        val coffee = r.coffees.add("Brasile")
        r.coffees.add("Etiopia")
        val grinder = r.grinders.add("JX", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val s = vm(mapOf("coffeeId" to coffee, "methodId" to v60)).uiState.first { it.loaded }
        assertEquals(coffee, s.coffeeId)
        assertEquals(v60, s.methodId)
        assertEquals(grinder, s.grinderId)
        assertTrue(s.canSave)
    }

    @Test fun stepsClampToSingleTurnGrinder() = runTest {
        r.grinders.add("Encore", GrinderSpec(false, 10))
        val vm = vm()
        vm.uiState.first { it.loaded && it.grinderId != null }
        repeat(12) { vm.stepsUp() }
        assertEquals(GrindSetting(0, 10), vm.uiState.value.setting)
        assertFalse(vm.uiState.value.turnsEnabled)
        assertFalse(vm.uiState.value.canIncrementSteps)
    }

    @Test fun selectingAGrinderReclamps() = runTest {
        val big = r.grinders.add("JX", GrinderSpec(true, 40))
        val small = r.grinders.add("Small", GrinderSpec(true, 20))
        val vm = vm()
        vm.uiState.first { it.loaded && it.grinders.size == 2 }
        vm.selectGrinder(big)
        repeat(39) { vm.stepsUp() }
        vm.selectGrinder(small)
        assertEquals(GrindSetting(0, 19), vm.uiState.value.setting)
    }

    @Test fun warnsWhenReplacingAnExistingCombination() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(1, 30), ""))
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.selectCoffee(c)
        vm.selectMethod(moka)
        vm.selectGrinder(g)
        assertEquals(GrindSetting(1, 30), vm.uiState.first { it.replaces != null }.replaces)
    }

    @Test fun editLoadsAndSaves() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        val id = r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(1, 30), "18g"))
        val vm = vm(mapOf("calibrationId" to id))
        val s = vm.uiState.first { it.loaded }
        assertTrue(s.isEdit)
        assertEquals(GrindSetting(1, 30), s.setting)
        assertEquals("18g", s.notes)
        assertNull(s.replaces)
        vm.stepsUp()
        vm.save()
        assertEquals(id, vm.uiState.first { it.savedId != null }.savedId)
        assertEquals(GrindSetting(1, 31), r.calibrations.observe(id).first()!!.setting)
    }

    @Test fun outOfRangeValuesAreNotClampedOnLoad() = runTest {
        val c = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        val id = r.calibrations.save(SaveCalibration(null, c, moka, g, GrindSetting(0, 35), ""))
        r.grinders.update(g, "JX", GrinderSpec(true, 20))
        val s = vm(mapOf("calibrationId" to id)).uiState.first { it.loaded }
        assertEquals(GrindSetting(0, 35), s.setting)
    }

    @Test fun doubleSaveCreatesOneCalibration() = runTest {
        val c = r.coffees.add("Brasile")
        r.grinders.add("JX", GrinderSpec(true, 40))
        val vm = vm(mapOf("coffeeId" to c, "methodId" to r.builtInMethod(BuiltInMethod.V60).id))
        vm.uiState.first { it.canSave }
        vm.save()
        vm.save()
        vm.uiState.first { it.savedId != null }
        assertEquals(1, r.calibrations.observeAll().first().size)
    }

    @Test fun newGrinderResultIsSelected() = runTest {
        r.grinders.add("JX", GrinderSpec(true, 40))
        val handle = SavedStateHandle()
        val vm = vm(handle = handle)
        vm.uiState.first { it.loaded }
        val created = r.grinders.add("Comandante", GrinderSpec(true, 30))
        handle[NEW_GRINDER_RESULT_KEY] = created
        assertEquals(created, vm.uiState.first { it.grinderId == created }.grinderId)
    }

    @Test fun recentCoffeesComeFirstAndListIsCollapsed() = runTest {
        val old = r.coffees.add("Old")
        r.coffees.add("B")
        r.coffees.add("C")
        r.coffees.add("D")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        r.calibrations.save(SaveCalibration(null, old, r.builtInMethod(BuiltInMethod.MOKA).id, g, GrindSetting(0, 1), ""))
        val vm = vm()
        val s = vm.uiState.first { it.loaded && it.coffees.size == 4 }
        assertEquals("Old", s.coffees.first().name)
        assertEquals(3, s.visibleCoffees.size)
        assertEquals(1, s.hiddenCoffeeCount)
        vm.showAllCoffees()
        assertEquals(4, vm.uiState.value.visibleCoffees.size)
    }

    @Test fun addCoffeeSelectsIt() = runTest {
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.addCoffee("Kenya")
        val s = vm.uiState.first { it.coffees.any { c -> c.name == "Kenya" } && it.coffeeId != null }
        assertEquals("Kenya", s.coffees.first { it.id == s.coffeeId }.name)
    }
}
```

- [ ] **Step 2: Write the failing screen test**

`app/src/test/java/it/matteocolombo/tacca/ui/editor/CalibrationEditorScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    private val coffee = Coffee(1, "Brasile", 0)
    private val v60 = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2)
    private val single = Grinder(1, "Encore", GrinderSpec(false, 40), 0)
    private val multi = Grinder(2, "JX", GrinderSpec(true, 40), 0)
    private val base = EditorUiState(
        loaded = true, coffees = listOf(coffee), methods = listOf(v60), grinders = listOf(single, multi),
        coffeeId = 1, methodId = 3, grinderId = 2, setting = GrindSetting(1, 30),
    )

    private fun show(state: EditorUiState) = compose.setContent { TaccaTheme { CalibrationEditorScreen(state, EditorActions.Noop) } }

    @Test fun showsReplaceWarningAndSummary() {
        show(base.copy(replaces = GrindSetting(2, 5)))
        compose.onNodeWithText("Replaces the current setting (2 turns + 5 steps)").assertIsDisplayed()
        compose.onNodeWithText("Save · 1 turn + 30 steps").assertIsDisplayed()
    }

    @Test fun singleTurnGrinderHidesTurnsStepper() {
        show(base.copy(grinderId = 1, setting = GrindSetting(0, 12)))
        compose.onNodeWithText("This grinder adjusts within a single turn: steps are enough.").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithContentDescription("More turns").fetchSemanticsNodes().size)
    }

    @Test fun saveDisabledWithoutCoffee() {
        show(base.copy(coffeeId = null))
        compose.onNodeWithText("Save · 1 turn + 30 steps").assertIsNotEnabled()
    }
}
```

- [ ] **Step 3: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.editor.*"'`
Expected: compilation FAILS.

- [ ] **Step 4: Implement the ViewModel**

`ui/editor/CalibrationEditorViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.navigation.NEW_GRINDER_RESULT_KEY
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    /** Most recently used first. */
    val coffees: List<Coffee> = emptyList(),
    val showAllCoffees: Boolean = false,
    val methods: List<Method> = emptyList(),
    val grinders: List<Grinder> = emptyList(),
    val coffeeId: Long? = null,
    val methodId: Long? = null,
    val grinderId: Long? = null,
    val setting: GrindSetting = GrindSetting.ZERO,
    val notes: String = "",
    /** Setting of another calibration that saving would overwrite. */
    val replaces: GrindSetting? = null,
    val coffeeNames: Set<String> = emptySet(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val deleted: Boolean = false,
) {
    val grinder: Grinder? get() = grinders.firstOrNull { it.id == grinderId }
    val canSave: Boolean
        get() = loaded && !saving && coffees.any { it.id == coffeeId } && methods.any { it.id == methodId } && grinder != null
    val visibleCoffees: List<Coffee>
        get() {
            if (showAllCoffees || coffees.size <= RECENT_COFFEES) return coffees
            val top = coffees.take(RECENT_COFFEES)
            val selected = coffees.firstOrNull { it.id == coffeeId }
            return if (selected != null && selected !in top) top + selected else top
        }
    val hiddenCoffeeCount: Int get() = coffees.size - visibleCoffees.size
    val maxSteps: Int? get() = grinder?.let { SettingRules.maxSteps(it.spec) }
    val turnsEnabled: Boolean get() = grinder?.spec?.multiTurn ?: true
    val canIncrementSteps: Boolean get() = maxSteps?.let { setting.steps < it } ?: true

    companion object {
        const val RECENT_COFFEES = 3
    }
}

class CalibrationEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val coffees: CoffeeRepository,
    methods: MethodRepository,
    grinders: GrinderRepository,
    private val calibrations: CalibrationRepository,
) : ViewModel() {
    private val editingId = savedStateHandle.idArg("calibrationId")
    private val presetCoffee = savedStateHandle.idArg("coffeeId")
    private val presetMethod = savedStateHandle.idArg("methodId")
    private val state = MutableStateFlow(EditorUiState(isEdit = editingId != null))
    val uiState: StateFlow<EditorUiState> = state.asStateFlow()
    private var pendingGrinderId: Long? = null
    private var replacesJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = editingId?.let { calibrations.observe(it).first() }
            combine(coffees.observeCoffees(), methods.observeMethods(), grinders.observeGrinders(), calibrations.observeAll()) { cs, ms, gs, cals ->
                Lists(cs, ms, gs, cals)
            }.collect { lists ->
                val firstLoad = !state.value.loaded
                state.update { s -> merge(s, lists, if (firstLoad) existing else null, firstLoad) }
                if (firstLoad) refreshReplaces()
            }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>(NEW_GRINDER_RESULT_KEY, null).filterNotNull().collect { id ->
                savedStateHandle[NEW_GRINDER_RESULT_KEY] = null
                pendingGrinderId = id
                state.update { s -> if (s.grinders.any { it.id == id }) selectGrinderIn(s, id).also { pendingGrinderId = null } else s }
                refreshReplaces()
            }
        }
    }

    private data class Lists(val coffees: List<Coffee>, val methods: List<Method>, val grinders: List<Grinder>, val calibrations: List<Calibration>)

    private fun merge(s: EditorUiState, l: Lists, existing: Calibration?, firstLoad: Boolean): EditorUiState {
        val lastUse = l.calibrations.groupBy { it.coffee.id }.mapValues { (_, v) -> v.maxOf { it.updatedAt } }
        val ordered = l.coffees.sortedByDescending { maxOf(lastUse[it.id] ?: 0L, it.createdAt) }
        var next = s.copy(
            loaded = true,
            coffees = ordered,
            methods = l.methods,
            grinders = l.grinders,
            coffeeNames = l.coffees.map { TextMatch.normalize(it.name) }.toSet(),
        )
        if (firstLoad) {
            next = if (existing != null) {
                next.copy(
                    coffeeId = existing.coffee.id, methodId = existing.method.id, grinderId = existing.grinder.id,
                    setting = existing.setting, notes = existing.notes,
                )
            } else {
                next.copy(
                    coffeeId = presetCoffee ?: l.coffees.singleOrNull()?.id,
                    methodId = presetMethod,
                    grinderId = l.grinders.singleOrNull()?.id,
                )
            }
        }
        val pending = pendingGrinderId
        if (pending != null && l.grinders.any { it.id == pending }) {
            pendingGrinderId = null
            next = selectGrinderIn(next, pending)
        }
        return next
    }

    private fun selectGrinderIn(s: EditorUiState, id: Long): EditorUiState {
        val g = s.grinders.firstOrNull { it.id == id }
        return s.copy(grinderId = id, setting = g?.let { SettingRules.clamp(s.setting, it.spec) } ?: s.setting)
    }

    private fun refreshReplaces() {
        val s = state.value
        val c = s.coffeeId
        val m = s.methodId
        val g = s.grinderId
        replacesJob?.cancel()
        if (c == null || m == null || g == null) {
            state.update { it.copy(replaces = null) }
            return
        }
        replacesJob = viewModelScope.launch {
            val existing = calibrations.findExisting(c, m, g)
            state.update { it.copy(replaces = existing?.takeIf { e -> e.id != editingId }?.setting) }
        }
    }

    fun selectCoffee(id: Long) {
        state.update { it.copy(coffeeId = id) }
        refreshReplaces()
    }

    fun selectMethod(id: Long) {
        state.update { it.copy(methodId = id) }
        refreshReplaces()
    }

    fun selectGrinder(id: Long) {
        state.update { selectGrinderIn(it, id) }
        refreshReplaces()
    }

    fun turnsUp() = updateSetting { it.copy(turns = it.turns + 1) }
    fun turnsDown() = updateSetting { it.copy(turns = it.turns - 1) }
    fun stepsUp() = updateSetting { it.copy(steps = it.steps + 1) }
    fun stepsDown() = updateSetting { it.copy(steps = it.steps - 1) }

    /** Steppers clamp the whole setting to the selected grinder; loading never clamps. */
    private fun updateSetting(transform: (GrindSetting) -> GrindSetting) = state.update { s ->
        val next = transform(s.setting)
        val clamped = s.grinder?.let { SettingRules.clamp(next, it.spec) }
            ?: GrindSetting(next.turns.coerceAtLeast(0), next.steps.coerceAtLeast(0))
        s.copy(setting = clamped)
    }

    fun onNotesChange(value: String) = state.update { it.copy(notes = value) }

    fun showAllCoffees() = state.update { it.copy(showAllCoffees = true) }

    fun addCoffee(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = coffees.add(name)
            state.update { it.copy(coffeeId = id) }
            refreshReplaces()
        }
    }

    fun save() {
        val s = state.value
        if (!s.canSave) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = calibrations.save(SaveCalibration(editingId, s.coffeeId!!, s.methodId!!, s.grinderId!!, s.setting, s.notes))
            state.update { it.copy(saving = false, savedId = id) }
        }
    }

    fun delete() {
        val id = editingId ?: return
        viewModelScope.launch {
            calibrations.delete(id)
            state.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                CalibrationEditorViewModel(createSavedStateHandle(), c.coffees, c.methods, c.grinders, c.calibrations)
            }
        }
    }
}
```

- [ ] **Step 5: Implement the screen**

`ui/editor/CalibrationEditorScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.ui.coffees.CoffeeSheet
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.DangerTextButton
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.PrimaryButton
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.Stepper
import it.matteocolombo.tacca.ui.components.TaccaTextField
import it.matteocolombo.tacca.ui.format.grinderKindText
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

data class EditorActions(
    val onClose: () -> Unit,
    val onSelectCoffee: (Long) -> Unit,
    val onSelectMethod: (Long) -> Unit,
    val onSelectGrinder: (Long) -> Unit,
    val onTurnsUp: () -> Unit,
    val onTurnsDown: () -> Unit,
    val onStepsUp: () -> Unit,
    val onStepsDown: () -> Unit,
    val onNotesChange: (String) -> Unit,
    val onShowAllCoffees: () -> Unit,
    val onAddCoffee: (String) -> Unit,
    val onNewGrinder: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
) {
    companion object {
        val Noop = EditorActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun CalibrationEditorEntry(onClose: () -> Unit, onNewGrinder: () -> Unit, onSaved: (Long, Boolean) -> Unit, onDeleted: () -> Unit) {
    val vm: CalibrationEditorViewModel = viewModel(factory = CalibrationEditorViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let { onSaved(it, state.isEdit) } }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    CalibrationEditorScreen(
        state,
        EditorActions(
            onClose = onClose,
            onSelectCoffee = vm::selectCoffee,
            onSelectMethod = vm::selectMethod,
            onSelectGrinder = vm::selectGrinder,
            onTurnsUp = vm::turnsUp,
            onTurnsDown = vm::turnsDown,
            onStepsUp = vm::stepsUp,
            onStepsDown = vm::stepsDown,
            onNotesChange = vm::onNotesChange,
            onShowAllCoffees = vm::showAllCoffees,
            onAddCoffee = vm::addCoffee,
            onNewGrinder = onNewGrinder,
            onSave = vm::save,
            onDelete = vm::delete,
        ),
    )
}

@Composable
fun CalibrationEditorScreen(state: EditorUiState, actions: EditorActions) {
    val c = TaccaTheme.colors
    var coffeeSheet by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (state.isEdit) R.string.editor_title_edit else R.string.editor_title_new),
                titleStyle = TaccaType.h1Small,
            ) { SquareIconButton(R.drawable.ic_close, stringResource(R.string.action_close), actions.onClose) }

            Section(stringResource(R.string.editor_section_coffee)) {
                if (state.coffees.isEmpty()) Hint(stringResource(R.string.editor_no_coffee))
                state.visibleCoffees.forEach { coffee ->
                    val selected = coffee.id == state.coffeeId
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                            .border(1.5.dp, if (selected) c.accent else c.outline, RoundedCornerShape(16.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectCoffee(coffee.id) })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).border(5.dp, if (selected) c.accent else c.tickInactive, CircleShape).background(c.surface))
                        Text(coffee.name, style = TaccaType.label, color = c.text)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.hiddenCoffeeCount > 0) {
                        TextButton(onClick = actions.onShowAllCoffees) {
                            Text(stringResource(R.string.action_show_all, state.coffees.size), style = TaccaType.label, color = c.accentText)
                        }
                    }
                    AddChip(stringResource(R.string.action_new)) { coffeeSheet = true }
                }
            }

            Section(stringResource(R.string.editor_section_method)) {
                state.methods.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            val selected = m.id == state.methodId
                            Column(
                                Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(18.dp)).background(c.tint(m.tint))
                                    .border(1.5.dp, if (selected) c.text else Color.Transparent, RoundedCornerShape(18.dp))
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectMethod(m.id) })
                                    .padding(horizontal = 2.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                            ) {
                                Icon(painterResource(m.icon.drawableRes()), contentDescription = null, tint = c.text, modifier = Modifier.size(26.dp))
                                Text(methodName(m), style = TaccaType.unit.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = c.text, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Section(stringResource(R.string.editor_section_grinder)) {
                if (state.grinders.isEmpty()) Hint(stringResource(R.string.editor_no_grinder))
                state.grinders.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { g ->
                            val selected = g.id == state.grinderId
                            Column(
                                Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                                    .border(1.5.dp, if (selected) c.text else c.outline, RoundedCornerShape(16.dp))
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelectGrinder(g.id) })
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            ) {
                                Text(g.name, style = TaccaType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.text)
                                Text(grinderKindText(g.spec), style = TaccaType.bodySmall.copy(fontSize = 12.sp), color = c.textMuted)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                AddChip(stringResource(R.string.action_new), actions.onNewGrinder)
            }

            Section(stringResource(R.string.editor_section_adjustment)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdjustCard(
                        title = stringResource(R.string.editor_turns),
                        hint = stringResource(R.string.editor_optional),
                        highlighted = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.turnsEnabled) {
                            Stepper(
                                value = state.setting.turns.toString(),
                                onDecrement = actions.onTurnsDown, onIncrement = actions.onTurnsUp,
                                decrementLabel = stringResource(R.string.editor_fewer_turns),
                                incrementLabel = stringResource(R.string.editor_more_turns),
                                canDecrement = state.setting.turns > 0,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(stringResource(R.string.editor_single_turn_info), style = TaccaType.bodySmall, color = c.textMuted)
                        }
                    }
                    val max = state.maxSteps
                    AdjustCard(
                        title = stringResource(R.string.editor_steps),
                        hint = when {
                            max == null -> stringResource(R.string.editor_steps_unbounded)
                            state.turnsEnabled -> stringResource(R.string.editor_steps_range_after_turns, 0, max)
                            else -> stringResource(R.string.editor_steps_range, 0, max)
                        },
                        highlighted = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Stepper(
                            value = state.setting.steps.toString(),
                            onDecrement = actions.onStepsDown, onIncrement = actions.onStepsUp,
                            decrementLabel = stringResource(R.string.editor_fewer_steps),
                            incrementLabel = stringResource(R.string.editor_more_steps),
                            canDecrement = state.setting.steps > 0,
                            canIncrement = state.canIncrementSteps,
                            valueColor = c.accentText,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            TaccaTextField(
                value = state.notes,
                onValueChange = actions.onNotesChange,
                label = stringResource(R.string.editor_section_notes),
                placeholder = stringResource(R.string.editor_notes_hint),
                singleLine = false,
                minLines = 2,
                imeAction = androidx.compose.ui.text.input.ImeAction.Default,
            )

            if (state.isEdit) DangerTextButton(stringResource(R.string.editor_delete), { confirmDelete = true })
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.replaces?.let {
                Text(
                    stringResource(R.string.editor_replaces, settingText(it)),
                    style = TaccaType.bodySmall, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(stringResource(R.string.editor_save_summary, settingText(state.setting)), actions.onSave, enabled = state.canSave)
        }
    }

    if (coffeeSheet) {
        CoffeeSheet(
            title = stringResource(R.string.coffee_sheet_title_new),
            initialName = "",
            existingNames = state.coffeeNames,
            onSave = { actions.onAddCoffee(it); coffeeSheet = false },
            onDismiss = { coffeeSheet = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.editor_delete_confirm),
            text = null,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(title)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = TaccaType.bodySmall, color = TaccaTheme.colors.textMuted, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.heightIn(min = 40.dp).clip(CircleShape).background(c.surface).border(1.dp, c.outline, CircleShape)
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
        Text(label, style = TaccaType.label, color = c.text)
    }
}

@Composable
private fun AdjustCard(title: String, hint: String, highlighted: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val c = TaccaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.heightIn(min = 112.dp).clip(shape).background(c.surface)
            .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) c.accent else c.outline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, style = TaccaType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.text)
            Text(hint, style = TaccaType.unit.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = c.textMuted)
        }
        content()
    }
}
```

- [ ] **Step 6: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.editor.*"'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add calibration editor

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 14: Calibration detail

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/detail/DetailViewModel.kt`, `DetailScreen.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/detail/DetailViewModelTest.kt`, `DetailScreenTest.kt`

**Interfaces:**
- Consumes: `CalibrationRepository`, `Dial`, components, labels, `drawableRes()`, `idArg`.
- Produces:
  - `DetailViewModel(savedStateHandle, calibrations)` reading `"calibrationId"`; `data class DetailUiState(loading: Boolean = true, calibration: Calibration? = null, sameCoffee: List<Calibration> = emptyList())` with `notFound`
  - `@Composable fun DetailEntry(onBack: () -> Unit, onEdit: (Long) -> Unit, onOpen: (Long) -> Unit)`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/detail/DetailViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    @Test fun sameCoffeeExcludesSelfAndOtherCoffees() = runTest {
        val a = r.coffees.add("Etiopia")
        val b = r.coffees.add("Brasile")
        val g = r.grinders.add("JX", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val aero = r.builtInMethod(BuiltInMethod.AEROPRESS).id
        val self = r.calibrations.save(SaveCalibration(null, a, v60, g, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, a, aero, g, GrindSetting(0, 16), ""))
        r.calibrations.save(SaveCalibration(null, b, v60, g, GrindSetting(1, 1), ""))
        val vm = DetailViewModel(SavedStateHandle(mapOf("calibrationId" to self)), r.calibrations)
        val s = vm.uiState.first { !it.loading && it.sameCoffee.isNotEmpty() }
        assertEquals(self, s.calibration!!.id)
        assertEquals(listOf(BuiltInMethod.AEROPRESS), s.sameCoffee.map { it.method.builtIn })
    }

    @Test fun deletedCalibrationIsNotFound() = runTest {
        val vm = DetailViewModel(SavedStateHandle(mapOf("calibrationId" to 99L)), r.calibrations)
        assertTrue(vm.uiState.first { !it.loading }.notFound)
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/detail/DetailScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun cal(setting: GrindSetting, stepsPerTurn: Int? = 40) = Calibration(
        id = 1,
        coffee = Coffee(1, "Etiopia Yirgacheffe Kochere — lavato", 0),
        method = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2),
        grinder = Grinder(1, "1Zpresso JX-Pro", GrinderSpec(true, stepsPerTurn), 0),
        setting = setting, notes = "", updatedAt = 0,
    )

    private fun show(c: Calibration) = compose.setContent {
        TaccaTheme { DetailScreen(DetailUiState(loading = false, calibration = c), {}, {}, {}) }
    }

    @Test fun showsThreeStepTiles() {
        show(cal(GrindSetting(2, 12)))
        compose.onNodeWithText("Close to zero").assertIsDisplayed()
        compose.onNodeWithText("Turn 2 full turns").assertIsDisplayed()
        compose.onNodeWithText("Add 12 steps").assertIsDisplayed()
    }

    @Test fun zeroTurnsHidesTurnsTile() {
        show(cal(GrindSetting(0, 5)))
        assertEquals(0, compose.onAllNodesWithText("Turn 0 full turns").fetchSemanticsNodes().size)
        compose.onNodeWithText("Add 5 steps").assertIsDisplayed()
    }

    @Test fun unknownStepsPerTurnShowsSetting() {
        show(cal(GrindSetting(1, 17), stepsPerTurn = null))
        compose.onNodeWithContentDescription("Dial: 1 turn + 17 steps").assertIsDisplayed()
        compose.onNodeWithText("Add 17 steps").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.detail.*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Implement**

`ui/detail/DetailViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val loading: Boolean = true,
    val calibration: Calibration? = null,
    val sameCoffee: List<Calibration> = emptyList(),
) {
    val notFound: Boolean get() = !loading && calibration == null
}

class DetailViewModel(savedStateHandle: SavedStateHandle, calibrations: CalibrationRepository) : ViewModel() {
    private val id: Long = savedStateHandle.idArg("calibrationId") ?: -1L

    val uiState: StateFlow<DetailUiState> =
        combine(calibrations.observe(id), calibrations.observeAll()) { current, all ->
            DetailUiState(
                loading = false,
                calibration = current,
                sameCoffee = if (current == null) emptyList() else all
                    .filter { it.coffee.id == current.coffee.id && it.id != current.id }
                    .sortedWith(compareBy({ it.method.sortOrder }, { it.grinder.name })),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                DetailViewModel(createSavedStateHandle(), c.calibrations)
            }
        }
    }
}
```

`ui/detail/DetailScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.ui.components.Dial
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.GroupedList
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.format.settingText
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

@Composable
fun DetailEntry(onBack: () -> Unit, onEdit: (Long) -> Unit, onOpen: (Long) -> Unit) {
    val vm: DetailViewModel = viewModel(factory = DetailViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    DetailScreen(state, onBack, onEdit, onOpen)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(state: DetailUiState, onBack: () -> Unit, onEdit: (Long) -> Unit, onOpen: (Long) -> Unit) {
    val c = TaccaTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val cal = state.calibration
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_back), onBack)
            if (cal != null) {
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
                        .border(1.dp, c.outline, RoundedCornerShape(16.dp)).clickable { onEdit(cal.id) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.action_edit), style = TaccaType.label, color = c.text)
                }
            }
        }
        if (state.notFound) {
            EmptyState(R.drawable.ic_tab_calibrations, stringResource(R.string.detail_not_found), null)
            return@Column
        }
        if (cal == null) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(cal.coffee.name, style = TaccaType.h1Small, color = c.text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderChip(cal.method.icon.drawableRes(), methodName(cal.method), c.tint(cal.method.tint), bordered = false)
                HeaderChip(R.drawable.ic_grinder, cal.grinder.name, c.surface, bordered = true)
            }
        }

        SurfaceCard(Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Dial(cal.setting, cal.grinder.spec.stepsPerTurn)
                val tiles = buildList {
                    add(stringResource(R.string.detail_step_zero))
                    if (cal.setting.turns > 0) add(pluralStringResource(R.plurals.detail_step_turns, cal.setting.turns, cal.setting.turns))
                    add(pluralStringResource(R.plurals.detail_step_steps, cal.setting.steps, cal.setting.steps))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiles.forEachIndexed { i, text ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (i == tiles.lastIndex) c.highlight else c.background).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("${i + 1}", style = TaccaType.numberS.copy(fontSize = 13.sp), color = c.textMuted)
                            Text(text, style = TaccaType.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                        }
                    }
                }
            }
        }

        if (cal.notes.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(stringResource(R.string.detail_notes))
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(cal.notes, style = TaccaType.body, color = c.text, modifier = Modifier.padding(16.dp))
                }
            }
        }

        if (state.sameCoffee.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(stringResource(R.string.detail_same_coffee))
                GroupedList(Modifier.fillMaxWidth()) {
                    state.sameCoffee.forEach { other -> SameCoffeeRow(other) { onOpen(other.id) } }
                }
            }
        }
    }
}

@Composable
private fun HeaderChip(icon: Int, text: String, background: Color, bordered: Boolean) {
    val c = TaccaTheme.colors
    Row(
        Modifier.height(34.dp).clip(CircleShape).background(background)
            .then(if (bordered) Modifier.border(1.dp, c.outline, CircleShape) else Modifier)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = c.text, modifier = Modifier.size(20.dp))
        Text(text, style = TaccaType.label, color = c.text)
    }
}

@Composable
private fun SameCoffeeRow(cal: Calibration, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(c.surface).clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TintedIconBox(cal.method.icon.drawableRes(), c.tint(cal.method.tint), boxSize = 36.dp, iconSize = 20.dp, radius = 12.dp)
        Row(Modifier.weight(1f)) {
            Text("${methodName(cal.method)} · ", style = TaccaType.label, color = c.text)
            Text(cal.grinder.name, style = TaccaType.label.copy(fontWeight = FontWeight.Medium), color = c.textMuted, maxLines = 1)
        }
        Text(settingText(cal.setting), style = TaccaType.numberS, color = c.text)
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.detail.*"'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add calibration detail with dial

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 15: Home (calibrations list)

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/home/HomeViewModel.kt`, `HomeScreen.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/home/HomeViewModelTest.kt`, `HomeScreenTest.kt`

**Interfaces:**
- Consumes: all repositories, `HomeFilterStore`, `SettingRules.showsTurns`, `TextMatch`, components, labels, `drawableRes()`.
- Produces:
  - `HomeViewModel(calibrations, methods, coffees, grinders, homeFilter, methodLabel: (Method) -> String)`; `data class HomeGroup(coffee: Coffee, rows: List<Calibration>)`; `data class HomeUiState(...)` (see code); actions `selectMethod(Long?)`, `clearCoffeeFilter()`, `openSearch()`, `closeSearch()`, `onQueryChange(String)`, `clearFilters()`; read-only `currentFilter: HomeFilter`
  - `@Composable fun HomeEntry(onOpen: (Long) -> Unit, onNew: (coffeeId: Long?, methodId: Long?) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/home/HomeViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()

    @After fun tearDown() = r.db.close()

    private fun vm() = HomeViewModel(r.calibrations, r.methods, r.coffees, r.grinders, r.homeFilter) { m ->
        m.customName ?: when (m.builtIn) {
            BuiltInMethod.FRENCH_PRESS -> "French press"
            else -> m.builtIn?.name.orEmpty()
        }
    }

    private suspend fun seed(): Map<String, Long> {
        val etiopia = r.coffees.add("Etiopia")
        val brasile = r.coffees.add("Brasile")
        val jx = r.grinders.add("JX-Pro", GrinderSpec(true, 40))
        val v60 = r.builtInMethod(BuiltInMethod.V60).id
        val french = r.builtInMethod(BuiltInMethod.FRENCH_PRESS).id
        val moka = r.builtInMethod(BuiltInMethod.MOKA).id
        r.calibrations.save(SaveCalibration(null, etiopia, french, jx, GrindSetting(3, 5), ""))
        r.calibrations.save(SaveCalibration(null, etiopia, v60, jx, GrindSetting(2, 12), ""))
        r.calibrations.save(SaveCalibration(null, brasile, moka, jx, GrindSetting(1, 30), ""))
        return mapOf("etiopia" to etiopia, "brasile" to brasile, "v60" to v60, "moka" to moka)
    }

    @Test fun groupsByCoffeeMostRecentFirstAndRowsByMethodOrder() = runTest {
        seed()
        val s = vm().uiState.first { !it.loading && it.groups.size == 2 }
        assertEquals(listOf("Brasile", "Etiopia"), s.groups.map { it.coffee.name })
        assertEquals(listOf(BuiltInMethod.V60, BuiltInMethod.FRENCH_PRESS), s.groups[1].rows.map { it.method.builtIn })
        assertEquals(listOf(BuiltInMethod.MOKA, BuiltInMethod.V60, BuiltInMethod.FRENCH_PRESS), s.chips.map { it.builtIn })
    }

    @Test fun methodFilterAndSearch() = runTest {
        val ids = seed()
        val vm = vm()
        vm.selectMethod(ids.getValue("v60"))
        val filtered = vm.uiState.first { it.selectedMethodId == ids.getValue("v60") && !it.loading }
        assertEquals(1, filtered.groups.sumOf { it.rows.size })
        vm.clearFilters()
        vm.onQueryChange("french")
        val searched = vm.uiState.first { it.query == "french" }
        assertEquals(listOf("Etiopia"), searched.groups.map { it.coffee.name })
        assertTrue(searched.searchOpen)
    }

    @Test fun filterOnDeletedCoffeeIsIgnored() = runTest {
        val ids = seed()
        val vm = vm()
        r.homeFilter.showCoffee(ids.getValue("etiopia"))
        vm.uiState.first { it.coffeeFilter?.id == ids.getValue("etiopia") }
        r.coffees.delete(ids.getValue("etiopia"))
        val s = vm.uiState.first { it.groups.size == 1 && it.coffeeFilter == null }
        assertNull(s.coffeeFilter)
        assertEquals("Brasile", s.groups.single().coffee.name)
    }

    @Test fun needsSetupWithoutGrinders() = runTest {
        r.coffees.add("Etiopia")
        val s = vm().uiState.first { !it.loading }
        assertTrue(s.needsSetup)
        assertEquals(false, s.hasCalibrations)
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/home/HomeScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    private val v60 = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2)

    private fun show(state: HomeUiState, onNew: () -> Unit = {}) = compose.setContent {
        TaccaTheme { HomeScreen(state, HomeActions.Noop.copy(onNew = onNew)) }
    }

    @Test fun emptyStateInvitesToStart() {
        var clicked = false
        show(HomeUiState(loading = false, needsSetup = true)) { clicked = true }
        compose.onNodeWithText("No calibrations yet").assertIsDisplayed()
        compose.onNodeWithText("New calibration").performClick()
        assertTrue(clicked)
    }

    @Test fun longCoffeeNameKeepsSettingVisible() {
        val longName = "Colombia Huila Finca El Paraíso — lavato, lotto speciale doppia fermentazione anaerobica con frutta tropicale"
        val cal = Calibration(
            1, Coffee(1, longName, 0), v60, Grinder(1, "1Zpresso JX-Pro con un nome molto lungo", GrinderSpec(true, 40), 0),
            GrindSetting(2, 12), "", 0,
        )
        show(HomeUiState(loading = false, hasCalibrations = true, chips = listOf(v60), groups = listOf(HomeGroup(cal.coffee, listOf(cal)))))
        compose.onNodeWithText(longName).assertIsDisplayed()
        compose.onNodeWithText("12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun fabHasDescription() {
        show(HomeUiState(loading = false, hasCalibrations = true))
        compose.onNodeWithContentDescription("New calibration").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.home.*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Implement**

`ui/home/HomeViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.format.methodName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeGroup(val coffee: Coffee, val rows: List<Calibration>)

data class HomeUiState(
    val loading: Boolean = true,
    val hasCalibrations: Boolean = false,
    /** No coffee or no grinder exists yet. */
    val needsSetup: Boolean = false,
    val chips: List<Method> = emptyList(),
    val selectedMethodId: Long? = null,
    val coffeeFilter: Coffee? = null,
    val query: String = "",
    val searchOpen: Boolean = false,
    val groups: List<HomeGroup> = emptyList(),
)

class HomeViewModel(
    calibrations: CalibrationRepository,
    methods: MethodRepository,
    coffees: CoffeeRepository,
    grinders: GrinderRepository,
    private val homeFilter: HomeFilterStore,
    private val methodLabel: (Method) -> String,
) : ViewModel() {
    private val searchOpen = MutableStateFlow(false)
    private val filterAndSearch = combine(homeFilter.filter, searchOpen) { f, open -> f to open }

    val uiState: StateFlow<HomeUiState> =
        combine(calibrations.observeAll(), methods.observeMethods(), coffees.observeCoffees(), grinders.observeGrinders(), filterAndSearch) { cals, ms, cs, gs, (f, open) ->
            // Filters pointing at deleted data are ignored (e.g. after a delete or an import).
            val methodId = f.methodId?.takeIf { id -> ms.any { it.id == id } }
            val coffee = f.coffeeId?.let { id -> cs.firstOrNull { it.id == id } }
            val used = cals.map { it.method.id }.toSet()
            val filtered = cals.filter {
                (methodId == null || it.method.id == methodId) &&
                    (coffee == null || it.coffee.id == coffee.id) &&
                    TextMatch.matches(f.query, it.coffee.name, methodLabel(it.method), it.grinder.name)
            }
            val groups = filtered.groupBy { it.coffee.id }.values
                .map { rows -> HomeGroup(rows.first().coffee, rows.sortedWith(compareBy({ it.method.sortOrder }, { it.grinder.name }))) }
                .sortedByDescending { g -> g.rows.maxOf { it.updatedAt } }
            HomeUiState(
                loading = false,
                hasCalibrations = cals.isNotEmpty(),
                needsSetup = cs.isEmpty() || gs.isEmpty(),
                chips = ms.filter { it.id in used || it.id == methodId },
                selectedMethodId = methodId,
                coffeeFilter = coffee,
                query = f.query,
                searchOpen = open || f.query.isNotEmpty(),
                groups = groups,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val currentFilter: HomeFilter get() = homeFilter.filter.value

    fun selectMethod(id: Long?) = homeFilter.update { it.copy(methodId = id) }
    fun clearCoffeeFilter() = homeFilter.update { it.copy(coffeeId = null) }
    fun onQueryChange(query: String) = homeFilter.update { it.copy(query = query) }
    fun clearFilters() = homeFilter.update { HomeFilter() }

    fun openSearch() {
        searchOpen.value = true
    }

    fun closeSearch() {
        searchOpen.value = false
        homeFilter.update { it.copy(query = "") }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication
                val c = app.container
                HomeViewModel(c.calibrations, c.methods, c.coffees, c.grinders, c.homeFilter) { methodName(app.resources, it) }
            }
        }
    }
}
```

`ui/home/HomeScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.ui.components.BrandMark
import it.matteocolombo.tacca.ui.components.EmptyState
import it.matteocolombo.tacca.ui.components.FilterPill
import it.matteocolombo.tacca.ui.components.GroupedList
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SearchField
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.TintedIconBox
import it.matteocolombo.tacca.ui.components.topLevelContentPadding
import it.matteocolombo.tacca.ui.format.methodName
import it.matteocolombo.tacca.ui.icons.drawableRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType

data class HomeActions(
    val onOpen: (Long) -> Unit,
    val onNew: () -> Unit,
    val onSettings: () -> Unit,
    val onSelectMethod: (Long?) -> Unit,
    val onClearCoffeeFilter: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClearFilters: () -> Unit,
) {
    companion object {
        val Noop = HomeActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun HomeEntry(onOpen: (Long) -> Unit, onNew: (Long?, Long?) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state,
        HomeActions(
            onOpen = onOpen,
            onNew = { onNew(state.coffeeFilter?.id, state.selectedMethodId) },
            onSettings = onSettings,
            onSelectMethod = vm::selectMethod,
            onClearCoffeeFilter = vm::clearCoffeeFilter,
            onOpenSearch = vm::openSearch,
            onCloseSearch = vm::closeSearch,
            onQueryChange = vm::onQueryChange,
            onClearFilters = vm::clearFilters,
        ),
        modifier,
    )
}

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val c = TaccaTheme.colors
    val base = topLevelContentPadding()
    Box(modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = base.calculateTopPadding(), bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    if (state.searchOpen) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_close), actions.onCloseSearch)
                            SearchField(
                                query = state.query,
                                onQueryChange = actions.onQueryChange,
                                placeholder = stringResource(R.string.home_search_hint),
                                label = stringResource(R.string.action_search),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        ScreenHeader(title = stringResource(R.string.home_title), top = { BrandMark() }) {
                            SquareIconButton(R.drawable.ic_search, stringResource(R.string.action_search), actions.onOpenSearch)
                            SquareIconButton(R.drawable.ic_settings, stringResource(R.string.action_settings), actions.onSettings)
                        }
                    }
                }
            }
            if (state.hasCalibrations) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.coffeeFilter?.let { coffee ->
                            item {
                                val removeLabel = stringResource(R.string.home_remove_coffee_filter)
                                FilterPill(
                                    label = coffee.name, selected = true, onClick = actions.onClearCoffeeFilter,
                                    leadingIcon = R.drawable.ic_bean, trailingIcon = R.drawable.ic_close,
                                    modifier = Modifier.widthIn(max = 240.dp).semantics { contentDescription = removeLabel },
                                )
                            }
                        }
                        item {
                            FilterPill(stringResource(R.string.home_filter_all), state.selectedMethodId == null, { actions.onSelectMethod(null) })
                        }
                        items(state.chips, key = { it.id }) { m ->
                            FilterPill(methodName(m), state.selectedMethodId == m.id, { actions.onSelectMethod(m.id) })
                        }
                    }
                }
            }
            when {
                state.loading -> Unit
                !state.hasCalibrations -> item {
                    EmptyState(
                        icon = R.drawable.ic_tab_calibrations,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body) + if (state.needsSetup) "\n" + stringResource(R.string.home_empty_setup) else "",
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = stringResource(R.string.home_new_calibration),
                        onAction = actions.onNew,
                    )
                }
                state.groups.isEmpty() -> item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.home_no_results), style = TaccaType.title, color = c.textMuted)
                        TextButton(onClick = actions.onClearFilters) {
                            Text(stringResource(R.string.home_clear_filters), style = TaccaType.label, color = c.accentText)
                        }
                    }
                }
            }
            items(state.groups, key = { it.coffee.id }) { group ->
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.ic_bean), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                        Text(group.coffee.name, style = TaccaType.title, color = c.text)
                    }
                    GroupedList(Modifier.fillMaxWidth()) {
                        group.rows.forEach { cal -> CalibrationRow(cal) { actions.onOpen(cal.id) } }
                    }
                }
            }
        }
        if (state.hasCalibrations) {
            val fabLabel = stringResource(R.string.home_new_calibration)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp).size(60.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x8C5A280F), spotColor = Color(0x8C5A280F))
                    .clip(RoundedCornerShape(22.dp)).background(c.accent)
                    .clickable(role = Role.Button, onClick = actions.onNew)
                    .semantics { contentDescription = fabLabel },
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = c.onAccent, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun CalibrationRow(cal: Calibration, onClick: () -> Unit) {
    val c = TaccaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(c.surface).clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TintedIconBox(cal.method.icon.drawableRes(), c.tint(cal.method.tint))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(methodName(cal.method), style = TaccaType.rowTitle, color = c.text)
            Text(cal.grinder.name, style = TaccaType.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (SettingRules.showsTurns(cal.setting, cal.grinder.spec)) {
                ValueColumn(cal.setting.turns.toString(), stringResource(R.string.unit_turns), c.text)
                Box(Modifier.width(1.dp).height(28.dp).background(c.outline))
            }
            ValueColumn(cal.setting.steps.toString(), stringResource(R.string.unit_steps), c.accentText)
        }
    }
}

@Composable
private fun ValueColumn(value: String, unit: String, color: Color) {
    Column(Modifier.widthIn(min = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = TaccaType.numberM, color = color)
        Text(unit.uppercase(), style = TaccaType.unit, color = TaccaTheme.colors.textMuted)
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.home.*"'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add home screen with filters and search

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 16: Settings screen (accent, theme, language, export/import)

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`, `AppLanguage.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/ui/settings/SettingsViewModelTest.kt`, `SettingsScreenTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository`, `BackupService`, `BackupException`, `BackupError`, `DocumentStreams`, `HomeFilterStore`, labels (`labelRes`, `messageRes`), components.
- Produces:
  - `sealed interface SettingsMessage { data object Exported; data object Imported; data class Failed(val error: BackupError) }`
  - `data class SettingsUiState(settings: AppSettings = AppSettings(), busy: Boolean = false, message: SettingsMessage? = null)`
  - `SettingsViewModel(settings, backup, documents, homeFilter)` with `setAccent`, `setThemeMode`, `export(uri)`, `import(uri)`, `consumeMessage()`
  - `object AppLanguage { supported; current(): String?; set(tag: String?); labelRes(tag: String?): Int; systemSettingsIntent(context): Intent? }`
  - `@Composable fun SettingsEntry(onBack: () -> Unit)`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/it/matteocolombo/tacca/ui/settings/SettingsViewModelTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.settings

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.data.backup.BackupService
import it.matteocolombo.tacca.data.backup.DocumentStreams
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.settings.SettingsRepository
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.testutil.MainDispatcherRule
import it.matteocolombo.tacca.testutil.TestRepos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val r = TestRepos()
    private val uri = Uri.parse("content://test/backup.json")

    @After fun tearDown() = r.db.close()

    private class FakeSettings : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings = state
        override suspend fun setAccent(accent: AccentOption) = state.update { it.copy(accent = accent) }
        override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    }

    private class FakeDocuments(var input: ByteArray? = null) : DocumentStreams {
        val output = ByteArrayOutputStream()
        override fun openInput(uri: Uri): InputStream? = input?.let(::ByteArrayInputStream)
        override fun openOutput(uri: Uri): OutputStream = output
    }

    private val settings = FakeSettings()

    private fun vm(docs: DocumentStreams) = SettingsViewModel(settings, BackupService(r.db), docs, r.homeFilter)

    @Test fun changesAccentAndTheme() = runTest {
        val vm = vm(FakeDocuments())
        vm.setAccent(AccentOption.PINE)
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(AppSettings(AccentOption.PINE, ThemeMode.DARK), vm.uiState.first { it.settings.themeMode == ThemeMode.DARK }.settings)
    }

    @Test fun exportWritesABackup() = runTest {
        r.coffees.add("Brasile")
        val docs = FakeDocuments()
        val vm = vm(docs)
        vm.export(uri)
        assertEquals(SettingsMessage.Exported, vm.uiState.first { it.message != null }.message)
        assertTrue(docs.output.toString(Charsets.UTF_8).contains("\"tacca-backup\""))
    }

    @Test fun importReplacesDataAndResetsHomeFilter() = runTest {
        r.coffees.add("Brasile")
        val docs = FakeDocuments()
        vm(docs).also { it.export(uri) }.uiState.first { it.message == SettingsMessage.Exported }
        r.coffees.add("Extra")
        r.homeFilter.showCoffee(42)
        docs.input = docs.output.toByteArray()
        val vm = vm(docs)
        vm.import(uri)
        assertEquals(SettingsMessage.Imported, vm.uiState.first { it.message != null }.message)
        assertEquals(listOf("Brasile"), r.coffees.observeCoffees().first().map { it.name })
        assertEquals(HomeFilter(), r.homeFilter.filter.value)
    }

    @Test fun invalidImportReportsError() = runTest {
        val vm = vm(FakeDocuments(input = "not json".toByteArray()))
        vm.import(uri)
        assertEquals(SettingsMessage.Failed(BackupError.NOT_A_BACKUP), vm.uiState.first { it.message != null }.message)
        vm.consumeMessage()
        assertEquals(null, vm.uiState.first { it.message == null }.message)
    }

    @Test fun unreadableDocumentIsAnIoError() = runTest {
        val vm = vm(FakeDocuments(input = null))
        vm.import(uri)
        assertEquals(SettingsMessage.Failed(BackupError.IO), vm.uiState.first { it.message != null }.message)
    }
}
```

`app/src/test/java/it/matteocolombo/tacca/ui/settings/SettingsScreenTest.kt`:

```kotlin
package it.matteocolombo.tacca.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pickingThemeAndAccentCallsBack() {
        var theme: ThemeMode? = null
        var accent: AccentOption? = null
        compose.setContent {
            TaccaTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                    languageLabel = "System default",
                    versionName = "1.0.0",
                    actions = SettingsActions.Noop.copy(onTheme = { theme = it }, onAccent = { accent = it }),
                )
            }
        }
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithContentDescription("Plum").performClick()
        compose.onNodeWithText("Version 1.0.0").assertIsDisplayed()
        assertEquals(ThemeMode.DARK, theme)
        assertEquals(AccentOption.PLUM, accent)
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.settings.*"'`
Expected: compilation FAILS.

- [ ] **Step 3: Implement**

`ui/settings/AppLanguage.kt`:

```kotlin
package it.matteocolombo.tacca.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import it.matteocolombo.tacca.R

/** Per-app language via AppCompat (persisted on API < 33 by AppLocalesMetadataHolderService). */
object AppLanguage {
    val supported = listOf("en", "it", "es", "fr", "de")

    /** Language tag chosen for the app, or null when following the system. */
    fun current(): String? = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }?.get(0)?.language

    fun set(tag: String?) = AppCompatDelegate.setApplicationLocales(
        if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
    )

    @StringRes
    fun labelRes(tag: String?): Int = when (tag) {
        "en" -> R.string.language_en
        "it" -> R.string.language_it
        "es" -> R.string.language_es
        "fr" -> R.string.language_fr
        "de" -> R.string.language_de
        else -> R.string.language_system
    }

    /** System per-app language screen (API 33+), or null to use the in-app picker. */
    fun systemSettingsIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
        } else {
            null
        }
}
```

`ui/settings/SettingsViewModel.kt`:

```kotlin
package it.matteocolombo.tacca.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.backup.BackupError
import it.matteocolombo.tacca.data.backup.BackupException
import it.matteocolombo.tacca.data.backup.BackupService
import it.matteocolombo.tacca.data.backup.DocumentStreams
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.settings.SettingsRepository
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface SettingsMessage {
    data object Exported : SettingsMessage
    data object Imported : SettingsMessage
    data class Failed(val error: BackupError) : SettingsMessage
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: SettingsMessage? = null,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupService,
    private val documents: DocumentStreams,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<SettingsMessage?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.settings, busy, message) { s, b, m -> SettingsUiState(s, b, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setAccent(accent: AccentOption) {
        viewModelScope.launch { settings.setAccent(accent) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun export(uri: Uri) = runBackup(SettingsMessage.Exported) {
        val out = documents.openOutput(uri) ?: throw BackupException(BackupError.IO)
        out.use { backup.export(it) }
    }

    fun import(uri: Uri) = runBackup(SettingsMessage.Imported) {
        val input = documents.openInput(uri) ?: throw BackupException(BackupError.IO)
        input.use { backup.import(it) }
        // Ids may have changed: drop filters that point at old data.
        homeFilter.update { HomeFilter() }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun runBackup(success: SettingsMessage, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            message.value = try {
                block()
                success
            } catch (e: BackupException) {
                SettingsMessage.Failed(e.error)
            } catch (e: IOException) {
                SettingsMessage.Failed(BackupError.IO)
            } catch (e: SecurityException) {
                SettingsMessage.Failed(BackupError.IO)
            } finally {
                busy.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                SettingsViewModel(c.settings, c.backup, c.documents, c.homeFilter)
            }
        }
    }
}
```

`ui/settings/SettingsScreen.kt`:

```kotlin
package it.matteocolombo.tacca.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.ui.components.ConfirmDialog
import it.matteocolombo.tacca.ui.components.FilterPill
import it.matteocolombo.tacca.ui.components.Legend
import it.matteocolombo.tacca.ui.components.ScreenHeader
import it.matteocolombo.tacca.ui.components.SquareIconButton
import it.matteocolombo.tacca.ui.components.SurfaceCard
import it.matteocolombo.tacca.ui.format.labelRes
import it.matteocolombo.tacca.ui.format.messageRes
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import it.matteocolombo.tacca.ui.theme.TaccaType
import java.time.LocalDate

data class SettingsActions(
    val onBack: () -> Unit,
    val onAccent: (AccentOption) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onLanguage: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
) {
    companion object {
        val Noop = SettingsActions({}, {}, {}, {}, {}, {})
    }
}

@Composable
fun SettingsEntry(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val res = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri?.toString()
    }
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    LaunchedEffect(state.message) {
        val text = when (val m = state.message) {
            null -> return@LaunchedEffect
            SettingsMessage.Exported -> res.getString(R.string.export_done)
            SettingsMessage.Imported -> res.getString(R.string.import_done)
            is SettingsMessage.Failed -> res.getString(m.error.messageRes())
        }
        vm.consumeMessage()
        snackbar.showSnackbar(text)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = TaccaTheme.colors.background) { inner ->
        Box(Modifier.padding(inner)) {
            SettingsScreen(
                state = state,
                languageLabel = stringResource(AppLanguage.labelRes(AppLanguage.current())),
                versionName = versionName,
                actions = SettingsActions(
                    onBack = onBack,
                    onAccent = vm::setAccent,
                    onTheme = vm::setThemeMode,
                    onLanguage = {
                        val intent = AppLanguage.systemSettingsIntent(context)
                        if (intent != null) context.startActivity(intent) else languageDialog = true
                    },
                    onExport = { exportLauncher.launch(res.getString(R.string.backup_file_name, LocalDate.now().toString())) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                ),
            )
        }
    }

    pendingImport?.let { uriString ->
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            text = stringResource(R.string.import_confirm_body),
            confirmLabel = stringResource(R.string.import_confirm_action),
            onConfirm = { pendingImport = null; vm.import(android.net.Uri.parse(uriString)) },
            onDismiss = { pendingImport = null },
        )
    }
    if (languageDialog) {
        LanguageDialog(current = AppLanguage.current(), onPick = { AppLanguage.set(it); languageDialog = false }, onDismiss = { languageDialog = false })
    }
}

@Composable
fun SettingsScreen(state: SettingsUiState, languageLabel: String, versionName: String, actions: SettingsActions) {
    val c = TaccaTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SquareIconButton(R.drawable.ic_back, stringResource(R.string.action_back), actions.onBack)
            ScreenHeader(stringResource(R.string.settings_title), titleStyle = TaccaType.h1Small)
        }

        Section(stringResource(R.string.settings_accent)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentOption.entries.forEach { option ->
                    val selected = option == state.settings.accent
                    val label = stringResource(option.labelRes())
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color(option.argb))
                            .border(3.dp, if (selected) c.text else Color.Transparent, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onAccent(option) })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Section(stringResource(R.string.settings_theme)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterPill(stringResource(mode.labelRes()), mode == state.settings.themeMode, { actions.onTheme(mode) })
                }
            }
        }

        Section(stringResource(R.string.settings_language)) {
            SettingsRow(stringResource(R.string.settings_language), languageLabel, actions.onLanguage)
        }

        Section(stringResource(R.string.settings_data)) {
            SettingsRow(stringResource(R.string.settings_export), stringResource(R.string.settings_export_body), actions.onExport, enabled = !state.busy)
            SettingsRow(stringResource(R.string.settings_import), stringResource(R.string.settings_import_body), actions.onImport, enabled = !state.busy)
        }

        Section(stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_version, versionName), style = TaccaType.body, color = c.textMuted, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Legend(title)
        content()
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = TaccaTheme.colors
    SurfaceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = TaccaType.title, color = c.text)
                Text(subtitle, style = TaccaType.bodySmall, color = c.textMuted)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LanguageDialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val c = TaccaTheme.colors
    val options = listOf<String?>(null) + AppLanguage.supported
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.text) } },
        title = { Text(stringResource(R.string.settings_language), style = TaccaType.title, color = c.text) },
        text = {
            Column {
                options.forEach { tag ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = tag == current, role = Role.RadioButton, onClick = { onPick(tag) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                        Text(stringResource(AppLanguage.labelRes(tag)), style = TaccaType.body, color = c.text, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        containerColor = c.surface,
    )
}
```

- [ ] **Step 4: Run the tests**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "it.matteocolombo.tacca.ui.settings.*"'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add settings screen with theme, language and backup

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 17: App shell, navigation and end-to-end flow

**Files:**
- Create: `app/src/main/java/it/matteocolombo/tacca/ui/navigation/TaccaApp.kt`
- Modify: `app/src/main/java/it/matteocolombo/tacca/MainActivity.kt`
- Delete: `app/src/test/java/it/matteocolombo/tacca/AppLaunchTest.kt`
- Test: `app/src/test/java/it/matteocolombo/tacca/AppFlowTest.kt`

**Interfaces:**
- Consumes: every `XEntry` composable from Tasks 10–16, routes, `TopLevelDestination`, `TaccaBottomBar`, `TaccaTheme`, `AppContainer.settings`.
- Produces: `@Composable fun TaccaApp(navController: NavHostController = rememberNavController())`; `fun NavController.navigateToTab(tab: TopLevelDestination)`; `fun NavController.showHomeFiltered()`.

- [ ] **Step 1: Write the failing end-to-end test**

Delete `AppLaunchTest.kt`. Create `app/src/test/java/it/matteocolombo/tacca/AppFlowTest.kt`:

```kotlin
package it.matteocolombo.tacca

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createGrinderCoffeeAndCalibration() {
        // Grinder
        compose.onNodeWithText("Grinders").performClick()
        compose.onNodeWithText("Add grinder").performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("JX-Pro")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("JX-Pro"), 5_000)

        // Calibration with an inline new coffee
        compose.onNodeWithText("Calibrations").performClick()
        compose.waitUntilAtLeastOneExists(hasText("New calibration"), 5_000)
        compose.onNodeWithText("New calibration").performClick()
        compose.onAllNodesWithText("New").onFirst().performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("Brasile")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Brasile"), 5_000)
        compose.onNodeWithText("V60").performClick()
        repeat(3) { compose.onNodeWithContentDescription("More steps").performScrollTo().performClick() }
        compose.onNodeWithText("Save · 3 steps").performClick()

        // Detail, then back to home
        compose.waitUntilAtLeastOneExists(hasContentDescription("Dial: 3 steps"), 5_000)
        compose.onNodeWithContentDescription("Dial: 3 steps").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Brasile"), 5_000)
        compose.onNodeWithText("JX-Pro").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "it")
    fun italianLabels() {
        compose.onNodeWithText("Macine").assertIsDisplayed()
        compose.onNodeWithText("Metodi").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `bash -l -c './gradlew testDebugUnitTest --tests "*AppFlowTest*"'`
Expected: FAIL (the activity still shows only the app name; "Grinders" not found).

- [ ] **Step 3: Implement the app shell**

`ui/navigation/TaccaApp.kt`:

```kotlin
package it.matteocolombo.tacca.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.matteocolombo.tacca.ui.coffees.CoffeesEntry
import it.matteocolombo.tacca.ui.components.TaccaBottomBar
import it.matteocolombo.tacca.ui.detail.DetailEntry
import it.matteocolombo.tacca.ui.editor.CalibrationEditorEntry
import it.matteocolombo.tacca.ui.grindereditor.GrinderEditorEntry
import it.matteocolombo.tacca.ui.grinders.GrindersEntry
import it.matteocolombo.tacca.ui.home.HomeEntry
import it.matteocolombo.tacca.ui.methods.MethodsEntry
import it.matteocolombo.tacca.ui.settings.SettingsEntry
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@Composable
fun TaccaApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    Scaffold(
        containerColor = TaccaTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (currentTab != null) TaccaBottomBar(currentTab, onSelect = navController::navigateToTab) },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
        ) {
            composable<HomeRoute> {
                HomeEntry(
                    onOpen = { navController.navigate(DetailRoute(it)) },
                    onNew = { coffeeId, methodId -> navController.navigate(EditorRoute(coffeeId = coffeeId ?: NEW_ID, methodId = methodId ?: NEW_ID)) },
                    onSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<CoffeesRoute> { CoffeesEntry(onShowOnHome = navController::showHomeFiltered) }
            composable<MethodsRoute> { MethodsEntry(onShowOnHome = navController::showHomeFiltered) }
            composable<GrindersRoute> {
                GrindersEntry(
                    onGrinderClick = { navController.navigate(GrinderEditorRoute(it)) },
                    onAddClick = { navController.navigate(GrinderEditorRoute()) },
                )
            }
            composable<DetailRoute> {
                DetailEntry(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditorRoute(calibrationId = it)) },
                    onOpen = { navController.navigate(DetailRoute(it)) },
                )
            }
            composable<EditorRoute> {
                CalibrationEditorEntry(
                    onClose = { navController.popBackStack() },
                    onNewGrinder = { navController.navigate(GrinderEditorRoute()) },
                    onSaved = { id, wasEdit ->
                        if (wasEdit) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(DetailRoute(id)) { popUpTo<EditorRoute> { inclusive = true } }
                        }
                    },
                    onDeleted = { navController.popBackStack(HomeRoute, inclusive = false) },
                )
            }
            composable<GrinderEditorRoute> {
                GrinderEditorEntry(
                    onClose = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(NEW_GRINDER_RESULT_KEY, id)
                        navController.popBackStack()
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> { SettingsEntry(onBack = { navController.popBackStack() }) }
        }
    }
}

fun NavController.navigateToTab(tab: TopLevelDestination) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Opens the Calibrations tab at its root, where the shared home filter has just been set. */
fun NavController.showHomeFiltered() = navigate(HomeRoute) {
    popUpTo(graph.findStartDestination().id) { inclusive = false }
    launchSingleTop = true
}
```

`MainActivity.kt`:

```kotlin
package it.matteocolombo.tacca

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.ui.navigation.TaccaApp
import it.matteocolombo.tacca.ui.theme.TaccaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TaccaApplication).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            TaccaTheme(settings) { TaccaApp() }
        }
    }
}
```

- [ ] **Step 4: Run the whole test suite**

Run: `bash -l -c './gradlew testDebugUnitTest'`
Expected: PASS (all tests, including `AppFlowTest`). If a `waitUntilAtLeastOneExists` times out, first check the Room query thread is not blocked (Robolectric `@LooperMode(PAUSED)` is the default; add `compose.mainClock.autoAdvance = true` — the default — and `ShadowLooper.idleMainLooper()` only if needed).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Wire navigation shell and end-to-end flow

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 18: App icon, release signing, README, final verification

**Files:**
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/drawable/ic_launcher_monochrome.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/main/res/values/colors.xml` (add `ic_launcher_background`), `app/src/main/AndroidManifest.xml` (icon attributes), `app/build.gradle.kts` (release signing), `.gitignore` (add `/keystore/`)
- Create: `README.md`
- Local only (git-ignored): `keystore/tacca-upload.jks`, `keystore.properties`

**Interfaces:**
- Consumes: the complete app.
- Produces: signed `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`.

- [ ] **Step 1: Adaptive icon**

`app/src/main/res/drawable/ic_launcher_foreground.xml` (brand mark scaled ×2.4 into the 108dp canvas, safe zone r=33):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#00000000"
        android:pathData="M33.6,54a20.4,20.4 0,1 0,40.8 0a20.4,20.4 0,1 0,-40.8 0"
        android:strokeWidth="5.3"
        android:strokeColor="#FFFFFFFF"
        android:strokeLineCap="round" />
    <path
        android:fillColor="#00000000"
        android:pathData="M54,33.6V42"
        android:strokeWidth="5.3"
        android:strokeColor="#FFFFFFFF"
        android:strokeLineCap="round" />
</vector>
```

`app/src/main/res/drawable/ic_launcher_monochrome.xml`: identical to the foreground but with `android:strokeColor="#FF000000"`.

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` (same content):

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

Add to `values/colors.xml`: `<color name="ic_launcher_background">#B4552E</color>`.

In the manifest `<application>` add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` (minSdk 26 means `mipmap-anydpi-v26` covers every device).

- [ ] **Step 2: Upload keystore and signing config**

```bash
cd /home/matteo/p/tmp/tacca
mkdir -p keystore
PASS=$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)
keytool -genkeypair -v -keystore keystore/tacca-upload.jks -alias tacca-upload -keyalg RSA -keysize 4096 -validity 36500 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=Matteo Colombo, O=Tacca, C=IT"
printf 'storeFile=keystore/tacca-upload.jks\nstorePassword=%s\nkeyAlias=tacca-upload\nkeyPassword=%s\n' "$PASS" "$PASS" > keystore.properties
echo "/keystore/" >> .gitignore
git status --short   # keystore/ and keystore.properties must NOT appear
```

In `app/build.gradle.kts` add at the top (after `plugins {}`):

```kotlin
import java.util.Properties

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
```

and inside `android {}`:

```kotlin
    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
```

and in `buildTypes.release`: `signingConfig = signingConfigs.findByName("release")`. (`import` lines must be at the very top of the file, before `plugins {}`.)

- [ ] **Step 3: README**

`README.md`:

````markdown
# Tacca

Native Android app that remembers the grinder setting for every coffee × brew method × grinder.

- Spec: `docs/superpowers/specs/2026-09-26-tacca-android-design.md`
- Plan: `docs/superpowers/plans/2026-09-26-tacca-android.md`
- Mockup sources: `docs/mockup/`

## Build

Requires JDK 21 and the Android SDK (`ANDROID_HOME`).

```bash
./gradlew testDebugUnitTest      # unit + Robolectric tests
./gradlew lint                   # lint (translations are errors)
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease bundleRelease
```

Release builds are signed with the upload key referenced by `keystore.properties`
(not in git). Keep `keystore/tacca-upload.jks` and its password backed up: losing
the upload key requires a key reset through Play Console.

## Languages

English (default), Italian, Spanish, French, German. The es/fr/de translations were
machine-written and must be reviewed by native speakers before publishing.

## Licenses

Fonts: Bricolage Grotesque and Figtree, SIL Open Font License (`third_party/fonts/`).
Settings icon path from Lucide (ISC).
````

- [ ] **Step 4: Full verification**

Run: `bash -l -c './gradlew clean testDebugUnitTest lint assembleRelease bundleRelease'`
Expected: `BUILD SUCCESSFUL`; lint with 0 errors; files exist:

```bash
ls -l app/build/outputs/apk/release/app-release.apk app/build/outputs/bundle/release/app-release.aab
bash -l -c '$ANDROID_HOME/build-tools/*/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk | head -3'
```

Expected: both files present; `apksigner` prints the `CN=Matteo Colombo` certificate. If R8 breaks something at runtime it will only show on a device — the release APK is handed to the user for manual QA on a real phone (no emulator in this environment).

- [ ] **Step 5: Commit**

```bash
git add -A
git status --short   # verify no keystore material is staged
git commit -m "Add launcher icon, release signing and README

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```
