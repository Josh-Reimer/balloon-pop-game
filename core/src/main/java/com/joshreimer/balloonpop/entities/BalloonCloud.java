package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A scattering cloud of small balloon fragments spawned when a balloon pops.
 * Fragments are purely decorative -- not targets -- and are flung outward
 * before gravity takes over and pulls them down off-screen.
 */
public class BalloonCloud {
    private static final float DURATION = 1.3f;
    private static final float GRAVITY = 380f;
    private static final int FRAGMENT_COUNT = 9;
    private static final float FADE_START_T = 0.55f;

    private final float[] fragX = new float[FRAGMENT_COUNT];
    private final float[] fragY = new float[FRAGMENT_COUNT];
    private final float[] velX = new float[FRAGMENT_COUNT];
    private final float[] velY = new float[FRAGMENT_COUNT];
    private final float[] fragRadius = new float[FRAGMENT_COUNT];

    private final Color color;
    private final Color tmpColor = new Color();

    public boolean alive = true;
    private float time = 0f;

    public BalloonCloud(float x, float y, float scale, Color color) {
        this.color = color;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            fragX[i] = x;
            fragY[i] = y;
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = MathUtils.random(70f, 200f) * scale;
            velX[i] = MathUtils.cos(angle) * speed;
            velY[i] = Math.abs(MathUtils.sin(angle)) * speed * 0.7f + MathUtils.random(40f, 100f) * scale;
            fragRadius[i] = MathUtils.random(3f, 8f) * scale;
        }
    }

    public void update(float delta) {
        time += delta;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            velY[i] -= GRAVITY * delta;
            fragX[i] += velX[i] * delta;
            fragY[i] += velY[i] * delta;
        }
        if (time >= DURATION) {
            alive = false;
        }
    }

    public void render(ShapeRenderer sr) {
        float t = time / DURATION;
        float alpha = t < FADE_START_T ? 1f : Math.max(0f, 1f - (t - FADE_START_T) / (1f - FADE_START_T));
        if (alpha <= 0f) return;

        sr.setColor(tmpColor.set(color).mul(1f, 1f, 1f, alpha));
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            sr.circle(fragX[i], fragY[i], fragRadius[i], 10);
        }
    }
}
