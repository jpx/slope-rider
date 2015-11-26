package com.sloperider.physics;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 08/11/15.
 */
public class PhysicsWorld {

    private World _world;
    private List<PhysicsActor> _actors;

    private Box2DDebugRenderer _renderer;

    public PhysicsWorld() {
        _world = new World(new Vector2(0.f, -10.f), true);

        _actors = new ArrayList<PhysicsActor>();

        _world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                final Object lhsObject = contact.getFixtureA().getUserData();
                final Object rhsObject = contact.getFixtureB().getUserData();

                if (lhsObject != null && rhsObject != null) {
                    final PhysicsActor.ContactData lhsData = (PhysicsActor.ContactData) lhsObject;
                    final PhysicsActor.ContactData rhsData = (PhysicsActor.ContactData) rhsObject;

                    if (!lhsData.contactBegin(rhsData))
                        rhsData.contactBegin(lhsData);
                }
            }

            @Override
            public void endContact(Contact contact) {
                final Object lhsObject = contact.getFixtureA().getUserData();
                final Object rhsObject = contact.getFixtureB().getUserData();

                if (lhsObject != null && rhsObject != null) {
                    final PhysicsActor.ContactData lhsData = (PhysicsActor.ContactData) lhsObject;
                    final PhysicsActor.ContactData rhsData = (PhysicsActor.ContactData) rhsObject;

                    if (!lhsData.contactEnd(rhsData))
                        rhsData.contactEnd(lhsData);
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });
    }

    public final void addActor(PhysicsActor actor) {
        actor.initializeBody(_world);

        _actors.add(actor);
    }

    public final void removeActor(PhysicsActor actor) {
        _actors.remove(actor);
    }

    public final void update(float deltaTime) {

        for (PhysicsActor actor : _actors) {
            actor.updateBody(_world);
        }

        _world.step(deltaTime, 6, 2);
    }

    public final void render(Camera camera) {
        Matrix4 projection = new Matrix4(camera.combined);
        projection.scale(
            SlopeRider.PIXEL_PER_UNIT,
            SlopeRider.PIXEL_PER_UNIT,
            0.f
        );
    }
}
