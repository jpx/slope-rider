package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 08/11/15.
 */
public class Track extends Component {
    private static final int PHYSICS_SPLINE_SAMPLE_COUNT = 101;
    private static final int GRAPHICS_SPLINE_SAMPLE_COUNT = 101;

    private Texture _trackGroundTexture;

    private Mesh _mesh;

    private ModelBatch _modelBatch;
    private ModelInstance _modelInstance;
    private Environment _environment;

    private ComponentFactory _componentFactory;

    private List<TrackPoint> _trackPoints;
    private FloatArray _trackPointValues;

    private Body _body;
    private List<Fixture> _fixtures;

    private boolean _physicsTrackUpdateNeeded;
    private boolean _graphicsTrackUpdateNeeded;

    public Track() {
    }

    private void initializeTrackPoints(int pointCount) {
        _trackPoints = new ArrayList<TrackPoint>(pointCount);
        _trackPointValues = new FloatArray(pointCount);

        for (int i = 0; i < pointCount; ++i)
            _trackPointValues.add(0.f);

        final Vector2 parentPosition = new Vector2(getX(), getY());
        final Vector2 size = new Vector2(getWidth(), getHeight());

        for (int i = 0; i < pointCount; ++i) {
            final Vector2 position = parentPosition.cpy().add(i * size.x / (pointCount - 1), getHeight());

            TrackPoint trackPoint = _componentFactory.createComponent(
                position,
                TrackPoint.class
            ).setChangedHandler(new TrackPoint.ChangedHandler() {
                @Override
                public void changed(TrackPoint self, float value) {
                    Gdx.app.log(SlopeRider.TAG, "track value changed ! " + value);

                    updateTrackPoint(_trackPoints.indexOf(self), value);
                }
            }).setInitialTrackValue(position.y);

            _trackPoints.add(trackPoint);
        }
    }

    private void updateTrackPoint(int index, float value) {
        _trackPointValues.set(index, value - getHeight());

        _physicsTrackUpdateNeeded = true;
        _graphicsTrackUpdateNeeded = true;
    }

    @Override
    protected void setParent(Group parent) {
        super.setParent(parent);

        setSize(40.f, 5.f);
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/track_ground.png", Texture.class);
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _trackGroundTexture = assetManager.get("texture/track_ground.png", Texture.class);
        _trackGroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _physicsTrackUpdateNeeded = false;
        _graphicsTrackUpdateNeeded = false;

        _componentFactory = componentFactory;

        initializeTrackPoints(11);

        _mesh = createMesh();

        ModelBuilder builder = new ModelBuilder();

        builder.begin();

        Material material = new Material(TextureAttribute.createDiffuse(_trackGroundTexture));

        builder.part("track", _mesh, Gdx.gl.GL_TRIANGLES, 0, _mesh.getNumIndices(), material);

        Model model = builder.end();

        _modelInstance = new ModelInstance(model);
        _modelInstance.transform
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(getX(), getY(), 0.f);

        _modelBatch = new ModelBatch();

        _environment = new Environment();
        _environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        _environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

    }

    @Override
    protected void doAct(float delta) {
        if (_graphicsTrackUpdateNeeded) {
            _graphicsTrackUpdateNeeded = false;

            resetMesh(_mesh);
        }
    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _modelBatch.begin(getStage().getCamera());
        _modelInstance.transform.cpy().mul(batch.getTransformMatrix());
        _modelBatch.render(_modelInstance);
        _modelBatch.end();

        batch.begin();
    }

    private void addEdgeFixture(World world, FloatArray vertices, short index0, short index1, Vector2 position) {
        EdgeShape shape = new EdgeShape();

        Vector2 vertex0 = new Vector2(
                vertices.get(index0 * 2 + 0),
                vertices.get(index0 * 2 + 1)
        );
        Vector2 vertex1 = new Vector2(
                vertices.get(index1 * 2 + 0),
                vertices.get(index1 * 2 + 1)
        );

        shape.set(vertex0, vertex1);

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 1.f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.f;

        Fixture fixture = _body.createFixture(fixtureDef);
        _fixtures.add(fixture);
    }

    @Override
    public void initializeBody(World world) {
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(getX(), getY());

        _body = world.createBody(bodyDef);
        _fixtures = new ArrayList<Fixture>();

        resetFixtures(world);
    }

    private void resetFixtures(World world) {
        for (Fixture fixture : _fixtures)
            _body.destroyFixture(fixture);
        _fixtures.clear();

        FloatArray vertices = new FloatArray();
        ShortArray indices = new ShortArray();

        createPolygon(_trackPointValues, getWidth(), getHeight(), PHYSICS_SPLINE_SAMPLE_COUNT, true, vertices, indices);

        for (int i = 0; i < vertices.size / 2; ++i) {
            short index0 = i == 0 ? (short) (vertices.size / 2 - 1) : (short) i;
            short index1 = i == vertices.size / 2 - 1 ? (short) 0 : (short) (i + 1);

            addEdgeFixture(world, vertices, index0, index1, new Vector2(getX(), getY()));
        }
    }

    @Override
    public void updateBody(World world) {
        if (_physicsTrackUpdateNeeded) {
            _physicsTrackUpdateNeeded = false;

            resetFixtures(world);
        }
    }

    private Mesh createMesh() {
        Texture texture = new Texture("texture/track_ground.png");
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();

        createPolygon(_trackPointValues, getWidth(), getHeight(), GRAPHICS_SPLINE_SAMPLE_COUNT, false, vertices, indices);

        final Mesh mesh = new Mesh(true, vertices.size / 2, indices.size,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
        );

        resetMesh(mesh);

        return mesh;
    }

    private void resetMesh(Mesh mesh) {
        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();

        createPolygon(_trackPointValues, getWidth(), getHeight(), GRAPHICS_SPLINE_SAMPLE_COUNT, false, vertices, indices);

        final int vertexCount = vertices.size / 2;
        final int indexCount = indices.size;

        final int vertexSize = 5;

        FloatArray meshVertices = new FloatArray(vertexCount * vertexSize);

        for (int i = 0; i < vertexCount; ++i) {
            final Vector3 position = new Vector3(
                vertices.get(i * 2 + 0),
                vertices.get(i * 2 + 1),
                0.f
            );

            meshVertices.add(position.x);
            meshVertices.add(position.y);
            meshVertices.add(position.z);

            final Vector2 uv = new Vector2(
                position.x / Math.max(getWidth(), getHeight()),
                position.y / Math.max(getWidth(), getHeight())
            );

            meshVertices.add(uv.x);
            meshVertices.add(uv.y);
        }

        mesh.setIndices(indices.toArray());
        mesh.setVertices(meshVertices.toArray());
    }

    private void createPolygon(FloatArray points, float width, float height, int sampleCount, boolean clockwise, FloatArray vertices, ShortArray indices) {
        final int pointCount = points.size;
        final int vertexCount = sampleCount + 2;
        final int splinePointCount = pointCount + 2;

        Vector2[] splinePoints = new Vector2[splinePointCount];

        vertices.ensureCapacity(vertexCount * 2);

        for (int i = 0; i < pointCount; ++i)
        {
            splinePoints[i + 1] = new Vector2(i * (width / (float) (pointCount - 1)), points.get(i) + height);
        }

        splinePoints[0] = new Vector2(0.f, points.get(0) + height);
        splinePoints[splinePoints.length - 1] = new Vector2(width, points.get(pointCount - 1) + height);

        CatmullRomSpline<Vector2> spline = new CatmullRomSpline<Vector2>(splinePoints, false);

        for (int i = 0; i < sampleCount; ++i)
        {
            Vector2 value = new Vector2();

            spline.valueAt(value, i / (float) (sampleCount - 1));

            vertices.insert(i * 2 + 0, i / (float) (sampleCount - 1) * width);
            vertices.insert(i * 2 + 1, value.y);
        }

        vertices.insert(sampleCount * 2 + 0, width);
        vertices.insert(sampleCount * 2 + 1, 0.f);
        vertices.insert(sampleCount * 2 + 2, 0.f);
        vertices.insert(sampleCount * 2 + 3, 0.f);

        if (!clockwise) {
            for (int i = 0; i < vertices.size / 4; ++i) {
                float x = vertices.get(vertices.size - 1 - i * 2 - 1);
                float y = vertices.get(vertices.size - 1 - i * 2 - 0);

                vertices.set(vertices.size - 1 - i * 2 - 1, vertices.get(i * 2 + 0));
                vertices.set(vertices.size - 1 - i * 2 - 0, vertices.get(i * 2 + 1));

                vertices.set(i * 2 + 0, x);
                vertices.set(i * 2 + 1, y);
            }
        }

        final boolean useDelaunay = false;

        if (useDelaunay) {
            // FIXME
            DelaunayTriangulator triangulator = new DelaunayTriangulator();

            indices.addAll(triangulator.computeTriangles(vertices, false));
        } else {
            EarClippingTriangulator triangulator = new EarClippingTriangulator();

            indices.addAll(triangulator.computeTriangles(vertices));
        }

        if (!clockwise) {
            indices.reverse();
        }
    }
}
