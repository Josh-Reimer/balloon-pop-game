package com.joshreimer.balloonpop.entities;

/**
 * The sound-effect sets offered by the "customize your sound" settings section. Each style points at
 * its own pop/fire asset pair; {@code GameScreen} reloads both {@code Sound}s whenever the chosen
 * style changes so switching takes effect immediately, without restarting the game.
 */
public enum SfxStyle {

    /** The original synthesized pop/fire pair, at the root of {@code assets/}. */
    CLASSIC("Classic", "pop.wav", "fire.wav"),

    /** 8-bit style: quantized square waves. */
    RETRO("Retro", "sfx/retro_pop.wav", "sfx/retro_fire.wav"),

    /** Sci-fi pitch-sweep zaps. */
    LASER("Laser", "sfx/laser_pop.wav", "sfx/laser_fire.wav"),

    /** Soft, rounded triangle-wave tones. */
    BUBBLE("Bubble", "sfx/bubble_pop.wav", "sfx/bubble_fire.wav");

    private final String displayName;
    private final String popPath;
    private final String firePath;

    SfxStyle(String displayName, String popPath, String firePath) {
        this.displayName = displayName;
        this.popPath = popPath;
        this.firePath = firePath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPopPath() {
        return popPath;
    }

    public String getFirePath() {
        return firePath;
    }

    public static SfxStyle byIndex(int index) {
        return values()[Math.max(0, Math.min(values().length - 1, index))];
    }
}
