package com.sloperider.physics;

import com.badlogic.gdx.physics.box2d.World;

/**
 * Created by jpx on 09/11/15.
 */
public interface PhysicsActor {
    void initializeBody(World world);

    void updateBody(World world);
}
