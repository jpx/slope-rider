package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.function.Function;

/**
 * Created by jpx on 08/12/15.
 */
public class LoadingScreen extends Screen {
    public interface Redirect {
        int limit();
        void call();
    }

    private final SpriteBatch batch = new SpriteBatch();
    private final TextureRegion sprite = new TextureRegion(new Texture("texture/loading_screen.png"));

    private boolean _ready = false;
    private Redirect _redirect = null;
    private int _redirectCallCount = 0;

    public final LoadingScreen redirect(Redirect redirect) {
        _redirect = redirect;

        return this;
    }

    @Override
    public void start() {
        if (_ready) {
            redirect();
        }
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

        _ready = true;

        redirect();
    }

    private void redirect() {
        if (_redirect == null || _redirectCallCount >= _redirect.limit()) {
            _masterScreen.push(new MainMenuScreen(_masterScreen));
            return;
        }

        ++_redirectCallCount;

        _redirect.call();
    }
}
