package com.sloperider.physics;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by jpx on 14/01/16.
 */
public class SmoothingState {
    public final Vector2 previousPosition;
    public final Vector2 smoothedPosition;

    public float previousRotation;
    public float smoothedRotation;

    public SmoothingState() {
        previousPosition = new Vector2();
        smoothedPosition = new Vector2();
        previousRotation = 0.f;
        smoothedRotation = 0.f;
    }
}
