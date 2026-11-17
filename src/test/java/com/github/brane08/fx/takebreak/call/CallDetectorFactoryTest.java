package com.github.brane08.fx.takebreak.call;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallDetectorFactoryTest {

    @Test
    void factoryReturnsNonNull() {
        assertNotNull(CallDetectorFactory.create());
    }

    @Test
    void factoryReturnsPlatformCorrectType() {
        CallDetector detector = CallDetectorFactory.create();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            assertInstanceOf(MacCallDetector.class, detector);
        } else if (os.contains("win")) {
            assertInstanceOf(WindowsCallDetector.class, detector);
        } else {
            assertInstanceOf(LinuxCallDetector.class, detector);
        }
    }

    @Test
    void linuxDetectorNeverThrowsWhenToolsAbsent() {
        LinuxCallDetector detector = new LinuxCallDetector();
        assertDoesNotThrow(detector::isCallActive);
    }

    @Test
    void platformDetectorNeverThrowsWithinTimeout() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            assertDoesNotThrow(() -> CallDetectorFactory.create().isCallActive());
        });
    }
}
