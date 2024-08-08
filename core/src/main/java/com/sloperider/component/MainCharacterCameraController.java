package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 26/12/15.
 */
public class MainCharacterCameraController extends Component {
    private final Vector3 _targetPosition = new Vector3();
    private float _targetZoom;

    private MainCharacter _target;

    public MainCharacterCameraController target(final MainCharacter mainCharacter) {
        _target = mainCharacter;

        updateTargetPosition();

        return this;
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
    }

    @Override
    protected void doAct(float delta) {
        updateTargetZoom();
        updateTargetPosition();

        final OrthographicCamera camera = getCamera();

        camera.position.lerp(_targetPosition, 0.1f);
        camera.zoom = MathUtils.lerp(camera.zoom, _targetZoom, 0.1f);

        camera.position.set(checkPosition(camera.position));
    }

    @Override
    protected void doDraw(Batch batch) {

    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

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

    private void updateTargetPosition() {
        final Vector2 velocity = _target.body() == null
            ? Vector2.Zero
            : _target.body().getLinearVelocity();

        final Vector3 offset = new Vector3(velocity.x, velocity.y, 0.f)
            .scl(0.2f)
            .scl(SlopeRider.PIXEL_PER_UNIT);

        final OrthographicCamera camera = getCamera();

        offset.set(
            Math.min(offset.x, camera.viewportWidth / 4.f),
            Math.min(offset.y, camera.viewportHeight / 4.f),
            offset.z
        );

        final Vector3 position = new Vector3(_target.getX(), _target.getY(), 0.f)
            .scl(SlopeRider.PIXEL_PER_UNIT)
            .add(offset);

        _targetPosition.set(checkPosition(position));
    }

    private void updateTargetZoom() {
        final float minVelocity = 0.f;
        final float maxVelocity = 50.f;
        final float velocity = _target.body() == null
            ? 0.f
            : _target.body().getLinearVelocity().len();

        final float zoom = MathUtils.lerp(
            minZoom(),
            maxZoom(),
            MathUtils.clamp((velocity - minVelocity) / (maxVelocity - minVelocity), 0.f, 1.f)
        );

        _targetZoom = MathUtils.clamp(
            zoom,
            minZoom(),
            maxZoom()
        );
    }

    private Vector3 checkPosition(final Vector3 position) {
        final OrthographicCamera camera = getCamera();

        final Vector2 minBound = new Vector2(getX(), getY()).scl(SlopeRider.PIXEL_PER_UNIT)
            .add(new Vector2(camera.viewportWidth, camera.viewportHeight).scl(camera.zoom * 0.5f));

        final Vector2 maxBound = new Vector2(getRight(), getTop()).scl(SlopeRider.PIXEL_PER_UNIT)
            .sub(new Vector2(camera.viewportWidth, camera.viewportHeight).scl(camera.zoom * 0.5f));

        return new Vector3(
            MathUtils.clamp(position.x, minBound.x, maxBound.x),
            MathUtils.clamp(position.y, minBound.y, maxBound.y),
            position.z
        );
    }

    private float minZoom() {
        return 0.5f;
    }

    private float maxZoom() {
        final float hRatio = getWidth() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportWidth;
        final float vRatio = getHeight() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportHeight;

        return Math.min(hRatio, vRatio) * 0.75f;
    }
}
