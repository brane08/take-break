package com.github.brane08.fx.takebreak.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallDetectorParsingTest {

    private static final String KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\webcam";

    @Test
    void windowsActiveSubkeyIsInUse() {
        String out = KEY + "\\NonPackaged\\C:#zoom.exe\r\n"
                + "    Value    REG_SZ    Allow\r\n"
                + "    LastUsedTimeStart    REG_QWORD    0x1dbb5a1c2d3e4f0\r\n"
                + "    LastUsedTimeStop    REG_QWORD    0x0\r\n";
        assertTrue(WindowsCallDetector.anySubkeyInUse(out));
    }

    @Test
    void windowsStoppedSubkeyIsNotInUse() {
        String out = KEY + "\\NonPackaged\\C:#zoom.exe\r\n"
                + "    LastUsedTimeStart    REG_QWORD    0x1dbb5a1c2d3e4f0\r\n"
                + "    LastUsedTimeStop    REG_QWORD    0x1dbb5a2aaaaaaaa\r\n";
        assertFalse(WindowsCallDetector.anySubkeyInUse(out));
    }

    @Test
    void windowsNeverUsedSubkeyIsNotInUse() {
        String out = KEY + "\\NonPackaged\\C:#never.exe\r\n"
                + "    LastUsedTimeStart    REG_QWORD    0x0\r\n"
                + "    LastUsedTimeStop    REG_QWORD    0x0\r\n";
        assertFalse(WindowsCallDetector.anySubkeyInUse(out));
    }

    @Test
    void windowsValuesDoNotLeakAcrossSubkeys() {
        String out = KEY + "\\NonPackaged\\C:#used.exe\r\n"
                + "    LastUsedTimeStart    REG_QWORD    0x1dbb5a1c2d3e4f0\r\n"
                + KEY + "\\NonPackaged\\C:#never.exe\r\n"
                + "    LastUsedTimeStop    REG_QWORD    0x0\r\n";
        assertFalse(WindowsCallDetector.anySubkeyInUse(out));
    }

    @Test
    void linuxCameraHeldByPipewireDaemonIsNotACall() {
        String out = "                     USER        PID ACCESS COMMAND\n"
                + "/dev/video0:         alice      1234 F.... wireplumber\n";
        assertFalse(LinuxCallDetector.hasRealHolder(out));
    }

    @Test
    void linuxCameraHeldByApplicationIsACall() {
        String out = "                     USER        PID ACCESS COMMAND\n"
                + "/dev/video0:         alice      1234 F.... wireplumber\n"
                + "                     alice      5678 F.... zoom\n";
        assertTrue(LinuxCallDetector.hasRealHolder(out));
    }

    @Test
    void linuxNumericUidIsNotMistakenForPid() {
        String out = "/dev/video0:         1000       5678 F.... firefox\n";
        assertTrue(LinuxCallDetector.hasRealHolder(out));
    }

    @Test
    void linuxEmptyOrUnparseableFuserOutputIsNotACall() {
        assertFalse(LinuxCallDetector.hasRealHolder(""));
        assertFalse(LinuxCallDetector.hasRealHolder("1234\n"));
    }

    @Test
    void linuxActiveCaptureStreamIsACall() {
        String out = "Source Output #42\n\tDriver: PipeWire\n\tCorked: no\n\tSource: 1\n"
                + "\tProperties:\n\t\tapplication.name = \"Firefox\"\n";
        assertTrue(LinuxCallDetector.hasActiveCapture(out));
    }

    @Test
    void linuxCorkedOrMeterStreamsAreNotACall() {
        String corked = "Source Output #42\n\tCorked: yes\n\tProperties:\n\t\tapplication.name = \"Firefox\"\n";
        String meter = "Source Output #43\n\tCorked: no\n\tProperties:\n\t\tmedia.name = \"Peak detect\"\n";
        assertFalse(LinuxCallDetector.hasActiveCapture(corked));
        assertFalse(LinuxCallDetector.hasActiveCapture(meter));
        assertFalse(LinuxCallDetector.hasActiveCapture(""));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void macNativeQueriesDoNotThrow() {
        assertDoesNotThrow(MacCallDetector::microphoneInUse);
        assertDoesNotThrow(MacCallDetector::cameraInUse);
    }
}
