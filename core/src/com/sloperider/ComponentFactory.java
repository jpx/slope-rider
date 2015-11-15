package com.sloperider;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.sloperider.component.Component;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 12/11/15.
 */
public class ComponentFactory {
    private boolean _ready;

    private Stage _stage;

    private AssetManager _assetManager;
    private PhysicsWorld _physicsWorld;

    private List<Component> _components;

    ComponentFactory(Stage stage, AssetManager assetManager, PhysicsWorld physicsWorld) {
        _ready = false;

        _stage = stage;
        _assetManager = assetManager;
        _physicsWorld = physicsWorld;

        _components = new ArrayList<Component>();
    }

    void requireAssets() {
        for (Component component : _components) {
            component.requireAssets(_assetManager);
        }
    }

    void ready() {
        if (_ready)
            return;

        _ready = true;

        for (Component component : _components) {
            component.manageAssets(_assetManager);
        }

        for (Component component : _components) {
            component.ready(this);
        }

        for (Component component : _components) {
            _physicsWorld.addActor(component);
        }
    }

    public <T extends Component> T createComponent(Vector2 position, Class<T> type) {
        T component = null;
        try {
            component = type.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

//        component.setScale(SlopeRider.PIXEL_PER_UNIT);
        component.setPosition(position.x, position.y);

        _components.add(component);

        _stage.addActor(component);

        if (_ready) {
            component.requireAssets(_assetManager);

            _assetManager.finishLoading();

            component.manageAssets(_assetManager);
            component.ready(this);

            _physicsWorld.addActor(component);
        }

        return component;
    }
}
