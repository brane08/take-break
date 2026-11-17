package com.github.brane08.fx.takebreak.idle;

public final class LinuxIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        try {
            Process p = new ProcessBuilder("xprintidle").start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            return Long.parseLong(out) / 1000;
        } catch (Exception e) {
            return 0; // xprintidle not installed — never skip breaks
        }
    }
}
