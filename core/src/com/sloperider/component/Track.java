package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.math.SplineCache;
import com.sloperider.physics.CollisionGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 08/11/15.
 */
public class Track extends Component {
    private static final int PHYSICS_SPLINE_SAMPLE_COUNT = 101;
    private static final int GRAPHICS_SPLINE_SAMPLE_COUNT = 101;
    private static final int CONTROL_POINT_COUNT = 6;

    private static final float TOP_LAYER_HEIGHT = 2.f;

    private static final float[] _startChunk = {
//        0.f, -1.0f, -1.5f, -2.5f
    };

    private Texture _trackGroundTexture;

    private Mesh _mesh;

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;

    private String _vertexShaderSource;
    private String _fragmentShaderSource;

    private float _baseWidth;
    private float _baseHeight;

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

        for (int i = 0; i < pointCount; ++i) {
            float value = 0.f;

            if (i < _startChunk.length)
                value = _startChunk[i];

            _trackPointValues.add(value);
        }

        final Vector2 parentPosition = new Vector2(getX(), getY());
        final Vector2 size = new Vector2(_baseWidth, _baseHeight);

        for (int i = _startChunk.length; i < pointCount; ++i) {
            final Vector2 position = parentPosition.cpy().add(i * size.x / (pointCount - 1), _baseHeight);

            TrackPoint trackPoint = _componentFactory.createComponent(
                position,
                TrackPoint.class
            ).setChangedHandler(new TrackPoint.ChangedHandler() {
                @Override
                public void changed(TrackPoint self, float value) {
                    updateTrackPoint(_trackPoints.indexOf(self), value);
                }
            }).setInitialTrackValue(position.y);

            _trackPoints.add(trackPoint);
        }
    }

    private void updateTrackPoint(int index, float value) {
        _trackPointValues.set(_startChunk.length + index, value - _baseHeight);

        _physicsTrackUpdateNeeded = true;
        _graphicsTrackUpdateNeeded = true;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/track_ground.png", Texture.class);

        _vertexShaderSource = Gdx.files.internal("shader/track.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/track.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _trackGroundTexture = assetManager.get("texture/track_ground.png", Texture.class);
        _trackGroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _baseWidth = 40.f;
        _baseHeight = 15.f;

        _physicsTrackUpdateNeeded = false;
        _graphicsTrackUpdateNeeded = false;

        _componentFactory = componentFactory;

        initializeTrackPoints(CONTROL_POINT_COUNT);

        resetMesh();

        ModelBuilder builder = new ModelBuilder();

        builder.begin();

        Material material = new Material(TextureAttribute.createDiffuse(_trackGroundTexture));

        builder.part("track", _mesh, Gdx.gl.GL_TRIANGLES, 0, _mesh.getNumIndices(), material);

        Model model = builder.end();

        _renderable = new Renderable();
        _renderable.worldTransform
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(getX(), getY(), 0.f);
        _renderable.environment = _environment;
        _renderable.meshPart.set(model.meshParts.first());
        _renderable.material = model.materials.first();

        _renderable.material
            .set(ColorAttribute.createDiffuse(Color.CYAN));

        _context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        _shader = new Shader() {
            ShaderProgram program;

            Camera camera;
            RenderContext context;

            @Override
            public void init() {
                program = new ShaderProgram(_vertexShaderSource, _fragmentShaderSource);
                if (!program.isCompiled())
                    throw new GdxRuntimeException(program.getLog());
            }

            @Override
            public int compareTo(Shader other) {
                return 0;
            }

            @Override
            public boolean canRender(Renderable instance) {
                return true;
            }

            @Override
            public void begin(Camera camera, RenderContext context) {
                this.camera = camera;
                this.context = context;

                program.begin();

                program.setUniformMatrix("u_worldToScreenMatrix", camera.combined);
            }

            @Override
            public void render(Renderable renderable) {

                final Material material = renderable.material;

                final Color diffuseColor = ((ColorAttribute)material.get(ColorAttribute.Diffuse)).color;

//                program.setUniformf("u_diffuseColor", diffuseColor.r, diffuseColor.g, diffuseColor.b, diffuseColor.a);
                program.setUniformMatrix("u_modelToWorldMatrix", renderable.worldTransform);
                renderable.meshPart.render(program);
            }

            @Override
            public void end() {
                program.end();
            }

            @Override
            public void dispose() {
                program.dispose();
            }
        };

        _shader.init();

        _environment = new Environment();
        _environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        _environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

    }

    @Override
    protected void doAct(float delta) {
        if (_graphicsTrackUpdateNeeded) {
            _graphicsTrackUpdateNeeded = false;

            resetMesh();
        }
    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _context.begin();

        _shader.begin(getStage().getCamera(), _context);

        _shader.render(_renderable);

        _context.end();

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

        fixtureDef.filter.categoryBits = CollisionGroup.TRACK.value();

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

        createPhysicsPolygon(
            _trackPointValues, _baseWidth, _baseHeight, PHYSICS_SPLINE_SAMPLE_COUNT, vertices
        );

        for (int i = 0; i < vertices.size / 2; ++i) {
            final short index0 = (short) i;
            final short index1 = i == vertices.size / 2 - 1 ? (short) 0 : (short) (i + 1);

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

    private void resetMesh() {
        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();
        final IntArray metadata = new IntArray();

        createGraphicsPolygon(
                _trackPointValues,
                _baseWidth, _baseHeight, TOP_LAYER_HEIGHT,
                GRAPHICS_SPLINE_SAMPLE_COUNT,
                vertices, indices, metadata
        );

        if (_mesh == null) {
            final int vertexCount = metadata.get(0);
            final int indexCount = metadata.get(2);

            _mesh = new Mesh(
                true,
                vertexCount,
                indexCount,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_mask")
            );
        }

        _mesh.setIndices(indices.toArray());
        _mesh.setVertices(vertices.toArray());

        BoundingBox boundingBox = new BoundingBox();
        _mesh.calculateBoundingBox(boundingBox);

        setSize(boundingBox.getWidth(), boundingBox.getHeight());
    }

    private void createGraphicsPolygon(FloatArray controlPoints,
                                       float width, float height, float topLayerHeight,
                                       int sampleCount,
                                       FloatArray vertices, ShortArray indices,
                                       IntArray metadata) {
        final List<Vector2> splinePositions = new ArrayList<Vector2>();
        final List<Vector2> splineNormals = new ArrayList<Vector2>();

        SplineCache.reset(controlPoints.toArray(), sampleCount, width, height, splinePositions, splineNormals);

        final int vertexCount = sampleCount * 3;
        final int vertexSize = 2 + 2 + 2;
        final int indexCount = (sampleCount - 1) * 12;

        metadata.add(vertexCount);
        metadata.add(vertexSize);
        metadata.add(indexCount);

        vertices.ensureCapacity(vertexCount * vertexSize);
        for (int i = 0; i < vertexCount * vertexSize; ++i)
            vertices.add(0.f);

        indices.ensureCapacity(indexCount);
        for (int i = 0; i < indexCount; ++i)
            indices.add(0);

        for (int i = 0; i < sampleCount; ++i) {
            final Vector2 splinePosition = splinePositions.get(i);
            final Vector2 splineNormal = new Vector2();

            splineNormal.set(splineNormals.get(i).cpy().add(splineNormals.get(Math.max(i - 1, 0))).nor());

            final Vector2[] positions = new Vector2[3];

            positions[0] = new Vector2(
                i * width / (float) (sampleCount - 1),
                splinePosition.y
            );

            positions[1] = new Vector2(
                MathUtils.clamp(positions[0].x + topLayerHeight * -splineNormal.x, 0.f, _baseWidth),
                positions[0].y + topLayerHeight * -splineNormal.y
            );

            positions[2] = new Vector2(
                positions[0].x,
                0.f
            );

            final Vector2[] uv = new Vector2[3];
            final float uvScale = 1.f / Math.max(_baseWidth, _baseHeight);

            for (int j = 0; j < 3; ++j) {
                uv[j] = new Vector2(positions[j].x, positions[j].y).scl(uvScale);
            }

            final Vector2[] mask = new Vector2[3];

            mask[0] = new Vector2(1.f, 0.f);
            mask[1] = new Vector2(0.f, 1.f);
            mask[2] = new Vector2(0.f, 1.f);

            for (int j = 0; j < 3; ++j) {
                final int vertexIndex = (i * 3 + j) * vertexSize;

                vertices.set(vertexIndex + 0, positions[j].x);
                vertices.set(vertexIndex + 1, positions[j].y);

                vertices.set(vertexIndex + 2, uv[j].x);
                vertices.set(vertexIndex + 3, uv[j].y);

                vertices.set(vertexIndex + 4, mask[j].x);
                vertices.set(vertexIndex + 5, mask[j].y);
            }
        }

        for (int i = 0; i < sampleCount - 1; ++i) {
            final int baseOffset = i * 12;
            final int baseIndex = i * 3;

            indices.set(baseOffset + 0, (short) (baseIndex + 0));
            indices.set(baseOffset + 1, (short) (baseIndex + 1));
            indices.set(baseOffset + 2, (short) (baseIndex + 4));

            indices.set(baseOffset + 3, (short) (baseIndex + 0));
            indices.set(baseOffset + 4, (short) (baseIndex + 4));
            indices.set(baseOffset + 5, (short) (baseIndex + 3));

            indices.set(baseOffset + 6, (short) (baseIndex + 1));
            indices.set(baseOffset + 7, (short) (baseIndex + 2));
            indices.set(baseOffset + 8, (short) (baseIndex + 5));

            indices.set(baseOffset + 9, (short) (baseIndex + 1));
            indices.set(baseOffset + 10, (short) (baseIndex + 5));
            indices.set(baseOffset + 11, (short) (baseIndex + 4));
        }
    }

    private void createPhysicsPolygon(FloatArray points,
                                      float width, float height,
                                      int sampleCount,
                                      FloatArray vertices) {
        final List<Vector2> splinePositions = new ArrayList<Vector2>();

        SplineCache.reset(points.toArray(), sampleCount, width, height, splinePositions, null);

        final int vertexCount = sampleCount + 2;

        vertices.ensureCapacity(vertexCount * 2);
        for (int i = 0; i < vertexCount * 2; ++i)
            vertices.add(0.f);

        for (int i = 0; i < sampleCount; ++i) {
            Vector2 value = splinePositions.get(i);

            vertices.set(i * 2 + 0, i * width / (float) (sampleCount - 1));
            vertices.set(i * 2 + 1, value.y);
        }

        vertices.set(sampleCount * 2 + 0, width);
        vertices.set(sampleCount * 2 + 1, 0.f);
        vertices.set(sampleCount * 2 + 2, 0.f);
        vertices.set(sampleCount * 2 + 3, 0.f);
    }
}
