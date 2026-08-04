package com.joshreimer.balloonpop.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
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
import com.joshreimer.balloonpop.audio.AlienVoiceManager;
import com.joshreimer.balloonpop.audio.AlienVoiceType;
import com.joshreimer.balloonpop.entities.Alien;
import com.joshreimer.balloonpop.entities.AmmoStyle;
import com.joshreimer.balloonpop.entities.Asteroid;
import com.joshreimer.balloonpop.entities.Balloon;
import com.joshreimer.balloonpop.entities.BalloonCloud;
import com.joshreimer.balloonpop.entities.Basketball;
import com.joshreimer.balloonpop.entities.Blimp;
import com.joshreimer.balloonpop.entities.Explosion;
import com.joshreimer.balloonpop.entities.Gun;
import com.joshreimer.balloonpop.entities.MuzzleFlash;
import com.joshreimer.balloonpop.entities.SfxStyle;
import com.joshreimer.balloonpop.entities.WaterSplash;

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

    // Overheat: the barrel gives out after this many shots and has to be doused before it will
    // fire again. See beginOverheat() for the sequence.
    private static final int OVERHEAT_SHOT_LIMIT = 267;
    private static final int OVERHEAT_ALIENS_REQUIRED = 3;
    // Per alien that reaches the ground unshot. Lowered when the cloud grew: the swarm can't be
    // shot at, so every alien in it lands, and the charge is really "cloud size x this" — at the
    // old 15 a bigger cloud would have quietly tripled the cost of an overheat.
    private static final int OVERHEAT_ALIEN_PENALTY = 8;
    // A proper cloud, not a trickle: they come down in overlapping pairs so the sky above the
    // stranded gun is genuinely full of them.
    private static final int OVERHEAT_CLOUD_SIZE = 12;
    private static final int OVERHEAT_SPAWN_BATCH = 2;
    private static final float OVERHEAT_ALIEN_SPAWN_INTERVAL = 0.28f;
    // Much faster than a bonus alien's drift: the player can't shoot during an overheat, so a
    // leisurely descent is dead time rather than tension.
    private static final float OVERHEAT_ALIEN_FALL_SPEED = 150f;
    private static final float OVERHEAT_ALIEN_STANDOFF = 58f;
    private static final float OVERHEAT_ALIEN_MIN_SPACING = 34f;
    private static final float OVERHEAT_BANNER_BLINK_RATE = 3.2f;

    // Spoken alien taunts, shown as text so they land with the sound off too.
    private static final float INSULT_TEXT_RISE = 26f;
    private static final Color INSULT_TEXT_COLOR = new Color(0.55f, 1f, 0.6f, 1f);

    // Heat wisps rising off the barrel while it's overheated.
    private static final int HEAT_WISP_COUNT = 5;
    private static final float HEAT_WISP_RISE = 70f;
    private static final float HEAT_WISP_PERIOD = 1.1f;
    private static final Color HEAT_COLOR = new Color(0.92f, 0.32f, 0.12f, 1f);
    private static final Color HEAT_COLOR_HOT = new Color(1f, 0.72f, 0.2f, 1f);

    private static final float ICON_MARGIN = 44f;
    private static final float ICON_RADIUS = 30f;
    private static final float GEAR_X = ICON_MARGIN;
    private static final float PAUSE_X = WORLD_WIDTH - ICON_MARGIN;
    private static final float MUTE_X = GEAR_X + ICON_RADIUS * 2f + 20f;

    private static final int GEAR_TEETH = 8;
    private static final float GEAR_TOOTH_R = 15f;
    private static final float GEAR_BASE_R = 10.5f;
    private static final float GEAR_HOLE_R = 5f;

    private enum State { READY, PLAYING, PAUSED, GAME_OVER }

    /**
     * Sub-phase of {@link State#PLAYING} covering the overheat penalty round. NONE is normal play;
     * SWARM is aliens raining down while the player waits for three of them to reach the ground;
     * DOUSING is the one carrying the bucket walking over to the gun and tipping it.
     */
    private enum Overheat { NONE, SWARM, DOUSING }

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
    private final Array<WaterSplash> waterSplashes = new Array<>();
    private final Array<ScorePopup> scorePopups = new Array<>();

    private final AlienVoiceManager voices;

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

    private Overheat overheat = Overheat.NONE;
    private int shotsSinceCooldown = 0;
    private int overheatLanded = 0;
    private int overheatSpawned = 0;
    private float overheatSpawnTimer = 0f;
    private float overheatTime = 0f;
    private Alien douser = null;
    private boolean dousePoured = false;
    /** The in-flight pour, kept so its spout can be steered to follow the bucket lip each frame. */
    private WaterSplash douseSplash = null;

    private final Vector3 touchWorld = new Vector3();

    /** Scratch for de-overlapping insult text rows; sized past any plausible concurrent count. */
    private final float[] insultRowY = new float[8];

    /** Actual world height for the current screen; the viewport extends it past WORLD_HEIGHT. */
    private float worldHeight = WORLD_HEIGHT;

    public GameScreen(BalloonPopGame game, GameSettings settings, AssetManager assets) {
        this.game = game;
        this.settings = settings;
        this.voices = new AlienVoiceManager(assets, WORLD_WIDTH);

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

    private void playIfUnmuted(Sound sound, float volume, float pitch) {
        if (settings.isMuted()) return;
        sound.play(volume, pitch, 0f);
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
        waterSplashes.clear();
        scorePopups.clear();
        score = 0;
        lives = (int) START_LIVES;
        bulletsFired = 0;
        overheat = Overheat.NONE;
        shotsSinceCooldown = 0;
        overheatLanded = 0;
        overheatSpawned = 0;
        overheatTime = 0f;
        douser = null;
        dousePoured = false;
        douseSplash = null;
        voices.reset();
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

        // Outside the PLAYING guard so a line queued by the shot that ended the run still gets
        // spoken, and so its text ages off the screen instead of freezing there.
        voices.update(delta, settings.isMuted());

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

        // The mute toggle is also always reachable, in every state, for the same reason.
        if (justTouched && withinCircle(touchWorld.x, touchWorld.y, MUTE_X, iconRowY(), ICON_RADIUS)) {
            settings.setMuted(!settings.isMuted());
            settings.save();
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
                    // The gun stays put once an alien is walking a bucket over to it, so it isn't
                    // dragged out from under the pour. Firing is gated separately, in updateFiring.
                    if (overheat != Overheat.DOUSING) {
                        gun.setCenterX(touchWorld.x);
                    }
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

        // Nothing new falls out of the sky during an overheat except the alien swarm itself.
        if (overheat == Overheat.NONE) {
            updateSpawning(delta);
            updateBlimpSpawning(delta);
            updateAsteroidSpawning(delta);
            updateAlienSpawning(delta);
        } else {
            updateOverheat(delta);
        }

        updateBalloons(delta);
        updateBlimps(delta);
        updateAsteroids(delta);
        updateAliens(delta);
        updateFiring(delta);
        updateBasketballs(delta);
        updateExplosions(delta);
        updateBalloonClouds(delta);
        updateMuzzleFlashes(delta);
        updateWaterSplashes(delta);
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
        aliens.add(new Alien(x, y, ALIEN_FALL_SPEED, GUN_Y));
    }

    /**
     * No life penalty for a missed alien -- like the blimp, it's a bonus, not a hazard, and that
     * holds for the overheat swarm too: those cost points, never a life. An un-popped alien lands
     * and waddles off screen under its own steam instead of vanishing.
     */
    private void updateAliens(float delta) {
        for (int i = aliens.size - 1; i >= 0; i--) {
            Alien a = aliens.get(i);
            a.update(delta);

            // Every alien of the swarm that reaches the ground unshot docks the player points.
            if (a.landedThisFrame && a.holdOnLanding) {
                overheatLanded++;
                addScoreChange(-OVERHEAT_ALIEN_PENALTY, a.x, a.y);
                spreadFromNeighbours(a);
            }

            if (!a.alive || (!a.popping && a.isOffScreen(WORLD_WIDTH))) {
                aliens.removeIndex(i);
            }
        }
    }

    /**
     * The barrel has given out after OVERHEAT_SHOT_LIMIT shots. Firing is locked out, the sky is
     * cleared of anything still falling (no balloons fall during an overheat, and the ones already
     * in the air aren't the player's fault), and a cloud of aliens is sent down in their place.
     * Play resumes once three aliens are on the ground and one has doused the gun — see
     * {@link #updateOverheat} and {@link #beginDousing}.
     */
    private void beginOverheat() {
        overheat = Overheat.SWARM;
        overheatLanded = 0;
        overheatSpawned = 0;
        overheatSpawnTimer = 0f;
        overheatTime = 0f;
        douser = null;
        dousePoured = false;
        douseSplash = null;
        firing = false;

        // Popped rather than deleted, so the sky visibly clears instead of things blinking out.
        // No points either way: the player didn't shoot these.
        for (Balloon b : balloons) {
            if (!b.popping) {
                b.pop();
                balloonClouds.add(new BalloonCloud(b.x, b.y, b.radius / 28f, b.color));
            }
        }
        for (Asteroid a : asteroids) {
            if (!a.popping) {
                a.pop();
                explosions.add(new Explosion(a.x, a.y, a.radius / 28f));
            }
        }
    }

    /**
     * Keeps the swarm topped up until three aliens have landed, then hands over to the dousing.
     * Aliens come down in overlapping batches rather than all at once, and more keep coming if
     * in-flight shots knock some of them down before they touch the ground.
     */
    private void updateOverheat(float delta) {
        overheatTime += delta;

        if (overheat == Overheat.SWARM) {
            overheatSpawnTimer -= delta;
            if (overheatSpawnTimer <= 0f && overheatSpawned < OVERHEAT_CLOUD_SIZE) {
                for (int i = 0; i < OVERHEAT_SPAWN_BATCH && overheatSpawned < OVERHEAT_CLOUD_SIZE; i++) {
                    spawnOverheatAlien();
                }
                overheatSpawnTimer = OVERHEAT_ALIEN_SPAWN_INTERVAL;
            }
            // The cloud is exhausted but not enough of it made it down — send another wave.
            if (overheatSpawned >= OVERHEAT_CLOUD_SIZE && countFallingOverheatAliens() == 0
                && overheatLanded < OVERHEAT_ALIENS_REQUIRED) {
                overheatSpawned = 0;
            }
            if (overheatLanded >= OVERHEAT_ALIENS_REQUIRED) {
                beginDousing();
            }
            return;
        }

        // DOUSING: wait for the bucket carrier to reach the gun, tip it, and for the water to finish.
        if (douser == null || !douser.alive) {
            endOverheat();
            return;
        }
        if (!dousePoured && douser.isStanding()) {
            douser.faceToward(gun.getCenterX());
            douser.pour();
            dousePoured = true;
            // Barrel box then body box: the alien is shorter than the barrel, so most of the water
            // lands on the gun's shoulders rather than its muzzle.
            douseSplash = new WaterSplash(
                gun.getCenterX(),
                GUN_Y + Gun.HEIGHT, Gun.BARREL_WIDTH / 2f,
                GUN_Y + Gun.HEIGHT * 0.5f, Gun.WIDTH / 2f,
                GUN_Y, douser.getFacing());
            waterSplashes.add(douseSplash);
            playIfUnmuted(popSound, POP_VOLUME, 0.55f);
        }

        // The spout follows the bucket lip every frame, so the stream stays attached to it as it
        // rolls over and cuts off exactly when the bucket runs dry.
        if (douseSplash != null) {
            douseSplash.setSource(douser.getBucketLipX(), douser.getBucketLipY());
            douseSplash.setEmitting(douser.isPouringWater());
        }

        if (dousePoured && douser.isPourFinished() && waterSplashes.size == 0) {
            endOverheat();
        }
    }

    /**
     * Nudges a just-landed alien clear of any it touched down on top of. With a dozen of them
     * coming down at random x, two landing on the same spot is common, and perfectly overlapped
     * aliens read as a rendering glitch rather than a crowd.
     */
    private void spreadFromNeighbours(Alien landed) {
        for (int attempt = 0; attempt < 4; attempt++) {
            boolean moved = false;
            for (Alien other : aliens) {
                if (other == landed || !other.isOnGround() || other.popping || !other.alive) continue;

                float gap = other.x - landed.x;
                if (Math.abs(gap) >= OVERHEAT_ALIEN_MIN_SPACING) continue;

                // Push away from the neighbour, or pick a side if they're exactly coincident.
                float push = gap == 0f
                    ? (MathUtils.randomBoolean() ? 1f : -1f)
                    : -Math.signum(gap);
                landed.x = MathUtils.clamp(
                    landed.x + push * (OVERHEAT_ALIEN_MIN_SPACING - Math.abs(gap)),
                    Alien.BODY_WIDTH, WORLD_WIDTH - Alien.BODY_WIDTH);
                moved = true;
            }
            if (!moved) return;
        }
    }

    private void spawnOverheatAlien() {
        float x = MathUtils.random(Alien.CANOPY_WIDTH / 2f, WORLD_WIDTH - Alien.CANOPY_WIDTH / 2f);
        // Staggered above the top edge and given slightly different fall speeds, so a batch
        // spawned on the same frame doesn't come down as a flat rank.
        float y = worldHeight + Alien.BODY_HEIGHT / 2f + Alien.RIG_LENGTH + Alien.CANOPY_HEIGHT
            + MathUtils.random(0f, 220f);
        float speed = OVERHEAT_ALIEN_FALL_SPEED * MathUtils.random(0.85f, 1.15f);
        Alien alien = new Alien(x, y, speed, GUN_Y);
        alien.holdOnLanding = true;
        aliens.add(alien);
        overheatSpawned++;
    }

    private int countFallingOverheatAliens() {
        int count = 0;
        for (Alien a : aliens) {
            if (a.holdOnLanding && a.alive && !a.popping && !a.isOnGround()) count++;
        }
        return count;
    }

    /** Picks the landed alien nearest the gun and sends it over with the bucket. */
    private void beginDousing() {
        Alien nearest = null;
        float bestDistance = Float.MAX_VALUE;
        for (Alien a : aliens) {
            if (!a.holdOnLanding || !a.alive || a.popping || !a.isStanding()) continue;
            float distance = Math.abs(a.x - gun.getCenterX());
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = a;
            }
        }
        if (nearest == null) {
            // Every candidate was shot out from under us between landing and now; keep waiting.
            overheatLanded = Math.max(0, overheatLanded - 1);
            return;
        }

        overheat = Overheat.DOUSING;
        douser = nearest;
        douser.giveBucket();
        // Stands to whichever side of the gun it's already on, so it doesn't walk through it.
        float side = douser.x <= gun.getCenterX() ? -1f : 1f;
        douser.walkTo(MathUtils.clamp(
            gun.getCenterX() + side * OVERHEAT_ALIEN_STANDOFF, 20f, WORLD_WIDTH - 20f));
    }

    /** Water's done: the barrel is cool, the aliens wander off, and normal play resumes. */
    private void endOverheat() {
        overheat = Overheat.NONE;
        shotsSinceCooldown = 0;
        douser = null;
        dousePoured = false;
        douseSplash = null;

        // Clearing the flag matters as much as releasing them: any of the swarm still in the air
        // reverts to an ordinary bonus alien, so it lands and waddles off rather than standing
        // around forever waiting for a release that only the *next* overheat would give it.
        for (Alien a : aliens) {
            if (!a.holdOnLanding) continue;
            a.holdOnLanding = false;
            if (a.isOnGround()) {
                a.walkAway();
            }
        }

        // Give the player a beat before the first balloon of the new round arrives.
        spawnTimer = settings.getBaseSpawnInterval();
        blimpSpawnTimer = MathUtils.random(MIN_BLIMP_INTERVAL, MAX_BLIMP_INTERVAL);
        asteroidSpawnTimer = MathUtils.random(MIN_ASTEROID_INTERVAL, MAX_ASTEROID_INTERVAL);
        alienSpawnTimer = MathUtils.random(MIN_ALIEN_INTERVAL, MAX_ALIEN_INTERVAL);
    }

    /** 0..1 fraction of the way to an overheat; drives the HUD gauge fill. */
    private float heatFraction() {
        if (overheat != Overheat.NONE) return 1f;
        return MathUtils.clamp((float) shotsSinceCooldown / OVERHEAT_SHOT_LIMIT, 0f, 1f);
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
        if (firing && overheat == Overheat.NONE) {
            fireCooldown -= delta;
            if (fireCooldown <= 0f) {
                basketballs.add(new Basketball(gun.getMuzzleX(), gun.getMuzzleY(), settings.getAmmoStyle()));
                muzzleFlashes.add(new MuzzleFlash(gun.getMuzzleX(), gun.getMuzzleY()));
                gun.fire();
                playIfUnmuted(fireSound, FIRE_VOLUME, MathUtils.random(0.95f, 1.15f));
                Gdx.input.vibrate(FIRE_VIBRATION_MS);
                addScoreChange(-FIRE_SCORE_PENALTY, gun.getMuzzleX(), gun.getMuzzleY());
                bulletsFired++;
                burstShotCount++;
                shotsSinceCooldown++;
                fireCooldown = fireInterval;

                if (shotsSinceCooldown >= OVERHEAT_SHOT_LIMIT) {
                    beginOverheat();
                }
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

    private void updateWaterSplashes(float delta) {
        for (int i = waterSplashes.size - 1; i >= 0; i--) {
            WaterSplash w = waterSplashes.get(i);
            w.update(delta);
            if (!w.alive) {
                waterSplashes.removeIndex(i);
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
                    playIfUnmuted(popSound, POP_VOLUME, MathUtils.random(0.9f, 1.2f));
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
                    playIfUnmuted(popSound, POP_VOLUME, MathUtils.random(0.7f, 0.9f));
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
                    playIfUnmuted(popSound, POP_VOLUME, MathUtils.random(0.6f, 0.8f));
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
                    playIfUnmuted(popSound, POP_VOLUME, MathUtils.random(0.8f, 1.0f));
                    // It gets the last word in. The swarm has its own, gun-specific set of lines.
                    voices.onAlienDeath(
                        alien.holdOnLanding ? AlienVoiceType.SWARM : AlienVoiceType.SCOUT,
                        alien.x, alien.y, gun.getCenterX());
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
        if (overheat != Overheat.NONE) drawHeatWisps();
        for (MuzzleFlash f : muzzleFlashes) f.render(shapeRenderer);
        for (Explosion e : explosions) e.render(shapeRenderer);
        for (BalloonCloud c : balloonClouds) c.render(shapeRenderer);
        for (WaterSplash w : waterSplashes) w.render(shapeRenderer);
        drawScorePopupArrows();
        drawGearIcon();
        drawMuteIcon();
        drawBulletsGaugeBackground();
        if (state == State.PLAYING || state == State.PAUSED) drawPauseButton();
        shapeRenderer.end();

        batch.begin();
        drawHud();
        drawBulletsGaugeText();
        drawBurstMessage();
        if (overheat != Overheat.NONE) drawOverheatBanner();
        drawInsults();
        drawScorePopupText();
        if (state == State.READY) drawReadyOverlay();
        if (state == State.PAUSED) drawPausedOverlay();
        if (state == State.GAME_OVER) drawGameOverOverlay();
        batch.end();
    }

    /**
     * The bullet chip doubles as the heat gauge: it fills left-to-right with barrel heat as the
     * shot count climbs toward OVERHEAT_SHOT_LIMIT, so the overheat never arrives unannounced.
     */
    private void drawBulletsGaugeBackground() {
        layout.setText(hudFont, bulletsGaugeText());
        float chipWidth = layout.width + 32f;
        float chipHeight = layout.height + 16f;
        float chipX = (WORLD_WIDTH - chipWidth) / 2f;
        float chipY = iconRowY() - chipHeight / 2f;

        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.rect(chipX, chipY, chipWidth, chipHeight);

        float heat = heatFraction();
        if (heat > 0f) {
            shapeRenderer.setColor(scoreColor.set(HEAT_COLOR_HOT).lerp(HEAT_COLOR, heat));
            shapeRenderer.rect(chipX, chipY, chipWidth * heat, chipHeight);
        }
    }

    private void drawBulletsGaugeText() {
        String text = bulletsGaugeText();
        hudFont.setColor(Color.WHITE);
        layout.setText(hudFont, text);
        float x = (WORLD_WIDTH - layout.width) / 2f;
        float y = iconRowY() + layout.height / 2f;
        hudFont.draw(batch, layout, x, y);
    }

    private String bulletsGaugeText() {
        return overheat == Overheat.NONE ? "Bullets: " + bulletsFired : "OVERHEATED";
    }

    /** Flame-coloured wisps curling off the barrel of an overheated gun. */
    private void drawHeatWisps() {
        float muzzleX = gun.getMuzzleX();
        float muzzleY = gun.getMuzzleY();

        for (int i = 0; i < HEAT_WISP_COUNT; i++) {
            // Staggered so the wisps stream continuously instead of pulsing together.
            float t = ((overheatTime / HEAT_WISP_PERIOD) + (float) i / HEAT_WISP_COUNT) % 1f;
            float rise = HEAT_WISP_RISE * t;
            float wobble = MathUtils.sin(t * MathUtils.PI2 * 1.5f + i) * 12f;
            float radius = 7f * MathUtils.sin(t * MathUtils.PI);
            if (radius <= 0.5f) continue;

            shapeRenderer.setColor(scoreColor.set(HEAT_COLOR_HOT).lerp(HEAT_COLOR, t));
            shapeRenderer.circle(muzzleX + wobble, muzzleY + rise, radius, 10);
        }
    }

    /**
     * The overheat message, blinking above the gun. Doubles as the progress readout for the wait:
     * the player has no other way to know how many aliens still have to come down.
     */
    private void drawOverheatBanner() {
        boolean blinkOn = MathUtils.sin(overheatTime * OVERHEAT_BANNER_BLINK_RATE) > -0.35f;
        float y = iconRowY() - ICON_RADIUS - 90f;

        if (blinkOn) {
            hudFont.setColor(SCORE_NEGATIVE_COLOR);
            centerText(hudFont, "!! GUN OVERHEATED !!", y);
        }

        hudFont.setColor(Color.WHITE);
        if (overheat == Overheat.SWARM) {
            int remaining = Math.max(0, OVERHEAT_ALIENS_REQUIRED - overheatLanded);
            centerText(hudFont, "Aliens still to land: " + remaining, y - 34f);
        } else {
            centerText(hudFont, dousePoured ? "Cooling the barrel..." : "Here comes the water...", y - 34f);
        }
        hudFont.setColor(Color.WHITE);
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

    /** A speaker icon: sound-wave arcs when audio is on, a diagonal strike-through when muted. */
    private void drawMuteIcon() {
        float cy = iconRowY();
        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.circle(MUTE_X, cy, ICON_RADIUS, 24);

        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(MUTE_X - 13f, cy - 6f, 7f, 12f);
        shapeRenderer.triangle(MUTE_X - 6f, cy - 6f, MUTE_X - 6f, cy + 6f, MUTE_X + 7f, cy + 13f);
        shapeRenderer.triangle(MUTE_X - 6f, cy - 6f, MUTE_X + 7f, cy + 13f, MUTE_X + 7f, cy - 13f);

        if (settings.isMuted()) {
            shapeRenderer.setColor(SCORE_NEGATIVE_COLOR);
            shapeRenderer.rectLine(MUTE_X - 16f, cy - 16f, MUTE_X + 16f, cy + 16f, 4f);
        } else {
            shapeRenderer.rectLine(MUTE_X + 12f, cy - 4f, MUTE_X + 16f, cy - 8f, 3f);
            shapeRenderer.rectLine(MUTE_X + 16f, cy - 8f, MUTE_X + 16f, cy + 8f, 3f);
            shapeRenderer.rectLine(MUTE_X + 16f, cy + 8f, MUTE_X + 12f, cy + 4f, 3f);
        }
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

    /**
     * The spoken insults, as text above where the alien died. Drawn regardless of the mute state —
     * it exists so the lines land with the sound off, so gating it on audio would defeat the point.
     *
     * <p>Clamped on both axes. Vertical matters more than it looks: aliens are very often shot the
     * moment they enter at the top of the screen, and text placed above such a kill lands entirely
     * off-screen — the line is then silently lost for exactly the players who most need it.
     */
    private void drawInsults() {
        Array<AlienVoiceManager.ActiveInsult> insults = voices.getActiveInsults();
        int placed = 0;

        for (int i = 0; i < insults.size; i++) {
            AlienVoiceManager.ActiveInsult insult = insults.get(i);
            float progress = 1f - insult.timer / AlienVoiceManager.INSULT_TEXT_DURATION;
            // Colour must be set before setText: GlyphLayout bakes the font's colour at layout
            // time, so setting it afterwards silently leaves the line whatever colour came before.
            hudFont.setColor(INSULT_TEXT_COLOR);
            layout.setText(hudFont, insult.text);

            float x = MathUtils.clamp(
                insult.x - layout.width / 2f, 8f, Math.max(8f, WORLD_WIDTH - layout.width - 8f));
            float y = insult.y + Alien.HEAD_RY + 24f + INSULT_TEXT_RISE * progress;
            // Keep clear of the HUD row at the top as well as the screen edge.
            float ceiling = iconRowY() - ICON_RADIUS - 52f;
            y = MathUtils.clamp(y, layout.height + 8f, ceiling);

            // Two kills near the top both clamp to the ceiling and would print on top of each
            // other, which is unreadable — exactly the case the text exists to serve. Step any
            // later line down until it clears the ones already placed.
            float lineHeight = layout.height + 10f;
            for (int guard = 0; guard < placed; guard++) {
                boolean clear = true;
                for (int j = 0; j < placed; j++) {
                    if (Math.abs(y - insultRowY[j]) < lineHeight) {
                        y = insultRowY[j] - lineHeight;
                        clear = false;
                    }
                }
                if (clear) break;
            }

            if (placed < insultRowY.length) {
                insultRowY[placed++] = y;
            }

            hudFont.draw(batch, layout, x, y);
        }
        hudFont.setColor(Color.WHITE);
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
        voices.dispose();
    }
}
