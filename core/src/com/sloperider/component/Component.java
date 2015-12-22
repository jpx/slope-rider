package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.JsonValue;
import com.sloperider.ComponentFactory;
import com.sloperider.Layer;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

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

    protected void levelPlayed(final Level level) { }
    protected void levelStopped(final Level level) { }

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
    public short group() {
        return CollisionGroup.ANYTHING.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.NOTHING.value();
    }
}
