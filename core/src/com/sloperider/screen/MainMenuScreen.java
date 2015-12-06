package com.sloperider.screen;

import com.badlogic.gdx.Gdx;
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

/**
 * Created by jpx on 03/12/15.
 */
public class MainMenuScreen extends Screen {
    private Stage _uiStage;

    private class UI {
        private Skin _skin;
        private Table _parent;

        private Table _menuTable;

        UI(final Stage stage) {
            _skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

            _parent = new Table(_skin);
            _parent.setFillParent(true);



            _menuTable = new Table(_skin);

            final TextButton playButton = new TextButton("Play", _skin);
            playButton.getLabel().setFontScale(4.f);

            playButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    playButtonClicked();
                }
            });

            final TextButton scoreButton = new TextButton("Scores", _skin);
            scoreButton.getLabel().setFontScale(4.f);

            scoreButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    scoreButtonClicked();
                }
            });

            _menuTable.defaults().space(20.f);
            _menuTable.add(playButton).prefSize(320.f, 40.f).row();
            _menuTable.add(scoreButton).prefSize(320.f, 40.f).row();
            _menuTable.pack();

            _parent.add(_menuTable);
            _parent.pack();

            stage.addActor(_parent);
        }
    }

    private void playButtonClicked() {
        _masterScreen.push(new GameScreen(_masterScreen));
    }

    private void scoreButtonClicked() {
        _masterScreen.push(new GameScreen(_masterScreen));
    }

    public MainMenuScreen() {
        _uiStage = new Stage();

        new UI(_uiStage);
    }

    @Override
    public void start() {
        Gdx.input.setInputProcessor(_uiStage);
    }

    @Override
    public void stop() {
    }

    @Override
    public void update(float deltaTime) {
        _uiStage.act(deltaTime);
    }

    @Override
    public void render() {
        _uiStage.draw();
    }

    @Override
    public void dispose() {
        _uiStage.dispose();
        _uiStage = null;
    }
}
