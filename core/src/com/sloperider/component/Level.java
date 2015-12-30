package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.sloperider.ComponentFactory;
import com.sloperider.Layer;
import com.sloperider.LevelSet;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jpx on 30/11/15.
 */
public class Level extends Component {
    public interface Listener {
        void stateChanged(final String state);

        void limitChanged(final float limit, final float quota);
    }

    private static class TrackBoundComponent {
        Component component;
        String trackName;
        float location;
        float offset;
        Track track;
    }

    private boolean _startedAsViewOnly = false;

    private ComponentFactory _componentFactory;
    private String _name;
    private String _description;

    private Track _mainTrack;
    private Begin _begin;
    private End _end;

    private final Map<Track, List<TrackBoundComponent>> _trackBoundComponents = new HashMap<Track, List<TrackBoundComponent>>();

    private TrackCameraController _editingCameraController;
    private SleighCameraController _playingCameraController;
    private final Vector2 _targetEditingCameraPosition = new Vector2();
    private float _targetEditingCameraZoom;

    private Sleigh _sleigh;
    private boolean _sleighIsMoving = false;

    private float _elapsedTimeSinceSleighMoveStoped;

    private boolean _over;

    private Listener _listener;

    public final Level setListener(final Listener listener) {
        _listener = listener;

        return this;
    }

    public final void initialize(final ComponentFactory componentFactory, final JsonValue root) {
        _name = root.getString("name");
        _description = root.getString("description");

        LevelSet.instance().updateDescription(_name, _description);

        final List<TrackBoundComponent> trackBoundComponents = new ArrayList<TrackBoundComponent>();

        for (final JsonValue componentNode : root.get("components")) {
            Component component = null;
            TrackBoundComponent trackBoundComponent = null;
            Vector2 position = null;

            final Vector2 scale = new Vector2(1.f, 1.f);

            if (componentNode.has("scale")) {
                final JsonValue scaleNode = componentNode.get("scale");

                scale.set(
                    scaleNode.get(0).asFloat(),
                    scaleNode.get(1).asFloat()
                );
            }

            final String type = componentNode.getString("type");

            final JsonValue positionNode = componentNode.get("position");

            if (positionNode.isArray()) {
                position = new Vector2(
                    positionNode.get(0).asFloat(),
                    positionNode.get(1).asFloat()
                );
            } else {
                position = Vector2.Zero;

                trackBoundComponent = new TrackBoundComponent();

                trackBoundComponent.trackName = positionNode.getString("track");
                trackBoundComponent.location = positionNode.getFloat("location");
                trackBoundComponent.offset = positionNode.has("offset")
                    ? positionNode.getFloat("offset")
                    : 0.f;

                trackBoundComponents.add(trackBoundComponent);
            }

            if (type.equals("Begin")) {
                _begin = addComponent(componentFactory.createComponent(position, Begin.class));
                component = _begin;
            } else if (type.equals("End")) {
                final End end = new End();
                end.setPosition(position.x, position.y);
                end.setSize(scale.x, scale.y);

                _end = addComponent(componentFactory.initializeComponent(end));
                component = _end;
            } else if (type.equals("FallingSign")) {
                component = addComponent(componentFactory.createComponent(Layer.BACKGROUND2, position, FallingSign.class));
            } else if (type.equals("ObjectSpawner")) {
                final ObjectSpawner objectSpawner = new ObjectSpawner();
                component = objectSpawner;

                objectSpawner.setPosition(position.x, position.y);

                final String spawnedObjectTypeName = componentNode.getString("spawnedObjectType");
                Class<? extends Component> spawnedObjectType = null;

                if (spawnedObjectTypeName.equals("RollingObject"))
                    spawnedObjectType = RollingObject.class;

                objectSpawner.setParameters(
                    spawnedObjectType,
                    componentNode.getFloat("cardinality"),
                    componentNode.getFloat("delay")
                );

                addComponent(componentFactory.initializeComponent(Layer.BACKGROUND2, objectSpawner));
            } else if (type.equals("DraggableNetwork")) {
                final DraggableNetwork draggableNetwork = new DraggableNetwork()
                    .quota(componentNode.getFloat("quota"))
                    .listener(new DraggableNetwork.Listener() {
                        @Override
                        public void valueChanged(DraggableNetwork self, float value, float quota) {
                            if (_listener != null)
                                _listener.limitChanged(value, quota);
                        }
                    });
                component = draggableNetwork;

                addComponent(componentFactory.initializeComponent(draggableNetwork));
            } else if (type.equals("Track")) {
                final Track track = new Track();
                component = track;

                if (_mainTrack == null) {
                    _mainTrack = track;
                }

                track.setPosition(position.x, position.y);
                track.setBaseSize(componentNode.getFloat("width"), componentNode.getFloat("height"));

                final List<Track.PointData> points = new ArrayList<Track.PointData>();

                for (final JsonValue pointNode : componentNode.get("points")) {
                    final Track.PointData point = new Track.PointData(
                        pointNode.getFloat("x"),
                        pointNode.getFloat("y"),
                        pointNode.getBoolean("editable"),
                        Track.GroundMaterialType.valueOf(pointNode.getString("material")),
                        pointNode.getFloat("minBound"),
                        pointNode.getFloat("maxBound")
                    );

                    points.add(point);
                }

                track.setPoints(points);

                addComponent(componentFactory.initializeComponent(track));
            }

            if (trackBoundComponent != null) {
                trackBoundComponent.component = component;
            }
        }

        final Track track = _mainTrack;
        _trackBoundComponents.put(track, new ArrayList<TrackBoundComponent>());

        for (final TrackBoundComponent trackBoundComponent : trackBoundComponents) {
            trackBoundComponent.track = track;
            _trackBoundComponents.get(track).add(trackBoundComponent);

            updateTrackBoundComponent(trackBoundComponent);
        }
    }

    private void updateTrackBoundComponent(final TrackBoundComponent trackBoundComponent) {
        trackBoundComponent.component.setPosition(
            trackBoundComponent.track.getX() + trackBoundComponent.track.getWidth() * trackBoundComponent.location,
            trackBoundComponent.track.heightAt(trackBoundComponent.location) + trackBoundComponent.offset
        );
    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    private void mainTrackChanged(final Track track) {
        for (final Map.Entry<Track, List<TrackBoundComponent>> entry : _trackBoundComponents.entrySet())
            for (final TrackBoundComponent trackBoundComponent : entry.getValue())
                updateTrackBoundComponent(trackBoundComponent);

        final Rectangle bounds = new Rectangle();
        computeBounds(bounds);

        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void doReady(final ComponentFactory componentFactory) {
        Gdx.app.log(SlopeRider.TAG, toString());

        _over = false;

        _componentFactory = componentFactory;

        setTouchable(Touchable.childrenOnly);

        componentFactory.createComponent(Layer.BACKGROUND0, Vector2.Zero, Background.class);

        _mainTrack.addListener(new Track.Listener() {
            @Override
            public void changed(Track self) {
                mainTrackChanged(self);
            }
        });

        mainTrackChanged(_mainTrack);

        _targetEditingCameraPosition.set(_begin.getX() * SlopeRider.PIXEL_PER_UNIT, _begin.getY() * SlopeRider.PIXEL_PER_UNIT);
        _targetEditingCameraZoom = 3.f;

        editingBegin();
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
        clear();
    }

    @Override
    public String toString() {
        return String.format("%s: %s", _name, _description);
    }

    @Override
    protected void doAct(float delta) {
        if (_sleigh != null) {
            final boolean sleighIsMoving = _sleigh.isMoving();

            if (_sleighIsMoving != sleighIsMoving) {
                _sleighIsMoving = sleighIsMoving;

                if (!sleighIsMoving) {
                    _elapsedTimeSinceSleighMoveStoped = 0.f;
                }
            }

            boolean won = false;
            boolean lost = false;

            if (_end != null && _end.hasSleigh(_sleigh)) {
                won = true;
            } else {
                if (sleighOutOfBounds()) {
                    lost = true;
                } else if (!sleighIsMoving) {
                    _elapsedTimeSinceSleighMoveStoped += delta;

                    if (_elapsedTimeSinceSleighMoveStoped > 1.f) {
                        lost = true;
                    }
                }
            }

            if (won) {
                _over = true;

                destroySleigh(true);

                if (_listener != null)
                    _listener.stateChanged("won");

            } else if (lost) {
                destroySleigh();

                if (_listener != null)
                    _listener.stateChanged("lost");
            }
        }
    }

    @Override
    protected void doDraw(Batch batch) {

    }

    @Override
    public void initializeBody(World world) {
    }

    @Override
    public void updateBody(World world, float deltaTime) {
    }

    @Override
    public void destroyBody(World world) {

    }

    private Level computeBounds(final Rectangle bounds) {
        bounds.set(_mainTrack.getX(), _mainTrack.getY(), _mainTrack.getWidth(), 250.f);

        return this;
    }

    private boolean sleighOutOfBounds() {
        return _sleigh != null && _sleigh.getX() < getX()
            || _sleigh.getY() < getY()
            || _sleigh.getRight() > getRight()
            || _sleigh.getTop() > getTop();
    }

    public final Level spawnSleigh() {
        if (_sleigh != null)
            destroySleigh();

        editingEnd();

        final Vector2 position = new Vector2(_begin.getX(), _begin.getY()).add(0.f, 1.f);

        _sleigh = _componentFactory.createComponent(position, Sleigh.class);

        playingBegin(_sleigh);

        return this;
    }

    public final Level destroySleigh() {
        return destroySleigh(false);
    }

    public final Level destroySleigh(final boolean end) {
        playingEnd();

        if (!end)
            editingBegin();

        if (end) {
            for (final Component component : _components) {
                component.levelComplete(this);
            }
        }

        if (_sleigh == null)
            return this;

        if (_end != null)
            _end.sleighDestroyed(_sleigh);
        _componentFactory.destroyComponent(_sleigh);
        _sleigh = null;

        return this;
    }

    public final Level startAsViewOnly() {
        if (_startedAsViewOnly)
            return this;

        _startedAsViewOnly = true;

        if (_editingCameraController != null) {
            _componentFactory.destroyComponent(_editingCameraController);
            _editingCameraController = null;
        }

        if (_mainTrack != null)
            _mainTrack.editable(false);

        getStage().getCamera().position.set(
            new Vector2(35.f, 20.f).scl(SlopeRider.PIXEL_PER_UNIT),
            0.f
        );
        ((OrthographicCamera) getStage().getCamera()).zoom = 3.f;

        return this;
    }

    private void playingBegin(final Sleigh sleigh) {
        if (_startedAsViewOnly)
            return;

        if (_listener != null)
            _listener.stateChanged("playing");

        for (final Component component : _components) {
            component.levelPlayed(this);
        }

        _playingCameraController = addComponent(_componentFactory.createComponent(Vector2.Zero, SleighCameraController.class))
            .target(sleigh);
        _playingCameraController.setBounds(getX(), getY(), getWidth(), getHeight());
    }

    private void playingEnd() {
        if (_startedAsViewOnly)
            return;

        for (final Component component : _components) {
            component.levelStopped(this);
        }

        if (_playingCameraController != null) {
            removeComponent(_componentFactory.destroyComponent(_playingCameraController));
            _playingCameraController = null;
        }
    }

    private void editingBegin() {
        if (_startedAsViewOnly)
            return;

        if (_listener != null)
            _listener.stateChanged("editing");

        if (_mainTrack != null)
            _mainTrack.editable(true);

        _editingCameraController = _componentFactory.createComponent(Vector2.Zero, TrackCameraController.class)
            .setTrack(_mainTrack)
            .moveTo(_targetEditingCameraPosition, _targetEditingCameraZoom);
    }

    private void editingEnd() {
        if (_startedAsViewOnly)
            return;

        if (_editingCameraController != null) {
            final OrthographicCamera camera = getCamera();

            _targetEditingCameraPosition.set(camera.position.x, camera.position.y);
            _targetEditingCameraZoom = camera.zoom;

            _componentFactory.destroyComponent(_editingCameraController);
            _editingCameraController = null;
        }

        if (_mainTrack != null)
            _mainTrack.editable(false);
    }
}
