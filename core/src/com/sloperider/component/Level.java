package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 30/11/15.
 */
public class Level extends Component {
    private String _name;
    private String _description;

    private Track _mainTrack;

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

            }

            if (type.equals("Track")) {
                final Track track = addComponent(componentFactory.createComponent(position, Track.class));

                if (_mainTrack == null) {
                    _mainTrack = track;
                }

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

    @Override
    protected void doReady(final ComponentFactory componentFactory) {
        Gdx.app.log(SlopeRider.TAG, toString());

        setPosition(0.f, 0.f);
        setSize(1000.f, 1000.f);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                final Vector2 position = new Vector2(x, y).scl(1.f / SlopeRider.PIXEL_PER_UNIT);

                final Sleigh sleigh = addComponent(componentFactory.createComponent(position, Sleigh.class));

                return super.touchDown(event, x, y, pointer, button);
            }
        });

        final TrackCameraController cameraController = componentFactory.createComponent(
            new Vector2(),
            TrackCameraController.class
        ).setTrack(_mainTrack);

        Gdx.input.setInputProcessor(new InputMultiplexer(getStage(), new GestureDetector(cameraController), cameraController));
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
    }

    @Override
    public String toString() {
        return String.format("%s: %s", _name, _description);
    }

    @Override
    protected void doAct(float delta) {

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
}
