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
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 30/11/15.
 */
public class Level extends Component {
    public interface Listener {
        void stageChanged(final String state);
    }

    private boolean _startedAsViewOnly = false;

    private InputMultiplexer _input;

    private ComponentFactory _componentFactory;
    private String _name;
    private String _description;

    private Track _mainTrack;
    private Begin _begin;
    private End _end;

    private TrackCameraController _editingCameraController;

    private Sleigh _sleigh;
    private boolean _sleighIsMoving = false;

    private float _elapsedTimeSinceSleighMoveStoped;

    private Listener _listener;

    public final Level setListener(final Listener listener) {
        _listener = listener;

        return this;
    }

    public final void initialize(final ComponentFactory componentFactory, final JsonValue root) {
        _name = root.getString("name");
        _description = root.getString("description");

        for (final JsonValue componentNode : root.get("components")) {
            final String type = componentNode.getString("type");

            final JsonValue positionNode = componentNode.get("position");
            final Vector2 position = new Vector2(
                positionNode.get(0).asFloat(),
                positionNode.get(1).asFloat()
            );

            if (type.equals("Begin")) {
                _begin = addComponent(componentFactory.createComponent(position, Begin.class));
            } else if (type.equals("End")) {
                _end = addComponent(componentFactory.createComponent(position, End.class));
            } else if (type.equals("ObjectSpawner")) {
                final ObjectSpawner objectSpawner = new ObjectSpawner();
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
                    .quota(componentNode.getFloat("quota"));

                addComponent(componentFactory.initializeComponent(draggableNetwork));
            } else if (type.equals("Track")) {
                final Track track = new Track();

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
                        Track.GroundMaterialType.valueOf(pointNode.getString("material"))
                    );

                    points.add(point);
                }

                track.setPoints(points);

                addComponent(componentFactory.initializeComponent(track));
            }
        }
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
        if (_begin != null)
            _begin.setPosition(_begin.getX(), track.heightAt(_begin.getX() - track.getX()));

        if (_end != null)
            _end.setPosition(_end.getX(), track.heightAt(_end.getX() - track.getX()));

        final Rectangle bounds = new Rectangle();
        computeBounds(bounds);

        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void doReady(final ComponentFactory componentFactory) {
        Gdx.app.log(SlopeRider.TAG, toString());

        _input = (InputMultiplexer) Gdx.input.getInputProcessor();

        _componentFactory = componentFactory;

        setSize(1000.f, 1000.f);

        setTouchable(Touchable.childrenOnly);

        componentFactory.createComponent(Layer.BACKGROUND0, Vector2.Zero, Background.class);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                final Vector2 position = new Vector2(x, y).scl(1.f / SlopeRider.PIXEL_PER_UNIT);

                final Sleigh sleigh = addComponent(componentFactory.createComponent(position, Sleigh.class));

                return super.touchDown(event, x, y, pointer, button);
            }
        });

        _mainTrack.addListener(new Track.Listener() {
            @Override
            public void changed(Track self) {
                mainTrackChanged(self);
            }
        });

        mainTrackChanged(_mainTrack);

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
                Gdx.app.log(SlopeRider.TAG, "won");

                destroySleigh();

            } else if (lost) {
                Gdx.app.log(SlopeRider.TAG, "lost");

                destroySleigh();
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
    public void updateBody(World world) {
    }

    @Override
    public void destroyBody(World world) {

    }

    private Level computeBounds(final Rectangle bounds) {
        bounds.set(_mainTrack.getX(), _mainTrack.getY(), _mainTrack.getWidth(), 100.f);

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
        playingBegin();

        final Vector2 position = new Vector2(_begin.getX(), _begin.getY()).add(0.f, 1.f);

        _sleigh = _componentFactory.createComponent(position, Sleigh.class);

        return this;
    }

    public final Level destroySleigh() {
        playingEnd();
        editingBegin();

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

    private void playingBegin() {
        playingEnd();

        if (_startedAsViewOnly)
            return;

        if (_listener != null)
            _listener.stageChanged("playing");

        for (final Component component : _components) {
            component.levelPlayed(this);
        }
    }

    private void playingEnd() {
        if (_startedAsViewOnly)
            return;

        for (final Component component : _components) {
            component.levelStopped(this);
        }

    }

    private void editingBegin() {
        editingEnd();

        if (_startedAsViewOnly)
            return;

        if (_listener != null)
            _listener.stageChanged("editing");

        if (_mainTrack != null)
            _mainTrack.editable(true);

        _editingCameraController = _componentFactory.createComponent(Vector2.Zero, TrackCameraController.class)
            .setTrack(_mainTrack)
            .startMove();
    }

    private void editingEnd() {
        if (_startedAsViewOnly)
            return;

        if (_editingCameraController != null) {
            _componentFactory.destroyComponent(_editingCameraController);
            _editingCameraController = null;
        }

        if (_mainTrack != null)
            _mainTrack.editable(false);
    }
}
