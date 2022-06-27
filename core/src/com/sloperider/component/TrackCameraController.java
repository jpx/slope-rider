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
    private GestureDetector _gestureDetector;

    private Track _track;
    private Vector2 _trackPosition;
    private Vector2 _trackSize;

    private float _moveDuration;

    private final Vector2 _targetPosition = new Vector2();
    private float _targetZoom;

    private boolean _moveActive;
    private float _elapsedTimeSinceMoveStart;
    private Vector2 _moveStartPosition;
    private float _moveStartZoom;

    private float _initialZoomDistance;
    private float _lastZoomDistance;

    private boolean _hasTarget;

    public TrackCameraController() {
        _gestureDetector = new GestureDetector(this);
    }

    public final TrackCameraController setTrack(Track track) {
        _track = track;
        return this;
    }

    public final boolean hasTarget() {
        return _hasTarget;
    }

    private void trackBoundsChanged(float x, float y, float width, float height) {
    }

    public final TrackCameraController startMove(final Vector2 targetPosition, final float targetZoom) {
        final OrthographicCamera camera = getCamera();

        _moveDuration = 0.5f;

        _moveActive = true;

        _elapsedTimeSinceMoveStart = 0.f;

        _moveStartPosition = new Vector2(camera.position.x, camera.position.y);
        _moveStartZoom = camera.zoom;

        _targetPosition.set(targetPosition);
        _targetZoom = targetZoom;

        return this;
    }

    public final TrackCameraController moveTo(final Vector2 targetPosition, final float targetZoom) {
        _targetPosition.set(targetPosition);
        _targetZoom = targetZoom;

        getCamera().zoom = _targetZoom;
        setCameraPosition(new Vector3(_targetPosition.x, _targetPosition.y, 0.f));

        _hasTarget = true;

        return this;
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
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        input().addProcessor(_gestureDetector);
        input().addProcessor(this);

        _moveActive = false;

        _trackPosition = new Vector2();
        _trackSize = new Vector2();

        _hasTarget = false;
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
        input().removeProcessor(_gestureDetector);
        input().removeProcessor(this);
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

            if (_hasTarget) {
                if (!_moveActive)
                    moveTo(_targetPosition, _targetZoom);
                else
                    setCameraPosition(getCamera().position);
            }
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

                setCameraPosition(new Vector3(position.x, position.y, camera.position.z));
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
    public void updateBody(World world, float deltaTime) {

    }

    @Override
    public void destroyBody(World world) {

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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
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

        setCameraPosition(camera.position.cpy().add(offset));

        _hasTarget = false;

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
            final float maxZoom = maxZoom();
            final float zoomSpeed = 0.005f;

            OrthographicCamera camera = getCamera();

            final float newValue = MathUtils.clamp(camera.zoom + delta * zoomSpeed, minZoom, maxZoom);

            _hasTarget = false;
            camera.zoom = newValue;

            setCameraPosition(camera.position);

            _lastZoomDistance = distance;
        }

        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private Vector3 checkPosition(final Vector3 position) {
        final OrthographicCamera camera = getCamera();

        final Vector2 minBound = _trackPosition.cpy().scl(SlopeRider.PIXEL_PER_UNIT)
            .add(new Vector2(camera.viewportWidth, camera.viewportHeight).scl(camera.zoom * 0.5f));

        final Vector2 maxBound = _trackPosition.cpy().add(_trackSize).scl(SlopeRider.PIXEL_PER_UNIT)
            .sub(new Vector2(camera.viewportWidth, camera.viewportHeight).scl(camera.zoom * 0.5f));

        return new Vector3(
            MathUtils.clamp(position.x, minBound.x, maxBound.x),
            MathUtils.clamp(position.y, minBound.y, maxBound.y),
            position.z
        );
    }

    private void setCameraPosition(final Vector3 position) {
        final OrthographicCamera camera = getCamera();

        camera.position.set(checkPosition(position));
    }

    private float maxZoom() {
        final float hRatio = _track.getWidth() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportWidth;
        final float vRatio = _track.getHeight() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportHeight;

        return Math.min(hRatio, vRatio);
    }
}
