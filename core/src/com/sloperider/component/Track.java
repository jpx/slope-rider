package com.sloperider.component;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.sloperider.physics.PhysicsActor;
import com.sloperider.scene.Scene;

/**
 * Created by jpx on 08/11/15.
 */
public class Track extends Actor implements PhysicsActor {
    private PolygonRegion _polygonRegion;

    public Track() {
        _polygonRegion = createPolygonRegion(new float[]{
            6.f, 5.f, 1.5f, 0.f, -0.2f, 0.f, 0.f, 1.2f, 0.f, 0.f
        });
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

    @Override
    public void draw(Batch batch, float parentAlpha) {
        ((PolygonSpriteBatch) batch).draw(_polygonRegion, 0.f, 0.f);
    }

    private PolygonRegion createPolygonRegion(float[] points) {
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0x00FF0044);
        pix.fill();

        Texture texture = new Texture("texture/track_ground.png");

        final int width = 20;
        final int height = 5;

        final int sampleCount = 101;

        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();

        createPolygon(new FloatArray(points), width, height, sampleCount, true, vertices, indices);

        return new PolygonRegion(new TextureRegion(texture, 0.f, 0.f, 10.f, 10.f), vertices.items, indices.items);
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

        EarClippingTriangulator triangulator = new EarClippingTriangulator();

        indices.addAll(triangulator.computeTriangles(vertices));
    }
}
