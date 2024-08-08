package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.sloperider.ComponentFactory;
import com.sloperider.physics.CollisionGroup;

import java.util.ArrayList;
import java.util.List;

public class Snowboarder extends MainCharacter {
    private final List<Body> _bodyParts = new ArrayList<>(2);

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
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {

    }

    @Override
    public void destroyBody(World world) {
        for (final Body body : _bodyParts) {
            world.destroyBody(body);
        }

        _bodyParts.clear();
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {
        final float boardWidth = 1.5f;
        final float boardHeight = 0.05f;
        final Vector2 boardPosition = new Vector2(getX(), getY());
        final Body boardBody = createBody(
            world,
            null,
            boardPosition,
            getRotation(),
            getRectVertices(boardWidth, boardHeight),
            null,
            null
        );

        final float feetWidth = 0.25f;
        final float feetHeight = 0.15f;

        final Vector2 leftFeetPosition = new Vector2(getX() - boardWidth / 4.f, getY() + boardHeight / 2.f + 0.1f);
        final Vector2 leftFeetAnchor = new Vector2(feetWidth / 4.f, feetHeight / 2.f);
        final Vector2 leftFeetBoardAnchor = new Vector2(leftFeetPosition.x + feetWidth / 4.f, -boardHeight / 2.f);
        final Body leftFeetBody = createBody(
            world,
            boardBody,
            leftFeetPosition,
            getRotation(),
            getRectVertices(feetWidth, feetHeight),
            leftFeetAnchor,
            leftFeetBoardAnchor
        );

        final Vector2 rightFeetPosition = new Vector2(getX() + boardWidth / 4.f, getY() + boardHeight / 2.f + 0.1f);
        final Vector2 rightFeetAnchor = new Vector2(-feetWidth / 4.f, feetHeight / 2.f);
        final Vector2 rightFeetBoardAnchor = new Vector2(rightFeetPosition.x - feetWidth / 4.f, -boardHeight / 2.f);
        final Body rightFeetBody = createBody(
            world,
            boardBody,
            rightFeetPosition,
            getRotation(),
            getRectVertices(feetWidth, feetHeight),
            rightFeetAnchor,
            rightFeetBoardAnchor
        );

        _body = boardBody;

        _bodyParts.add(boardBody);
        _bodyParts.add(leftFeetBody);
        _bodyParts.add(rightFeetBody);
    }

    private final Body createBody(World world,
                                  Body jointBody,
                                  Vector2 position,
                                  float angle,
                                  float[] vertices,
                                  Vector2 anchor,
                                  Vector2 jointAnchor) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(position);
        bodyDef.angle = angle;

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.set(vertices);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.isSensor = false;
        fixtureDef.shape = shape;
        fixtureDef.density = 3.f;
        fixtureDef.friction = 0.1f;
        fixtureDef.restitution = 0.0f;

        fixtureDef.filter.categoryBits = group();
        fixtureDef.filter.maskBits = collidesWith();

        Fixture fixture = body.createFixture(fixtureDef);

        fixture.setUserData(new ContactData(this));

        if (jointBody != null) {
            DistanceJointDef jointDef = new DistanceJointDef();
            jointDef.initialize(body, jointBody, anchor, jointAnchor);
            jointDef.frequencyHz = 30.f;
            jointDef.length = 10.f;
            jointDef.dampingRatio = 0.1f;
            world.createJoint(jointDef);
        }

        return body;
    }

    private float[] getRectVertices(float width, float height) {
        return new float[] {
            -width / 2.f, -height / 2.f,
            -width / 2.f, height / 2.f,
            width / 2.f, height / 2.f,
            width / 2.f, -height / 2.f,
        };
    }
}
