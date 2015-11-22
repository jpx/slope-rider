package com.sloperider.math;

import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

/**
 * Created by jpx on 22/11/15.
 */
public class SplineCache {

    public static final void reset(float[] controlPoints,
                                   int sampleCount,
                                   float width, float height,
                                   List<Vector2> positions, List<Vector2> normals) {
        final int controlPointCount = controlPoints.length;
        final int splinePointCount = controlPointCount + 2;

        Vector2[] splinePoints = new Vector2[splinePointCount];

        for (int i = 0; i < controlPointCount; ++i)
        {
            splinePoints[i + 1] = new Vector2(i * (width / (float) (controlPointCount - 1)), controlPoints[i] + height);
        }

        splinePoints[0] = new Vector2(0.f, controlPoints[0] + height);
        splinePoints[splinePoints.length - 1] = new Vector2(width, controlPoints[controlPointCount - 1] + height);

        CatmullRomSpline<Vector2> spline = new CatmullRomSpline<Vector2>(splinePoints, false);

        for (int i = 0; i < sampleCount; ++i) {
            positions.add(new Vector2());

            if (normals != null)
                normals.add(new Vector2());

            spline.valueAt(positions.get(i), i / (float) (sampleCount - 1));

            if (normals != null) {
                final Vector2 derivative = new Vector2();
                spline.derivativeAt(derivative, i / (float) (sampleCount - 1));
                normals.get(i).set(-derivative.y, derivative.x).nor();
            }
        }
    }
}
