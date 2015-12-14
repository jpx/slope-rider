package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.sloperider.ComponentFactory;
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

            _parent.setFillParent(true);
            _parent.pad(42.f);
            _parent.add(backButton).size(64.f).expand().align(Align.topLeft);
            _parent.add(_playButton).size(160.f).expand().align(Align.topRight).row();
            _parent.add(_stopButton).size(160.f).expand().align(Align.bottomRight).colspan(2);
            _parent.pack();

            stage.addActor(_parent);

            _editingActors.add(backButton);
            _editingActors.add(_playButton);

            _playingActors.add(_stopButton);

            setEditingMode();
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

    private Stage _levelStage;
    private Stage _uiStage;
    private UI _ui;

    private PhysicsWorld _physicsWorld;

    private ComponentFactory _componentFactory;

    private Level _activeLevel;

    public GameScreen(final MasterScreen masterScreen) {
        _levelStage = new Stage();
        _uiStage = new Stage();
        _ui = new UI(_uiStage, masterScreen);

        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();
    }

    @Override
    public void start() {
        Gdx.input.setInputProcessor(new InputMultiplexer(_uiStage, _levelStage));

        _activeLevel = _componentFactory.createLevel("level/level0.lvl");

        _activeLevel.setListener(new Level.Listener() {
            @Override
            public void stageChanged(final String state) {
                if (state.equals("playing")) {
                    _ui.setPlayingMode();
                } else if (state.equals("editing")) {
                    _ui.setEditingMode();
                }
            }
        });
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

        _physicsWorld.render(_levelStage.getCamera());
    }

    @Override
    public void dispose() {
    }
}
