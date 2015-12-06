package com.sloperider;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.sloperider.screen.GameScreen;
import com.sloperider.screen.MainMenuScreen;
import com.sloperider.screen.MasterScreen;

public class SlopeRider extends ApplicationAdapter {

    public static final float PIXEL_PER_UNIT = 100.0f;
    public static final String TAG = "slope-rider_DEBUG";

    private MasterScreen _masterScreen;

    private AssetManager _assetManager;
    private boolean _assetsLoaded;

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

	@Override
	public void create () {
        _assetsLoaded = false;
        _assetManager = new AssetManager();

        _masterScreen = new MasterScreen();
        _masterScreen.assetManager(_assetManager);
        _masterScreen.start();
        _masterScreen.push(new MainMenuScreen());
	}

	@Override
	public void render () {
        if (!_assetsLoaded) {
            if (_assetManager.update()) {
                _assetsLoaded = true;
            } else {
                final float progress = _assetManager.getProgress();

                Gdx.app.log(TAG, "loading progress: " + progress);
            }

            return;
        }

		Gdx.gl.glClearColor(0.2f, 0.4f, 0.9f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        _masterScreen.update(Gdx.graphics.getDeltaTime());
        _masterScreen.render();
	}

    @Override
    public void dispose() {
        super.dispose();

        _masterScreen.dispose();
    }
}
