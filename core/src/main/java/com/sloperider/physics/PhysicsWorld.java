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
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jpx on 08/11/15.
 */
public class PhysicsWorld {

    private static final float FIXED_TIMESTEP = 1.f / 60.f;
    private static final int MAX_STEP_COUNT = 5;

    private float _fixedTimestepAccumulator;
    private float _fixedTimestepAccumulatorRate;

    private World _world;
    private List<PhysicsActor> _actors;
    private final List<PhysicsActor> _actorsToRemove = new LinkedList<PhysicsActor>();
    private final List<PhysicsActor> _actorsToAdd = new LinkedList<PhysicsActor>();

    private Box2DDebugRenderer _renderer;

    public PhysicsWorld() {
        _fixedTimestepAccumulator = 0.f;
        _fixedTimestepAccumulatorRate = 0.f;

        _renderer = new Box2DDebugRenderer();

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

                    if (!lhsData.contactBegin(rhsData, contact))
                        rhsData.contactBegin(lhsData, contact);
                }
            }

            @Override
            public void endContact(Contact contact) {
                final Object lhsObject = contact.getFixtureA().getUserData();
                final Object rhsObject = contact.getFixtureB().getUserData();

                if (lhsObject != null && rhsObject != null) {
                    final PhysicsActor.ContactData lhsData = (PhysicsActor.ContactData) lhsObject;
                    final PhysicsActor.ContactData rhsData = (PhysicsActor.ContactData) rhsObject;

                    if (!lhsData.contactEnd(rhsData, contact))
                        rhsData.contactEnd(lhsData, contact);
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
        if (_actorsToRemove.contains(actor))
            _actorsToRemove.remove(actor);

        _actorsToAdd.add(actor);
    }

    public final void removeActor(PhysicsActor actor) {
        if (_actorsToAdd.contains(actor))
            _actorsToAdd.remove(actor);

        _actorsToRemove.add(actor);
    }

    private void doAddActor(final PhysicsActor actor) {
        actor.initializeBody(_world);
        actor.resetSmoothingState(_world, 0.f);

        _actors.add(actor);
    }

    private void doRemoveActor(final PhysicsActor actor) {
        actor.destroyBody(_world);

        _actors.remove(actor);
    }

    private void flushPendingActors() {
        while (!_actorsToRemove.isEmpty()) {
            final PhysicsActor actor = _actorsToRemove.remove(0);

            doRemoveActor(actor);
        }

        while (!_actorsToAdd.isEmpty()) {
            final PhysicsActor actor = _actorsToAdd.remove(0);

            doAddActor(actor);
        }
    }

    public final void update(float deltaTime) {
        flushPendingActors();

        _fixedTimestepAccumulator += deltaTime;

        final int stepCount = (int) Math.floor((double) (_fixedTimestepAccumulator / FIXED_TIMESTEP));

        if (stepCount > 0) {
            _fixedTimestepAccumulator -= stepCount * FIXED_TIMESTEP;
        }

        _fixedTimestepAccumulatorRate = _fixedTimestepAccumulator / FIXED_TIMESTEP;

        final int boundStepCount = Math.min(stepCount, MAX_STEP_COUNT);

        for (int i = 0; i < boundStepCount; ++i) {
            for (PhysicsActor actor : _actors) {
                actor.resetSmoothingState(_world, FIXED_TIMESTEP);
                actor.updateBody(_world, FIXED_TIMESTEP);
            }

            flushPendingActors();

            _world.step(FIXED_TIMESTEP, 6, 2);
        }

        for (final PhysicsActor actor : _actors)
            actor.applySmoothingState(_world, deltaTime, _fixedTimestepAccumulatorRate);

        flushPendingActors();
    }

    public final void render(Camera camera) {
        Matrix4 projection = new Matrix4(camera.combined);
        projection.scale(
            SlopeRider.PIXEL_PER_UNIT,
            SlopeRider.PIXEL_PER_UNIT,
            0.f
        );

        _renderer.render(_world, projection);
    }
}
