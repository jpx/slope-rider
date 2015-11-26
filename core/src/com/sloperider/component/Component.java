package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

import sun.rmi.runtime.Log;

/**
 * Created by jpx on 11/11/15.
 */
public abstract class Component extends Group implements PhysicsActor {

    private boolean _ready;

    public Component() {
        super();

        _ready = false;
    }

    public abstract  void requireAssets(AssetManager assetManager);
    public abstract void manageAssets(AssetManager assetManager);

    protected abstract void doReady(ComponentFactory componentFactory);
    protected abstract void doAct(float delta);
    protected abstract void doDraw(Batch batch);

    public final void ready(ComponentFactory componentFactory) {
        if (_ready)
            return;

        _ready = true;

        doReady(componentFactory);
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
    public CollisionGroup group() {
        return CollisionGroup.ANYTHING;
    }

    @Override
    public CollisionGroup collidesWith() {
        return CollisionGroup.NOTHING;
    }
}
