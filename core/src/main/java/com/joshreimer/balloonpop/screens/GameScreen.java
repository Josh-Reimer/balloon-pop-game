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
import com.joshreimer.balloonpop.entities.Balloon;
import com.joshreimer.balloonpop.entities.BalloonCloud;
import com.joshreimer.balloonpop.entities.Basketball;
import com.joshreimer.balloonpop.entities.Blimp;
import com.joshreimer.balloonpop.entities.Explosion;
import com.joshreimer.balloonpop.entities.Gun;
import com.joshreimer.balloonpop.entities.MuzzleFlash;

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

    private static final float POP_VOLUME = 0.6f;
    private static final float FIRE_VOLUME = 0.4f;
    private static final int FIRE_SCORE_PENALTY = 5;
    private static final float BURST_MESSAGE_DURATION = 1.4f;
    private static final int FIRE_VIBRATION_MS = 6;

    private static final float MIN_BLIMP_INTERVAL = 9f;
    private static final float MAX_BLIMP_INTERVAL = 18f;
    private static final float BLIMP_MIN_SPEED = 80f;
    private static final float BLIMP_MAX_SPEED = 130f;
    private static final float BLIMP_MIN_Y_FRACTION = 0.55f;
    private static final float BLIMP_MAX_Y_FRACTION = 0.85f;

    private static final float ICON_MARGIN = 44f;
    private static final float ICON_RADIUS = 30f;
    private static final float GEAR_X = ICON_MARGIN;
    private static final float PAUSE_X = WORLD_WIDTH - ICON_MARGIN;

    private static final int GEAR_TEETH = 8;
    private static final float GEAR_TOOTH_R = 15f;
    private static final float GEAR_BASE_R = 10.5f;
    private static final float GEAR_HOLE_R = 5f;

    private enum State { READY, PLAYING, PAUSED, GAME_OVER }

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
    private final Array<Basketball> basketballs = new Array<>();
    private final Array<Blimp> blimps = new Array<>();
    private final Array<Explosion> explosions = new Array<>();
    private final Array<BalloonCloud> balloonClouds = new Array<>();
    private final Array<MuzzleFlash> muzzleFlashes = new Array<>();

    private final Sound popSound;
    private final Sound fireSound;

    private State state = State.READY;
    private int score = 0;
    private int lives = (int) START_LIVES;
    private int bulletsFired = 0;

    private int burstShotCount = 0;
    private int lastBurstShotCount = 0;
    private float burstMessageTimer = 0f;

    private float spawnTimer = 0f;
    private float difficultyTime = 0f;
    private float fireCooldown = 0f;
    private float blimpSpawnTimer = 0f;
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

        popSound = Gdx.audio.newSound(Gdx.files.internal("pop.wav"));
        fireSound = Gdx.audio.newSound(Gdx.files.internal("fire.wav"));
    }

    private void resetGame() {
        balloons.clear();
        basketballs.clear();
        blimps.clear();
        explosions.clear();
        balloonClouds.clear();
        muzzleFlashes.clear();
        score = 0;
        lives = (int) START_LIVES;
        bulletsFired = 0;
        burstShotCount = 0;
        lastBurstShotCount = 0;
        burstMessageTimer = 0f;
        spawnTimer = 0f;
        difficultyTime = 0f;
        fireCooldown = 0f;
        blimpSpawnTimer = MathUtils.random(MIN_BLIMP_INTERVAL, MAX_BLIMP_INTERVAL);
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
        updateFiring(delta);
        updateBasketballs(delta);
        updateExplosions(delta);
        updateBalloonClouds(delta);
        updateMuzzleFlashes(delta);
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
        float radius = MathUtils.random(24f, 34f);
        float x = MathUtils.random(radius, WORLD_WIDTH - radius);
        float y = worldHeight + radius;

        float maxSpeed = Math.min(MAX_FALL_SPEED_CAP, MAX_FALL_SPEED_BASE + difficultyTime * 1.2f);
        float fallSpeed = MathUtils.random(MIN_FALL_SPEED, maxSpeed);

        balloons.add(new Balloon(x, y, radius, fallSpeed));
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
                score -= FIRE_SCORE_PENALTY;
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

    private void resolveCollisions() {
        for (int i = basketballs.size - 1; i >= 0; i--) {
            Basketball ball = basketballs.get(i);
            if (!ball.alive) continue;

            for (int j = balloons.size - 1; j >= 0; j--) {
                Balloon b = balloons.get(j);
                if (b.popping || !b.alive) continue;

                if (ball.overlaps(b.x, b.y, b.radius)) {
                    b.pop();
                    score += b.points;
                    ball.alive = false;
                    balloonClouds.add(new BalloonCloud(b.x, b.y, b.radius / 28f, b.color));
                    popSound.play(POP_VOLUME, MathUtils.random(0.9f, 1.2f), 0f);
                    break;
                }
            }
            if (!ball.alive) continue;

            for (int j = blimps.size - 1; j >= 0; j--) {
                Blimp blimp = blimps.get(j);
                if (blimp.popping || !blimp.alive) continue;

                if (blimp.overlaps(ball.x, ball.y, Basketball.RADIUS)) {
                    blimp.pop();
                    score += Blimp.POINTS;
                    ball.alive = false;
                    explosions.add(new Explosion(blimp.x, blimp.y, Blimp.WIDTH / 50f));
                    popSound.play(POP_VOLUME, MathUtils.random(0.6f, 0.8f), 0f);
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
        for (Balloon b : balloons) b.render(shapeRenderer);
        for (Basketball ball : basketballs) ball.render(shapeRenderer);
        gun.render(shapeRenderer);
        for (MuzzleFlash f : muzzleFlashes) f.render(shapeRenderer);
        for (Explosion e : explosions) e.render(shapeRenderer);
        for (BalloonCloud c : balloonClouds) c.render(shapeRenderer);
        drawGearIcon();
        drawBulletsGaugeBackground();
        if (state == State.PLAYING || state == State.PAUSED) drawPauseButton();
        shapeRenderer.end();

        batch.begin();
        drawHud();
        drawBulletsGaugeText();
        drawBurstMessage();
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

    private void drawGround() {
        shapeRenderer.setColor(new Color(0.35f, 0.65f, 0.3f, 1f));
        shapeRenderer.rect(0, 0, WORLD_WIDTH, GUN_Y);
    }

    private void drawHud() {
        // Drawn below the gear/pause icon row (which occupies the very top corners) to avoid overlap.
        float hudY = iconRowY() - ICON_RADIUS - 16f;

        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "Score: " + score, 16, hudY);

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

    @Override
    public void show() {}

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
