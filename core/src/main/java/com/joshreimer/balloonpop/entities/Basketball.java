package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * A round fired from the gun, travelling upward until it hits a balloon or leaves the screen. Its
 * appearance is player-chosen: {@link #render} delegates to the {@link AmmoStyle} it was constructed
 * with. Every style draws inside {@link #RADIUS}, so the choice never affects flight or collisions.
 */
public class Basketball {
    public static final float RADIUS = 13f;
    public static final float SPEED = 620f; // world units per second

    public float x, y;
    public boolean alive = true;

    private final AmmoStyle style;

    public Basketball(float x, float y, AmmoStyle style) {
        this.x = x;
        this.y = y;
        this.style = style;
    }

    public void update(float delta) {
        y += SPEED * delta;
    }

    public boolean isOffScreen(float worldHeight) {
        return y - RADIUS > worldHeight;
    }

    public boolean overlaps(float ox, float oy, float otherRadius) {
        float dx = x - ox;
        float dy = y - oy;
        float distSq = dx * dx + dy * dy;
        float rSum = RADIUS + otherRadius;
        return distSq <= rSum * rSum;
    }

    public void render(ShapeRenderer sr) {
        style.render(sr, x, y, RADIUS);
    }
}
