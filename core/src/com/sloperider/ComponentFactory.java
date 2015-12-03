package com.sloperider;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.sloperider.component.Component;
import com.sloperider.component.Level;
import com.sloperider.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jpx on 12/11/15.
 */
public class ComponentFactory {
    private boolean _ready;
    private boolean _runningPostPhase;

    private Stage _stage;

    private AssetManager _assetManager;
    private PhysicsWorld _physicsWorld;

    private List<Component> _components;
    private List<Component> _componentsToAdd;

    public ComponentFactory(final Stage stage, final AssetManager assetManager, final PhysicsWorld physicsWorld) {
        _ready = false;
        _runningPostPhase = false;

        _stage = stage;
        _assetManager = assetManager;
        _physicsWorld = physicsWorld;

        _components = new ArrayList<Component>();
        _componentsToAdd = new LinkedList<Component>();
    }

    public final void requireAssets() {
        for (Component component : _components) {
            component.requireAssets(_assetManager);
        }
    }

    public final void ready() {
        if (_ready)
            return;

        _ready = true;
        _runningPostPhase = true;

        for (Component component : _components) {
            component.manageAssets(_assetManager);
        }

        for (Component component : _components) {
            component.ready(this);
        }

        for (Component component : _components) {
            _physicsWorld.addActor(component);
        }

        _runningPostPhase = false;

        while (!_componentsToAdd.isEmpty()) {
            Component component = _componentsToAdd.get(0);
            _componentsToAdd.remove(0);

            _components.add(component);
        }
    }

    public Level createLevel(final String filename) {
        final Level level = new Level();

        final JsonReader reader = new JsonReader();

        final JsonValue root = reader.parse(Gdx.files.internal(filename));

        level.initialize(this, root);

        return initializeComponent(level);
    }

    public final <T extends Component> T createComponent(final Vector2 position, final Class<T> type) {
        T component = null;
        try {
            component = type.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        component.setPosition(position.x, position.y);

        initializeComponent(component);

        return component;
    }

    public final <T extends Component> T initializeComponent(final T component) {
        if (!_runningPostPhase)
            _components.add(component);
        else
            _componentsToAdd.add(component);

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

    public final void destroyComponent(final Component component) {
        component.releaseAssets(_assetManager);

        component.destroy(this);

        _physicsWorld.removeActor(component);
        _stage.getRoot().removeActor(component);

        _components.remove(component);
    }
}
