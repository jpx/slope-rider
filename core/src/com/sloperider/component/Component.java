package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.JsonValue;
import com.sloperider.ComponentFactory;
import com.sloperider.Layer;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;
import com.sloperider.physics.SmoothingState;

import java.util.ArrayList;
import java.util.List;

import sun.rmi.runtime.Log;

/**
 * Created by jpx on 11/11/15.
 */
public abstract class Component extends Group implements PhysicsActor {

    private boolean _ready;

    protected Layer _layer;

    protected final List<Component> _components = new ArrayList<Component>();

    protected final <T extends Component> T addComponent(final T component) {
        _components.add(component);

        return component;
    }

    protected final <T extends Component> T removeComponent(final T component) {
        _components.remove(component);

        return component;
    }

    protected final boolean hasComponent(final Component component) {
        return _components.contains(component);
    }

    public final Component setLayer(final Layer layer) {
        _layer = layer;

        return this;
    }

    public Component() {
        super();

        _ready = false;
    }

    protected InputMultiplexer input() {
        return (InputMultiplexer) Gdx.input.getInputProcessor();
    }

    public abstract void requireAssets(AssetManager assetManager);
    public abstract void manageAssets(AssetManager assetManager);
    public abstract void doReleaseAssets(AssetManager assetManager);

    public void releaseAssets(AssetManager assetManager) {
        for (final Component component : _components) {
            component.releaseAssets(assetManager);
        }

        doReleaseAssets(assetManager);
    }

    protected abstract void doReady(ComponentFactory componentFactory);
    protected abstract void doAct(float delta);
    protected abstract void doDraw(Batch batch);
    protected abstract void doDestroy(ComponentFactory componentFactory);

    protected final void levelPlayed(final Level level) {
        for (final Component component : _components) {
            component.levelPlayed(level);
        }

        doLevelPlayed(level);
    }

    protected final void levelStopped(final Level level) {
        for (final Component component : _components) {
            component.levelStopped(level);
        }

        doLevelStopped(level);
    }

    protected final void levelComplete(final Level level) {
        for (final Component component : _components) {
            component.levelComplete(level);
        }

        doLevelComplete(level);
    }

    protected final void levelPhysicsUpdate(final World world, final float deltaTime, final int stepId, final int frameId) {
        for (final Component component : _components) {
            component.levelPhysicsUpdate(world, deltaTime, stepId, frameId);
        }

        doLevelPhysicsUpdate(world, deltaTime, stepId, frameId);
    }

    protected void doLevelPlayed(final Level level) { }
    protected void doLevelStopped(final Level level) { }
    protected void doLevelComplete(final Level level) { }
    protected void doLevelPhysicsUpdate(final World world, final float deltaTime, final int stepId, final int frameId) { }

    public final void ready(ComponentFactory componentFactory) {
        if (_ready)
            return;

        _ready = true;

        doReady(componentFactory);
    }

    public final void destroy(ComponentFactory componentFactory) {
        if (!_ready)
            return;

        for (final Component component : _components) {
            componentFactory.destroyComponent(component);
        }

        _components.clear();

        clear();

        doDestroy(componentFactory);
    }

    @Override
    public final void act(float delta) {
        super.act(delta);

        if (!_ready)
            return;

        doAct(delta);
    }

    @Override
    public final void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        if (!_ready)
            return;

        doDraw(batch);
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        Vector2 position = new Vector2(x, y)
            .sub(getOriginX(), getOriginY())
            .scl(1.f / SlopeRider.PIXEL_PER_UNIT)
            .sub(getX(), getY())
            .add(getOriginX(), getOriginY());

        return doHit(position.x, position.y, touchable);
    }

    protected Actor doHit(float x, float y, boolean touchable) {
        return super.hit(x, y, touchable);
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);

        if (stage == null)
            _ready = false;
    }

    @Override
    public short group() {
        return CollisionGroup.ANYTHING.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.NOTHING.value();
    }

    @Override
    public void resetSmoothingState(World world, float deltaTime) {
    }

    @Override
    public void applySmoothingState(World world, float deltaTime, float alpha) {
    }

    protected final OrthographicCamera getCamera() {
        return (OrthographicCamera) getStage().getCamera();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        for (final Component component : _components)
            component.setVisible(visible);
    }

    @Override
    public void setTouchable(Touchable touchable) {
        super.setTouchable(touchable);

        for (final Component component : _components)
            component.setTouchable(touchable);
    }
}
