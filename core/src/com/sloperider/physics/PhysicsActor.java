package com.sloperider.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.World;

/**
 * Created by jpx on 09/11/15.
 */
public interface PhysicsActor {
    public interface ContactData {
        boolean contactBegin(ContactData data, Contact contact);
        boolean contactEnd(ContactData data, Contact contact);
    }

    short group();
    short collidesWith();

    void initializeBody(World world);
    void updateBody(World world, float deltaTime);
    void resetSmoothingState(World world, float deltaTime);
    void applySmoothingState(World world, float deltaTime, final float alpha);
    void destroyBody(World world);
}
