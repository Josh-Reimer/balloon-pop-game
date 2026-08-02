package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * The gun shapes offered by the "customize your gun" settings section. Every style draws inside the
 * same {@link Gun#WIDTH} x {@link Gun#HEIGHT} footprint and puts its muzzle at the top of that box,
 * so the choice is purely cosmetic — firing position and hit behaviour are identical for all of them.
 * Also drawn stand-alone by {@code SettingsScreen} for the live preview, hence the static entry point.
 */
public enum GunStyle {

    /** The original: blocky body, straight barrel, trim stripe across the top of the body. */
    CLASSIC("Classic") {
        @Override
        public void render(ShapeRenderer sr, float x, float baseY, int colorIndex) {
            sr.setColor(GunPalette.body(colorIndex));
            sr.rect(x - Gun.WIDTH / 2f, baseY, Gun.WIDTH, Gun.HEIGHT * 0.5f);

            sr.setColor(GunPalette.barrel(colorIndex));
            sr.rect(x - Gun.BARREL_WIDTH / 2f, baseY + Gun.HEIGHT * 0.3f,
                Gun.BARREL_WIDTH, Gun.HEIGHT - Gun.HEIGHT * 0.3f);

            sr.setColor(GunPalette.trim(colorIndex));
            sr.rect(x - Gun.WIDTH / 2f, baseY + Gun.HEIGHT * 0.5f - 6f, Gun.WIDTH, 6f);
        }
    },

    /** Wheeled artillery piece with a barrel that flares out toward the muzzle. */
    CANNON("Cannon") {
        @Override
        public void render(ShapeRenderer sr, float x, float baseY, int colorIndex) {
            sr.setColor(GunPalette.barrel(colorIndex));
            sr.circle(x - 22f, baseY + 16f, 15f, 20);
            sr.circle(x + 22f, baseY + 16f, 15f, 20);

            sr.setColor(GunPalette.body(colorIndex));
            sr.rect(x - Gun.WIDTH / 2f, baseY + 8f, Gun.WIDTH, 26f);

            // Trapezoid barrel, two triangles wide: 11 units half-width at the breech, 17 at the muzzle.
            float breechY = baseY + 26f;
            float muzzleY = baseY + Gun.HEIGHT;
            sr.setColor(GunPalette.barrel(colorIndex));
            sr.triangle(x - 11f, breechY, x + 11f, breechY, x + 17f, muzzleY);
            sr.triangle(x - 11f, breechY, x + 17f, muzzleY, x - 17f, muzzleY);

            sr.setColor(GunPalette.trim(colorIndex));
            sr.rect(x - 19f, muzzleY - 10f, 38f, 8f);
        }
    },

    /** Sci-fi blaster: narrow body with swept-back fins and a glowing emitter ring. */
    BLASTER("Blaster") {
        @Override
        public void render(ShapeRenderer sr, float x, float baseY, int colorIndex) {
            sr.setColor(GunPalette.trim(colorIndex));
            sr.triangle(x - 14f, baseY + 8f, x - 14f, baseY + 46f, x - 34f, baseY + 2f);
            sr.triangle(x + 14f, baseY + 8f, x + 14f, baseY + 46f, x + 34f, baseY + 2f);

            sr.setColor(GunPalette.body(colorIndex));
            sr.rect(x - 16f, baseY, 32f, 58f);

            sr.setColor(GunPalette.barrel(colorIndex));
            sr.rect(x - 9f, baseY + 40f, 18f, Gun.HEIGHT - 40f);

            sr.setColor(GunPalette.trim(colorIndex));
            sr.rect(x - 4f, baseY + 10f, 8f, 34f);
            sr.circle(x, baseY + Gun.HEIGHT - 6f, 9f, 16);
        }
    };

    private final String displayName;

    GunStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Draws the gun with its bottom-centre at ({@code x}, {@code baseY}). */
    public abstract void render(ShapeRenderer sr, float x, float baseY, int colorIndex);

    public static GunStyle byIndex(int index) {
        return values()[Math.max(0, Math.min(values().length - 1, index))];
    }
}
