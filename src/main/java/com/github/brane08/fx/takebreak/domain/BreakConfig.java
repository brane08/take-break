package com.github.brane08.fx.takebreak.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BreakConfig(int smallBreak, int longBreak, int spacing,
                          int warningTime, int idleThreshold) {

    public static final int DEFAULT_SMALL   = 60;
    public static final int DEFAULT_LONG    = 300;
    public static final int DEFAULT_SPACING = 1200;
    public static final int DEFAULT_WARNING = 30;
    public static final int DEFAULT_IDLE    = 300;

    private static final Path CONFIG_FILE;
    static {
        String override = System.getProperty("take-break.config.dir");
        CONFIG_FILE = override != null
                ? Path.of(override, "config.properties")
                : Path.of(System.getProperty("user.home"), ".config", "take-break", "config.properties");
    }

    public int getBreakTime(int instance) {
        return ((instance % 3) == 0) ? longBreak : smallBreak;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            var props = new Properties();
            props.setProperty("small",   String.valueOf(smallBreak));
            props.setProperty("long",    String.valueOf(longBreak));
            props.setProperty("spacing", String.valueOf(spacing));
            props.setProperty("warning", String.valueOf(warningTime));
            props.setProperty("idle",    String.valueOf(idleThreshold));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "take-break configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    public static BreakConfig fromFile() {
        if (!Files.exists(CONFIG_FILE)) {
            BreakConfig defaults = new BreakConfig(DEFAULT_SMALL, DEFAULT_LONG,
                    DEFAULT_SPACING, DEFAULT_WARNING, DEFAULT_IDLE);
            defaults.save();
            return defaults;
        }
        var props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + CONFIG_FILE, e);
        }
        return new BreakConfig(
                Integer.parseInt(props.getProperty("small",   String.valueOf(DEFAULT_SMALL))),
                Integer.parseInt(props.getProperty("long",    String.valueOf(DEFAULT_LONG))),
                Integer.parseInt(props.getProperty("spacing", String.valueOf(DEFAULT_SPACING))),
                Integer.parseInt(props.getProperty("warning", String.valueOf(DEFAULT_WARNING))),
                Integer.parseInt(props.getProperty("idle",    String.valueOf(DEFAULT_IDLE))));
    }
}
