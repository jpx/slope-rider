package com.sloperider.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.Configuration;
import com.sloperider.PersistentIO;
import com.sloperider.SlopeRider;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new SlopeRider()
            .configuration(new Configuration() {
                @Override
                public boolean debug() {
                    return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                }
            })
            .io(new PersistentIO() {
                SharedPreferences sharedPreferences;
                SharedPreferences.Editor sharedPreferencesEditor;

                private SharedPreferences sharedPreferences() {
                    if (sharedPreferences == null) {
                        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    }

                    return sharedPreferences;
                }

                private SharedPreferences.Editor editor() {
                    if (sharedPreferencesEditor == null) {
                        sharedPreferencesEditor = sharedPreferences().edit();
                    }

                    return sharedPreferencesEditor;
                }

                @Override
                public int readInt(String key, int defaultValue) {
                    return sharedPreferences().getInt(key, defaultValue);
                }

                @Override
                public void beginWrite() {
                }

                @Override
                public void writeInt(String key, final int value) {
                    editor().putInt(key, value);
                }

                @Override
                public void endWrite() {
                    editor().commit();
                }

                @Override
                public boolean readBoolean(String key, boolean defaultValue) {
                    return sharedPreferences().getBoolean(key, defaultValue);
                }

                @Override
                public void writeBoolean(String key, boolean value) {
                    editor().putBoolean(key, value);
                }

                @Override
                public String readString(String key, String defaultValue) {
                    return sharedPreferences().getString(key, defaultValue);
                }

                @Override
                public void writeString(String key, String value) {
                    editor().putString(key, value);
                }
            }), config);
	}
}
