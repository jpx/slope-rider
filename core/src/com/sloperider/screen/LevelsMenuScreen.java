package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
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
        boolean unlocked;
        float bestScore;
    }

    private class UI {
        private Skin _skin;
        private Table _parent;

        private Table _menuTable;

        Stack levelInfoStack;
        Table levelInfoTable;
        Table lockedImageTable;
        TextButton playButton;
        Image lockedImage;

        void currentIndexChanged(final int index) {
            if (_levelIndex == index)
                return;

            _levelIndex = index;

            final LevelEntry levelEntry = _levels.get(index);

            if (levelEntry.unlocked) {
                levelInfoStack.add(levelInfoTable);
                levelInfoStack.removeActor(lockedImageTable);
            } else {
                levelInfoStack.add(lockedImageTable);
                levelInfoStack.removeActor(levelInfoTable);
            }

            setBackgroundLevel(index);
        }

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
                    final int index = _menu.getSelectedIndex();

                    currentIndexChanged(index);
                }
            });

            levelInfoStack = new Stack();

            levelInfoTable = new Table(_skin);
            final Label bestScoreLabel = new Label("BEST", _skin);
            final Label bestScoreValueLabel = new Label("42", _skin);

            playButton = new TextButton("Play", _skin);

            playButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    playLevel(_levelIndex);
                }
            });

            final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0.f, 0.f, 0.f, 0.6f);
            pixmap.fill();

            final Drawable background = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));

            lockedImageTable = new Table(_skin);
            lockedImageTable.setBackground(background);
            lockedImage = new Image(new TextureRegion(new Texture("ui/locked_icon.png")));
            lockedImageTable.add(lockedImage).maxSize(128.f, 128.f);

            levelInfoStack.add(levelInfoTable);
            levelInfoStack.add(lockedImageTable);

            levelInfoTable.defaults().spaceLeft(20.f).spaceRight(20.f);
            levelInfoTable.setBackground(background);
            levelInfoTable.add(bestScoreLabel).align(Align.right).expand();
            levelInfoTable.add(bestScoreValueLabel).align(Align.left).expand().row();
            levelInfoTable.add(playButton).colspan(2).expand();
            levelInfoTable.pack();

            _parent.add(_scrollPane).size(300.f, 400.f).align(Align.right);
            _parent.add(levelInfoStack).size(300.f, 400.f).align(Align.left);
            _parent.pack();

            stage.addActor(_parent);
        }
    }

    private Stage _levelStage;
    private PhysicsWorld _physicsWorld;
    private ComponentFactory _componentFactory;
    private Level _backgroundLevel;
    private int _levelIndex;

    private Timer _spawnSleighTimer;

    private final java.util.List<LevelEntry> _levels = new ArrayList<LevelEntry>();

    public LevelsMenuScreen(final MasterScreen masterScreen) {
        _levelIndex = -1;
        _uiStage = new Stage();

        // tmp
        for (int i = 0; i < 15; ++i) {
            _levels.add(new LevelEntry());
            _levels.get(_levels.size() - 1).name = "level0" + i;
            _levels.get(_levels.size() - 1).filename = "level/level0.lvl";
            _levels.get(_levels.size() - 1).unlocked = true;
            _levels.add(new LevelEntry());
            _levels.get(_levels.size() - 1).name = "level1" + i;
            _levels.get(_levels.size() - 1).filename = "level/level1.lvl";
            _levels.get(_levels.size() - 1).unlocked = true;
        }

        final UI ui = new UI(_uiStage, masterScreen);

        _levelStage = new Stage();
        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();

        ui.currentIndexChanged(0);
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
        _backgroundLevel = null;
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

    private void playLevel(final int index) {
        _masterScreen.push(new GameScreen(_masterScreen)
            .startingLevel(_levels.get(index).filename)
        );
    }
}
