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
 * Created by jpx on 01/01/16.
 */
public class OverviewCameraController extends Component {
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
        updateCamera();
    }

    @Override
    protected void doAct(float delta) {
        updateCamera();
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

    private void updateCamera() {
        final OrthographicCamera camera = getCamera();

        final Vector3 position = checkPosition(new Vector3(getX(), getY(), 0.f));

        camera.position.set(position);
        camera.zoom = optimalZoom();
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

    private float optimalZoom() {
        final float hRatio = getWidth() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportWidth;
        final float vRatio = getHeight() * SlopeRider.PIXEL_PER_UNIT / getCamera().viewportHeight;

        return Math.min(hRatio, vRatio);
    }
}
