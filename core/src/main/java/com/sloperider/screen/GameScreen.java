package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.sloperider.ComponentFactory;
import com.sloperider.LevelInfo;
import com.sloperider.LevelSet;
import com.sloperider.SlopeRider;
import com.sloperider.component.Level;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 03/12/15.
 */
public class GameScreen extends Screen {
    private class UI {
        private Skin _skin;

        private Table _parent;

        private Button _playButton;
        private Button _stopButton;

        private final List<Actor> _playingActors = new ArrayList<Actor>();
        private final List<Actor> _editingActors = new ArrayList<Actor>();

        private final List<Actor> _levelEndActors = new ArrayList<Actor>();

        private Table _labelTable;

        private Table _tryCountTable;
        private Label _tryCountLabel;
        private Label _tryCountValueLabel;

        private Table _remainingTable;
        private Label _remainingLabel;
        private Label _remainingValueLabel;

        private Label _levelEndScoreValueLabel;
        private Label _levelEndBestScoreValueLabel;

        private TextButton _levelEndNextLevelButton;

        UI(final Stage stage, final MasterScreen masterScreen) {
            _skin = masterScreen._assetManager.get("ui/uiskin.json", Skin.class);

            _parent = new Table(_skin);

            final Button backButton = new Button(
                new Image(new TextureRegion(new Texture("ui/back_button.png"))),
                _skin
            );

            backButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    backButtonClicked(UI.this, (Button) actor);
                }
            });

            _playButton = new Button(
                new Image(new TextureRegion(new Texture("ui/play_button.png"))),
                _skin
            );

            _playButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    playButtonClicked(UI.this, (Button) actor);
                }
            });

            _stopButton = new Button(
                new Image(new TextureRegion(new Texture("ui/stop_button.png"))),
                _skin
            );

            _stopButton.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    stopButtonClicked(UI.this, (Button) actor);
                }
            });

            _labelTable = new Table(_skin);
            _labelTable.setFillParent(true);

            _tryCountTable = new Table(_skin);

            _tryCountLabel = new Label("Attempts", _skin, "default-font", Color.WHITE);
            _tryCountValueLabel = new Label("", _skin, "default-font", Color.WHITE);

            _tryCountTable.add(_tryCountLabel).row();
            _tryCountTable.add(_tryCountValueLabel);
            _tryCountTable.pack();

            _remainingTable = new Table();

            _remainingLabel = new Label("Remaining", _skin, "default-font", Color.WHITE);
            _remainingValueLabel = new Label("", _skin, "default-font", Color.WHITE);

            _remainingTable.add(_remainingLabel).row();
            _remainingTable.add(_remainingValueLabel);
            _remainingTable.pack();

            _labelTable.defaults().pad(42.f).padRight(64.f).padLeft(64.f);
            _labelTable.add(_remainingTable).expand().align(Align.topRight);
            _labelTable.add(_tryCountTable).expand().align(Align.topLeft);

            final Table levelEndLayoutTable = new Table(_skin);
            levelEndLayoutTable.setFillParent(true);
            levelEndLayoutTable.defaults().pad(42.f);
            levelEndLayoutTable.add().expand();
            levelEndLayoutTable.add().expand();
            levelEndLayoutTable.add().expand();
            levelEndLayoutTable.add().expand().row();

            final Table levelEndTable = new Table(_skin);

            final Label levelEndScoreLabel = new Label("Attempts", _skin, "default-font", Color.WHITE);
            final Label levelEndBestScoreLabel = new Label("Least attempts", _skin, "default-font", Color.WHITE);
            _levelEndScoreValueLabel = new Label("0", _skin, "default-font", Color.WHITE);
            _levelEndBestScoreValueLabel = new Label("0", _skin, "default-font", Color.WHITE);

            final TextButton levelEndBackButton = new TextButton("Menu", _skin);
            _levelEndNextLevelButton = new TextButton("Next level", _skin);
            _levelEndNextLevelButton.getLabel().getStyle().font = _skin.getFont("default24-font");

            levelEndBackButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    levelEndBackButtonClicked(UI.this, (Button) actor);
                }
            });

            _levelEndNextLevelButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    levelEndNextLevelButtonClicked(UI.this, (Button) actor);
                }
            });

            levelEndTable.defaults().pad(30.f);
            levelEndTable.setBackground(_skin.getDrawable("default-rect"));
            levelEndTable.add(levelEndScoreLabel).expand().align(Align.right);
            levelEndTable.add(_levelEndScoreValueLabel).expand().align(Align.left).row();
            levelEndTable.add(levelEndBestScoreLabel).expand().align(Align.right);
            levelEndTable.add(_levelEndBestScoreValueLabel).expand().align(Align.left).row();
            levelEndTable.add(levelEndBackButton).minWidth(220.f);
            levelEndTable.add(_levelEndNextLevelButton).minWidth(220f);

            levelEndLayoutTable.add().expand();
            levelEndLayoutTable.add(levelEndTable).colspan(2).expand();
            levelEndLayoutTable.add().expand();
            levelEndLayoutTable.pack();

            _levelEndActors.add(levelEndLayoutTable);

            _parent.setFillParent(true);
            _parent.pad(42.f);
            _parent.add(backButton).size(64.f).expand().align(Align.topLeft);
            _parent.add(_playButton).size(160.f).expand().align(Align.topRight).row();
            _parent.add(_stopButton).size(80.f).expand().align(Align.bottomRight).colspan(2);
            _parent.pack();

            stage.addActor(_labelTable);
            stage.addActor(_parent);
            stage.addActor(levelEndLayoutTable);

            _editingActors.add(backButton);
            _editingActors.add(_playButton);
            _editingActors.add(_labelTable);

            _playingActors.add(_stopButton);
        }

        public final void setPlayingMode() {
            for (final Actor actor : _editingActors) {
                actor.setTouchable(Touchable.disabled);
                actor.setVisible(false);
            }

            for (final Actor actor : _playingActors) {
                actor.setTouchable(Touchable.enabled);
                actor.setVisible(true);
            }
        }

        public final void setEditingMode() {
            for (final Actor actor : _playingActors) {
                actor.setTouchable(Touchable.disabled);
                actor.setVisible(false);
            }

            for (final Actor actor : _editingActors) {
                actor.setTouchable(Touchable.enabled);
                actor.setVisible(true);
            }
        }

        public final void limit(final float limit, final float quota) {
            _remainingValueLabel.setText(String.format("%d / %d", Math.round(limit),  Math.round(quota)));
        }

        public final void attemptCount(final int value) {
            _tryCountValueLabel.setText(String.format("%d", value));
            _levelEndScoreValueLabel.setText(String.format("%d", value));
        }

        public final void leastAttemptCount(final int value) {
            _levelEndBestScoreValueLabel.setText(String.format("%d", value));
        }

        public final void showLevelEnd(final boolean visible, final boolean hasNextLevel) {
            if (visible) {
                for (final Actor actor : _playingActors) {
                    actor.setTouchable(Touchable.disabled);
                    actor.setVisible(false);
                }

                for (final Actor actor : _editingActors) {
                    actor.setTouchable(Touchable.disabled);
                    actor.setVisible(false);
                }

                if (!hasNextLevel) {
                    _levelEndNextLevelButton.setTouchable(Touchable.disabled);
                    _levelEndNextLevelButton.setVisible(false);
                }
            }

            for (final Actor actor : _levelEndActors) {
                actor.setTouchable(visible ? Touchable.enabled : Touchable.disabled);
                actor.setVisible(visible);
            }
        }
    }

    private void backButtonClicked(final UI ui, final Button button) {
        _masterScreen.pop();
    }

    private void playButtonClicked(final UI ui, final Button button) {
        ui.setPlayingMode();
        _activeLevel.spawnSleigh();
    }

    private void stopButtonClicked(final UI ui, final Button button) {
        ui.setEditingMode();
        _activeLevel.destroySleigh();
    }

    private void levelEndBackButtonClicked(final UI ui, final Button button) {
        _masterScreen.push(new MainMenuScreen(_masterScreen));
    }

    private void levelEndNextLevelButtonClicked(final UI ui, final Button button) {
        ++_startingLevel;
        changeLevel(LevelSet.instance().levels().get(_startingLevel));
    }

    private Stage _levelStage;
    private Stage _uiStage;
    private UI _ui;

    private PhysicsWorld _physicsWorld;

    private ComponentFactory _componentFactory;

    private Level _activeLevel;

    private int _startingLevel;

    private int _attemptCount;

    public GameScreen(final MasterScreen masterScreen) {
        _attemptCount = 0;

        _levelStage = new Stage();
        _uiStage = new Stage();
        _ui = new UI(_uiStage, masterScreen);
        _ui.showLevelEnd(false, true);

        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();
    }

    public final GameScreen startingLevel(final int index) {
        _startingLevel = index;

        return this;
    }

    @Override
    public void start() {
        Gdx.input.setInputProcessor(new InputMultiplexer(_uiStage, _levelStage));

        changeLevel(LevelSet.instance().levels().get(_startingLevel));
    }

    @Override
    public void stop() {
        _componentFactory.destroyComponent(_activeLevel);
        _activeLevel = null;
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

        if (_masterScreen.configuration().debug())
            _physicsWorld.render(_levelStage.getCamera());
    }

    @Override
    public void dispose() {
    }

    private void changeLevel(final LevelInfo levelInfo) {
        if (_activeLevel != null) {
            _componentFactory.destroyComponent(_activeLevel);
        }

        _attemptCount = 0;
        _ui.attemptCount(_attemptCount);

        _activeLevel = new Level().setListener(new Level.Listener() {
            @Override
            public void stateChanged(final String state) {
                if (state.equals("playing")) {
                    ++_attemptCount;
                    _ui.attemptCount(_attemptCount);

                    _ui.setPlayingMode();
                } else if (state.equals("editing")) {
                    _ui.setEditingMode();
                } else if (state.equals("won")) {
                    final LevelInfo levelInfo = LevelSet.instance().levels().get(_startingLevel);

                    levelInfo.bestScore = levelInfo.bestScore > 0 ?
                        Math.min(levelInfo.bestScore, _attemptCount)
                        : _attemptCount;

                    LevelSet.instance().updateLevel(levelInfo);

                    final LevelInfo nextLevelInfo = nextLevelInfo();

                    if (nextLevelInfo != null && !nextLevelInfo.unlocked) {
                        nextLevelInfo.unlocked = true;

                        LevelSet.instance().updateLevel(nextLevelInfo);
                    }

                    if (nextLevelInfo != null)
                        LevelSet.instance().updateCurrentLevel(_startingLevel + 1);

                    _ui.leastAttemptCount((int) levelInfo.bestScore);
                    _ui.showLevelEnd(true, nextLevelInfo != null);
                }
            }

            @Override
            public void limitChanged(float limit, float quota) {
                _ui.limit(limit, quota);
            }
        });

        _activeLevel = _componentFactory.initializeLevel(_activeLevel, levelInfo.filename);

        _ui.setEditingMode();
        _ui.showLevelEnd(false, true);
    }

    private LevelInfo nextLevelInfo() {
        if (_startingLevel >= LevelSet.instance().levels().size() - 1)
            return null;

        return LevelSet.instance().levels().get(_startingLevel + 1);
    }
}
