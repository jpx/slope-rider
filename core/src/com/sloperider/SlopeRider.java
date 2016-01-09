package com.sloperider;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.sloperider.screen.LoadingScreen;
import com.sloperider.screen.MainMenuScreen;
import com.sloperider.screen.MasterScreen;

public class SlopeRider extends ApplicationAdapter {

    public static final float PIXEL_PER_UNIT = 100.0f;
    public static final String TAG = "slope-rider_DEBUG";

    private MasterScreen _masterScreen;

    private AssetManager _assetManager;
    private boolean _assetsLoaded;

    private Configuration _configuration;

    public SlopeRider() {
        _configuration = new Configuration() {
            @Override
            public boolean debug() {
                return true;
            }
        };
    }

    public final SlopeRider configuration(final Configuration configuration) {
        _configuration = configuration;

        return this;
    }

    public final Configuration configuration() {
        return _configuration;
    }

    public final SlopeRider io(final PersistentIO io) {
        LevelSet.instance().io(io);

        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

	@Override
	public void create () {
        _assetsLoaded = false;
        _assetManager = new AssetManager();

        _masterScreen = new MasterScreen();
        _masterScreen.configuration(_configuration);
        _masterScreen.assetManager(_assetManager);
        _masterScreen.start();
        _masterScreen.push(new LoadingScreen());

        if (!configuration().debug())
            LevelSet.instance().loadFromFile("level/main.lvl", false);
        else
            LevelSet.instance().loadFromFile("level/debug.lvl", true);

        _assetManager.load("ui/uiskin.json", Skin.class);
	}

	@Override
	public void render () {
        if (!_assetsLoaded) {
            if (_assetManager.update()) {
                _assetsLoaded = true;
                _masterScreen.ready();
            } else {
                final float progress = _assetManager.getProgress();

                Gdx.app.log(TAG, "loading progress: " + progress);
            }
        }

		Gdx.gl.glClearColor(1.f, 1.f, 1.f, 1.f);
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
