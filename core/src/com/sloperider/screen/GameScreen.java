package com.sloperider.screen;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.sloperider.ComponentFactory;
import com.sloperider.physics.PhysicsWorld;

/**
 * Created by jpx on 03/12/15.
 */
public class GameScreen extends Screen {
    private Stage _levelStage;
    private Stage _uiStage;

    private PhysicsWorld _physicsWorld;

    private ComponentFactory _componentFactory;

    public GameScreen(final MasterScreen masterScreen) {
        _levelStage = new Stage();
        _uiStage = new Stage();

        _physicsWorld = new PhysicsWorld();

        _componentFactory = new ComponentFactory(_levelStage, masterScreen._assetManager, _physicsWorld);
        _componentFactory.ready();
    }

    @Override
    public void start() {
        _componentFactory.createLevel("level/level0.lvl");
    }

    @Override
    public void stop() {

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
