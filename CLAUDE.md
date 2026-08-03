# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Balloon Pop" — a 2D Android game built with [libGDX](https://libgdx.com/), package `com.joshreimer.balloonpop`. A cannon at the bottom of the screen is dragged to aim and fires automatically while held, popping balloons that fall from the top of the screen (plus a rare, higher-value purple blimp that drifts across). Firing costs points and triggers a short vibration pulse, and after enough sustained fire the barrel overheats and has to be cooled by a passing alien before it will shoot again. All visuals are procedural (drawn with `ShapeRenderer`/`BitmapFont` primitives) and all audio is synthesized — there are no image or bought/licensed audio assets anywhere in the project.

Standard two-module libGDX layout:
- `core/` — a plain `java-library` module with zero Android dependencies. All game logic, screens, and entities live here.
- `android/` — a thin Android application module that launches the `core` game inside an `AndroidApplication` and puts it fullscreen (see `AndroidLauncher`).

## Build & run

Requires JDK 17+ and an Android SDK (compileSdk/targetSdk 37, minSdk 21). Gradle 9.6.1 and AGP 9.3.0 are pinned; the Gradle wrapper handles the Gradle download.

Before the first build, copy `local.properties.example` to `local.properties` and point `sdk.dir` at your Android SDK.

```sh
./gradlew android:assembleDebug     # build debug APK -> android/build/outputs/apk/debug/android-debug.apk
./gradlew android:installDebug      # build and install to a connected device/emulator
```

There is no automated test suite and no CI configuration in this repo. `./gradlew android:lint` (standard Android Gradle Plugin lint) is available but not specially configured.

`gdxVersion` is pinned once in the root `gradle.properties` and referenced from both `core/build.gradle` (`com.badlogicgames.gdx:gdx`) and `android/build.gradle` (`gdx-backend-android` + per-ABI `gdx-platform` natives).

### Building on this device (aarch64 proot environment)

This dev environment (a proot-distro container on an aarch64 Android device) has no JDK and only a partial Android SDK preinstalled, and hits an architecture mismatch that a normal Linux/Mac/Windows dev machine never would. `scripts/build-termux.sh` automates all of the below (run with `bash scripts/build-termux.sh` — invoking it with `sh` breaks on `BASH_SOURCE`, and the executable bit doesn't reliably stick on this device's filesystem). It's idempotent: safe to re-run each session, since it detects what's already set up in stable (`~/tools`) paths and skips redoing it. The manual steps it automates:

1. **JDK**: none is installed system-wide, and there's no root/sudo, so `apt`/`pkg install` fail. Download a portable Temurin 21 tarball for `linux/aarch64` from Adoptium and extract it (no root needed):
   ```sh
   curl -sL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/aarch64/jdk/hotspot/normal/eclipse" -o jdk21.tar.gz
   tar xzf jdk21.tar.gz   # -> jdk-21.x.x+y/
   export JAVA_HOME=".../jdk-21.x.x+y"
   export PATH="$JAVA_HOME/bin:$PATH"
   ```
   These exports don't persist across shell invocations in this harness — set them before every `gradlew` call.

2. **Android SDK**: lives at `/root/coding/android-sdk` (a sibling of this repo, not inside it), with only `cmdline-tools/latest` present. Install the rest and accept licenses via `sdkmanager`:
   ```sh
   yes | sh /root/coding/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/coding/android-sdk --licenses
   sh /root/coding/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/root/coding/android-sdk \
     "platform-tools" "platforms;android-36" "build-tools;36.0.0"
   ```
   Then create `local.properties` in the repo root with `sdk.dir=/root/coding/android-sdk`.

3. **The actual blocker — aapt2 is x86_64-only, this device is aarch64**: AGP's Maven-resolved `aapt2` binary (and the SDK's own `build-tools/*/aapt2`) only ship as x86_64 Linux ELF binaries; there is no aarch64 build for this AGP version. Running `./gradlew android:assembleDebug` fails at `:android:processDebugResources` with `AAPT2 ... Daemon startup failed`. Fix by running the x86_64 `aapt2` under `qemu-x86_64` (already installed on this device via Termux at `/data/data/com.termux/files/usr/bin/qemu-x86_64`) against a minimal x86_64 glibc sysroot:
   ```sh
   # Build a small x86_64 sysroot (no root needed — dpkg-deb -x just unpacks)
   mkdir -p amd64root && cd amd64root
   curl -sLO http://deb.debian.org/debian/pool/main/g/glibc/libc6_<ver>_amd64.deb
   curl -sLO http://deb.debian.org/debian/pool/main/g/gcc-14/libgcc-s1_<ver>_amd64.deb
   # (match <ver> to `dpkg -l libc6 libgcc-s1` on this arm64 host)
   dpkg-deb -x libc6_*_amd64.deb sysroot
   dpkg-deb -x libgcc-s1_*_amd64.deb sysroot
   ln -sfn usr/lib64 sysroot/lib64
   ln -sfn usr/lib   sysroot/lib

   # Wrap the Maven-cached aapt2 (path varies by content hash — find it first):
   AAPT2_DIR=$(dirname "$(find ~/.gradle/caches -path '*/transformed/aapt2-*-linux/aapt2' | head -1)")
   mv "$AAPT2_DIR/aapt2" "$AAPT2_DIR/aapt2.real"
   cat > "$AAPT2_DIR/aapt2" <<EOF
   #!/bin/sh
   exec /data/data/com.termux/files/usr/bin/qemu-x86_64 -L $(pwd)/amd64root/sysroot "$AAPT2_DIR/aapt2.real" "\$@"
   EOF
   chmod +x "$AAPT2_DIR/aapt2"
   ```
   This must target the *Maven-cached* copy under `~/.gradle/caches`, not the SDK's `build-tools/*/aapt2` — files under `/root/coding/android-sdk` are owned by a different uid than the build shell, and `chmod` on them silently no-ops (the exec bit never actually gets set), so they can't be made runnable this way. The `~/.gradle` cache is owned by the build shell's own user, so `chmod +x` there works normally.

   Once wrapped, `./gradlew android:assembleDebug` succeeds normally. Since the wrapper lives under `~/.gradle/caches` (content-hash-keyed, and untouched by a plain rebuild) it survives repeat builds within the same environment, but not across a fresh container/session — redo this setup if `assembleDebug` again fails at `processDebugResources` with a daemon startup error.

4. **`git push` needs `gh` on `PATH`**: there are no stored git credentials (`https` remote, no credential helper configured), so a plain `git push` fails with `fatal: could not read Username for 'https://github.com'`. The `gh` CLI is installed and already authenticated as `Josh-Reimer`, but lives at `/home/coder/tools/gh_<version>_linux_arm64/bin/gh`, which isn't on `PATH` by default. Fix per-shell:
   ```sh
   export PATH="/home/coder/tools/gh_2.97.0_linux_arm64/bin:$PATH"
   gh auth setup-git   # wires gh in as git's credential helper for github.com
   git push
   ```
   `gh auth setup-git` only needs to run once per shell/session before the first push; after that, plain `git push`/`git pull` in that same shell work normally.

## Architecture

**Entry point / screen management** (`BalloonPopGame`, extends libGDX `Game`): creates a single `GameSettings` instance plus two long-lived screens — `GameScreen` and `SettingsScreen` — once in `create()`, then switches between them via `showGame()` / `showSettings()`. Screens are never recreated on navigation; `BalloonPopGame.dispose()` explicitly disposes both regardless of which is currently active. This matters because libGDX's default `Game.dispose()` only disposes the *current* screen — recreating a screen per navigation instead of reusing the persistent instance would leak its `ShapeRenderer`/`SpriteBatch`/fonts/sounds every time.

**`GameScreen`** is the core of the game — a single class that owns the full game loop: a `State` enum (`READY` → `PLAYING` → `PAUSED` / `GAME_OVER`), all entity collections, spawn/wave logic, collision detection, HUD, and on-screen icon buttons (settings gear, mute, pause). It renders to a 480-wide virtual world via an `ExtendViewport` capped at that width, so horizontal layout constants are fixed world units while the world *height* stretches past 800 to fill the display (there is no letterbox bar, so balloons enter at the physical top edge). Anything positioned relative to the top — the gear/mute/pause/bullets row via `iconRowY()`, balloon spawns, overlay text — must use the `worldHeight` field, updated in `resize()`, not the `WORLD_HEIGHT` minimum. `SettingsScreen` is unaffected and still uses a plain `FitViewport`. Touch input is polled each frame (not via `InputProcessor`): dragging anywhere aims the cannon and fires automatically while held (except the gear/mute icon hot zones, which are checked first and take priority so they never get swallowed by gameplay input, matching the always-reachable gear). Balloons cost a life if they fall past the bottom unpopped; the blimp is a bonus target with no penalty for missing it. Firing costs `FIRE_SCORE_PENALTY` points per shot and triggers `Gdx.input.vibrate(...)` (requires the `VIBRATE` permission in `AndroidManifest.xml`). The mute icon toggles `GameSettings.isMuted()` and saves immediately on tap (unlike every other setting, which only commits via `settings.save()` on the settings screen's BACK button) — pop/fire `Sound.play(...)` calls are routed through a `playIfUnmuted(...)` helper that no-ops while muted, so score/vibration/firing behaviour is unaffected, only the audio.

**The overheat sequence** is a second, orthogonal state machine (`Overheat` enum: `NONE` → `SWARM` → `DOUSING`) running *inside* `State.PLAYING`, not a new `State` — the world keeps updating throughout, so it can't be modelled as a pause. Every `OVERHEAT_SHOT_LIMIT` (267) shots the barrel gives out: `beginOverheat()` locks out firing, clears the sky (balloons/asteroids already falling are `pop()`ed into clouds — worth no points, and popping suppresses their fall-past-the-bottom life penalty), and halts all normal spawning. In its place a swarm of parachuting aliens comes down, each one that reaches the ground unshot docking `OVERHEAT_ALIEN_PENALTY` points but never a life. The swarm keeps topping itself up until `OVERHEAT_ALIENS_REQUIRED` (3) aliens have *landed*, which is what guarantees the sequence terminates: firing is off, so only shots still in flight when the overheat began can thin the swarm. Landings are counted off `Alien.landedThisFrame` (a one-shot flag cleared at the top of `Alien.update`) rather than by scanning phases, and only for aliens with `holdOnLanding` set, so a plain bonus alien that happens to be mid-fall when the overheat starts is never charged for. Then `beginDousing()` sends the landed alien nearest the gun over with a bucket; the gun is frozen in place for that leg only (so it can't be dragged out from under the pour) while aiming stays free during `SWARM`. `endOverheat()` fires when the `WaterSplash` finishes, resetting `shotsSinceCooldown` and releasing the held aliens to waddle off — so the whole thing recurs every 267 shots. `bulletsFired` stays a lifetime counter; the separate `shotsSinceCooldown` drives the heat fill drawn behind the bullets chip, which is the only warning the player gets that an overheat is coming.

**`entities/`** — plain update/render classes, not libGDX `Actor`/`Stage`; `GameScreen` owns and iterates an `Array<T>` of each directly. They split into two kinds: collidable targets (`Balloon`, `Basketball`, `Blimp`) expose `overlaps(...)` (circle/ellipse collision) and are checked in `GameScreen.resolveCollisions()`; purely decorative effects (`Explosion`, `BalloonCloud`, `MuzzleFlash`, `WaterSplash`) have no `overlaps()` and are never collision-checked — they just animate for a fixed `DURATION` and set `alive = false` when done. Note that effects fade by *shrinking*, never by alpha: `ShapeRenderer` draws with blending off, so a translucent colour comes out solid black. `Alien` is the most stateful of them, with a four-phase lifecycle (`FALLING` under its parachute → `WALKING` → `STANDING` → `POURING`); the last two exist only for the overheat sequence, which needs aliens that stay put where they land (`holdOnLanding`), walk to a given x (`walkTo`), and tip a bucket (`giveBucket`/`pour`) instead of immediately waddling off screen. `Gun` is the one singleton (not a pooled `Array<T>`) and additionally tracks recoil state, kicked by `fire()` and eased back in `update(delta)`. Its appearance is player-chosen: `Gun.render()` delegates to a `GunStyle` enum constant (each constant draws its own shape) coloured from `GunPalette`, and `GameScreen.show()` pushes the saved choice in via `gun.setSkin(...)` so it updates on every return from settings. Every `GunStyle` draws inside the same `Gun.WIDTH` x `Gun.HEIGHT` box with its muzzle at the top of it, so the choice stays purely cosmetic — `getMuzzleX()`/`getMuzzleY()` and all firing behaviour are style-independent. The projectile follows the same pattern one level down: `Basketball` (still the class name, though it is now the generic round) delegates its look to the `AmmoStyle` it was constructed with, and every style draws inside the same `Basketball.RADIUS`, so ammo choice never affects flight or collisions either. Everything is drawn with `ShapeRenderer` primitives only — including the hand-triangulated settings gear icon (fan-triangulated from a center point) — there is no texture/atlas pipeline in this project.

**`GameSettings`** — user-adjustable difficulty (fire rate, balloon spawn rate), stored internally as normalized `0..1` sliders, plus the cosmetic gun style/colour, ammo style and sfx style indices and a `muted` flag; all persisted via libGDX `Preferences`. `SettingsScreen` is a self-contained screen with hand-rolled drag sliders in the same `ShapeRenderer`/`BitmapFont` style (no scene2d.ui anywhere in the project), then three arrow-cycled customizer rows (sound, ammo, gun), each of which previews its own choice live: the sound arrows play the new pop, the ammo arrows redraw the round between them, and the gun section adds a `GunPalette` swatch row plus a full-size preview. Everything is committed to `Preferences` by the single `settings.save()` on BACK. The screen's `FitViewport` is a fixed 480x800 with every row at a hardcoded Y, so **adding a row means re-laying out the existing ones** — there is no layout engine to absorb it.

**`android/build.gradle` native-libs wiring**: libGDX's native library (`libgdx.so`) is not picked up by AGP's default `jniLibs` convention. A `copyAndroidNatives` task extracts the per-ABI natives jars into `android/libs/<abi>/`, and `sourceSets { main.jniLibs.srcDirs = ['libs'] }` is required for AGP to actually package them. Both pieces are needed — an APK built without the `jniLibs.srcDirs` line installs fine but crashes immediately on launch with `UnsatisfiedLinkError`, since a libGDX `Matrix4` static initializer loads the native lib the moment the first camera is constructed.

**Assets**: `assets/` lives at the repo root (not under `android/`) and is wired in via `sourceSets.main.assets.srcDirs += ['../assets']` in `android/build.gradle`. Referenced from code with plain relative paths, e.g. `Gdx.files.internal("pop.wav")`.
