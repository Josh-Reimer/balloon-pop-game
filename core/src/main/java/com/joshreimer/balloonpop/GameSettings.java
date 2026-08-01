package com.joshreimer.balloonpop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

/** User-adjustable difficulty settings, persisted across app runs via Preferences. */
public class GameSettings {
    private static final String PREFS_NAME = "balloonpop-settings";
    private static final String KEY_FIRE_RATE = "fireRateT";
    private static final String KEY_SPAWN_RATE = "spawnRateT";

    public static final float MIN_FIRE_INTERVAL = 0.08f;
    public static final float MAX_FIRE_INTERVAL = 0.45f;
    private static final float DEFAULT_FIRE_INTERVAL = 0.22f;

    public static final float MIN_SPAWN_INTERVAL = 0.5f;
    public static final float MAX_SPAWN_INTERVAL = 2.2f;
    private static final float DEFAULT_SPAWN_INTERVAL = 1.35f;

    private float fireRateT;
    private float spawnRateT;

    public GameSettings() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        fireRateT = prefs.getFloat(KEY_FIRE_RATE, toT(DEFAULT_FIRE_INTERVAL, MAX_FIRE_INTERVAL, MIN_FIRE_INTERVAL));
        spawnRateT = prefs.getFloat(KEY_SPAWN_RATE, toT(DEFAULT_SPAWN_INTERVAL, MAX_SPAWN_INTERVAL, MIN_SPAWN_INTERVAL));
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

    public void save() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat(KEY_FIRE_RATE, fireRateT);
        prefs.putFloat(KEY_SPAWN_RATE, spawnRateT);
        prefs.flush();
    }

    private static float toT(float value, float atZero, float atOne) {
        return MathUtils.clamp((value - atZero) / (atOne - atZero), 0f, 1f);
    }
}
