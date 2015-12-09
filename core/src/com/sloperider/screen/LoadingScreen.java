package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Created by jpx on 08/12/15.
 */
public class LoadingScreen extends Screen {
    private final SpriteBatch batch = new SpriteBatch();
    private final TextureRegion sprite = new TextureRegion(new Texture("texture/loading_image.png"));

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void update(float deltaTime) {
    }

    @Override
    public void render() {
        batch.begin();
        batch.draw(sprite, 0.f, 0.f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void dispose() {

    }

    @Override
    public void ready() {
        super.ready();

        _masterScreen.push(new MainMenuScreen(_masterScreen));
    }
}
