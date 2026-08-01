package com.joshreimer.balloonpop.android;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.joshreimer.balloonpop.BalloonPopGame;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        // Immersive sticky mode; the backend only applies the legacy flags, which still work below API 30.
        config.useImmersiveMode = true;
        config.renderUnderCutout = true;
        initialize(new BalloonPopGame(), config);
        hideSystemBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    /**
     * From API 30 on, the window flags and setSystemUiVisibility() calls the libGDX backend uses are
     * ignored (fully no-ops once targeting API 35+), so the bars have to be hidden via
     * WindowInsetsController instead. They come back with a swipe from the edge and auto-hide again.
     */
    @SuppressWarnings("deprecation") // setDecorFitsSystemWindows is deprecated in API 35, still needed on 30-34
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        Window window = getWindow();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Needed on API 30-34 so the GL surface draws behind the bars; API 35+ is edge-to-edge already.
            window.setDecorFitsSystemWindows(false);
        }
        WindowInsetsController controller = window.getInsetsController();
        if (controller == null) return;
        controller.hide(WindowInsets.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
