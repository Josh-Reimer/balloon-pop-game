package com.joshreimer.balloonpop.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
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

public class SettingsScreen implements Screen {

    private static final float WORLD_WIDTH = 480f;
    private static final float WORLD_HEIGHT = 800f;

    private static final float TRACK_X = 60f;
    private static final float TRACK_WIDTH = WORLD_WIDTH - TRACK_X * 2f;
    private static final float TRACK_HEIGHT = 10f;
    private static final float HANDLE_RADIUS = 18f;

    private static final float FIRE_TRACK_Y = 560f;
    private static final float SPAWN_TRACK_Y = 400f;

    private static final Rectangle BACK_BUTTON = new Rectangle(WORLD_WIDTH / 2f - 90f, 60f, 180f, 64f);

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

    private boolean nearHandle(float trackY, float t) {
        float handleX = TRACK_X + TRACK_WIDTH * t;
        float dx = touchWorld.x - handleX;
        float dy = touchWorld.y - trackY;
        float tapRadius = HANDLE_RADIUS + 14f;
        return dx * dx + dy * dy <= tapRadius * tapRadius;
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

    private void drawBackButton() {
        shapeRenderer.setColor(new Color(0.25f, 0.28f, 0.32f, 1f));
        shapeRenderer.rect(BACK_BUTTON.x, BACK_BUTTON.y, BACK_BUTTON.width, BACK_BUTTON.height);
    }

    private void drawLabels() {
        titleFont.setColor(Color.WHITE);
        centerText(titleFont, "SETTINGS", WORLD_HEIGHT * 0.85f);

        labelFont.setColor(Color.WHITE);
        centerText(labelFont, "Fire Rate: " + percent(settings.getFireRateT()), FIRE_TRACK_Y + 60f);
        centerText(labelFont, "Balloon Rate: " + percent(settings.getSpawnRateT()), SPAWN_TRACK_Y + 60f);

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
    }
}
