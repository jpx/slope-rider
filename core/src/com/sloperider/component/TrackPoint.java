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
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 12/11/15.
 */
public class TrackPoint extends Component {

    public interface ChangedHandler
    {
        public void changed(TrackPoint self, float y);
    }

    private ChangedHandler _changedHandler;
    private ShapeRenderer _renderer;
    private float _radius;

    private boolean _draggingActive;
    private Vector2 _draggingStartTouchPosition;
    private Vector2 _draggingTouchPosition;

    private Vector2 _draggingStartPosition;

    private float _draggingStartTrackValue;
    private float _trackValue;

    public TrackPoint setChangedHandler(ChangedHandler handler) {
        _changedHandler = handler;
        return this;
    }

    public TrackPoint setInitialTrackValue(float value) {
        _trackValue = value;
        return this;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _draggingActive = false;

        _renderer = new ShapeRenderer();
        _renderer.setAutoShapeType(true);

        _radius = 0.5f;

        setSize(_radius * 2.f, _radius * 2.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!_draggingActive)
                    startDragging(new Vector2(x, y));

                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (_draggingActive)
                    stopDragging(new Vector2(x, y));

                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (_draggingActive)
                    updateDragging(new Vector2(x, y));

                super.touchDragged(event, x, y, pointer);
            }
        });
    }

    @Override
    protected void doAct(float delta) {
        if (_draggingActive) {
            final float trackValueOffset = (_draggingTouchPosition.y - _draggingStartTouchPosition.y) / SlopeRider.PIXEL_PER_UNIT;

            setTrackValue(_draggingStartTrackValue + trackValueOffset);
        }
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
    public Actor doHit(float x, float y, boolean touchable) {
        final float additionalHitScale = 5.f;

        Vector2 minBound = new Vector2(
            -getOriginX() - getWidth() * additionalHitScale / 2.f,
            -getOriginY() - getHeight() * additionalHitScale / 2.f
        );

        Vector2 maxBound =  new Vector2(
            -getOriginX() + getWidth() * (1.f + additionalHitScale / 2.f),
            -getOriginY() + getHeight() * (1.f + additionalHitScale / 2.f)
        );

        if (x >= minBound.x && x < maxBound.x &&
            y >= minBound.y && y < maxBound.y)
            return this;

        return null;
    }

    private void startDragging(Vector2 position) {
        _draggingActive = true;

        _draggingStartTouchPosition = position;
        _draggingTouchPosition = _draggingStartTouchPosition;

        _draggingStartPosition = new Vector2(getX(), getY());

        _draggingStartTrackValue = _trackValue;
    }

    private void stopDragging(Vector2 position) {
        _draggingActive = false;
    }

    private void updateDragging(Vector2 position) {
        _draggingTouchPosition = position;
    }

    private void setPositionFromTrackValue(float trackValue) {
        setPosition(_draggingStartPosition.x, trackValue);
    }

    private void setTrackValue(float value) {
        if (_trackValue == value)
            return;

        Gdx.app.log(SlopeRider.TAG, "trackvalue: " + (value));

        setPositionFromTrackValue(value);

        _trackValue = value;

        _changedHandler.changed(this, _trackValue);
    }
}
