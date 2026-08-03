package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * The projectile looks offered by the "customize your ammo" settings section. Every style draws
 * inside the same {@link Basketball#RADIUS} circle, so the choice is purely cosmetic — travel speed
 * and hit behaviour are identical for all of them. Also drawn stand-alone by {@code SettingsScreen}
 * for the live preview, hence the style-agnostic entry point.
 */
public enum AmmoStyle {

    /** The original: orange ball with the two crossing seams. */
    BASKETBALL("Basketball") {
        @Override
        public void render(ShapeRenderer sr, float x, float y, float radius) {
            sr.setColor(0.85f, 0.42f, 0.09f, 1f);
            sr.circle(x, y, radius, 20);

            sr.setColor(0.15f, 0.08f, 0.02f, 1f);
            sr.rectLine(x - radius, y, x + radius, y, radius * 0.115f);
            sr.rectLine(x, y - radius, x, y + radius, radius * 0.115f);
        }
    },

    /** Fluorescent yellow with the curved white seam wrapping around both sides. */
    TENNIS("Tennis Ball") {
        @Override
        public void render(ShapeRenderer sr, float x, float y, float radius) {
            sr.setColor(0.85f, 0.93f, 0.2f, 1f);
            sr.circle(x, y, radius, 20);

            // Each seam is a chord of short segments bowing away from the near edge of the ball.
            sr.setColor(Color.WHITE);
            for (int side = -1; side <= 1; side += 2) {
                float prevX = x + side * radius;
                float prevY = y - radius * 0.72f;
                for (int i = 1; i <= 6; i++) {
                    float t = i / 6f;
                    float px = x + side * radius * (1f - 1.1f * t * t);
                    float py = y - radius * 0.72f + radius * 1.44f * t;
                    sr.rectLine(prevX, prevY, px, py, radius * 0.13f);
                    prevX = px;
                    prevY = py;
                }
            }
        }
    },

    /** Polished dark ball with the three finger holes drilled into the face. */
    BOWLING("Bowling Ball") {
        @Override
        public void render(ShapeRenderer sr, float x, float y, float radius) {
            sr.setColor(0.16f, 0.13f, 0.26f, 1f);
            sr.circle(x, y, radius, 20);

            // Highlight, offset up-left so the ball reads as glossy rather than flat.
            sr.setColor(0.42f, 0.38f, 0.58f, 1f);
            sr.circle(x - radius * 0.35f, y + radius * 0.35f, radius * 0.22f, 12);

            sr.setColor(0.05f, 0.04f, 0.08f, 1f);
            sr.circle(x - radius * 0.3f, y - radius * 0.12f, radius * 0.16f, 10);
            sr.circle(x + radius * 0.3f, y - radius * 0.12f, radius * 0.16f, 10);
            sr.circle(x, y - radius * 0.5f, radius * 0.16f, 10);
        }
    },

    /** Beach ball: alternating wedges fanned out from the centre, with a white cap on top. */
    BEACH("Beach Ball") {
        private final Color[] wedges = {
            new Color(0.95f, 0.25f, 0.3f, 1f),
            new Color(0.98f, 0.98f, 0.98f, 1f),
            new Color(0.2f, 0.55f, 0.9f, 1f),
            new Color(0.98f, 0.98f, 0.98f, 1f),
            new Color(0.98f, 0.82f, 0.2f, 1f),
            new Color(0.98f, 0.98f, 0.98f, 1f),
        };

        @Override
        public void render(ShapeRenderer sr, float x, float y, float radius) {
            int perWedge = 3;
            int segments = wedges.length * perWedge;
            float prevX = x + radius;
            float prevY = y;
            for (int i = 1; i <= segments; i++) {
                float angle = i * MathUtils.PI2 / segments;
                float vx = x + MathUtils.cos(angle) * radius;
                float vy = y + MathUtils.sin(angle) * radius;
                sr.setColor(wedges[(i - 1) / perWedge]);
                sr.triangle(x, y, prevX, prevY, vx, vy);
                prevX = vx;
                prevY = vy;
            }

            sr.setColor(0.98f, 0.98f, 0.98f, 1f);
            sr.circle(x, y, radius * 0.25f, 12);
        }
    },

    /** A wobbling water balloon, knot and all — the ammo the aliens presumably approve of. */
    WATER("Water Balloon") {
        @Override
        public void render(ShapeRenderer sr, float x, float y, float radius) {
            // Slightly taller than wide and hanging a little low, so it reads as heavy with water.
            sr.setColor(0.2f, 0.55f, 0.95f, 1f);
            filledEllipse(sr, x, y - radius * 0.08f, radius * 0.92f, radius, 18);

            sr.setColor(0.55f, 0.8f, 1f, 1f);
            sr.circle(x - radius * 0.3f, y + radius * 0.32f, radius * 0.2f, 10);

            sr.setColor(0.12f, 0.38f, 0.72f, 1f);
            sr.triangle(
                x - radius * 0.22f, y - radius * 0.86f,
                x + radius * 0.22f, y - radius * 0.86f,
                x, y - radius * 1.25f);
        }
    };

    private final String displayName;

    AmmoStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Draws the projectile centred on ({@code x}, {@code y}), filling the given collision radius. */
    public abstract void render(ShapeRenderer sr, float x, float y, float radius);

    /** Fan-triangulated ellipse in the current colour — {@link ShapeRenderer} only offers circles. */
    static void filledEllipse(ShapeRenderer sr, float cx, float cy, float rx, float ry, int segments) {
        float prevX = cx + rx;
        float prevY = cy;
        for (int i = 1; i <= segments; i++) {
            float angle = i * MathUtils.PI2 / segments;
            float vx = cx + MathUtils.cos(angle) * rx;
            float vy = cy + MathUtils.sin(angle) * ry;
            sr.triangle(cx, cy, prevX, prevY, vx, vy);
            prevX = vx;
            prevY = vy;
        }
    }

    public static AmmoStyle byIndex(int index) {
        return values()[Math.max(0, Math.min(values().length - 1, index))];
    }
}
