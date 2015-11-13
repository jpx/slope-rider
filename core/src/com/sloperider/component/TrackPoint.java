package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 12/11/15.
 */
public class TrackPoint extends Component {

    private ShapeRenderer _renderer;
    private float _radius;

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _renderer = new ShapeRenderer();
        _renderer.setAutoShapeType(true);

        _radius = 0.5f;

        setSize(_radius * 2.f, _radius * 2.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _renderer.begin(ShapeRenderer.ShapeType.Filled);
        _renderer.setColor(Color.BLACK);

        Matrix4 transformMatrix = batch.getTransformMatrix();

        transformMatrix.scale(SlopeRider.PIXEL_PER_UNIT, SlopeRider.PIXEL_PER_UNIT, SlopeRider.PIXEL_PER_UNIT);

        _renderer.setProjectionMatrix(batch.getProjectionMatrix());
        _renderer.setTransformMatrix(transformMatrix);

        Vector2 position = localToStageCoordinates(new Vector2(getX(), getY()));

        _renderer.circle(position.x, position.y, _radius, 20);

        _renderer.end();

        batch.begin();
    }

    @Override
    public void initializeBody(World world) {

    }

    @Override
    public void updateBody(World world) {

    }
}
