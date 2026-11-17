package com.github.brane08.fx.takebreak.idle;

public final class IdleDetectorFactory {

    private IdleDetectorFactory() {}

    public static IdleDetector create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return new MacIdleDetector();
        if (os.contains("win")) return new WindowsIdleDetector();
        return new LinuxIdleDetector();
    }
}
