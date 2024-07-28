package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import com.sloperider.ComponentFactory;
import com.sloperider.LevelSet;
import com.sloperider.component.Level;
import com.sloperider.physics.PhysicsWorld;

/**
 * Created by jpx on 03/12/15.
 */
public class MainMenuScreen extends Screen {
    private Stage _uiStage;

    private class UI {
        private Skin _skin;
        private Table _parent;

        private Table _menuTable;

        UI(final Stage stage, final MasterScreen masterScreen) {
            _skin = masterScreen._assetManager.get("ui/uiskin.json", Skin.class);

            _parent = new Table(_skin);
            _parent.setFillParent(true);

            _parent.defaults().space(60.f);

            final Image titleImage = new Image(new TextureRegion(new Texture("ui/title.png")));
            _parent.add(titleImage).row();

            _menuTable = new Table(_skin);

            final TextButton playButton = new TextButton("Play", _skin);

            playButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    playButtonClicked();
                }
            });

            final TextButton levelsButton = new TextButton("Levels", _skin);
            levelsButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    levelsButtonClicked();
                }
            });

            final float width = 180.f;
            final float height = 30.f;

            _menuTable.defaults().space(20.f);
            _menuTable.add(playButton).prefSize(width, height).row();
            _menuTable.add(levelsButton).prefSize(width, height).row();
            _menuTable.pack();

            _parent.add(_menuTable);
            _parent.pack();

            stage.addActor(_parent);
        }
    }

    private Stage _levelStage;
    private PhysicsWorld _physicsWorld;
    private ComponentFactory _componentFactory;
    private Level _backgroundLevel;

    private Timer _spawnSleighTimer;

    private int _currentLevelIndex;

    private void playButtonClicked() {
        _masterScreen.push(new GameScreen(_masterScreen).startingLevel(_currentLevelIndex));
    }

    private void levelsButtonClicked() {
        _masterScreen.push(new LevelsMenuScreen(_masterScreen));
    }

    public MainMenuScreen(final MasterScreen masterScreen) {
        _uiStage = new Stage();

        new UI(_uiStage, masterScreen);

        _levelStage = new Stage();
        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();
    }

    @Override
    public void start() {
        Gdx.input.setInputProcessor(new InputMultiplexer(_uiStage, _levelStage));

        findCurrentLevelIndex();

        _backgroundLevel = _componentFactory.createLevel("level/title_level.lvl")
            .startAsViewOnly();

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

    private void findCurrentLevelIndex() {
        _currentLevelIndex = LevelSet.instance().findCurrentLevel();
    }
}
