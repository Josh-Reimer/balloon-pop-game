package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** A basketball fired from the gun, travels upward until it hits a balloon or leaves the screen. */
public class Basketball {
    public static final float RADIUS = 13f;
    public static final float SPEED = 620f; // world units per second

    private static final Color BALL_COLOR = new Color(0.85f, 0.42f, 0.09f, 1f);
    private static final Color LINE_COLOR = new Color(0.15f, 0.08f, 0.02f, 1f);

    public float x, y;
    public boolean alive = true;

    public Basketball(float x, float y) {
        this.x = x;
        this.y = y;
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
        sr.setColor(BALL_COLOR);
        sr.circle(x, y, RADIUS, 20);

        sr.setColor(LINE_COLOR);
        sr.rectLine(x - RADIUS, y, x + RADIUS, y, 1.5f);
        sr.rectLine(x, y - RADIUS, x, y + RADIUS, 1.5f);
    }
}
