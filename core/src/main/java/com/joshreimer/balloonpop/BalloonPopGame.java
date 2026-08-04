package com.joshreimer.balloonpop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.joshreimer.balloonpop.screens.GameScreen;
import com.joshreimer.balloonpop.screens.SettingsScreen;

public class BalloonPopGame extends Game {
    private GameScreen gameScreen;
    private SettingsScreen settingsScreen;

    /**
     * Shared across screens and disposed here rather than by any one of them, matching how the
     * screens themselves are managed. Only the alien voice clips go through it — the pop/fire
     * sounds are still loaded directly, since they're swapped whenever the sfx style changes.
     */
    private AssetManager assets;

    @Override
    public void create() {
        GameSettings settings = new GameSettings();
        assets = new AssetManager();
        gameScreen = new GameScreen(this, settings, assets);
        settingsScreen = new SettingsScreen(this, settings);
        setScreen(gameScreen);
    }

    public void showGame() {
        setScreen(gameScreen);
    }

    public void showSettings() {
        setScreen(settingsScreen);
    }

    @Override
    public void dispose() {
        gameScreen.dispose();
        settingsScreen.dispose();
        assets.dispose();
    }
}
