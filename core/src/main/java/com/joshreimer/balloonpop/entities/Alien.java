package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A rare bonus target that drifts down slowly under a parachute instead of falling freely like a
 * {@link Balloon} — see {@code GameScreen.spawnAlien}. Its body is a green-to-blue vertical
 * gradient, built by feeding per-vertex colours (derived from each vertex's world Y) into
 * {@link ShapeRenderer}'s coloured triangle/rect overloads rather than a single flat fill.
 */
public class Alien {
    public static final float BODY_WIDTH = 34f;
    public static final float BODY_HEIGHT = 30f;
    public static final float HEAD_RADIUS = 17f;
    public static final float CANOPY_WIDTH = 60f;
    public static final float CANOPY_HEIGHT = 26f;
    public static final float RIG_LENGTH = 22f;
    public static final float RADIUS = HEAD_RADIUS + 4f; // collision radius, body

    public static final int POINTS = 25;

    private static final float POP_DURATION = 0.2f;
    private static final float SWAY_AMPLITUDE = 14f;
    private static final float SWAY_FREQUENCY = 1.1f;

    private static final Color TOP_COLOR = new Color(0.25f, 0.85f, 0.4f, 1f);
    private static final Color BOTTOM_COLOR = new Color(0.2f, 0.5f, 0.9f, 1f);
    private static final Color CANOPY_EDGE_COLOR = new Color(0.15f, 0.35f, 0.55f, 1f);
    private static final Color EYE_COLOR = new Color(0.05f, 0.08f, 0.1f, 1f);

    private final float baseX;
    public float x, y;
    public float fallSpeed;

    public boolean alive = true;
    public boolean popping = false;
    private float popTimer = 0f;
    private float age = 0f;
    private final float swayPhase;

    private final Color tmpColor = new Color();
    private final Color gradCenter = new Color();
    private final Color gradPrev = new Color();
    private final Color gradCur = new Color();

    public Alien(float x, float y, float fallSpeed) {
        this.baseX = x;
        this.x = x;
        this.y = y;
        this.fallSpeed = fallSpeed;
        this.swayPhase = MathUtils.random(MathUtils.PI2);
    }

    public void update(float delta) {
        if (popping) {
            popTimer += delta;
            if (popTimer >= POP_DURATION) {
                alive = false;
            }
            return;
        }
        age += delta;
        y -= fallSpeed * delta;
        x = baseX + MathUtils.sin(age * SWAY_FREQUENCY + swayPhase) * SWAY_AMPLITUDE;
    }

    public void pop() {
        if (!popping) {
            popping = true;
            popTimer = 0f;
        }
    }

    public boolean hasFallenBelow(float minY) {
        return !popping && y + BODY_HEIGHT / 2f + RIG_LENGTH + CANOPY_HEIGHT < minY;
    }

    public boolean overlaps(float ox, float oy, float otherRadius) {
        float dx = x - ox;
        float dy = y - oy;
        float distSq = dx * dx + dy * dy;
        float rSum = RADIUS + otherRadius;
        return distSq <= rSum * rSum;
    }

    /** Gradient colour for a vertex at world height vy, interpolated across [botY, topY]. */
    private Color colorAt(float vy, float topY, float botY) {
        float t = MathUtils.clamp((vy - botY) / (topY - botY), 0f, 1f);
        return tmpColor.set(BOTTOM_COLOR).lerp(TOP_COLOR, t);
    }

    public void render(ShapeRenderer sr) {
        float scale = popping ? Math.max(0f, 1f - popTimer / POP_DURATION) : 1f;
        if (popping && scale <= 0f) return;

        float canopyY = y + BODY_HEIGHT / 2f + RIG_LENGTH;
        float topY = canopyY + CANOPY_HEIGHT * scale;
        float botY = y - BODY_HEIGHT / 2f * scale;

        // Rigging lines from the canopy rim to the alien's shoulders.
        Color rigColor = colorAt(canopyY, topY, botY);
        sr.setColor(rigColor);
        sr.rectLine(x - CANOPY_WIDTH / 2f * scale, canopyY, x - BODY_WIDTH / 3f * scale, y + BODY_HEIGHT / 2f * scale, 1.2f);
        sr.rectLine(x + CANOPY_WIDTH / 2f * scale, canopyY, x + BODY_WIDTH / 3f * scale, y + BODY_HEIGHT / 2f * scale, 1.2f);

        filledGradientDome(sr, x, canopyY, CANOPY_WIDTH / 2f * scale, CANOPY_HEIGHT * scale, 10, topY, botY);
        sr.setColor(CANOPY_EDGE_COLOR);
        sr.rectLine(x - CANOPY_WIDTH / 2f * scale, canopyY, x + CANOPY_WIDTH / 2f * scale, canopyY, 1.5f);

        filledGradientEllipse(sr, x, y, BODY_WIDTH / 2f * scale, BODY_HEIGHT / 2f * scale, 16, topY, botY);

        float headY = y + BODY_HEIGHT / 2f * scale + HEAD_RADIUS * 0.7f * scale;
        filledGradientEllipse(sr, x, headY, HEAD_RADIUS * scale, HEAD_RADIUS * scale, 20, topY, botY);

        if (!popping) {
            sr.setColor(EYE_COLOR);
            float eyeR = HEAD_RADIUS * 0.28f;
            sr.ellipse(x - HEAD_RADIUS * 0.5f - eyeR / 2f, headY - eyeR * 0.3f, eyeR, eyeR * 1.6f, 12);
            sr.ellipse(x + HEAD_RADIUS * 0.5f - eyeR / 2f, headY - eyeR * 0.3f, eyeR, eyeR * 1.6f, 12);
        }
    }

    /** Fan-triangulated ellipse whose fill colour is derived per-vertex from world Y, not flat. */
    private void filledGradientEllipse(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                        int segments, float topY, float botY) {
        filledGradientFan(sr, cx, cy, rx, ry, segments, MathUtils.PI2, topY, botY);
    }

    /** Same as {@link #filledGradientEllipse}, but only the upper half — a parachute canopy dome. */
    private void filledGradientDome(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                     int segments, float topY, float botY) {
        filledGradientFan(sr, cx, cy, rx, ry, segments, MathUtils.PI, topY, botY);
    }

    /** Fan-triangulates an arc of an ellipse from angle 0 to sweep, colouring each vertex by its world Y. */
    private void filledGradientFan(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                    int segments, float sweep, float topY, float botY) {
        gradCenter.set(colorAt(cy, topY, botY));
        float prevX = cx + rx;
        float prevY = cy;
        gradPrev.set(colorAt(prevY, topY, botY));
        for (int i = 1; i <= segments; i++) {
            float angle = sweep * i / segments;
            float vx = cx + MathUtils.cos(angle) * rx;
            float vy = cy + MathUtils.sin(angle) * ry;
            gradCur.set(colorAt(vy, topY, botY));
            sr.triangle(cx, cy, prevX, prevY, vx, vy, gradCenter, gradPrev, gradCur);
            prevX = vx;
            prevY = vy;
            gradPrev.set(gradCur);
        }
    }
}
