# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Balloon Pop" — a 2D Android game built with [libGDX](https://libgdx.com/), package `com.joshreimer.balloonpop`. A cannon at the bottom of the screen is dragged to aim and fires automatically while held, popping balloons that fall from the top of the screen (plus a rare, higher-value purple blimp that drifts across). Firing costs points and triggers a short vibration pulse. All visuals are procedural (drawn with `ShapeRenderer`/`BitmapFont` primitives) and all audio is synthesized — there are no image or bought/licensed audio assets anywhere in the project.

Standard two-module libGDX layout:
- `core/` — a plain `java-library` module with zero Android dependencies. All game logic, screens, and entities live here.
- `android/` — a thin Android application module that just launches the `core` game inside an `AndroidApplication`.

## Build & run

Requires JDK 17+ and an Android SDK (compileSdk/targetSdk 36, minSdk 21). Gradle 8.13 and AGP 8.13.2 are pinned; the Gradle wrapper handles the Gradle download.

Before the first build, copy `local.properties.example` to `local.properties` and point `sdk.dir` at your Android SDK.

```sh
./gradlew android:assembleDebug     # build debug APK -> android/build/outputs/apk/debug/android-debug.apk
./gradlew android:installDebug      # build and install to a connected device/emulator
```

There is no automated test suite and no CI configuration in this repo. `./gradlew android:lint` (standard Android Gradle Plugin lint) is available but not specially configured.

`gdxVersion` is pinned once in the root `gradle.properties` and referenced from both `core/build.gradle` (`com.badlogicgames.gdx:gdx`) and `android/build.gradle` (`gdx-backend-android` + per-ABI `gdx-platform` natives).

## Architecture

**Entry point / screen management** (`BalloonPopGame`, extends libGDX `Game`): creates a single `GameSettings` instance plus two long-lived screens — `GameScreen` and `SettingsScreen` — once in `create()`, then switches between them via `showGame()` / `showSettings()`. Screens are never recreated on navigation; `BalloonPopGame.dispose()` explicitly disposes both regardless of which is currently active. This matters because libGDX's default `Game.dispose()` only disposes the *current* screen — recreating a screen per navigation instead of reusing the persistent instance would leak its `ShapeRenderer`/`SpriteBatch`/fonts/sounds every time.

**`GameScreen`** is the core of the game — a single class that owns the full game loop: a `State` enum (`READY` → `PLAYING` → `PAUSED` / `GAME_OVER`), all entity collections, spawn/wave logic, collision detection, HUD, and on-screen icon buttons (settings gear, pause). It renders to a fixed 480×800 virtual world size via a `FitViewport`, so all layout constants are in those world units, not pixels. Touch input is polled each frame (not via `InputProcessor`): dragging anywhere aims the cannon and fires automatically while held (except the gear/pause icon hot zones, which are checked first and take priority so they never get swallowed by gameplay input). Balloons cost a life if they fall past the bottom unpopped; the blimp is a bonus target with no penalty for missing it. Firing costs `FIRE_SCORE_PENALTY` points per shot and triggers `Gdx.input.vibrate(...)` (requires the `VIBRATE` permission in `AndroidManifest.xml`).

**`entities/`** — plain update/render classes, not libGDX `Actor`/`Stage`. Each typically exposes `update(delta, ...)`, `overlaps(...)` (circle or ellipse collision), and `render(ShapeRenderer)`; `GameScreen` owns and iterates `Array<T>` of each directly. Everything is drawn with `ShapeRenderer` primitives only — including the hand-triangulated settings gear icon (fan-triangulated from a center point) — there is no texture/atlas pipeline in this project.

**`GameSettings`** — user-adjustable difficulty (fire rate, balloon spawn rate), stored internally as normalized `0..1` sliders and persisted via libGDX `Preferences`. `SettingsScreen` is a self-contained screen with hand-rolled drag sliders in the same `ShapeRenderer`/`BitmapFont` style (no scene2d.ui anywhere in the project).

**`android/build.gradle` native-libs wiring**: libGDX's native library (`libgdx.so`) is not picked up by AGP's default `jniLibs` convention. A `copyAndroidNatives` task extracts the per-ABI natives jars into `android/libs/<abi>/`, and `sourceSets { main.jniLibs.srcDirs = ['libs'] }` is required for AGP to actually package them. Both pieces are needed — an APK built without the `jniLibs.srcDirs` line installs fine but crashes immediately on launch with `UnsatisfiedLinkError`, since a libGDX `Matrix4` static initializer loads the native lib the moment the first camera is constructed.

**Assets**: `assets/` lives at the repo root (not under `android/`) and is wired in via `sourceSets.main.assets.srcDirs += ['../assets']` in `android/build.gradle`. Referenced from code with plain relative paths, e.g. `Gdx.files.internal("pop.wav")`.
