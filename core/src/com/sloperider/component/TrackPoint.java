package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.sloperider.ComponentFactory;
import com.sloperider.Layer;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 12/11/15.
 */
public class TrackPoint extends Component {

    public interface ChangedHandler
    {
        void changed(TrackPoint self, float y);
    }

    private Draggable _draggable;

    private ChangedHandler _changedHandler;
    private ShapeRenderer _renderer;
    private float _radius;

    private float _trackValue;
    private float _minBound;
    private float _maxBound;

    public TrackPoint setChangedHandler(ChangedHandler handler) {
        _changedHandler = handler;
        return this;
    }

    public TrackPoint setInitialTrackValue(float value) {
        _trackValue = value;
        return this;
    }

    public TrackPoint setBounds(float minBound, float maxBound) {
        _minBound = minBound;
        _maxBound = maxBound;

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
        _renderer = new ShapeRenderer();
        _renderer.setAutoShapeType(true);

        _radius = 0.5f;

        setSize(_radius * 2.f, _radius * 2.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        _draggable = new Draggable()
            .draggedComponent(this)
            .draggingMask(Vector2.Y)
            .draggingBounds(new Vector2(getX(), getY()), new Vector2(-0.5f, _minBound), new Vector2(0.5f, _maxBound));

        addComponent(componentFactory.initializeComponent(Layer.FRONT0, _draggable)
            .registerListener(new Draggable.Listener() {
                @Override
                public void dragged(Draggable self, Vector2 move, Vector2 position, float deltaDistance) {
                    setTrackValue(_trackValue + move.y);
                }
            }));
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
    }

    @Override
    protected void doAct(float delta) {
    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _renderer.begin(ShapeRenderer.ShapeType.Filled);
        _renderer.setColor(Color.BLACK);

        Vector2 position = new Vector2(getX(), getY());

        Matrix4 transformMatrix = batch.getTransformMatrix().cpy();

        transformMatrix
            .translate(-getOriginX(), -getOriginY(), 0.f)
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(position.x, position.y, 0.f);

        _renderer.setProjectionMatrix(batch.getProjectionMatrix());
        _renderer.setTransformMatrix(transformMatrix);

        _renderer.circle(0.f, 0.f, _radius, 20);

        _renderer.end();

        batch.begin();
    }

    @Override
    public void initializeBody(World world) {

    }

    @Override
    public void updateBody(World world) {

    }

    @Override
    public void destroyBody(World world) {

    }

    private void setTrackValue(float value) {
        if (_trackValue == value)
            return;

        _trackValue = value;

        _changedHandler.changed(this, _trackValue);
    }
}
