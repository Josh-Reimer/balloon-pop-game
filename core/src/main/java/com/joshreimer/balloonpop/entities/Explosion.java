package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** A brief burst of fiery embers spawned when a target is popped. */
public class Explosion {
    private static final float DURATION = 0.45f;
    private static final int EMBER_COUNT = 14;

    private static final Color COLOR_HOT = new Color(1f, 0.95f, 0.4f, 1f);
    private static final Color COLOR_MID = new Color(1f, 0.55f, 0.1f, 1f);
    private static final Color COLOR_COOL = new Color(0.75f, 0.15f, 0.05f, 1f);

    private final float[] emberAngle = new float[EMBER_COUNT];
    private final float[] emberSpeed = new float[EMBER_COUNT];
    private final float[] emberSize = new float[EMBER_COUNT];
    private final Color tmpColor = new Color();

    private final float x, y;
    private float time = 0f;
    public boolean alive = true;

    public Explosion(float x, float y, float scale) {
        this.x = x;
        this.y = y;
        for (int i = 0; i < EMBER_COUNT; i++) {
            emberAngle[i] = MathUtils.random(MathUtils.PI2);
            emberSpeed[i] = MathUtils.random(60f, 220f) * scale;
            emberSize[i] = MathUtils.random(4f, 9f) * scale;
        }
    }

    public void update(float delta) {
        time += delta;
        if (time >= DURATION) {
            alive = false;
        }
    }

    public void render(ShapeRenderer sr) {
        float t = Math.min(1f, time / DURATION);
        for (int i = 0; i < EMBER_COUNT; i++) {
            float dist = emberSpeed[i] * t;
            float ex = x + MathUtils.cos(emberAngle[i]) * dist;
            float ey = y + MathUtils.sin(emberAngle[i]) * dist;
            float size = emberSize[i] * (1f - t);
            if (size <= 0.5f) continue;

            sr.setColor(emberColor(t));
            sr.circle(ex, ey, size, 10);
        }
    }

    private Color emberColor(float t) {
        if (t < 0.4f) {
            return tmpColor.set(COLOR_HOT).lerp(COLOR_MID, t / 0.4f);
        }
        return tmpColor.set(COLOR_MID).lerp(COLOR_COOL, (t - 0.4f) / 0.6f);
    }
}
