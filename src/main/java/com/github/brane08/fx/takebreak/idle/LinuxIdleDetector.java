package com.github.brane08.fx.takebreak.idle;

import java.util.concurrent.TimeUnit;

public final class LinuxIdleDetector implements IdleDetector {

    private static final long TIMEOUT_SECONDS = 2;

    @Override
    public long getIdleSeconds() {
        Process p = null;
        try {
            p = new ProcessBuilder("xprintidle").start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return 0; // xprintidle hung — never skip breaks
            }
            String out = new String(p.getInputStream().readAllBytes()).trim();
            return Long.parseLong(out) / 1000;
        } catch (Exception e) {
            return 0; // xprintidle not installed — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
