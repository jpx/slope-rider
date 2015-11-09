package com.sloperider.component;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.sloperider.physics.PhysicsActor;

/**
 * Created by jpx on 08/11/15.
 */
public class Sleigh extends Actor implements PhysicsActor {
    public Sleigh() {
    }

    @Override
    public void initializeBody(World world) {

        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(1.5f, 11.f);

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();

        final float size = 1.f;
        shape.set(new float[]{0.f, 0.f, 0.f, size, size, size, size, 0.f});

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 2.f;
        fixtureDef.friction = 0.1f;
        fixtureDef.restitution = 0.0f;

        Fixture fixture = body.createFixture(fixtureDef);
    }

    @Override
    public void updateBody(World world) {

    }

}
