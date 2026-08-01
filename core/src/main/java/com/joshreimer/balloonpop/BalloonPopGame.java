package com.joshreimer.balloonpop;

import com.badlogic.gdx.Game;
import com.joshreimer.balloonpop.screens.GameScreen;
import com.joshreimer.balloonpop.screens.SettingsScreen;

public class BalloonPopGame extends Game {
    private GameScreen gameScreen;
    private SettingsScreen settingsScreen;

    @Override
    public void create() {
        GameSettings settings = new GameSettings();
        gameScreen = new GameScreen(this, settings);
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
    }
}
