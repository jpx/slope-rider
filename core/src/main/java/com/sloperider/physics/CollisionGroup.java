package com.sloperider.physics;

/**
 * Created by jpx on 16/11/15.
 */
public enum CollisionGroup {
    NOTHING (0),
    MAIN_CHARACTER(1 << 0),
    TRACK (1 << 1),
    END (1 << 2),
    ANYTHING (MAIN_CHARACTER.value | TRACK.value | END.value);

    private int value;

    CollisionGroup(int value) {
        this.value = value;
    }

    public final short value() {
        return (short) value;
    }
}
