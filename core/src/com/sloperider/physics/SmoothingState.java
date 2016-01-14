package com.sloperider.physics;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by jpx on 14/01/16.
 */
public class SmoothingState {
    public Vector2 previousPosition;
    public Vector2 smoothedPosition;

    public float previousRotation;
    public float smoothedRotation;
}
