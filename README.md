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
