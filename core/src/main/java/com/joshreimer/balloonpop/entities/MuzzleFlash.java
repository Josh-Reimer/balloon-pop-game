package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** A brief flash of light at the gun's muzzle, spawned on every shot fired. */
public class MuzzleFlash {
    private static final float DURATION = 0.07f;
    private static final int SPIKE_COUNT = 5;

    private static final Color COLOR_CORE = new Color(1f, 0.98f, 0.85f, 1f);
    private static final Color COLOR_OUTER = new Color(1f, 0.75f, 0.15f, 1f);
    private final Color tmpColor = new Color();

    private final float x, y;
    private final float scale;
    private final float[] spikeAngle = new float[SPIKE_COUNT];
    public boolean alive = true;
    private float time = 0f;

    public MuzzleFlash(float x, float y) {
        this.x = x;
        this.y = y;
        this.scale = MathUtils.random(0.85f, 1.15f);
        for (int i = 0; i < SPIKE_COUNT; i++) {
            spikeAngle[i] = MathUtils.PI / 2f + MathUtils.random(-0.9f, 0.9f);
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
        float fade = 1f - t;
        float coreRadius = 10f * scale * fade;

        sr.setColor(tmpColor.set(COLOR_OUTER).mul(1f, 1f, 1f, fade));
        for (int i = 0; i < SPIKE_COUNT; i++) {
            float len = (18f + (i % 3) * 6f) * scale * fade;
            float tipX = x + MathUtils.cos(spikeAngle[i]) * len;
            float tipY = y + MathUtils.sin(spikeAngle[i]) * len;
            sr.rectLine(x, y, tipX, tipY, 4f * fade);
        }

        sr.setColor(tmpColor.set(COLOR_CORE).mul(1f, 1f, 1f, fade));
        sr.circle(x, y, coreRadius, 14);
    }
}
