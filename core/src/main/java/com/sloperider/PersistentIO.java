package com.sloperider;

/**
 * Created by jpx on 29/12/15.
 */
public interface PersistentIO {
    boolean readBoolean(final String key, final boolean defaultValue);
    int readInt(final String key, final int defaultValue);
    String readString(final String key, final String defaultValue);

    void beginWrite();

    void writeBoolean(final String key, final boolean value);
    void writeInt(final String key, final int value);
    void writeString(final String key, final String value);

    void endWrite();
}
