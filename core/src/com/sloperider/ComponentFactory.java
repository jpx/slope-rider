package com.sloperider;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.sloperider.component.Component;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 12/11/15.
 */
public class ComponentFactory {
    private boolean _ready;

    private AssetManager _assetManager;
    private PhysicsWorld _physicsWorld;

    private List<Component> _components;

    ComponentFactory(AssetManager assetManager, PhysicsWorld physicsWorld) {
        _ready = false;

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
        _ready = true;

        for (Component component : _components) {
            component.manageAssets(_assetManager);
        }

        for (Component component : _components) {
            component.ready(this);
        }
    }

    public <T extends Component> T createComponent(Group parent, Vector2 position, int zIndex, Class<T> type) {
        T component = null;
        try {
            component = type.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        component.setPosition(position.x, position.y);
        component.setZIndex(zIndex);

        _components.add(component);

        parent.addActor(component);
        _physicsWorld.addActor(component);

        if (_ready) {
            component.manageAssets(_assetManager);
            component.ready(this);
        }

        return component;
    }
}
