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
import com.sloperider.component.End;
import com.sloperider.component.Flag;
import com.sloperider.component.Sleigh;
import com.sloperider.component.Track;
import com.sloperider.component.TrackCameraController;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;

public class SlopeRider extends ApplicationAdapter implements InputProcessor {

    public static final float PIXEL_PER_UNIT = 100.0f;
    public static final String TAG = "slope-rider_DEBUG";

    Stage _stage;

    SpriteBatch _spriteBatch;

    PhysicsWorld _physicsWorld;

    Track _track;
    Flag _flag;
    End _end;

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

        final List<Track.PointData> points0 = new ArrayList<Track.PointData>();
        points0.add(new Track.PointData(0.0f, 0.f, false, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.1f, 0.f, true, Track.GroundMaterialType.BOOSTER));
        points0.add(new Track.PointData(0.2f, 0.f, true, Track.GroundMaterialType.BOOSTER));
        points0.add(new Track.PointData(0.3f, 0.f, true, Track.GroundMaterialType.BOOSTER));
        points0.add(new Track.PointData(0.4f, -0.6f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.5f, 0.f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.6f, 0.f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.7f, 0.f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.8f, 0.f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(0.9f, 0.f, true, Track.GroundMaterialType.SNOW));
        points0.add(new Track.PointData(1.f, 0.f, true, Track.GroundMaterialType.SNOW));
        _track.setPoints(points0);

        TrackCameraController cameraController = _componentFactory.createComponent(new Vector2(), TrackCameraController.class)
            .setTrack(_track);

        _track.addListener(new Track.Listener() {
            @Override
            public void changed(Track self) {
                Gdx.app.log(SlopeRider.TAG, "" + self.heightAt(5.f));
                _flag.setPosition(_flag.getX(), self.heightAt(5.f));
                _end.setPosition(_end.getX(), self.heightAt(50.f));
            }
        });

        final List<Track.PointData> points1 = new ArrayList<Track.PointData>();
        points1.add(new Track.PointData(0.0f, 0.f, true, Track.GroundMaterialType.SNOW));
        points1.add(new Track.PointData(0.25f, 0.f, true, Track.GroundMaterialType.SNOW));
        points1.add(new Track.PointData(0.5f, 0.f, true, Track.GroundMaterialType.SNOW));
        points1.add(new Track.PointData(0.75f, 0.f, true, Track.GroundMaterialType.SNOW));
        points1.add(new Track.PointData(1.f, 0.f, true, Track.GroundMaterialType.SNOW));
        _componentFactory.createComponent(new Vector2(72.f, -25.f), Track.class).setPoints(points1);

        _flag = _componentFactory.createComponent(new Vector2(15.f, 24.8f), Flag.class);
        _end = _componentFactory.createComponent(new Vector2(60.f, 25.f), End.class);

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

		Gdx.gl.glClearColor(0.2f, 0.4f, 0.9f, 1.0f);
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
        final Vector3 viewPosition = _stage.getCamera().unproject(new Vector3(screenX, screenY, 0.f));
        final Vector2 position = new Vector2(viewPosition.x, viewPosition.y).scl(1.f / SlopeRider.PIXEL_PER_UNIT);

        Sleigh sleigh = _componentFactory.createComponent(position, Sleigh.class);

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
