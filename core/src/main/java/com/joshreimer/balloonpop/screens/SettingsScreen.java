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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joshreimer.balloonpop.BalloonPopGame;
import com.joshreimer.balloonpop.GameSettings;
import com.joshreimer.balloonpop.entities.GunPalette;
import com.joshreimer.balloonpop.entities.SfxStyle;

public class SettingsScreen implements Screen {

    private static final float WORLD_WIDTH = 480f;
    private static final float WORLD_HEIGHT = 800f;

    private static final float TRACK_X = 60f;
    private static final float TRACK_WIDTH = WORLD_WIDTH - TRACK_X * 2f;
    private static final float TRACK_HEIGHT = 10f;
    private static final float HANDLE_RADIUS = 18f;

    private static final float TITLE_Y = 740f;
    private static final float FIRE_TRACK_Y = 650f;
    private static final float SPAWN_TRACK_Y = 560f;

    // "Customize your sound" section: arrows flanking a style label; tapping either arrow also
    // plays the new pop sound, so the row doubles as its own live preview.
    private static final float SFX_ROW_Y = 480f;
    private static final float SFX_ARROW_RADIUS = 22f;
    private static final float SFX_ARROW_TAP_RADIUS = SFX_ARROW_RADIUS + 12f;
    private static final float SFX_ARROW_X_OFFSET = 130f;

    // "Customize your gun" section: a label, arrows flanking a live preview, then a swatch row.
    private static final float GUN_LABEL_Y = 415f;
    private static final float GUN_PREVIEW_BASE_Y = 255f;
    private static final float ARROW_Y = GUN_PREVIEW_BASE_Y + 45f;
    private static final float ARROW_RADIUS = 26f;
    private static final float ARROW_TAP_RADIUS = ARROW_RADIUS + 12f;
    private static final float LEFT_ARROW_X = 70f;
    private static final float RIGHT_ARROW_X = WORLD_WIDTH - LEFT_ARROW_X;

    private static final float SWATCH_Y = 185f;
    private static final float SWATCH_RADIUS = 18f;
    private static final float SWATCH_TAP_RADIUS = SWATCH_RADIUS + 12f;
    private static final float SWATCH_SPACING = 68f;

    private static final Rectangle BACK_BUTTON = new Rectangle(WORLD_WIDTH / 2f - 90f, 60f, 180f, 64f);

    private static final Color PANEL_COLOR = new Color(0.25f, 0.28f, 0.32f, 1f);
    private static final Color GROUND_COLOR = new Color(0.35f, 0.65f, 0.3f, 1f);

    private enum Slider { NONE, FIRE, SPAWN }

    private final BalloonPopGame game;
    private final GameSettings settings;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont labelFont;
    private final GlyphLayout layout = new GlyphLayout();

    private final Vector3 touchWorld = new Vector3();
    private Slider draggingSlider = Slider.NONE;

    // Lazily (re)loaded whenever the sfx style changes, so tapping the style arrows previews it.
    private Sound previewSound;
    private SfxStyle previewedStyle;

    public SettingsScreen(BalloonPopGame game, GameSettings settings) {
        this.game = game;
        this.settings = settings;

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.6f);

        labelFont = new BitmapFont();
        labelFont.getData().setScale(1.6f);
    }

    @Override
    public void render(float delta) {
        handleInput();
        draw();
    }

    private void handleInput() {
        boolean touched = Gdx.input.isTouched();

        if (!touched) {
            draggingSlider = Slider.NONE;
            return;
        }

        touchWorld.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchWorld);

        if (Gdx.input.justTouched()) {
            if (BACK_BUTTON.contains(touchWorld.x, touchWorld.y)) {
                settings.save();
                game.showGame();
                return;
            }
            if (nearHandle(FIRE_TRACK_Y, settings.getFireRateT())) {
                draggingSlider = Slider.FIRE;
            } else if (nearHandle(SPAWN_TRACK_Y, settings.getSpawnRateT())) {
                draggingSlider = Slider.SPAWN;
            } else if (within(WORLD_WIDTH / 2f - SFX_ARROW_X_OFFSET, SFX_ROW_Y, SFX_ARROW_TAP_RADIUS)) {
                settings.cycleSfxStyle(-1);
                playSfxPreview();
            } else if (within(WORLD_WIDTH / 2f + SFX_ARROW_X_OFFSET, SFX_ROW_Y, SFX_ARROW_TAP_RADIUS)) {
                settings.cycleSfxStyle(1);
                playSfxPreview();
            } else if (within(LEFT_ARROW_X, ARROW_Y, ARROW_TAP_RADIUS)) {
                settings.cycleGunStyle(-1);
            } else if (within(RIGHT_ARROW_X, ARROW_Y, ARROW_TAP_RADIUS)) {
                settings.cycleGunStyle(1);
            } else {
                for (int i = 0; i < GunPalette.size(); i++) {
                    if (within(swatchX(i), SWATCH_Y, SWATCH_TAP_RADIUS)) {
                        settings.setGunColorIndex(i);
                        break;
                    }
                }
            }
        }

        if (draggingSlider != Slider.NONE) {
            float t = MathUtils.clamp((touchWorld.x - TRACK_X) / TRACK_WIDTH, 0f, 1f);
            if (draggingSlider == Slider.FIRE) {
                settings.setFireRateT(t);
            } else {
                settings.setSpawnRateT(t);
            }
        }
    }

    /** (Re)loads the pop sound for the currently selected style if needed, then plays it. */
    private void playSfxPreview() {
        SfxStyle style = settings.getSfxStyle();
        if (style != previewedStyle) {
            if (previewSound != null) {
                previewSound.dispose();
            }
            previewSound = Gdx.audio.newSound(Gdx.files.internal(style.getPopPath()));
            previewedStyle = style;
        }
        previewSound.play(1f);
    }

    private boolean nearHandle(float trackY, float t) {
        return within(TRACK_X + TRACK_WIDTH * t, trackY, HANDLE_RADIUS + 14f);
    }

    private boolean within(float cx, float cy, float radius) {
        float dx = touchWorld.x - cx;
        float dy = touchWorld.y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    /** Centre x of the nth colour swatch, in a row centred on the screen. */
    private static float swatchX(int index) {
        float offset = index - (GunPalette.size() - 1) / 2f;
        return WORLD_WIDTH / 2f + offset * SWATCH_SPACING;
    }

    private void draw() {
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawSlider(FIRE_TRACK_Y, settings.getFireRateT());
        drawSlider(SPAWN_TRACK_Y, settings.getSpawnRateT());
        drawSfxArrows();
        drawGunPreview();
        drawStyleArrows();
        drawColorSwatches();
        drawBackButton();
        shapeRenderer.end();

        batch.begin();
        drawLabels();
        batch.end();
    }

    private void drawSlider(float trackY, float t) {
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.25f));
        shapeRenderer.rect(TRACK_X, trackY - TRACK_HEIGHT / 2f, TRACK_WIDTH, TRACK_HEIGHT);

        shapeRenderer.setColor(new Color(0.9f, 0.55f, 0.1f, 1f));
        shapeRenderer.rect(TRACK_X, trackY - TRACK_HEIGHT / 2f, TRACK_WIDTH * t, TRACK_HEIGHT);

        float handleX = TRACK_X + TRACK_WIDTH * t;
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(handleX, trackY, HANDLE_RADIUS, 24);
        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.circle(handleX, trackY, HANDLE_RADIUS * 0.55f, 24);
    }

    /** The chosen gun, drawn full size and live so tapping an arrow or swatch updates it immediately. */
    private void drawGunPreview() {
        // Opaque green, matching the game's ground: ShapeRenderer draws with blending off,
        // so a translucent colour here would come out solid black.
        shapeRenderer.setColor(GROUND_COLOR);
        shapeRenderer.rect(TRACK_X, GUN_PREVIEW_BASE_Y - 10f, TRACK_WIDTH, 10f);

        settings.getGunStyle().render(
            shapeRenderer, WORLD_WIDTH / 2f, GUN_PREVIEW_BASE_Y, settings.getGunColorIndex());
    }

    private void drawStyleArrows() {
        drawArrow(LEFT_ARROW_X, ARROW_Y, ARROW_RADIUS, -1f);
        drawArrow(RIGHT_ARROW_X, ARROW_Y, ARROW_RADIUS, 1f);
    }

    private void drawSfxArrows() {
        drawArrow(WORLD_WIDTH / 2f - SFX_ARROW_X_OFFSET, SFX_ROW_Y, SFX_ARROW_RADIUS, -1f);
        drawArrow(WORLD_WIDTH / 2f + SFX_ARROW_X_OFFSET, SFX_ROW_Y, SFX_ARROW_RADIUS, 1f);
    }

    private void drawArrow(float cx, float cy, float radius, float direction) {
        shapeRenderer.setColor(PANEL_COLOR);
        shapeRenderer.circle(cx, cy, radius, 24);

        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.triangle(
            cx - 8f * direction, cy - 11f,
            cx - 8f * direction, cy + 11f,
            cx + 10f * direction, cy);
    }

    private void drawColorSwatches() {
        for (int i = 0; i < GunPalette.size(); i++) {
            float cx = swatchX(i);
            if (i == settings.getGunColorIndex()) {
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.circle(cx, SWATCH_Y, SWATCH_RADIUS + 5f, 24);
            }
            shapeRenderer.setColor(GunPalette.body(i));
            shapeRenderer.circle(cx, SWATCH_Y, SWATCH_RADIUS, 24);
            shapeRenderer.setColor(GunPalette.trim(i));
            shapeRenderer.circle(cx, SWATCH_Y, SWATCH_RADIUS * 0.42f, 20);
        }
    }

    private void drawBackButton() {
        shapeRenderer.setColor(PANEL_COLOR);
        shapeRenderer.rect(BACK_BUTTON.x, BACK_BUTTON.y, BACK_BUTTON.width, BACK_BUTTON.height);
    }

    private void drawLabels() {
        titleFont.setColor(Color.WHITE);
        centerText(titleFont, "SETTINGS", TITLE_Y);

        labelFont.setColor(Color.WHITE);
        centerText(labelFont, "Fire Rate: " + percent(settings.getFireRateT()), FIRE_TRACK_Y + 50f);
        centerText(labelFont, "Balloon Rate: " + percent(settings.getSpawnRateT()), SPAWN_TRACK_Y + 50f);
        centerText(labelFont, "Sound: " + settings.getSfxStyle().getDisplayName(), SFX_ROW_Y - 6f);
        centerText(labelFont,
            "Gun: " + settings.getGunColorName() + " " + settings.getGunStyle().getDisplayName(),
            GUN_LABEL_Y);

        layout.setText(labelFont, "BACK");
        labelFont.draw(batch, layout,
            BACK_BUTTON.x + (BACK_BUTTON.width - layout.width) / 2f,
            BACK_BUTTON.y + (BACK_BUTTON.height + layout.height) / 2f);
    }

    private String percent(float t) {
        return Math.round(t * 100f) + "%";
    }

    private void centerText(BitmapFont font, String text, float y) {
        layout.setText(font, text);
        font.draw(batch, layout, (WORLD_WIDTH - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
        titleFont.dispose();
        labelFont.dispose();
        if (previewSound != null) {
            previewSound.dispose();
        }
    }
}
