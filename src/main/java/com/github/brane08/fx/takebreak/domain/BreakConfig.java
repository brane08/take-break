package com.github.brane08.fx.takebreak.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BreakConfig(int smallBreak, int longBreak, int spacing, String version) {

    public static final int DEFAULT_SMALL = 5;
    public static final int DEFAULT_LONG = 10;
    public static final int DEFAULT_SPACING = 10;

    private static final Path CONFIG_FILE = Path.of(
            System.getProperty("user.home"), ".config", "take-break", "config.properties");

    public int getBreakTime(int instance) {
        return ((instance % 3) == 0) ? longBreak : smallBreak;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            var props = new Properties();
            props.setProperty("small", String.valueOf(smallBreak));
            props.setProperty("long", String.valueOf(longBreak));
            props.setProperty("spacing", String.valueOf(spacing));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "take-break configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    public static BreakConfig fromFile() {
        if (!Files.exists(CONFIG_FILE)) {
            BreakConfig defaults = new BreakConfig(DEFAULT_SMALL, DEFAULT_LONG, DEFAULT_SPACING, "1.0");
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
                Integer.parseInt(props.getProperty("small", String.valueOf(DEFAULT_SMALL))),
                Integer.parseInt(props.getProperty("long", String.valueOf(DEFAULT_LONG))),
                Integer.parseInt(props.getProperty("spacing", String.valueOf(DEFAULT_SPACING))),
                "1.0");
    }
}
