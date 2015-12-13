package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
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
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
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
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.math.SplineCache;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.Line;

/**
 * Created by jpx on 08/11/15.
 */
public class Track extends Component {
    private static final int PHYSICS_SPLINE_SAMPLE_COUNT = 101;
    private static final int GRAPHICS_SPLINE_SAMPLE_COUNT = 101;

    private static final float TOP_LAYER0_HEIGHT = 2.5f;
    private static final float TOP_LAYER1_HEIGHT = 0.7f;

    private static final float HORIZONTAL_SPLATTING_SIZE = 0.2f;

    public static enum GroundMaterialType {
        SNOW,
        STONE,
        DIRT,
        BOOSTER
    }

    public static class GroundMaterial {
        GroundMaterialType type;
        int index;

        public GroundMaterial(GroundMaterialType type, int index) {
            this.type = type;
            this.index = index;
        }
    }

    public static final class PointData {
        float x;
        float y;
        boolean editable;
        GroundMaterialType groundMaterial;

        public PointData(float x, float y, boolean editable, GroundMaterialType groundMaterial) {
            this.x = x;
            this.y = y;
            this.editable = editable;
            this.groundMaterial = groundMaterial;
        }
    }

    public interface Listener {
        void changed(Track self);
    }

    private final List<Listener> _listeners = new ArrayList<Listener>();

    private Map<GroundMaterialType, GroundMaterial> _materials;

    private Texture _groundDirtTexture;
    private Texture _groundSnowTexture;
    private Texture _groundStoneTexture;
    private Texture _groundBoosterTexture;

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

    private final List<PointData> _trackPointData = new ArrayList<PointData>();
    private Map<TrackPoint, Integer> _trackPoints;
    private FloatArray _trackPointValues;

    private Body _body;
    private List<Fixture> _fixtures;

    private boolean _physicsTrackUpdateNeeded;
    private boolean _graphicsTrackUpdateNeeded;

    private boolean _editable;

    public Track() {
        // FIXME initialize from factory

        _materials = new HashMap<GroundMaterialType, GroundMaterial>();
        _materials.put(GroundMaterialType.SNOW, new GroundMaterial(GroundMaterialType.SNOW, 0));
        _materials.put(GroundMaterialType.DIRT, new GroundMaterial(GroundMaterialType.DIRT, 1));
        _materials.put(GroundMaterialType.STONE, new GroundMaterial(GroundMaterialType.STONE, 2));
        _materials.put(GroundMaterialType.BOOSTER, new GroundMaterial(GroundMaterialType.BOOSTER, 3));
    }

    public final Track editable(boolean editable) {
        if (editable == _editable)
            return this;

        _editable = editable;

        for (final Map.Entry<TrackPoint, Integer> entry : _trackPoints.entrySet()) {
            final TrackPoint trackPoint = entry.getKey();

            if (editable) {
                trackPoint.setVisible(true);
                trackPoint.setTouchable(Touchable.enabled);
            } else {
                trackPoint.setVisible(false);
                trackPoint.setTouchable(Touchable.disabled);
            }
        }

        return this;
    }

    public final void addListener(Listener listener) {
        _listeners.add(listener);
    }

    public final void removeListener(Listener listener) {
        _listeners.remove(listener);
    }

    public final Track setPoints(final List<PointData> points) {
        _trackPointData.addAll(points);
        return this;
    }

    public final void setBaseSize(float width, float height) {
        _baseWidth = width;
        _baseHeight = height;
    }

    private Vector2[] buildControlPoints() {
        final Vector2[] controlPoints = new Vector2[_trackPointData.size()];

        for (int i = 0; i < _trackPointData.size(); ++i)
            controlPoints[i] = new Vector2(_baseWidth * _trackPointData.get(i).x, _trackPointValues.get(i));

        return controlPoints;
    }

    private void initializeTrackPoints(int pointCount) {
        _trackPoints = new HashMap<TrackPoint, Integer>(pointCount);
        _trackPointValues = new FloatArray(pointCount);

        final Vector2 parentPosition = new Vector2(getX(), getY());
        final Vector2 size = new Vector2(_baseWidth, _baseHeight);

        for (int i = 0; i < pointCount; ++i) {
            final PointData pointData = _trackPointData.get(i);

            final float x = pointData.x;
            final float y = pointData.y;

            _trackPointValues.add(y * size.y);

            if (!pointData.editable)
                continue;

            final Vector2 position = parentPosition.cpy().add(x * size.x, y * size.y + size.y);

            TrackPoint trackPoint = addComponent(_componentFactory.createComponent(
                position,
                TrackPoint.class
            ).setChangedHandler(new TrackPoint.ChangedHandler() {
                @Override
                public void changed(TrackPoint self, float value) {
                    updateTrackPoint(_trackPoints.get(self), value);
                }
            })).setInitialTrackValue(position.y);

            _trackPoints.put(trackPoint, i);
        }
    }

    private void updateTrackPoint(int index, float value) {
        _trackPointValues.set(index, value - _baseHeight - getY());

        _physicsTrackUpdateNeeded = true;
        _graphicsTrackUpdateNeeded = true;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/track_ground_dirt.png", Texture.class);
        assetManager.load("texture/track_ground_snow.png", Texture.class);
        assetManager.load("texture/track_ground_stone.png", Texture.class);

        _vertexShaderSource = Gdx.files.internal("shader/track.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/track.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _groundDirtTexture = assetManager.get("texture/track_ground_dirt.png", Texture.class);
        _groundDirtTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        _groundDirtTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        _groundSnowTexture = assetManager.get("texture/track_ground_snow.png", Texture.class);
        _groundSnowTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        _groundSnowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        _groundStoneTexture = assetManager.get("texture/track_ground_stone.png", Texture.class);
        _groundStoneTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        _groundStoneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        final Pixmap boosterPixmap = new Pixmap(32, 32, Pixmap.Format.RGB888);
        boosterPixmap.setColor(Color.RED);
        boosterPixmap.fill();
        _groundBoosterTexture = new Texture(boosterPixmap);
        _groundBoosterTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        _groundBoosterTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _editable = true;

        setTouchable(Touchable.disabled);

        _physicsTrackUpdateNeeded = false;
        _graphicsTrackUpdateNeeded = false;

        _componentFactory = componentFactory;

        initializeTrackPoints(_trackPointData.size());

        resetMesh();

        ModelBuilder builder = new ModelBuilder();

        builder.begin();

        Material material = new Material();

        builder.part("track", _mesh, Gdx.gl.GL_TRIANGLES, 0, _mesh.getNumIndices(), material);

        Model model = builder.end();

        _renderable = new Renderable();
        _renderable.worldTransform
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(getX(), getY(), 0.f);
        _renderable.environment = _environment;
        _renderable.meshPart.set(model.meshParts.first());
        _renderable.material = model.materials.first();

        _context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        _shader = new Shader() {
            ShaderProgram program;

            Camera camera;
            RenderContext context;

            float time;

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

                final Array<Attribute> textureAttributes = new Array<Attribute>();
                material.get(textureAttributes, TextureAttribute.Diffuse);

                final Array<Texture> splattingMaps = new Array<Texture>();
                splattingMaps.add(_groundSnowTexture);
                splattingMaps.add(_groundDirtTexture);
                splattingMaps.add(_groundStoneTexture);
                splattingMaps.add(_groundBoosterTexture);

                for (int i = 0; i < splattingMaps.size; ++i) {
                    splattingMaps.get(i).bind(i);
                    program.setUniformi("u_splattingMap" + i, i);
                }

                program.setUniformMatrix("u_modelToWorldMatrix", renderable.worldTransform);

                time += Gdx.graphics.getDeltaTime();
                if (time > 15.f)
                    time = 0.f;

                program.setUniformf("u_time", time);

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
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    protected void doAct(float delta) {
        if (_graphicsTrackUpdateNeeded) {
            _graphicsTrackUpdateNeeded = false;

            resetMesh();

            for (final Listener listener : _listeners) {
                listener.changed(this);
            }
        }
    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _context.begin();

        _shader.begin(getStage().getCamera(), _context);

        _shader.render(_renderable);

        _shader.end();

        _context.end();

        batch.begin();
    }

    static class EdgeContactData implements PhysicsActor.ContactData {
        GroundMaterial material;
        Vector2 normal;

        EdgeContactData(GroundMaterial material, Vector2 normal) {
            this.material = material;
            this.normal = normal;
        }

        @Override
        public boolean contactBegin(ContactData data) {
            return false;
        }

        @Override
        public boolean contactEnd(ContactData data) {
            return false;
        }
    }

    private void addEdgeFixture(final World world,
                                final FloatArray vertices,
                                short index0, short index1,
                                final Vector2 position, final Vector2 normal,
                                final GroundMaterial material) {
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
        fixtureDef.restitution = 0.f;

        switch (material.type) {
            case STONE:
                fixtureDef.friction = 10.f;
                break;
            default:
                fixtureDef.friction = 0.f;
                break;
        }

        fixtureDef.filter.categoryBits = group();

        Fixture fixture = _body.createFixture(fixtureDef);
        _fixtures.add(fixture);

        fixture.setUserData(new EdgeContactData(material, normal));
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
        final List<Vector2> normals = new ArrayList<Vector2>();
        final List<GroundMaterial> materials = new ArrayList<GroundMaterial>();

        createPhysicsPolygon(
                buildControlPoints(), _baseWidth, _baseHeight, PHYSICS_SPLINE_SAMPLE_COUNT, vertices, normals, materials
        );

        for (int i = 0; i < vertices.size / 2; ++i) {
            final short index0 = (short) i;
            final short index1 = i == vertices.size / 2 - 1 ? (short) 0 : (short) (i + 1);

            addEdgeFixture(world, vertices, index0, index1, new Vector2(getX(), getY()), normals.get(i), materials.get(i));
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
                buildControlPoints(),
                _baseWidth, _baseHeight, TOP_LAYER0_HEIGHT, TOP_LAYER1_HEIGHT,
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
                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_mask")
            );
        }

        _mesh.setIndices(indices.toArray());
        _mesh.setVertices(vertices.toArray());

        BoundingBox boundingBox = new BoundingBox();
        _mesh.calculateBoundingBox(boundingBox);

        setSize(boundingBox.getWidth(), boundingBox.getHeight());
    }

    private float noise(final float max) {
        return MathUtils.randomTriangular(0.f, max, max / 2.f);
    }

    private void createGraphicsPolygon(final Vector2[] controlPoints,
                                       float width, float height, float topLayer0Height, float topLayer1Height,
                                       int sampleCount,
                                       FloatArray vertices, ShortArray indices,
                                       IntArray metadata) {
        MathUtils.random.setSeed(43l);

        SplineCache.reset(controlPoints, sampleCount, width, height);

        final List<Vector2> splinePositions = SplineCache.positions();
        final List<Vector2> splineNormals = SplineCache.normals();

        final int layerCount = 4;
        final int vertexCount = sampleCount * layerCount;
        final int vertexSize = 2 + 2 + 4;
        final int indexCount = (sampleCount - 1) * 6 * (layerCount - 1);

        metadata.add(vertexCount);
        metadata.add(vertexSize);
        metadata.add(indexCount);

        vertices.ensureCapacity(vertexCount * vertexSize);
        for (int i = 0; i < vertexCount * vertexSize; ++i)
            vertices.add(0.f);

        indices.ensureCapacity(indexCount);
        for (int i = 0; i < indexCount; ++i)
            indices.add(0);

        int nextTrackPointIndex = 0;
        PointData previousTrackPoint = null;
        PointData trackPoint = null;
        PointData nextTrackPoint = _trackPointData.get(nextTrackPointIndex);
        float trackPointXRate = 0.f;

        GroundMaterial groundMaterial = null;
        GroundMaterial previousGroundMaterial = null;
        GroundMaterial nextGroundMaterial = null;

        for (int i = 0; i < sampleCount; ++i) {
            final float x = i * width / (float) (sampleCount - 1);
            final float globalXRate = x / _baseWidth;

            if (trackPoint == null || (nextTrackPoint != null && x >= nextTrackPoint.x * width)) {
                previousTrackPoint = trackPoint;
                trackPoint = nextTrackPoint;

                ++nextTrackPointIndex;
                nextTrackPoint = nextTrackPointIndex < _trackPointData.size()
                    ? _trackPointData.get(nextTrackPointIndex)
                    : null;

                groundMaterial = _materials.get(trackPoint.groundMaterial);
                previousGroundMaterial = previousTrackPoint != null ? _materials.get(previousTrackPoint.groundMaterial) : groundMaterial;
                nextGroundMaterial = nextTrackPoint != null ? _materials.get(nextTrackPoint.groundMaterial) : groundMaterial;
            }

            if (nextTrackPoint != null)
                trackPointXRate = (globalXRate - trackPoint.x) / (nextTrackPoint.x - trackPoint.x);
            else
                trackPointXRate = 1.f;

            final Vector2 splinePosition = splinePositions.get(i);
            final Vector2 splineNormal = new Vector2();

            if (i == 0 || i == sampleCount - 1)
                splineNormal.set(0.f, 1.f);
            else
                splineNormal.set(splineNormals.get(i).cpy().add(splineNormals.get(Math.max(i - 1, 0))).nor());

            final Vector2[] positions = new Vector2[layerCount];

            positions[0] = new Vector2(
                x,
                splinePosition.y
            );

            float localTopLayer0Height = topLayer0Height;
            float topLayer0HeightScale = 1.f;
            float noiseAmplitude = 0.f;

            if (groundMaterial.type == GroundMaterialType.SNOW) {
                topLayer0HeightScale = 1.2f;
                noiseAmplitude = 0.3f;
            } else if (groundMaterial.type == GroundMaterialType.STONE) {
                noiseAmplitude = 0.2f;
            } else if (groundMaterial.type == GroundMaterialType.BOOSTER) {
                topLayer0HeightScale = 0.8f;
            }

            if (topLayer0HeightScale != 1.f) {
                localTopLayer0Height *= topLayer0HeightScale;
            }

            if (noiseAmplitude > 0.f) {
                localTopLayer0Height *= 1.f + noise(noiseAmplitude);
            }

            positions[1] = new Vector2(
                MathUtils.clamp(positions[0].x + localTopLayer0Height * -splineNormal.x, 0.f, _baseWidth),
                positions[0].y + localTopLayer0Height * -splineNormal.y
            );

            positions[2] = new Vector2(
                MathUtils.clamp(positions[1].x + topLayer1Height * -splineNormal.x, 0.f, _baseWidth),
                positions[1].y + topLayer1Height * -splineNormal.y
            );

            positions[3] = new Vector2(
                positions[0].x,
                0.f
            );

            final Vector2[] uv = new Vector2[layerCount];
            final float uvScale = SlopeRider.PIXEL_PER_UNIT / Math.max(_baseWidth, _baseHeight);

            for (int j = 0; j < layerCount; ++j) {
                uv[j] = new Vector2(positions[j].x, positions[j].y).scl(uvScale);
            }

            final float[][] mask = new float[4][layerCount];

            float currentGroundMaterialMask = 1.f;
            GroundMaterial secondaryGroundMaterial = groundMaterial;

            if (trackPointXRate < HORIZONTAL_SPLATTING_SIZE) {
                if (previousGroundMaterial != groundMaterial) {
                    secondaryGroundMaterial = previousGroundMaterial;
                    currentGroundMaterialMask = MathUtils.lerp(0.5f, 1.f, trackPointXRate / HORIZONTAL_SPLATTING_SIZE);
                } else {
                    currentGroundMaterialMask = 0.5f;
                }
            }
            else if (trackPointXRate > 1.f - HORIZONTAL_SPLATTING_SIZE) {
                if (nextGroundMaterial != groundMaterial) {
                    secondaryGroundMaterial = nextGroundMaterial;
                    currentGroundMaterialMask = MathUtils.lerp(1.f, 0.5f, (trackPointXRate - (1.f - HORIZONTAL_SPLATTING_SIZE)) / (1.f - (1.f - HORIZONTAL_SPLATTING_SIZE)));
                } else {
                    currentGroundMaterialMask = 0.5f;
                }
            } else {
                currentGroundMaterialMask = 0.5f;
            }

            final float secondaryGroundMaterialMask = 1.f - currentGroundMaterialMask;

            final float[] maskXScale = new float[] { 0.f, 0.f, 0.f, 0.f };

            maskXScale[groundMaterial.index] += currentGroundMaterialMask;
            maskXScale[secondaryGroundMaterial.index] += secondaryGroundMaterialMask;

            final float[] maskY0Scale = new float[] { 0.f, 0.f, 0.f, 0.f };
            maskY0Scale[groundMaterial.index] = 1.f;

            final float[] maskY1Scale = new float[] { 0.f, 0.f, 0.f, 0.f };
            maskY1Scale[groundMaterial.index] = 0.8f;
            maskY1Scale[_materials.get(GroundMaterialType.DIRT).index] = 0.2f;

            mask[0] = new float[] { maskXScale[0] * maskY0Scale[0], maskXScale[1] * maskY0Scale[1], maskXScale[2] * maskY0Scale[2], maskXScale[3] * maskY0Scale[3] };
            mask[1] = new float[] { maskXScale[0] * maskY1Scale[0], maskXScale[1] * maskY1Scale[1], maskXScale[2] * maskY1Scale[2], maskXScale[3] * maskY1Scale[3] };
            mask[2] = new float[] { 0.f, 1.f, 0.f, 0.f };
            mask[3] = new float[] { 0.f, 1.f, 0.f, 0.f };

            for (int j = 0; j < layerCount; ++j) {
                final int vertexIndex = (i * layerCount + j) * vertexSize;

                vertices.set(vertexIndex + 0, positions[j].x);
                vertices.set(vertexIndex + 1, positions[j].y);

                vertices.set(vertexIndex + 2, uv[j].x);
                vertices.set(vertexIndex + 3, uv[j].y);

                vertices.set(vertexIndex + 4, mask[j][0]);
                vertices.set(vertexIndex + 5, mask[j][1]);
                vertices.set(vertexIndex + 6, mask[j][2]);
                vertices.set(vertexIndex + 7, mask[j][3]);
            }
        }

        for (int i = 0; i < sampleCount - 1; ++i) {
            final int baseOffset = i * 6 * (layerCount - 1);
            final int baseIndex = i * layerCount;

            indices.set(baseOffset + 0, (short) (baseIndex + 0));
            indices.set(baseOffset + 1, (short) (baseIndex + 1));
            indices.set(baseOffset + 2, (short) (baseIndex + 5));

            indices.set(baseOffset + 3, (short) (baseIndex + 0));
            indices.set(baseOffset + 4, (short) (baseIndex + 5));
            indices.set(baseOffset + 5, (short) (baseIndex + 4));

            indices.set(baseOffset + 6, (short) (baseIndex + 1));
            indices.set(baseOffset + 7, (short) (baseIndex + 2));
            indices.set(baseOffset + 8, (short) (baseIndex + 6));

            indices.set(baseOffset + 9, (short) (baseIndex + 1));
            indices.set(baseOffset + 10, (short) (baseIndex + 6));
            indices.set(baseOffset + 11, (short) (baseIndex + 5));

            indices.set(baseOffset + 12, (short) (baseIndex + 2));
            indices.set(baseOffset + 13, (short) (baseIndex + 3));
            indices.set(baseOffset + 14, (short) (baseIndex + 7));

            indices.set(baseOffset + 15, (short) (baseIndex + 2));
            indices.set(baseOffset + 16, (short) (baseIndex + 7));
            indices.set(baseOffset + 17, (short) (baseIndex + 6));
        }
    }

    private void createPhysicsPolygon(final Vector2[] controlPoints,
                                      float width, float height,
                                      int sampleCount,
                                      FloatArray vertices, final List<Vector2> normals, final List<GroundMaterial> materials) {
        SplineCache.reset(controlPoints, sampleCount, width, height);

        normals.addAll(SplineCache.normals());

        final List<Vector2> splinePositions = SplineCache.positions();

        final int vertexCount = sampleCount + 2;

        vertices.ensureCapacity(vertexCount * 2);
        for (int i = 0; i < vertexCount * 2; ++i)
            vertices.add(0.f);

        int nextTrackPointIndex = 0;
        PointData trackPoint = null;
        PointData nextTrackPoint = _trackPointData.get(nextTrackPointIndex);

        GroundMaterial groundMaterial = null;

        for (int i = 0; i < sampleCount; ++i) {
            Vector2 value = splinePositions.get(i);

            final float x = i * width / (float) (sampleCount - 1);

            vertices.set(i * 2 + 0, x);
            vertices.set(i * 2 + 1, value.y);

            if (trackPoint == null || (nextTrackPoint != null && x >= nextTrackPoint.x * width)) {
                ++nextTrackPointIndex;
                trackPoint = nextTrackPoint;
                nextTrackPoint = nextTrackPointIndex < _trackPointData.size() ? _trackPointData.get(nextTrackPointIndex) : null;
                groundMaterial = _materials.get(trackPoint.groundMaterial);
            }

            materials.add(groundMaterial);
        }

        vertices.set(sampleCount * 2 + 0, width);
        vertices.set(sampleCount * 2 + 1, 0.f);
        vertices.set(sampleCount * 2 + 2, 0.f);
        vertices.set(sampleCount * 2 + 3, 0.f);

        normals.add(new Vector2(0.f, -1.f));
        normals.add(new Vector2(0.f, -1.f));

        materials.add(_materials.get(GroundMaterialType.DIRT));
        materials.add(_materials.get(GroundMaterialType.DIRT));
    }

    @Override
    public void destroyBody(World world) {
        world.destroyBody(_body);
    }

    @Override
    public short group() {
        return CollisionGroup.TRACK.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.ANYTHING.value();
    }

    public final float heightAt(float x) {
        return SplineCache.heightAt(x) + getY();
    }
}
