package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.component.Level;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;

/**
 * Created by jpx on 03/12/15.
 */
public class LevelsMenuScreen extends Screen {
    private Stage _uiStage;

    private class LevelEntry {
        String name;
        String filename;
        boolean complete;
        float bestScore;
    }

    private class UI {
        private Skin _skin;
        private Table _parent;

        private Table _menuTable;

        UI(final Stage stage, final MasterScreen masterScreen) {
            _skin = masterScreen._assetManager.get("ui/uiskin.json", Skin.class);

            _parent = new Table(_skin);
            _parent.setFillParent(true);

            final List<String> _menu = new List<String>(_skin);
            final ScrollPane _scrollPane = new ScrollPane(_menu, _skin);

            final Array items = new Array();

            for (final LevelEntry level : _levels) {
                items.add(level.name);
            }
            _menu.setItems(items);

            _menu.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setBackgroundLevel(_menu.getSelectedIndex());
                }
            });

            _parent.defaults().space(60.f);
            _parent.add(_scrollPane).maxSize(480.f, 320.f);
            _parent.pack();

            stage.addActor(_parent);
        }
    }

    private Stage _levelStage;
    private PhysicsWorld _physicsWorld;
    private ComponentFactory _componentFactory;
    private Level _backgroundLevel;

    private Timer _spawnSleighTimer;

    private final java.util.List<LevelEntry> _levels = new ArrayList<LevelEntry>();

    public LevelsMenuScreen(final MasterScreen masterScreen) {
        _uiStage = new Stage();

        // tmp
        for (int i = 0; i < 15; ++i) {
            _levels.add(new LevelEntry());
            _levels.get(_levels.size() - 1).name = "level0" + i;
            _levels.get(_levels.size() - 1).filename = "level/level0.lvl";
            _levels.add(new LevelEntry());
            _levels.get(_levels.size() - 1).name = "level1" + i;
            _levels.get(_levels.size() - 1).filename = "level/level1.lvl";
        }

        new UI(_uiStage, masterScreen);

        _levelStage = new Stage();
        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();
    }

    @Override
    public void start() {
        Gdx.input.setInputProcessor(new InputMultiplexer(_uiStage, _levelStage));

        setBackgroundLevel(0);

        _spawnSleighTimer = new Timer();
        _spawnSleighTimer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                _backgroundLevel.spawnSleigh();
            }
        }, 2.f, 12.f);
        _spawnSleighTimer.start();
    }

    @Override
    public void stop() {
        _spawnSleighTimer.stop();
        _spawnSleighTimer = null;
        _backgroundLevel.destroySleigh();
        _componentFactory.destroyComponent(_backgroundLevel);
    }

    @Override
    public void update(float deltaTime) {
        _physicsWorld.update(deltaTime);
        _levelStage.act(deltaTime);
        _uiStage.act(deltaTime);
    }

    @Override
    public void render() {
        _levelStage.draw();
        _uiStage.draw();
    }

    @Override
    public void dispose() {
        _uiStage.dispose();
        _uiStage = null;
    }

    private void setBackgroundLevel(final int index) {
        if (_backgroundLevel != null) {
            _backgroundLevel.destroySleigh();
            _componentFactory.destroyComponent(_backgroundLevel);
        }

        _backgroundLevel = _componentFactory.createLevel(_levels.get(index).filename)
            .startAsViewOnly();
    }
}
