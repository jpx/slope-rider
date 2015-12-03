package com.sloperider.screen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jpx on 03/12/15.
 */
public class MasterScreen extends Screen {
    private final List<Screen> _screens = new ArrayList<Screen>();
    private Screen _activeScreen;

    public final void push(final Screen screen) {
        if (_activeScreen != null) {
            _activeScreen.stop();
        }

        screen._masterScreen = this;
        screen.assetManager(_assetManager);
        _screens.add(screen);
        _activeScreen = screen;

        _activeScreen.start();
    }

    public final void pop() {
        _activeScreen.stop();
        _activeScreen = _screens.remove(_screens.size() - 1);
        _activeScreen.start();
    }

    @Override
    public void start() {
        if (_activeScreen != null)
            _activeScreen.start();
    }

    @Override
    public void stop() {
        if (_activeScreen != null)
            _activeScreen.stop();
    }

    @Override
    public void update(float deltaTime) {
        _activeScreen.update(deltaTime);
    }

    @Override
    public void render() {
        _activeScreen.render();
    }

    @Override
    public void dispose() {
        _activeScreen.dispose();
    }
}
