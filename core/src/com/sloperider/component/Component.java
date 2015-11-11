package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.sloperider.physics.PhysicsActor;

/**
 * Created by jpx on 11/11/15.
 */
public abstract class Component extends Actor implements PhysicsActor {

    private boolean _ready;

    public Component() {
        super();

        _ready = false;
    }

    public abstract  void requireAssets(AssetManager assetManager);
    public abstract void manageAssets(AssetManager assetManager);

    protected abstract  void doReady();
    protected abstract void doAct(float delta);
    protected abstract void doDraw(Batch batch);

    public final void ready() {
        if (_ready)
            return;

        _ready = true;

        doReady();
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
}
