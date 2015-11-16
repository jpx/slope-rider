package com.sloperider;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sloperider.component.Flag;
import com.sloperider.component.Sleigh;
import com.sloperider.component.Track;
import com.sloperider.component.TrackCameraController;
import com.sloperider.physics.PhysicsWorld;

public class SlopeRider extends ApplicationAdapter implements InputProcessor {

    public static final float PIXEL_PER_UNIT = 100.0f;
    public static final String TAG = "slope-rider_DEBUG";

    Stage _stage;

    SpriteBatch _spriteBatch;

    PhysicsWorld _physicsWorld;

    Track _track;

    private AssetManager _assetManager;
    private boolean _assetsLoaded;

    private ComponentFactory _componentFactory;

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        _stage.getViewport().update(width, height, false);

        _stage.getCamera().viewportWidth = width;
        _stage.getCamera().viewportHeight = height;

        _stage.getCamera().update();
    }

	@Override
	public void create () {
        _spriteBatch = new SpriteBatch();

        _stage = new Stage(new ScreenViewport(), _spriteBatch);

        _assetsLoaded = false;
        _assetManager = new AssetManager();

        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_stage, _assetManager, _physicsWorld);
        _track = _componentFactory.createComponent(new Vector2(10.f, 0.f), Track.class);

        TrackCameraController cameraController = _componentFactory.createComponent(new Vector2(), TrackCameraController.class)
            .setTrack(_track);

        _componentFactory.createComponent(new Vector2(11.f, 14.8f), Flag.class);

        _stage.getCamera().position.add(new Vector3(2.f, 4.f, 0.f).scl(SlopeRider.PIXEL_PER_UNIT));

        ((OrthographicCamera) _stage.getCamera()).zoom += 2.5f;

        Gdx.input.setInputProcessor(new InputMultiplexer(_stage, new GestureDetector(cameraController), cameraController, this));

        _componentFactory.requireAssets();
	}

	@Override
	public void render () {
        if (!_assetsLoaded) {
            if (_assetManager.update()) {
                _assetsLoaded = true;

                _componentFactory.ready();
            } else {
                final float progress = _assetManager.getProgress();

                Gdx.app.log(TAG, "loading progress: " + progress);
            }

            return;
        }

		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        _physicsWorld.update(Gdx.graphics.getDeltaTime());

        _stage.act(Gdx.graphics.getDeltaTime());

        _stage.draw();

        _physicsWorld.render(_stage.getCamera());
	}

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Sleigh sleigh = _componentFactory.createComponent(new Vector2(11.f, 16.f), Sleigh.class);

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
