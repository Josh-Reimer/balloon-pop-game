package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A rare, high-value target that falls like a {@link Balloon} but is worth triple the points --
 * see {@code GameScreen.spawnAsteroid}. Rendered as a jagged rock instead of a balloon.
 */
public class Asteroid {
    private static final Color BODY_COLOR = new Color(0.48f, 0.44f, 0.4f, 1f);
    private static final Color CRATER_COLOR = new Color(0.32f, 0.29f, 0.26f, 1f);
    private static final float POP_DURATION = 0.18f;
    private static final int SIDES = 9;

    public final float radius;
    public float x, y;
    public float fallSpeed;
    public final int points;

    public boolean alive = true;
    public boolean popping = false;
    private float popTimer = 0f;

    private final float rotation;
    private final float[] jag = new float[SIDES];

    public Asteroid(float x, float y, float radius, float fallSpeed, int points) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.fallSpeed = fallSpeed;
        this.points = points;
        this.rotation = MathUtils.random(MathUtils.PI2);
        for (int i = 0; i < SIDES; i++) {
            jag[i] = MathUtils.random(0.75f, 1f);
        }
    }

    public void update(float delta) {
        if (popping) {
            popTimer += delta;
            if (popTimer >= POP_DURATION) {
                alive = false;
            }
            return;
        }
        y -= fallSpeed * delta;
    }

    public void pop() {
        if (!popping) {
            popping = true;
            popTimer = 0f;
        }
    }

    public boolean hasFallenBelow(float minY) {
        return !popping && y + radius < minY;
    }

    public boolean overlaps(float ox, float oy, float otherRadius) {
        float dx = x - ox;
        float dy = y - oy;
        float distSq = dx * dx + dy * dy;
        float rSum = radius + otherRadius;
        return distSq <= rSum * rSum;
    }

    public void render(ShapeRenderer sr) {
        float scale = popping ? Math.max(0f, 1f - popTimer / POP_DURATION) : 1f;
        if (popping && scale <= 0f) return;

        float r = radius * scale;

        sr.setColor(BODY_COLOR);
        float prevAngle = rotation;
        float prevRadius = r * jag[0];
        float prevX = x + MathUtils.cos(prevAngle) * prevRadius;
        float prevY = y + MathUtils.sin(prevAngle) * prevRadius;
        for (int i = 1; i <= SIDES; i++) {
            float angle = rotation + i * MathUtils.PI2 / SIDES;
            float rad = r * jag[i % SIDES];
            float vx = x + MathUtils.cos(angle) * rad;
            float vy = y + MathUtils.sin(angle) * rad;
            sr.triangle(x, y, prevX, prevY, vx, vy);
            prevX = vx;
            prevY = vy;
        }

        if (!popping) {
            sr.setColor(CRATER_COLOR);
            sr.circle(x - r * 0.3f, y + r * 0.2f, r * 0.18f, 10);
            sr.circle(x + r * 0.28f, y - r * 0.18f, r * 0.14f, 10);
            sr.circle(x - r * 0.05f, y - r * 0.35f, r * 0.11f, 10);
        }
    }
}
