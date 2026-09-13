package com.github.brane08.fx.takebreak.call;

public final class CallDetectorFactory {

    private CallDetectorFactory() {}

    public static CallDetector create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return new MacCallDetector();
        if (os.contains("win")) return new WindowsCallDetector();
        return new LinuxCallDetector();
    }
}
