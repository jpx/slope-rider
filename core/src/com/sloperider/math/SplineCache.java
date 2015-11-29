package com.sloperider.math;

import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 22/11/15.
 */
public class SplineCache {

    private static int _sampleCount;
    private static float _width;
    private static float _height;
    private static final List<Vector2> _positions = new ArrayList<Vector2>();
    private static final List<Vector2> _normals = new ArrayList<Vector2>();

    public static List<Vector2> positions() {
        return _positions;
    }

    public static List<Vector2> normals () {
        return _normals;
    }

    public static float heightAt(float x) {
        return _positions.get((int) ((x / _width) * (_positions.size() - 1))).y;
    }

    public static void reset(float[] controlPoints,
                             int sampleCount,
                             float width, float height) {
        final Vector2[] vec2ControlPoints = new Vector2[controlPoints.length];

        for (int i = 0; i < controlPoints.length; ++i)
            vec2ControlPoints[i] = new Vector2(i * (width / (float) (controlPoints.length - 1)), controlPoints[i]);

        reset(vec2ControlPoints, sampleCount, width, height);
    }

    public static final void reset(Vector2[] controlPoints,
                                   int sampleCount,
                                   float width, float height) {
        _sampleCount = sampleCount;
        _width = width;
        _height = height;

        final int controlPointCount = controlPoints.length;
        final int splinePointCount = controlPointCount + 2;

        Vector2[] splinePoints = new Vector2[splinePointCount];

        for (int i = 0; i < controlPointCount; ++i)
        {
            splinePoints[i + 1] = new Vector2(controlPoints[i].x, controlPoints[i].y + height);
        }

        splinePoints[0] = new Vector2(0.f, controlPoints[0].y + height);
        splinePoints[splinePoints.length - 1] = new Vector2(width, controlPoints[controlPointCount - 1].y + height);

        CatmullRomSpline<Vector2> spline = new CatmullRomSpline<Vector2>(splinePoints, false);

        _positions.clear();
        _normals.clear();

        for (int i = 0; i < sampleCount; ++i) {
            _positions.add(new Vector2());

            if (_normals != null)
                _normals.add(new Vector2());

            spline.valueAt(_positions.get(i), i / (float) (sampleCount - 1));

            if (_normals != null) {
                final Vector2 derivative = new Vector2();
                spline.derivativeAt(derivative, i / (float) (sampleCount - 1));
                _normals.get(i).set(-derivative.y, derivative.x).nor();
            }
        }
    }
}
