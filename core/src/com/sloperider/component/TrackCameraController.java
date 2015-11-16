package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 15/11/15.
 */
public class TrackCameraController
        extends Component
        implements GestureDetector.GestureListener, InputProcessor {
    private Track _track;
    private Vector2 _trackPosition;
    private Vector2 _trackSize;

    private float _moveDuration;

    private Vector2 _targetPosition;
    private float _targetZoom;

    private boolean _moveActive;
    private float _elapsedTimeSinceMoveStart;
    private Vector2 _moveStartPosition;
    private float _moveStartZoom;

    private float _initialZoomDistance;
    private float _lastZoomDistance;

    public TrackCameraController() {

    }

    public final TrackCameraController setTrack(Track track) {
        _track = track;
        return this;
    }

    private OrthographicCamera getCamera() {
        return (OrthographicCamera) getStage().getCamera();
    }

    private void trackBoundsChanged(float x, float y, float width, float height) {
        _targetPosition = new Vector2(x, y)
                .add(new Vector2(width * 0.5f, height * 0.75f))
                .scl(SlopeRider.PIXEL_PER_UNIT);

        // FIXME
        _targetZoom = 3.5f;
    }

    private void startMove() {
        OrthographicCamera camera = getCamera();

        _moveDuration = 0.5f;

        _moveActive = true;

        _elapsedTimeSinceMoveStart = 0.f;

        _moveStartPosition = new Vector2(camera.position.x, camera.position.y);
        _moveStartZoom = camera.zoom;
    }

    private void stopMove() {
        _moveActive = false;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _moveActive = false;

        _trackPosition = new Vector2();
        _trackSize = new Vector2();

        startMove();
    }

    @Override
    protected void doAct(float delta) {
        final Vector2 trackPosition = new Vector2(_track.getX(), _track.getY());
        final Vector2 trackSize = new Vector2(_track.getWidth(), _track.getHeight());

        if (!_trackPosition.epsilonEquals(trackPosition, 1e-4f) ||
            !_trackSize.epsilonEquals(trackSize, 1e-4f)) {
            _trackPosition.set(trackPosition);
            _trackSize.set(trackSize);

            trackBoundsChanged(trackPosition.x, trackPosition.y, trackSize.x, trackSize.y);
        }

        if (_moveActive) {
            OrthographicCamera camera = getCamera();

            _elapsedTimeSinceMoveStart += delta;

            if (_elapsedTimeSinceMoveStart > _moveDuration) {
                _moveActive = false;
            } else {
                final float rate = MathUtils.clamp(_elapsedTimeSinceMoveStart / _moveDuration, 0.f, 1.f);

                final Vector2 position = _moveStartPosition.cpy().lerp(_targetPosition, rate);
                final float zoom = MathUtils.lerp(_moveStartZoom, _targetZoom, rate);

                camera.position.set(position.x, position.y, camera.position.z);
                camera.zoom = zoom;
            }
        }
    }

    @Override
    protected void doDraw(Batch batch) {

    }

    @Override
    public void initializeBody(World world) {

    }

    @Override
    public void updateBody(World world) {

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
        if (_moveActive)
            stopMove();
        return false;
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

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        if (!_moveActive && count == 2) {
            startMove();
        }

        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        final OrthographicCamera camera = getCamera();

        final Vector3 previousScreenPosition = new Vector3(x - deltaX, y - deltaY, 0.f).scl(-1.f);
        final Vector3 previousViewPosition = camera.unproject(previousScreenPosition);

        final Vector3 screenPosition = new Vector3(x, y, 0.f).scl(-1.f);
        final Vector3 viewPosition = camera.unproject(screenPosition);

        final Vector3 offset = viewPosition.cpy().sub(previousViewPosition);

        camera.position.add(offset);

        return true;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        if (_initialZoomDistance != initialDistance) {
            _initialZoomDistance = initialDistance;
            _lastZoomDistance = _initialZoomDistance;
        } else {
            final float delta = _lastZoomDistance - distance;

            final float minZoom = 1.5f;
            final float maxZoom = 4.5f;
            final float zoomSpeed = 0.005f;

            OrthographicCamera camera = getCamera();

            camera.zoom = MathUtils.clamp(camera.zoom + delta * zoomSpeed, minZoom, maxZoom);

            _lastZoomDistance = distance;
        }

        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }
}
