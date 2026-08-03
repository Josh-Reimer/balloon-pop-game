package com.joshreimer.balloonpop.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joshreimer.balloonpop.BalloonPopGame;
import com.joshreimer.balloonpop.GameSettings;
import com.joshreimer.balloonpop.entities.Alien;
import com.joshreimer.balloonpop.entities.Asteroid;
import com.joshreimer.balloonpop.entities.Balloon;
import com.joshreimer.balloonpop.entities.BalloonCloud;
import com.joshreimer.balloonpop.entities.Basketball;
import com.joshreimer.balloonpop.entities.Blimp;
import com.joshreimer.balloonpop.entities.Explosion;
import com.joshreimer.balloonpop.entities.Gun;
import com.joshreimer.balloonpop.entities.MuzzleFlash;
import com.joshreimer.balloonpop.entities.SfxStyle;

public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 480f;
    private static final float WORLD_HEIGHT = 800f;
    private static final float GUN_Y = 12f;

    private static final float START_LIVES = 3;

    // Floor for the in-run spawn-interval ramp; always below GameSettings.MIN_SPAWN_INTERVAL.
    private static final float DIFFICULTY_MIN_SPAWN_INTERVAL = 0.3f;
    private static final float MIN_FALL_SPEED = 55f;
    private static final float MAX_FALL_SPEED_BASE = 105f;
    private static final float MAX_FALL_SPEED_CAP = 260f;

    private static final float MIN_BALLOON_RADIUS = 24f;
    private static final float MAX_BALLOON_RADIUS = 34f;

    // A pop is worth more the harder the balloon was to hit: small and fast pays MAX, big and
    // slow pays MIN. Size and speed weigh equally.
    private static final int MIN_BALLOON_POINTS = 4;
    private static final int MAX_BALLOON_POINTS = 10;

    private static final float POP_VOLUME = 0.6f;
    private static final float FIRE_VOLUME = 0.4f;
    private static final int FIRE_SCORE_PENALTY = 2;
    private static final float BURST_MESSAGE_DURATION = 1.4f;
    private static final int FIRE_VIBRATION_MS = 6;

    private static final float SCORE_POPUP_DURATION = 0.8f;
    private static final float SCORE_POPUP_RISE = 46f;
    private static final float SCORE_POPUP_ARROW_WIDTH = 10f;
    private static final float SCORE_POPUP_ARROW_HEIGHT = 12f;
    private static final float SCORE_FLASH_DURATION = 0.5f;
    private static final Color SCORE_POSITIVE_COLOR = new Color(0.25f, 0.85f, 0.35f, 1f);
    private static final Color SCORE_NEGATIVE_COLOR = new Color(0.9f, 0.25f, 0.25f, 1f);

    private static final float MIN_BLIMP_INTERVAL = 9f;
    private static final float MAX_BLIMP_INTERVAL = 18f;
    private static final float BLIMP_MIN_SPEED = 80f;
    private static final float BLIMP_MAX_SPEED = 130f;
    private static final float BLIMP_MIN_Y_FRACTION = 0.55f;
    private static final float BLIMP_MAX_Y_FRACTION = 0.85f;

    // Asteroids fall like balloons but are rarer and pay triple what an equivalent balloon would.
    private static final float MIN_ASTEROID_INTERVAL = 10f;
    private static final float MAX_ASTEROID_INTERVAL = 20f;
    private static final int ASTEROID_POINTS_MULTIPLIER = 3;

    // Aliens are a rare bonus that drifts down slowly under a parachute; like the blimp, missing
    // one costs nothing -- it's pure upside, not a hazard.
    private static final float MIN_ALIEN_INTERVAL = 12f;
    private static final float MAX_ALIEN_INTERVAL = 22f;
    private static final float ALIEN_FALL_SPEED = 42f;

    private static final float ICON_MARGIN = 44f;
    private static final float ICON_RADIUS = 30f;
    private static final float GEAR_X = ICON_MARGIN;
    private static final float PAUSE_X = WORLD_WIDTH - ICON_MARGIN;

    private static final int GEAR_TEETH = 8;
    private static final float GEAR_TOOTH_R = 15f;
    private static final float GEAR_BASE_R = 10.5f;
    private static final float GEAR_HOLE_R = 5f;

    private enum State { READY, PLAYING, PAUSED, GAME_OVER }

    /** A floating "+N"/"-N" indicator rising from the point a score change happened. */
    private static class ScorePopup {
        final float x;
        final float startY;
        final int amount;
        float timer = SCORE_POPUP_DURATION;

        ScorePopup(float x, float startY, int amount) {
            this.x = x;
            this.startY = startY;
            this.amount = amount;
        }
    }

    private final BalloonPopGame game;
    private final GameSettings settings;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont hudFont;
    private final BitmapFont titleFont;
    private final GlyphLayout layout = new GlyphLayout();

    private final Gun gun;
    private final Array<Balloon> balloons = new Array<>();
    private final Array<Asteroid> asteroids = new Array<>();
    private final Array<Alien> aliens = new Array<>();
    private final Array<Basketball> basketballs = new Array<>();
    private final Array<Blimp> blimps = new Array<>();
    private final Array<Explosion> explosions = new Array<>();
    private final Array<BalloonCloud> balloonClouds = new Array<>();
    private final Array<MuzzleFlash> muzzleFlashes = new Array<>();
    private final Array<ScorePopup> scorePopups = new Array<>();

    private Sound popSound;
    private Sound fireSound;
    private SfxStyle loadedSfxStyle;
    private final Color scoreColor = new Color();

    private State state = State.READY;
    private int score = 0;
    private int lives = (int) START_LIVES;
    private int bulletsFired = 0;

    private int burstShotCount = 0;
    private int lastBurstShotCount = 0;
    private float burstMessageTimer = 0f;

    private float scoreFlashTimer = 0f;
    private boolean scoreFlashPositive = false;

    private float spawnTimer = 0f;
    private float difficultyTime = 0f;
    private float fireCooldown = 0f;
    private float blimpSpawnTimer = 0f;
    private float asteroidSpawnTimer = 0f;
    private float alienSpawnTimer = 0f;
    private boolean firing = false;

    private final Vector3 touchWorld = new Vector3();

    /** Actual world height for the current screen; the viewport extends it past WORLD_HEIGHT. */
    private float worldHeight = WORLD_HEIGHT;

    public GameScreen(BalloonPopGame game, GameSettings settings) {
        this.game = game;
        this.settings = settings;

        camera = new OrthographicCamera();
        // Extends the world vertically to fill the screen (never horizontally, so the 480-wide
        // layout constants stay valid) — otherwise balloons enter at a letterbox bar part way
        // down the display instead of at its top edge.
        viewport = new ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT, WORLD_WIDTH, 0, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        hudFont = new BitmapFont();
        hudFont.getData().setScale(1.6f);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.6f);

        gun = new Gun(WORLD_WIDTH / 2f, GUN_Y, WORLD_WIDTH);

        loadSfx(settings.getSfxStyle());
    }

    /** (Re)loads the pop/fire sounds for the given style, disposing whatever was loaded before. */
    private void loadSfx(SfxStyle style) {
        if (popSound != null) {
            popSound.dispose();
            fireSound.dispose();
        }
        popSound = Gdx.audio.newSound(Gdx.files.internal(style.getPopPath()));
        fireSound = Gdx.audio.newSound(Gdx.files.internal(style.getFirePath()));
        loadedSfxStyle = style;
    }

    private void resetGame() {
        balloons.clear();
        asteroids.clear();
        aliens.clear();
        basketballs.clear();
        blimps.clear();
        explosions.clear();
        balloonClouds.clear();
        muzzleFlashes.clear();
        scorePopups.clear();
        score = 0;
        lives = (int) START_LIVES;
        bulletsFired = 0;
        burstShotCount = 0;
        lastBurstShotCount = 0;
        burstMessageTimer = 0f;
        scoreFlashTimer = 0f;
        spawnTimer = 0f;
        difficultyTime = 0f;
        fireCooldown = 0f;
        blimpSpawnTimer = MathUtils.random(MIN_BLIMP_INTERVAL, MAX_BLIMP_INTERVAL);
        asteroidSpawnTimer = MathUtils.random(MIN_ASTEROID_INTERVAL, MAX_ASTEROID_INTERVAL);
        alienSpawnTimer = MathUtils.random(MIN_ALIEN_INTERVAL, MAX_ALIEN_INTERVAL);
        gun.setCenterX(WORLD_WIDTH / 2f);
    }

    @Override
    public void render(float delta) {
        handleInput();

        if (state == State.PLAYING) {
            update(delta);
        }

        draw();
    }

    private void handleInput() {
        boolean touched = Gdx.input.isTouched();

        if (touched) {
            touchWorld.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchWorld);
        }

        boolean justTouched = Gdx.input.justTouched();

        // The settings gear is always reachable, in every state, and takes priority
        // over any other tap so it never gets swallowed by gameplay input.
        if (justTouched && withinCircle(touchWorld.x, touchWorld.y, GEAR_X, iconRowY(), ICON_RADIUS)) {
            firing = false;
            game.showSettings();
            return;
        }

        switch (state) {
            case READY:
                firing = false;
                if (justTouched) {
                    resetGame();
                    state = State.PLAYING;
                }
                break;

            case PLAYING:
                if (justTouched && withinCircle(touchWorld.x, touchWorld.y, PAUSE_X, iconRowY(), ICON_RADIUS)) {
                    firing = false;
                    state = State.PAUSED;
                    break;
                }
                if (touched) {
                    gun.setCenterX(touchWorld.x);
                    firing = true;
                } else {
                    firing = false;
                }
                break;

            case PAUSED:
                firing = false;
                if (justTouched) {
                    state = State.PLAYING;
                }
                break;

            case GAME_OVER:
                firing = false;
                if (justTouched) {
                    state = State.READY;
                }
                break;
        }
    }

    /** Y of the gear/pause/bullets row, pinned to the top of the world however tall it ends up. */
    private float iconRowY() {
        return worldHeight - ICON_MARGIN;
    }

    private boolean withinCircle(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void update(float delta) {
        difficultyTime += delta;

        updateSpawning(delta);
        updateBalloons(delta);
        updateBlimpSpawning(delta);
        updateBlimps(delta);
        updateAsteroidSpawning(delta);
        updateAsteroids(delta);
        updateAlienSpawning(delta);
        updateAliens(delta);
        updateFiring(delta);
        updateBasketballs(delta);
        updateExplosions(delta);
        updateBalloonClouds(delta);
        updateMuzzleFlashes(delta);
        updateScorePopups(delta);
        gun.update(delta);
        resolveCollisions();

        if (lives <= 0) {
            state = State.GAME_OVER;
        }
    }

    private void updateSpawning(float delta) {
        spawnTimer -= delta;
        if (spawnTimer <= 0f) {
            spawnBalloon();
            float baseInterval = settings.getBaseSpawnInterval();
            float interval = MathUtils.clamp(
                baseInterval - difficultyTime * 0.01f,
                DIFFICULTY_MIN_SPAWN_INTERVAL, baseInterval);
            spawnTimer = interval * MathUtils.random(0.8f, 1.2f);
        }
    }

    private void updateBlimpSpawning(float delta) {
        blimpSpawnTimer -= delta;
        if (blimpSpawnTimer <= 0f) {
            spawnBlimp();
            blimpSpawnTimer = MathUtils.random(MIN_BLIMP_INTERVAL, MAX_BLIMP_INTERVAL);
        }
    }

    private void spawnBlimp() {
        boolean movingRight = MathUtils.randomBoolean();
        float speed = MathUtils.random(BLIMP_MIN_SPEED, BLIMP_MAX_SPEED) * (movingRight ? 1f : -1f);
        float x = movingRight ? -Blimp.WIDTH / 2f : WORLD_WIDTH + Blimp.WIDTH / 2f;
        float y = MathUtils.random(worldHeight * BLIMP_MIN_Y_FRACTION, worldHeight * BLIMP_MAX_Y_FRACTION);

        blimps.add(new Blimp(x, y, speed));
    }

    private void updateBlimps(float delta) {
        for (int i = blimps.size - 1; i >= 0; i--) {
            Blimp b = blimps.get(i);
            b.update(delta);

            if (!b.alive || (!b.popping && b.isOffScreen(WORLD_WIDTH))) {
                blimps.removeIndex(i);
            }
        }
    }

    private void spawnBalloon() {
        float radius = MathUtils.random(MIN_BALLOON_RADIUS, MAX_BALLOON_RADIUS);
        float x = MathUtils.random(radius, WORLD_WIDTH - radius);
        float y = worldHeight + radius;

        float maxSpeed = Math.min(MAX_FALL_SPEED_CAP, MAX_FALL_SPEED_BASE + difficultyTime * 1.2f);
        float fallSpeed = MathUtils.random(MIN_FALL_SPEED, maxSpeed);

        balloons.add(new Balloon(x, y, radius, fallSpeed, pointsFor(radius, fallSpeed)));
    }

    /**
     * Value of a pop, from MIN_BALLOON_POINTS to MAX_BALLOON_POINTS. Smaller balloons are a
     * narrower target and faster ones give less time to line up, so each raises the payout;
     * speed is judged against the whole run's range, not the current ramp, so a balloon worth 10
     * only shows up once the difficulty ramp is producing near-cap fall speeds.
     */
    private int pointsFor(float radius, float fallSpeed) {
        float smallness = 1f - normalize(radius, MIN_BALLOON_RADIUS, MAX_BALLOON_RADIUS);
        float quickness = normalize(fallSpeed, MIN_FALL_SPEED, MAX_FALL_SPEED_CAP);
        float difficulty = (smallness + quickness) / 2f;

        return Math.round(MathUtils.lerp(MIN_BALLOON_POINTS, MAX_BALLOON_POINTS, difficulty));
    }

    private static float normalize(float value, float min, float max) {
        return MathUtils.clamp((value - min) / (max - min), 0f, 1f);
    }

    private void updateAsteroidSpawning(float delta) {
        asteroidSpawnTimer -= delta;
        if (asteroidSpawnTimer <= 0f) {
            spawnAsteroid();
            asteroidSpawnTimer = MathUtils.random(MIN_ASTEROID_INTERVAL, MAX_ASTEROID_INTERVAL);
        }
    }

    private void spawnAsteroid() {
        float radius = MathUtils.random(MIN_BALLOON_RADIUS, MAX_BALLOON_RADIUS);
        float x = MathUtils.random(radius, WORLD_WIDTH - radius);
        float y = worldHeight + radius;

        float maxSpeed = Math.min(MAX_FALL_SPEED_CAP, MAX_FALL_SPEED_BASE + difficultyTime * 1.2f);
        float fallSpeed = MathUtils.random(MIN_FALL_SPEED, maxSpeed);

        int points = pointsFor(radius, fallSpeed) * ASTEROID_POINTS_MULTIPLIER;
        asteroids.add(new Asteroid(x, y, radius, fallSpeed, points));
    }

    private void updateAsteroids(float delta) {
        for (int i = asteroids.size - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            a.update(delta);

            if (a.hasFallenBelow(0f)) {
                asteroids.removeIndex(i);
                lives--;
                continue;
            }
            if (!a.alive) {
                asteroids.removeIndex(i);
            }
        }
    }

    private void updateAlienSpawning(float delta) {
        alienSpawnTimer -= delta;
        if (alienSpawnTimer <= 0f) {
            spawnAlien();
            alienSpawnTimer = MathUtils.random(MIN_ALIEN_INTERVAL, MAX_ALIEN_INTERVAL);
        }
    }

    private void spawnAlien() {
        float x = MathUtils.random(Alien.CANOPY_WIDTH / 2f, WORLD_WIDTH - Alien.CANOPY_WIDTH / 2f);
        float y = worldHeight + Alien.BODY_HEIGHT / 2f + Alien.RIG_LENGTH + Alien.CANOPY_HEIGHT;
        aliens.add(new Alien(x, y, ALIEN_FALL_SPEED));
    }

    /** No life penalty for a missed alien -- like the blimp, it's a bonus, not a hazard. */
    private void updateAliens(float delta) {
        for (int i = aliens.size - 1; i >= 0; i--) {
            Alien a = aliens.get(i);
            a.update(delta);

            if (a.hasFallenBelow(0f) || !a.alive) {
                aliens.removeIndex(i);
            }
        }
    }

    /** Applies a score delta, spawns a floating +/-N indicator at (x, y), and flashes the HUD score. */
    private void addScoreChange(int amount, float x, float y) {
        if (amount == 0) return;
        score += amount;
        scorePopups.add(new ScorePopup(x, y, amount));
        scoreFlashTimer = SCORE_FLASH_DURATION;
        scoreFlashPositive = amount > 0;
    }

    private void updateBalloons(float delta) {
        for (int i = balloons.size - 1; i >= 0; i--) {
            Balloon b = balloons.get(i);
            b.update(delta);

            if (b.hasFallenBelow(0f)) {
                balloons.removeIndex(i);
                lives--;
                continue;
            }
            if (!b.alive) {
                balloons.removeIndex(i);
            }
        }
    }

    private void updateFiring(float delta) {
        if (burstMessageTimer > 0f) {
            burstMessageTimer = Math.max(0f, burstMessageTimer - delta);
        }

        float fireInterval = settings.getFireInterval();
        if (firing) {
            fireCooldown -= delta;
            if (fireCooldown <= 0f) {
                basketballs.add(new Basketball(gun.getMuzzleX(), gun.getMuzzleY()));
                muzzleFlashes.add(new MuzzleFlash(gun.getMuzzleX(), gun.getMuzzleY()));
                gun.fire();
                fireSound.play(FIRE_VOLUME, MathUtils.random(0.95f, 1.15f), 0f);
                Gdx.input.vibrate(FIRE_VIBRATION_MS);
                addScoreChange(-FIRE_SCORE_PENALTY, gun.getMuzzleX(), gun.getMuzzleY());
                bulletsFired++;
                burstShotCount++;
                fireCooldown = fireInterval;
            }
        } else {
            fireCooldown = Math.min(fireCooldown, fireInterval * 0.5f);
            if (burstShotCount > 0) {
                lastBurstShotCount = burstShotCount;
                burstMessageTimer = BURST_MESSAGE_DURATION;
                burstShotCount = 0;
            }
        }
    }

    private void updateBasketballs(float delta) {
        for (int i = basketballs.size - 1; i >= 0; i--) {
            Basketball ball = basketballs.get(i);
            ball.update(delta);
            if (!ball.alive || ball.isOffScreen(worldHeight)) {
                basketballs.removeIndex(i);
            }
        }
    }

    private void updateExplosions(float delta) {
        for (int i = explosions.size - 1; i >= 0; i--) {
            Explosion e = explosions.get(i);
            e.update(delta);
            if (!e.alive) {
                explosions.removeIndex(i);
            }
        }
    }

    private void updateBalloonClouds(float delta) {
        for (int i = balloonClouds.size - 1; i >= 0; i--) {
            BalloonCloud c = balloonClouds.get(i);
            c.update(delta);
            if (!c.alive) {
                balloonClouds.removeIndex(i);
            }
        }
    }

    private void updateMuzzleFlashes(float delta) {
        for (int i = muzzleFlashes.size - 1; i >= 0; i--) {
            MuzzleFlash f = muzzleFlashes.get(i);
            f.update(delta);
            if (!f.alive) {
                muzzleFlashes.removeIndex(i);
            }
        }
    }

    private void updateScorePopups(float delta) {
        for (int i = scorePopups.size - 1; i >= 0; i--) {
            ScorePopup p = scorePopups.get(i);
            p.timer -= delta;
            if (p.timer <= 0f) {
                scorePopups.removeIndex(i);
            }
        }
        if (scoreFlashTimer > 0f) {
            scoreFlashTimer = Math.max(0f, scoreFlashTimer - delta);
        }
    }

    /** Current rise/fade of a popup, shared by its arrow (shape pass) and its number (text pass). */
    private float popupY(ScorePopup p) {
        float progress = 1f - (p.timer / SCORE_POPUP_DURATION);
        return p.startY + SCORE_POPUP_RISE * progress;
    }

    private float popupAlpha(ScorePopup p) {
        return MathUtils.clamp(p.timer / SCORE_POPUP_DURATION, 0f, 1f);
    }

    private void resolveCollisions() {
        for (int i = basketballs.size - 1; i >= 0; i--) {
            Basketball ball = basketballs.get(i);
            if (!ball.alive) continue;

            for (int j = balloons.size - 1; j >= 0; j--) {
                Balloon b = balloons.get(j);
                if (b.popping || !b.alive) continue;

                if (ball.overlaps(b.x, b.y, b.radius)) {
                    b.pop();
                    addScoreChange(b.points, b.x, b.y);
                    ball.alive = false;
                    balloonClouds.add(new BalloonCloud(b.x, b.y, b.radius / 28f, b.color));
                    popSound.play(POP_VOLUME, MathUtils.random(0.9f, 1.2f), 0f);
                    break;
                }
            }
            if (!ball.alive) continue;

            for (int j = asteroids.size - 1; j >= 0; j--) {
                Asteroid a = asteroids.get(j);
                if (a.popping || !a.alive) continue;

                if (ball.overlaps(a.x, a.y, a.radius)) {
                    a.pop();
                    addScoreChange(a.points, a.x, a.y);
                    ball.alive = false;
                    explosions.add(new Explosion(a.x, a.y, a.radius / 28f));
                    popSound.play(POP_VOLUME, MathUtils.random(0.7f, 0.9f), 0f);
                    break;
                }
            }
            if (!ball.alive) continue;

            for (int j = blimps.size - 1; j >= 0; j--) {
                Blimp blimp = blimps.get(j);
                if (blimp.popping || !blimp.alive) continue;

                if (blimp.overlaps(ball.x, ball.y, Basketball.RADIUS)) {
                    blimp.pop();
                    addScoreChange(Blimp.POINTS, blimp.x, blimp.y);
                    ball.alive = false;
                    explosions.add(new Explosion(blimp.x, blimp.y, Blimp.WIDTH / 50f));
                    popSound.play(POP_VOLUME, MathUtils.random(0.6f, 0.8f), 0f);
                    break;
                }
            }
            if (!ball.alive) continue;

            for (int j = aliens.size - 1; j >= 0; j--) {
                Alien alien = aliens.get(j);
                if (alien.popping || !alien.alive) continue;

                if (alien.overlaps(ball.x, ball.y, Basketball.RADIUS)) {
                    alien.pop();
                    addScoreChange(Alien.POINTS, alien.x, alien.y);
                    ball.alive = false;
                    explosions.add(new Explosion(alien.x, alien.y, Alien.RADIUS / 28f));
                    popSound.play(POP_VOLUME, MathUtils.random(0.8f, 1.0f), 0f);
                    break;
                }
            }
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawGround();
        for (Blimp b : blimps) b.render(shapeRenderer);
        for (Alien a : aliens) a.render(shapeRenderer);
        for (Balloon b : balloons) b.render(shapeRenderer);
        for (Asteroid a : asteroids) a.render(shapeRenderer);
        for (Basketball ball : basketballs) ball.render(shapeRenderer);
        gun.render(shapeRenderer);
        for (MuzzleFlash f : muzzleFlashes) f.render(shapeRenderer);
        for (Explosion e : explosions) e.render(shapeRenderer);
        for (BalloonCloud c : balloonClouds) c.render(shapeRenderer);
        drawScorePopupArrows();
        drawGearIcon();
        drawBulletsGaugeBackground();
        if (state == State.PLAYING || state == State.PAUSED) drawPauseButton();
        shapeRenderer.end();

        batch.begin();
        drawHud();
        drawBulletsGaugeText();
        drawBurstMessage();
        drawScorePopupText();
        if (state == State.READY) drawReadyOverlay();
        if (state == State.PAUSED) drawPausedOverlay();
        if (state == State.GAME_OVER) drawGameOverOverlay();
        batch.end();
    }

    private void drawBulletsGaugeBackground() {
        layout.setText(hudFont, "Bullets: " + bulletsFired);
        float chipWidth = layout.width + 32f;
        float chipHeight = layout.height + 16f;
        float chipX = (WORLD_WIDTH - chipWidth) / 2f;
        float chipY = iconRowY() - chipHeight / 2f;

        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.rect(chipX, chipY, chipWidth, chipHeight);
    }

    private void drawBulletsGaugeText() {
        String text = "Bullets: " + bulletsFired;
        hudFont.setColor(Color.WHITE);
        layout.setText(hudFont, text);
        float x = (WORLD_WIDTH - layout.width) / 2f;
        float y = iconRowY() + layout.height / 2f;
        hudFont.draw(batch, layout, x, y);
    }

    private void drawBurstMessage() {
        if (burstMessageTimer <= 0f) return;

        String text = lastBurstShotCount + (lastBurstShotCount == 1 ? " shot" : " shots");
        float alpha = MathUtils.clamp(burstMessageTimer / BURST_MESSAGE_DURATION, 0f, 1f);
        hudFont.setColor(1f, 1f, 1f, alpha);
        layout.setText(hudFont, text);
        float x = gun.getCenterX() - layout.width / 2f;
        float y = GUN_Y + Gun.HEIGHT + 90f;
        hudFont.draw(batch, layout, x, y);
        hudFont.setColor(Color.WHITE);
    }

    private void drawGearIcon() {
        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.circle(GEAR_X, iconRowY(), ICON_RADIUS, 24);

        shapeRenderer.setColor(Color.WHITE);
        int points = GEAR_TEETH * 2;
        float prevX = GEAR_X + GEAR_TOOTH_R;
        float prevY = iconRowY();
        for (int i = 1; i <= points; i++) {
            float angle = i * MathUtils.PI2 / points;
            float r = (i % 2 == 0) ? GEAR_TOOTH_R : GEAR_BASE_R;
            float x = GEAR_X + MathUtils.cos(angle) * r;
            float y = iconRowY() + MathUtils.sin(angle) * r;
            shapeRenderer.triangle(GEAR_X, iconRowY(), prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }

        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.circle(GEAR_X, iconRowY(), GEAR_HOLE_R, 16);
    }

    private void drawPauseButton() {
        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.circle(PAUSE_X, iconRowY(), ICON_RADIUS, 24);

        shapeRenderer.setColor(Color.WHITE);
        if (state == State.PLAYING) {
            shapeRenderer.rect(PAUSE_X - 10f, iconRowY() - 12f, 7f, 24f);
            shapeRenderer.rect(PAUSE_X + 3f, iconRowY() - 12f, 7f, 24f);
        } else {
            shapeRenderer.triangle(
                PAUSE_X - 9f, iconRowY() - 12f,
                PAUSE_X - 9f, iconRowY() + 12f,
                PAUSE_X + 12f, iconRowY());
        }
    }

    private void drawScorePopupArrows() {
        for (ScorePopup p : scorePopups) {
            float y = popupY(p);
            float alpha = popupAlpha(p);
            boolean positive = p.amount > 0;

            Color color = positive ? SCORE_POSITIVE_COLOR : SCORE_NEGATIVE_COLOR;
            shapeRenderer.setColor(scoreColor.set(color).mul(1f, 1f, 1f, alpha));

            float halfW = SCORE_POPUP_ARROW_WIDTH / 2f;
            if (positive) {
                shapeRenderer.triangle(
                    p.x - halfW, y, p.x + halfW, y, p.x, y + SCORE_POPUP_ARROW_HEIGHT);
            } else {
                shapeRenderer.triangle(
                    p.x - halfW, y + SCORE_POPUP_ARROW_HEIGHT, p.x + halfW, y + SCORE_POPUP_ARROW_HEIGHT, p.x, y);
            }
        }
    }

    private void drawScorePopupText() {
        for (ScorePopup p : scorePopups) {
            float y = popupY(p);
            float alpha = popupAlpha(p);
            boolean positive = p.amount > 0;

            Color color = positive ? SCORE_POSITIVE_COLOR : SCORE_NEGATIVE_COLOR;
            hudFont.setColor(scoreColor.set(color).mul(1f, 1f, 1f, alpha));
            String text = (positive ? "+" : "") + p.amount;
            layout.setText(hudFont, text);
            hudFont.draw(batch, layout, p.x + SCORE_POPUP_ARROW_WIDTH, y + SCORE_POPUP_ARROW_HEIGHT / 2f - layout.height / 2f);
        }
        hudFont.setColor(Color.WHITE);
    }

    private void drawGround() {
        shapeRenderer.setColor(new Color(0.35f, 0.65f, 0.3f, 1f));
        shapeRenderer.rect(0, 0, WORLD_WIDTH, GUN_Y);
    }

    private void drawHud() {
        // Drawn below the gear/pause icon row (which occupies the very top corners) to avoid overlap.
        float hudY = iconRowY() - ICON_RADIUS - 16f;

        if (scoreFlashTimer > 0f) {
            Color flashColor = scoreFlashPositive ? SCORE_POSITIVE_COLOR : SCORE_NEGATIVE_COLOR;
            hudFont.setColor(scoreColor.set(Color.WHITE).lerp(flashColor, scoreFlashTimer / SCORE_FLASH_DURATION));
        } else {
            hudFont.setColor(Color.WHITE);
        }
        hudFont.draw(batch, "Score: " + score, 16, hudY);
        hudFont.setColor(Color.WHITE);

        String livesText = "Lives: " + Math.max(0, lives);
        layout.setText(hudFont, livesText);
        hudFont.draw(batch, livesText, WORLD_WIDTH - layout.width - 16, hudY);
    }

    private void drawReadyOverlay() {
        titleFont.setColor(Color.WHITE);
        centerText(titleFont, "BALLOON POP", worldHeight * 0.62f);

        hudFont.setColor(Color.WHITE);
        centerText(hudFont, "Drag anywhere to move & hold to fire", worldHeight * 0.52f);
        centerText(hudFont, "Tap to start", worldHeight * 0.44f);
    }

    private void drawPausedOverlay() {
        titleFont.setColor(Color.WHITE);
        centerText(titleFont, "PAUSED", worldHeight * 0.62f);

        hudFont.setColor(Color.WHITE);
        centerText(hudFont, "Tap to resume", worldHeight * 0.52f);
    }

    private void drawGameOverOverlay() {
        titleFont.setColor(Color.WHITE);
        centerText(titleFont, "GAME OVER", worldHeight * 0.62f);

        hudFont.setColor(Color.WHITE);
        centerText(hudFont, "Score: " + score, worldHeight * 0.52f);
        centerText(hudFont, "Tap to play again", worldHeight * 0.44f);
    }

    private void centerText(BitmapFont font, String text, float y) {
        layout.setText(font, text);
        font.draw(batch, layout, (WORLD_WIDTH - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        worldHeight = viewport.getWorldHeight();
    }

    /** Called on every navigation back from SettingsScreen, so a new gun skin/sfx style takes effect at once. */
    @Override
    public void show() {
        gun.setSkin(settings.getGunStyle(), settings.getGunColorIndex());
        if (settings.getSfxStyle() != loadedSfxStyle) {
            loadSfx(settings.getSfxStyle());
        }
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        hudFont.dispose();
        titleFont.dispose();
        popSound.dispose();
        fireSound.dispose();
    }
}
