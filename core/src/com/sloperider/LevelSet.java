package com.sloperider;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 29/12/15.
 */
public class LevelSet {
    private static LevelSet _instance;

    public static LevelSet instance() {
        if (_instance == null)
            _instance = new LevelSet();

        return _instance;
    }

    private PersistentIO _io;

    private final List<LevelInfo> _levels = new ArrayList<LevelInfo>();

    public LevelSet() {
        _io = new PersistentIO() {
            @Override
            public boolean readBoolean(String key, boolean defaultValue) {
                return false;
            }

            @Override
            public int readInt(String key, int defaultValue) {
                return 0;
            }

            @Override
            public String readString(String key, String defaultValue) {
                return "";
            }

            @Override
            public void beginWrite() {

            }

            @Override
            public void writeBoolean(String key, boolean value) {

            }

            @Override
            public void writeInt(String key, int value) {

            }

            @Override
            public void writeString(String key, String value) {

            }

            @Override
            public void endWrite() {

            }
        };
    }

    public final LevelSet io(final PersistentIO io) {
        _io = io;

        return this;
    }

    public final List<LevelInfo> levels() {
        return _levels;
    }

    public final void updateDescription(final String name, final String description) {
        for (final LevelInfo level : levels()) {
            if (level.name.equals(name)) {
                level.description = description;

                return;
            }
        }
    }

    public final boolean loadFromFile(final String filename) {
        _levels.clear();

        final JsonReader reader = new JsonReader();

        final JsonValue root = reader.parse(Gdx.files.internal(filename));

        final JsonValue levelsNode = root.get("levels");

        for (final JsonValue levelNode : levelsNode) {
            final LevelInfo levelInfo = new LevelInfo();

            levelInfo.id = _levels.size();
            levelInfo.name = levelNode.getString("name");
            levelInfo.filename = levelNode.getString("filename");
            levelInfo.bestScore = _io.readInt(String.format("%s;score", levelInfo.name), 0);
            levelInfo.unlocked = _io.readBoolean(String.format("%s;unlocked", levelInfo.name), false);

            _levels.add(levelInfo);
        }

        if (!levels().isEmpty()) {
            final LevelInfo firstLevel = levels().get(0);

            if (!firstLevel.unlocked) {
                firstLevel.unlocked = true;
                updateLevel(firstLevel);
            }
        }

        return true;
    }

    public final int findCurrentLevel() {
        final String currentLevelName = _io.readString("current", "");

        if (currentLevelName.isEmpty())
            return 0;

        for (int i = 0; i < levels().size(); ++i) {
            if (levels().get(i).name.equals(currentLevelName))
                return i;
        }

        return 0;
    }

    public final void updateCurrentLevel(final int index) {
        _io.beginWrite();
        _io.writeString("current", levels().get(index).name);
        _io.endWrite();
    }

    public final void updateLevel(final LevelInfo levelInfo) {
        _io.beginWrite();
        _io.writeBoolean(String.format("%s;unlocked", levelInfo.name), levelInfo.unlocked);
        _io.writeInt(String.format("%s;score", levelInfo.name), (int) levelInfo.bestScore);
        _io.endWrite();
    }
}
