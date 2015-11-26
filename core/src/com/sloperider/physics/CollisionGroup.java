package com.sloperider.physics;

/**
 * Created by jpx on 16/11/15.
 */
public enum CollisionGroup {
    NOTHING (0),
    SLEIGH (1 << 0),
    TRACK (1 << 1),
    ANYTHING (SLEIGH.value | TRACK.value);

    private int value;

    CollisionGroup(int value) {
        this.value = value;
    }

    public final short value() {
        return (short) value;
    }
}
