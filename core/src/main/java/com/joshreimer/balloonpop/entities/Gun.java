package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** The basketball-launching gun that slides left/right along the bottom of the screen. */
public class Gun {
    public static final float WIDTH = 70f;
    public static final float HEIGHT = 90f;
    public static final float BARREL_WIDTH = 26f;
    public static final float BARREL_HEIGHT = 40f;

    private static final float RECOIL_KICK = 11f;
    private static final float RECOIL_RECOVERY_SPEED = 130f;

    private float x;
    private final float y;
    private final float worldWidth;
    private float recoil = 0f;

    // Cosmetic only — see GunStyle. Kept in sync with GameSettings by GameScreen.show().
    private GunStyle style = GunStyle.CLASSIC;
    private int colorIndex = 0;

    public Gun(float startX, float y, float worldWidth) {
        this.x = startX;
        this.y = y;
        this.worldWidth = worldWidth;
    }

    /** Applies the player's chosen look. Affects rendering only, never firing position or timing. */
    public void setSkin(GunStyle style, int colorIndex) {
        this.style = style;
        this.colorIndex = GunPalette.clamp(colorIndex);
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
        style.render(sr, x, y - recoil, colorIndex);
    }
}
