package com.sloperider;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sloperider.component.Component;
import com.sloperider.component.Sleigh;
import com.sloperider.component.Track;
import com.sloperider.physics.PhysicsWorld;
import com.sloperider.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SlopeRider extends ApplicationAdapter implements InputProcessor {

    public static final float PIXEL_PER_UNIT = 100.0f;
    public static final String TAG = "slope-rider_DEBUG";

    CameraInputController _cameraController;

    Stage _stage;

	SpriteBatch batch;
	Texture img;

    SpriteBatch _spriteBatch;

    PhysicsWorld _physicsWorld;

    Sleigh _sleigh;
    Track _track;

    boolean _touchDown;
    Vector2 _lastTouchPoint;

    private List<Component> _components;
    private AssetManager _assetManager;
    private boolean _assetsLoaded;

    private void addComponent(Component component, Group parent) {
        _components.add(component);

        _physicsWorld.addActor(component);
        parent.addActor(component);
    }

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

        _touchDown = false;

        _physicsWorld = new PhysicsWorld();

        _sleigh = new Sleigh();
        _track = new Track();

        _stage.getRoot().scaleBy(SlopeRider.PIXEL_PER_UNIT, SlopeRider.PIXEL_PER_UNIT);

        _components = new ArrayList<Component>();

        addComponent(_sleigh, _stage.getRoot());
        addComponent(_track, _stage.getRoot());

        _stage.getCamera().position.add(new Vector3(2.f, 4.f, 0.f).scl(SlopeRider.PIXEL_PER_UNIT));

        ((OrthographicCamera) _stage.getCamera()).zoom += 0.5f;

        _cameraController = new CameraInputController(_stage.getCamera());

        Gdx.input.setInputProcessor(new InputMultiplexer(this, _cameraController));

        _assetsLoaded = false;
        _assetManager = new AssetManager();

        requireAssets(_assetManager);
	}

    private void requireAssets(AssetManager assetManager) {
        for (Component component : _components) {
            component.requireAssets(assetManager);
        }
    }

    private void assetsLoaded() {
        for (Component component : _components) {
            component.manageAssets(_assetManager);
        }

        for (Component component : _components) {
            component.ready();
        }
    }

	@Override
	public void render () {
        if (!_assetsLoaded) {
            if (_assetManager.update()) {
                _assetsLoaded = true;

                assetsLoaded();
            } else {
                final float progress = _assetManager.getProgress();

                Gdx.app.log(TAG, "loading progress: " + progress);
            }

            return;
        }

        _cameraController.update();

		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        _stage.act(Gdx.graphics.getDeltaTime());
        _stage.draw();

        _physicsWorld.update(Gdx.graphics.getDeltaTime());
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

        Sleigh sleigh = new Sleigh();
        _stage.addActor(sleigh);
        _physicsWorld.addActor(sleigh);

        _touchDown = true;
        Vector3 position = _stage.getCamera().unproject(new Vector3(screenX, screenY, 0.f));
        _lastTouchPoint = new Vector2(position.x, position.y);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        _touchDown = false;
        Vector3 position = _stage.getCamera().unproject(new Vector3(screenX, screenY, 0.f));
        _lastTouchPoint = new Vector2(position.x, position.y);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!_touchDown)
            return false;

        Vector3 position = _stage.getCamera().unproject(new Vector3(screenX, screenY, 0.f));

        Vector2 move = new Vector2(position.x, position.y).sub(_lastTouchPoint).scl(-1.f);

        Camera camera = _stage.getCamera();
        camera.position.add(new Vector3(move.x, move.y, 0.f));

        _lastTouchPoint = new Vector2(position.x, position.y);
        return true;
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
