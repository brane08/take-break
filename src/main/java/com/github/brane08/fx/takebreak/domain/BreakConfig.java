package com.github.brane08.fx.takebreak.domain;

import java.util.prefs.Preferences;

public record BreakConfig(int smallBreak, int longBreak, int spacing, String version) {

    public int getBreakTime(int instance) {
        return ((instance % 3) == 0) ? longBreak : smallBreak;
    }

    public static BreakConfig fromPrefs() {
        var prefs = Preferences.userRoot().node("fx.takebreak");
        return new BreakConfig(prefs.getInt("small", 1), prefs.getInt("long", 2),
                prefs.getInt("spacing", 1), "1.0");
    }
}
