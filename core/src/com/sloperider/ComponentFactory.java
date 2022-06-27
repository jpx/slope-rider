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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by jpx on 12/11/15.
 */
public class ComponentFactory {
    public interface Listener {
        void componentCreated(final Component component);

        void componentDestroyed(final Component component);
    }

    private boolean _ready;
    private boolean _runningPostPhase;

    private Stage _stage;

    private AssetManager _assetManager;
    private PhysicsWorld _physicsWorld;

    private List<Map.Entry<Component, Layer>> _components;
    private List<Map.Entry<Component, Layer>> _componentsToAdd;

    private final Map<Layer, Group> _roots = new HashMap<Layer, Group>();

    private final List<Listener> _listeners = new ArrayList<Listener>();

    public ComponentFactory(final Stage stage, final AssetManager assetManager, final PhysicsWorld physicsWorld) {
        _ready = false;
        _runningPostPhase = false;

        _stage = stage;
        _assetManager = assetManager;
        _physicsWorld = physicsWorld;

        _components = new ArrayList();
        _componentsToAdd = new ArrayList();

        for (final Layer layer : Layer.values()){
            final Group root = new Group();
            _stage.addActor(root);
            _roots.put(layer, root);
        }
    }

    public final ComponentFactory registerListener(final Listener listener) {
        _listeners.add(listener);

        return this;
    }

    public final ComponentFactory unregisterListener(final Listener listener) {
        _listeners.remove(listener);

        return this;
    }

    public final void requireAssets() {
        for (final Map.Entry<Component, Layer> component : _components) {
            component.getKey().requireAssets(_assetManager);
        }
    }

    public final void ready() {
        if (_ready)
            return;

        _ready = true;
        _runningPostPhase = true;

        for (final Map.Entry<Component, Layer> component : _components) {
            component.getKey().manageAssets(_assetManager);
        }

        for (final Map.Entry<Component, Layer> component : _components) {
            component.getKey().ready(this);
        }

        for (final Map.Entry<Component, Layer> component : _components) {
            _physicsWorld.addActor(component.getKey());
        }

        _runningPostPhase = false;

        for (final Map.Entry<Component, Layer> component : _componentsToAdd) {
            _components.add(component);
        }

        _componentsToAdd.clear();
    }

    public final Level createLevel(final String filename) {
        final Level level = new Level();

        return initializeLevel(level, filename);
    }

    public final Level initializeLevel(final Level level, final String filename) {
        final JsonReader reader = new JsonReader();

        final JsonValue root = reader.parse(Gdx.files.internal(filename));

        level.initialize(this, root);

        return initializeComponent(level);
    }

    public final <T extends Component> T createComponent(final Layer layer, final Vector2 position, final Class<T> type) {
        T component = null;
        try {
            component = type.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        component.setPosition(position.x, position.y);

        initializeComponent(layer, component);

        return component;
    }

    public final <T extends Component> T createComponent(final Vector2 position, final Class<T> type) {
        return createComponent(Layer.FOREGROUND, position, type);
    }

    public final <T extends Component> T initializeComponent(final T component) {
        return initializeComponent(Layer.FOREGROUND, component);
    }

    public final <T extends Component> T initializeComponent(final Layer layer, final T component) {
        component.setLayer(layer);

        if (!_runningPostPhase)
            _components.add(new AbstractMap.SimpleEntry(component, layer));
        else
            _componentsToAdd.add(new AbstractMap.SimpleEntry(component, layer));

        _roots.get(layer).addActor(component);

        if (_ready) {
            component.requireAssets(_assetManager);

            _assetManager.finishLoading();

            component.manageAssets(_assetManager);
            component.ready(this);

            _physicsWorld.addActor(component);
        }

        for (final Listener listener : _listeners)
            listener.componentCreated(component);

        return component;
    }

    public final <T extends Component> T destroyComponent(final T component) {
        final Map.Entry<Component, Layer> entry = _components.stream().filter(c -> c.getKey() == component).findFirst().orElse(null);
        final Layer layer = entry.getValue();
        _components.remove(entry);

        for (final Listener listener : _listeners)
            listener.componentDestroyed(component);

        component.releaseAssets(_assetManager);

        component.destroy(this);

        _physicsWorld.removeActor(component);
        _roots.get(layer).removeActor(component);

        return component;
    }
}
