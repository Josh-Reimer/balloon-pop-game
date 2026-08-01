package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** A rare bonus target that drifts horizontally across the screen instead of falling. */
public class Blimp {
    public static final float WIDTH = 96f;
    public static final float HEIGHT = 44f;
    public static final int POINTS = 50;

    private static final float POP_DURATION = 0.2f;
    private static final Color BODY_COLOR = new Color(0.55f, 0.15f, 0.65f, 1f);
    private static final Color FIN_COLOR = new Color(0.4f, 0.1f, 0.5f, 1f);
    private static final Color GONDOLA_COLOR = new Color(0.2f, 0.2f, 0.22f, 1f);

    public float x, y;
    public final float speed; // signed: positive drifts right, negative drifts left

    public boolean alive = true;
    public boolean popping = false;
    private float popTimer = 0f;

    public Blimp(float x, float y, float speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public void update(float delta) {
        if (popping) {
            popTimer += delta;
            if (popTimer >= POP_DURATION) {
                alive = false;
            }
            return;
        }
        x += speed * delta;
    }

    public void pop() {
        if (!popping) {
            popping = true;
            popTimer = 0f;
        }
    }

    public boolean isOffScreen(float worldWidth) {
        if (speed >= 0) return x - WIDTH / 2f > worldWidth;
        return x + WIDTH / 2f < 0;
    }

    /** Elliptical overlap test, inflated by the other shape's radius. */
    public boolean overlaps(float ox, float oy, float otherRadius) {
        float rx = WIDTH / 2f + otherRadius;
        float ry = HEIGHT / 2f + otherRadius;
        float dx = (ox - x) / rx;
        float dy = (oy - y) / ry;
        return dx * dx + dy * dy <= 1f;
    }

    public void render(ShapeRenderer sr) {
        float scale = popping ? Math.max(0f, 1f - popTimer / POP_DURATION) : 1f;
        if (popping && scale <= 0f) return;

        float w = WIDTH * scale;
        float h = HEIGHT * scale;
        float dir = speed >= 0 ? 1f : -1f;

        sr.setColor(BODY_COLOR);
        sr.ellipse(x - w / 2f, y - h / 2f, w, h, 24);

        sr.setColor(FIN_COLOR);
        sr.triangle(
            x - dir * w * 0.5f, y - h * 0.15f,
            x - dir * w * 0.5f, y + h * 0.45f,
            x - dir * w * 0.75f, y + h * 0.15f);

        sr.setColor(GONDOLA_COLOR);
        sr.rect(x - w * 0.18f, y - h * 0.65f, w * 0.36f, h * 0.28f);
    }
}
