package com.sloperider;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.sloperider.screen.GameScreen;
import com.sloperider.screen.LoadingScreen;
import com.sloperider.screen.MasterScreen;

import java.util.List;
import java.util.OptionalInt;

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

            @Override
            public String startingLevel() {
                return null;
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

        if (!configuration().debug())
            LevelSet.instance().loadFromFile("level/main.lvl", false);
        else
            LevelSet.instance().loadFromFile("level/debug.lvl", true);

        _masterScreen = new MasterScreen();
        _masterScreen.configuration(configuration());
        _masterScreen.assetManager(_assetManager);
        _masterScreen.start();

        final LoadingScreen loadingScreen = new LoadingScreen();

        if (configuration().startingLevel() != null) {
            OptionalInt startingLevel = OptionalInt.empty();
            List<LevelInfo> levels = LevelSet.instance().levels();
            for (int i = 0; i < levels.size(); ++i) {
                if (levels.get(i).name.equals(configuration().startingLevel())) {
                    startingLevel = OptionalInt.of(i);
                }
            }

            if (startingLevel.isPresent()) {
                OptionalInt finalStartingLevel = startingLevel;
                loadingScreen
                    .redirect(new LoadingScreen.Redirect() {
                        @Override
                        public int limit() {
                            return 1;
                        }

                        @Override
                        public void call() {
                            _masterScreen.push(new GameScreen(_masterScreen)
                                .startingLevel(finalStartingLevel.getAsInt()));
                        }
                    });
            }
        }

        _masterScreen.push(loadingScreen);

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
