package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ShortArray;
import com.sloperider.physics.PhysicsActor;
import com.sloperider.scene.Scene;

import java.nio.BufferOverflowException;
import java.util.logging.Logger;

import javax.lang.model.type.PrimitiveType;

/**
 * Created by jpx on 08/11/15.
 */
public class Track extends Component {
    private Texture _trackGroundTexture;

    private Mesh _mesh;

    private ModelBatch _modelBatch;
    private ModelInstance _modelInstance;
    private Environment _environment;

    public Track() {
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
    protected void doReady() {

        _mesh = createMesh(new float[]{
                6.f, 5.f, 1.5f, 0.f, -0.2f, 0.f, 0.f, 1.2f, 0.f, 0.f
        });

        ModelBuilder builder = new ModelBuilder();

        final float size = 20.f;

        builder.begin();

        builder.part("track", _mesh, Gdx.gl.GL_TRIANGLES, 0, _mesh.getNumIndices(),
                new Material(ColorAttribute.createDiffuse(Color.GREEN)));

        Model model = builder.end();

//        Model model = builder.createRect(0.f, 0.f, 0.f, size, 0.f, 0.f, size, size, 0.f, 0.f, size, 0.f, 0.f, 0.f, -1.f,
//                new Material(ColorAttribute.createDiffuse(Color.GREEN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        _modelInstance = new ModelInstance(model);

        _modelBatch = new ModelBatch();

        _environment = new Environment();
        _environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        _environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _modelBatch.begin(getStage().getCamera());
        _modelInstance.transform.set(batch.getTransformMatrix());
        _modelBatch.render(_modelInstance);
        _modelBatch.end();

        batch.begin();
    }

    private void addTriangleBody(World world, FloatArray vertices, ShortArray indices, Vector2 position) {
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(position);

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();

        Vector2[] triangleVertices = new Vector2[indices.size];

        for (int i = 0; i < indices.size; ++i) {
            triangleVertices[i] = new Vector2(
                vertices.get(indices.get(i) * 2 + 0),
                vertices.get(indices.get(i) * 2 + 1)
            );
        }

        shape.set(triangleVertices);

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 1.f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.f;

        Fixture fixture = body.createFixture(fixtureDef);
    }

    private void addEdgeBody(World world, FloatArray vertices, short index0, short index1, Vector2 position) {
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(position);

        Body body = world.createBody(bodyDef);

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

        Fixture fixture = body.createFixture(fixtureDef);
    }

    @Override
    public void initializeBody(World world) {
        FloatArray vertices = new FloatArray();
        ShortArray indices = new ShortArray();

        createPolygon(new FloatArray(new float[]{
                6.f, 5.f, 1.5f, 0.f, -0.2f, 0.f, 0.f, 1.2f, 0.f, 0.f
        }), 20.f, 5.f, 41, true, vertices, indices);

        final boolean useTriangles = false;

        if (useTriangles) {
            for (int i = 0; i < indices.size / 3; ++i) {
                ShortArray triangleIndices = new ShortArray();

                triangleIndices.add(indices.get(i * 3 + 0));
                triangleIndices.add(indices.get(i * 3 + 1));
                triangleIndices.add(indices.get(i * 3 + 2));

                addTriangleBody(world, vertices, triangleIndices, localToStageCoordinates(new Vector2(getX(), getY())));
            }
        }
        else {
            for (int i = 0; i < vertices.size / 2; ++i) {
                short index0 = i == 0 ? (short) ((vertices.size - 1) / 2) : (short) i;
                short index1 = i == (vertices.size - 1) / 2 ? (short) 0 : (short) (i + 1);

                addEdgeBody(world, vertices, index0, index1, localToStageCoordinates(new Vector2(getX(), getY())));
            }
        }
    }

    @Override
    public void updateBody(World world) {

    }

    private Mesh createMesh(float[] points) {
        Texture texture = new Texture("texture/track_ground.png");
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        final int width = 20;
        final int height = 5;

        final int sampleCount = 101;

        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();

        createPolygon(new FloatArray(points), width, height, sampleCount, true, vertices, indices);

        final int vertexCount = vertices.size / 2;
        final int indexCount = indices.size;

        final Mesh mesh = new Mesh(true, vertexCount, indexCount,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
        );

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
                position.x,
                position.y
            );

            meshVertices.add(uv.x);
            meshVertices.add(uv.y);
        }

        indices.reverse();
        mesh.setIndices(indices.toArray());
        mesh.setVertices(meshVertices.toArray());

        return mesh;
    }

    private static void createPolygon(FloatArray points, float width, float height, int sampleCount, boolean clockwise, FloatArray vertices, ShortArray indices) {
        final int pointCount = points.size;
        final int vertexCount = sampleCount + 2;

        Vector2[] splinePoints = new Vector2[pointCount + 2];

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
    }
}
