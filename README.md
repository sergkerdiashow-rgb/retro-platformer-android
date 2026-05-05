# Retro Platformer Android

Original 8-bit Android platformer inspired by classic console games. This is not a Mario clone and does not use Nintendo characters, music, images, sprites, or other copyrighted assets.

## Features

- Native Android game built with `Canvas`.
- Run left and right, jump, and land on platforms.
- Bricks, bonus blocks, coins, moving enemies, lives, score, camera, and finish flag.
- Touch controls for landscape screens.
- All visuals are drawn in code, so the project has no external art assets.

## Build APK Locally

Open this folder in Android Studio, then choose:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

Or from a terminal with JDK, Android SDK, and Gradle installed:

```powershell
gradle assembleDebug
```

The APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build APK On GitHub

This repository includes `.github/workflows/build-apk.yml`.

1. Open the `Actions` tab.
2. Choose `Build APK`.
3. Click `Run workflow`.
4. Download the `retro-platformer-debug-apk` artifact after the run finishes.
