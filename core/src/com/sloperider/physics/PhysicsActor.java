package com.sloperider.physics;

import com.badlogic.gdx.physics.box2d.World;

/**
 * Created by jpx on 09/11/15.
 */
public interface PhysicsActor {
    public interface ContactData {
        boolean contactBegin(ContactData data);
        boolean contactEnd(ContactData data);
    }

    short group();
    short collidesWith();

    void initializeBody(World world);

    void updateBody(World world);
}
