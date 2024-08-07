package com.sloperider;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class EventLogger {
    private static EventLogger _instance = null;

    private final HashMap<String, String> _env = new HashMap<>();

    private PrintWriter _writer = null;

    public static EventLogger instance() {
        if (_instance == null) {
            _instance = new EventLogger();
        }

        return _instance;
    }

    public final EventLogger setEnv(final String key, final String value) {
        _env.put(key, value);

        if (Gdx.app.getType() != Application.ApplicationType.Android) {
            if (key.equals("session_id")) {
                try {
                    _writer = new PrintWriter("events_" + value + ".log");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return this;
    }

    public final EventLogger log(final String... params) {
        if (_writer == null) {
            return this;
        }

         List<String> envVars = _env
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.toList());

         _writer.println(String.join(", ", envVars) + ", " + String.join(",", params));

         return this;
    }
}
