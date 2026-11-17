package com.github.brane08.fx.takebreak.idle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdleDetectorFactoryTest {

    @Test
    void factoryReturnsNonNull() {
        assertNotNull(IdleDetectorFactory.create());
    }

    @Test
    void factoryReturnsPlatformCorrectType() {
        IdleDetector detector = IdleDetectorFactory.create();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            assertInstanceOf(MacIdleDetector.class, detector);
        } else if (os.contains("win")) {
            assertInstanceOf(WindowsIdleDetector.class, detector);
        } else {
            assertInstanceOf(LinuxIdleDetector.class, detector);
        }
    }

    @Test
    void linuxDetectorReturnsFallbackWhenXprintidleAbsent() {
        LinuxIdleDetector detector = new LinuxIdleDetector();
        long result = detector.getIdleSeconds();
        assertTrue(result >= 0, "Idle seconds must be non-negative");
    }
}
