package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * The bucket of water an alien tips over an overheated gun: a stream of droplets falling from the
 * bucket's lip, a puddle spreading at the gun's feet, and steam boiling off the barrel as it cools.
 * Decorative only — like {@link Explosion} it has no {@code overlaps()} and is never collision
 * checked; {@code GameScreen} watches its {@link #alive} flag to know when the gun is usable again.
 *
 * <p>Everything fades by shrinking rather than by alpha, since {@code ShapeRenderer} draws with
 * blending off and a translucent colour would come out solid black.
 */
public class WaterSplash {
    public static final float DURATION = 1.9f;

    /** Water only falls for the first part of the effect; the rest is steam clearing.  */
    private static final float POUR_DURATION = 1.1f;

    private static final int DROP_COUNT = 40;
    private static final float DROP_FALL_TIME = 0.42f;
    private static final float DROP_SPREAD = 26f;
    private static final float DROP_RADIUS_MIN = 2.5f;
    private static final float DROP_RADIUS_MAX = 5.5f;

    private static final int STEAM_COUNT = 14;
    private static final float STEAM_RISE = 95f;
    private static final float STEAM_LIFETIME = 0.9f;
    private static final float STEAM_RADIUS_MIN = 7f;
    private static final float STEAM_RADIUS_MAX = 15f;

    private static final float PUDDLE_MAX_WIDTH = 92f;
    private static final float PUDDLE_HEIGHT = 7f;

    private static final Color WATER_COLOR = new Color(0.25f, 0.6f, 0.95f, 1f);
    private static final Color WATER_PALE = new Color(0.6f, 0.85f, 1f, 1f);
    private static final Color PUDDLE_COLOR = new Color(0.2f, 0.5f, 0.85f, 1f);
    private static final Color STEAM_COLOR = new Color(0.92f, 0.94f, 0.96f, 1f);

    private final float sourceX, sourceY;
    private final float targetX, groundY;
    /** Top of the gun: where the falling water lands and the steam boils off. */
    private final float steamBaseY;

    private final float[] dropDelay = new float[DROP_COUNT];
    private final float[] dropOffset = new float[DROP_COUNT];
    private final float[] dropRadius = new float[DROP_COUNT];

    private final float[] steamDelay = new float[STEAM_COUNT];
    private final float[] steamOffset = new float[STEAM_COUNT];
    private final float[] steamRadius = new float[STEAM_COUNT];
    private final float[] steamDrift = new float[STEAM_COUNT];

    private float time = 0f;
    public boolean alive = true;

    /**
     * @param sourceX x the water is poured from (the bucket lip)
     * @param sourceY y the water is poured from
     * @param targetX centre of the gun being doused
     * @param targetTopY top of the gun, where the steam boils off
     * @param groundY ground level, where the puddle spreads
     */
    public WaterSplash(float sourceX, float sourceY, float targetX, float targetTopY, float groundY) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.targetX = targetX;
        this.steamBaseY = targetTopY;
        this.groundY = groundY;

        for (int i = 0; i < DROP_COUNT; i++) {
            dropDelay[i] = MathUtils.random(0f, POUR_DURATION - DROP_FALL_TIME);
            dropOffset[i] = MathUtils.random(-DROP_SPREAD / 2f, DROP_SPREAD / 2f);
            dropRadius[i] = MathUtils.random(DROP_RADIUS_MIN, DROP_RADIUS_MAX);
        }
        for (int i = 0; i < STEAM_COUNT; i++) {
            // Steam only starts once water has actually reached the barrel.
            steamDelay[i] = MathUtils.random(DROP_FALL_TIME, DURATION - STEAM_LIFETIME);
            steamOffset[i] = MathUtils.random(-Gun.WIDTH / 2f, Gun.WIDTH / 2f);
            steamRadius[i] = MathUtils.random(STEAM_RADIUS_MIN, STEAM_RADIUS_MAX);
            steamDrift[i] = MathUtils.random(-18f, 18f);
        }
    }

    public void update(float delta) {
        time += delta;
        if (time >= DURATION) {
            alive = false;
        }
    }

    public void render(ShapeRenderer sr) {
        renderDrops(sr);
        renderPuddle(sr);
        renderSteam(sr);
    }

    /** Droplets arcing from the bucket lip across to the gun and down onto it. */
    private void renderDrops(ShapeRenderer sr) {
        for (int i = 0; i < DROP_COUNT; i++) {
            float t = (time - dropDelay[i]) / DROP_FALL_TIME;
            if (t < 0f || t > 1f) continue;

            // Lateral travel eases out while the fall accelerates, so the stream reads as an arc.
            float x = MathUtils.lerp(sourceX, targetX + dropOffset[i], Math.min(1f, t * 1.6f));
            float y = MathUtils.lerp(sourceY, steamBaseY, t * t);

            sr.setColor(i % 3 == 0 ? WATER_PALE : WATER_COLOR);
            sr.circle(x, y, dropRadius[i], 8);
        }
    }

    /** A puddle spreading out under the gun, then soaking away over the tail of the effect. */
    private void renderPuddle(ShapeRenderer sr) {
        float grow = MathUtils.clamp((time - DROP_FALL_TIME) / POUR_DURATION, 0f, 1f);
        if (grow <= 0f) return;

        float soak = MathUtils.clamp((time - POUR_DURATION) / (DURATION - POUR_DURATION), 0f, 1f);
        float width = PUDDLE_MAX_WIDTH * grow * (1f - soak * 0.6f);
        if (width <= 1f) return;

        sr.setColor(PUDDLE_COLOR);
        float prevX = targetX + width / 2f;
        float prevY = groundY;
        for (int i = 1; i <= 16; i++) {
            float angle = i * MathUtils.PI2 / 16;
            float vx = targetX + MathUtils.cos(angle) * width / 2f;
            float vy = groundY + MathUtils.sin(angle) * PUDDLE_HEIGHT / 2f;
            sr.triangle(targetX, groundY, prevX, prevY, vx, vy);
            prevX = vx;
            prevY = vy;
        }
    }

    /** Puffs boiling off the barrel: they rise, drift sideways, and shrink out of existence. */
    private void renderSteam(ShapeRenderer sr) {
        sr.setColor(STEAM_COLOR);
        for (int i = 0; i < STEAM_COUNT; i++) {
            float t = (time - steamDelay[i]) / STEAM_LIFETIME;
            if (t < 0f || t > 1f) continue;

            float radius = steamRadius[i] * MathUtils.sin(t * MathUtils.PI);
            if (radius <= 0.5f) continue;

            sr.circle(
                targetX + steamOffset[i] + steamDrift[i] * t,
                steamBaseY + STEAM_RISE * t,
                radius, 12);
        }
    }
}
