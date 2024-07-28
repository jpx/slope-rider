package com.sloperider.screen;

import com.badlogic.gdx.assets.AssetManager;

/**
 * Created by jpx on 03/12/15.
 */
public abstract class Screen {
    protected MasterScreen _masterScreen;
    AssetManager _assetManager;

    public final void assetManager(final AssetManager assetManager) {
        _assetManager = assetManager;
    }

    public abstract void start();
    public abstract void stop();

    public abstract void update(float deltaTime);
    public abstract void render();

    public abstract void dispose();

    public void ready() {}
}
