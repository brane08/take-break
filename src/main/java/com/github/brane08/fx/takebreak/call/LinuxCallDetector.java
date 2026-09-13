package com.github.brane08.fx.takebreak.call;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class LinuxCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 2;

    @Override
    public boolean isCallActive() {
        File[] videoDevices = new File("/dev").listFiles((dir, name) -> name.startsWith("video"));
        if (videoDevices == null) {
            return false; // no video devices — never skip breaks
        }
        for (File device : videoDevices) {
            if (isInUse(device)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInUse(File device) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("fuser", device.getAbsolutePath());
            pb.redirectErrorStream(true);
            p = pb.start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return false; // fuser hung — never skip breaks
            }
            return p.exitValue() == 0; // fuser exits 0 iff a process holds the device open
        } catch (Exception e) {
            return false; // fuser not installed — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
