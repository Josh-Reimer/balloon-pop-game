package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;

/**
 * The colour schemes offered by the "customize your gun" settings section. Purely cosmetic —
 * index 0 is the original gun colouring and is the default.
 */
public final class GunPalette {
    private static final String[] NAMES = {
        "Steel", "Crimson", "Forest", "Royal", "Amber", "Violet"
    };

    private static final Color[] BODY = {
        new Color(0.25f, 0.28f, 0.32f, 1f),
        new Color(0.70f, 0.15f, 0.17f, 1f),
        new Color(0.14f, 0.42f, 0.24f, 1f),
        new Color(0.16f, 0.30f, 0.68f, 1f),
        new Color(0.85f, 0.55f, 0.08f, 1f),
        new Color(0.44f, 0.20f, 0.64f, 1f),
    };

    private static final Color[] TRIM = {
        new Color(0.90f, 0.55f, 0.10f, 1f),
        new Color(1.00f, 0.80f, 0.30f, 1f),
        new Color(0.70f, 0.95f, 0.45f, 1f),
        new Color(0.45f, 0.85f, 1.00f, 1f),
        new Color(0.30f, 0.25f, 0.20f, 1f),
        new Color(1.00f, 0.55f, 0.90f, 1f),
    };

    /** Darkened body colour used for barrels and wheels; derived once so rendering allocates nothing. */
    private static final Color[] BARREL = new Color[BODY.length];

    static {
        for (int i = 0; i < BODY.length; i++) {
            BARREL[i] = new Color(BODY[i]).lerp(Color.BLACK, 0.4f);
        }
    }

    private GunPalette() {}

    public static int size() {
        return BODY.length;
    }

    public static String name(int index) {
        return NAMES[clamp(index)];
    }

    public static Color body(int index) {
        return BODY[clamp(index)];
    }

    public static Color barrel(int index) {
        return BARREL[clamp(index)];
    }

    public static Color trim(int index) {
        return TRIM[clamp(index)];
    }

    public static int clamp(int index) {
        return Math.max(0, Math.min(BODY.length - 1, index));
    }
}
