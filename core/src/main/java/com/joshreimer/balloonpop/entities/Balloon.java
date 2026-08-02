package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A balloon that falls from the top of the screen. Popping it awards {@code points}, which the
 * spawner sets from how hard the balloon is to hit — see {@code GameScreen.pointsFor}.
 */
public class Balloon {
    private static final Color[] PALETTE = {
        new Color(0.90f, 0.20f, 0.25f, 1f),
        new Color(0.20f, 0.55f, 0.90f, 1f),
        new Color(0.95f, 0.80f, 0.15f, 1f),
        new Color(0.30f, 0.75f, 0.35f, 1f),
        new Color(0.65f, 0.30f, 0.85f, 1f),
        new Color(0.95f, 0.55f, 0.15f, 1f),
    };
    private static final Color KNOT_SHADE = new Color(0f, 0f, 0f, 0.35f);
    private static final float POP_DURATION = 0.15f;

    public final float radius;
    public float x, y;
    public float fallSpeed;
    public final Color color;
    public final int points;

    public boolean alive = true;
    public boolean popping = false;
    private float popTimer = 0f;

    public Balloon(float x, float y, float radius, float fallSpeed, int points) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.fallSpeed = fallSpeed;
        this.color = PALETTE[MathUtils.random(PALETTE.length - 1)];
        this.points = points;
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
        float r = radius * (popping ? (1f + (1f - scale) * 0.6f) : 1f) * (popping ? scale : 1f);
        if (popping && r <= 0f) return;

        sr.setColor(color);
        sr.circle(x, y, popping ? r : radius, 24);

        if (!popping) {
            // knot
            sr.setColor(KNOT_SHADE);
            sr.triangle(x - radius * 0.15f, y - radius,
                        x + radius * 0.15f, y - radius,
                        x, y - radius * 1.2f);
            // string
            sr.rectLine(x, y - radius * 1.2f, x, y - radius * 1.6f, 1.2f);
        }
    }
}
