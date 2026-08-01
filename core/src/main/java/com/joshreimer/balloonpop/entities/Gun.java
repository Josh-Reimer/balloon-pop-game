package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** The basketball-launching gun that slides left/right along the bottom of the screen. */
public class Gun {
    public static final float WIDTH = 70f;
    public static final float HEIGHT = 90f;
    public static final float BARREL_WIDTH = 26f;
    public static final float BARREL_HEIGHT = 40f;

    private static final Color BODY_COLOR = new Color(0.25f, 0.28f, 0.32f, 1f);
    private static final Color BARREL_COLOR = new Color(0.15f, 0.16f, 0.19f, 1f);
    private static final Color TRIM_COLOR = new Color(0.9f, 0.55f, 0.1f, 1f);

    private static final float RECOIL_KICK = 11f;
    private static final float RECOIL_RECOVERY_SPEED = 130f;

    private float x;
    private final float y;
    private final float worldWidth;
    private float recoil = 0f;

    public Gun(float startX, float y, float worldWidth) {
        this.x = startX;
        this.y = y;
        this.worldWidth = worldWidth;
    }

    /** Kicks the barrel back; call once per shot fired. */
    public void fire() {
        recoil = RECOIL_KICK;
    }

    public void update(float delta) {
        if (recoil > 0f) {
            recoil = Math.max(0f, recoil - RECOIL_RECOVERY_SPEED * delta);
        }
    }

    /** Moves the gun so its center follows the given world x coordinate, clamped to stay on screen. */
    public void setCenterX(float targetX) {
        float half = WIDTH / 2f;
        x = Math.max(half, Math.min(worldWidth - half, targetX));
    }

    public float getCenterX() {
        return x;
    }

    public float getMuzzleX() {
        return x;
    }

    public float getMuzzleY() {
        return y + HEIGHT + 4f;
    }

    public void render(ShapeRenderer sr) {
        float half = WIDTH / 2f;
        float ry = y - recoil;

        sr.setColor(BODY_COLOR);
        sr.rect(x - half, ry, WIDTH, HEIGHT * 0.5f);

        sr.setColor(BARREL_COLOR);
        sr.rect(x - BARREL_WIDTH / 2f, ry + HEIGHT * 0.3f, BARREL_WIDTH, HEIGHT - HEIGHT * 0.3f);

        sr.setColor(TRIM_COLOR);
        sr.rect(x - half, ry + HEIGHT * 0.5f - 6f, WIDTH, 6f);
    }
}
