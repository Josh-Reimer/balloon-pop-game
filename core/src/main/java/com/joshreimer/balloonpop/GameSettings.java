package com.joshreimer.balloonpop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;
import com.joshreimer.balloonpop.entities.GunPalette;
import com.joshreimer.balloonpop.entities.GunStyle;
import com.joshreimer.balloonpop.entities.SfxStyle;

/** User-adjustable difficulty, gun-appearance, and sound-effect settings, persisted across app runs via Preferences. */
public class GameSettings {
    private static final String PREFS_NAME = "balloonpop-settings";
    private static final String KEY_FIRE_RATE = "fireRateT";
    private static final String KEY_SPAWN_RATE = "spawnRateT";
    private static final String KEY_GUN_STYLE = "gunStyle";
    private static final String KEY_GUN_COLOR = "gunColor";
    private static final String KEY_SFX_STYLE = "sfxStyle";

    public static final float MIN_FIRE_INTERVAL = 0.035f;
    public static final float MAX_FIRE_INTERVAL = 0.45f;
    private static final float DEFAULT_FIRE_INTERVAL = 0.22f;

    public static final float MIN_SPAWN_INTERVAL = 0.5f;
    public static final float MAX_SPAWN_INTERVAL = 2.2f;
    private static final float DEFAULT_SPAWN_INTERVAL = 1.35f;

    private float fireRateT;
    private float spawnRateT;
    private int gunStyleIndex;
    private int gunColorIndex;
    private int sfxStyleIndex;

    public GameSettings() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        fireRateT = prefs.getFloat(KEY_FIRE_RATE, toT(DEFAULT_FIRE_INTERVAL, MAX_FIRE_INTERVAL, MIN_FIRE_INTERVAL));
        spawnRateT = prefs.getFloat(KEY_SPAWN_RATE, toT(DEFAULT_SPAWN_INTERVAL, MAX_SPAWN_INTERVAL, MIN_SPAWN_INTERVAL));
        gunStyleIndex = MathUtils.clamp(prefs.getInteger(KEY_GUN_STYLE, 0), 0, GunStyle.values().length - 1);
        gunColorIndex = GunPalette.clamp(prefs.getInteger(KEY_GUN_COLOR, 0));
        sfxStyleIndex = MathUtils.clamp(prefs.getInteger(KEY_SFX_STYLE, 0), 0, SfxStyle.values().length - 1);
    }

    /** Seconds between shots while firing. Lower fireRateT means slower (longer interval). */
    public float getFireInterval() {
        return MathUtils.lerp(MAX_FIRE_INTERVAL, MIN_FIRE_INTERVAL, fireRateT);
    }

    /** Base seconds between balloon spawns, before the in-run difficulty ramp. */
    public float getBaseSpawnInterval() {
        return MathUtils.lerp(MAX_SPAWN_INTERVAL, MIN_SPAWN_INTERVAL, spawnRateT);
    }

    public float getFireRateT() {
        return fireRateT;
    }

    public float getSpawnRateT() {
        return spawnRateT;
    }

    public void setFireRateT(float t) {
        fireRateT = MathUtils.clamp(t, 0f, 1f);
    }

    public void setSpawnRateT(float t) {
        spawnRateT = MathUtils.clamp(t, 0f, 1f);
    }

    public GunStyle getGunStyle() {
        return GunStyle.byIndex(gunStyleIndex);
    }

    public int getGunColorIndex() {
        return gunColorIndex;
    }

    public String getGunColorName() {
        return GunPalette.name(gunColorIndex);
    }

    /** Steps to the next/previous gun shape, wrapping around at both ends. */
    public void cycleGunStyle(int step) {
        int count = GunStyle.values().length;
        gunStyleIndex = ((gunStyleIndex + step) % count + count) % count;
    }

    public void setGunColorIndex(int index) {
        gunColorIndex = GunPalette.clamp(index);
    }

    public SfxStyle getSfxStyle() {
        return SfxStyle.byIndex(sfxStyleIndex);
    }

    /** Steps to the next/previous sound effect set, wrapping around at both ends. */
    public void cycleSfxStyle(int step) {
        int count = SfxStyle.values().length;
        sfxStyleIndex = ((sfxStyleIndex + step) % count + count) % count;
    }

    public void save() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat(KEY_FIRE_RATE, fireRateT);
        prefs.putFloat(KEY_SPAWN_RATE, spawnRateT);
        prefs.putInteger(KEY_GUN_STYLE, gunStyleIndex);
        prefs.putInteger(KEY_GUN_COLOR, gunColorIndex);
        prefs.putInteger(KEY_SFX_STYLE, sfxStyleIndex);
        prefs.flush();
    }

    private static float toT(float value, float atZero, float atOne) {
        return MathUtils.clamp((value - atZero) / (atOne - atZero), 0f, 1f);
    }
}
