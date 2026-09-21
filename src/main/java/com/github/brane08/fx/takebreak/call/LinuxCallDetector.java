package com.github.brane08.fx.takebreak.call;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class LinuxCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 2;

    // Session daemons keep /dev/video* open for device monitoring without any call in progress.
    private static final Set<String> IDLE_HOLDERS = Set.of(
            "wireplumber", "pipewire", "pipewire-media-session", "pipewire-pulse", "systemd", "udevd");

    @Override
    public boolean isCallActive() {
        return cameraInUse() || microphoneInUse();
    }

    private boolean cameraInUse() {
        File[] videoDevices = new File("/dev").listFiles((dir, name) -> name.startsWith("video"));
        if (videoDevices == null) {
            return false; // no video devices — never skip breaks
        }
        for (File device : videoDevices) {
            String out = run("fuser", "-v", device.getAbsolutePath());
            if (out != null && hasRealHolder(out)) {
                return true;
            }
        }
        return false;
    }

    private boolean microphoneInUse() {
        String out = run("pactl", "list", "source-outputs"); // PulseAudio and PipeWire-pulse
        return out != null && hasActiveCapture(out);
    }

    /**
     * Parses {@code fuser -v} output ({@code [file:] USER PID ACCESS COMMAND}) and reports whether
     * any holder is something other than a known session daemon. Unparseable output counts as "not in use".
     */
    static boolean hasRealHolder(String fuserOutput) {
        for (String line : fuserOutput.split("\\R")) {
            String[] tokens = line.trim().split("\\s+");
            for (int i = 0; i + 2 < tokens.length; i++) {
                // PID immediately followed by the 5-char ACCESS flags (e.g. "F...."), then COMMAND
                if (tokens[i].matches("\\d+") && tokens[i + 1].matches("[.a-zA-Z]{5}")) {
                    String command = tokens[i + 2].toLowerCase(Locale.ROOT);
                    if (!IDLE_HOLDERS.contains(command)) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Parses {@code pactl list source-outputs} and reports whether an uncorked capture stream exists
     * that is not a level meter (e.g. pavucontrol's "Peak detect").
     */
    static boolean hasActiveCapture(String pactlOutput) {
        for (String block : pactlOutput.split("(?m)^Source Output #")) {
            if (block.isBlank()) {
                continue;
            }
            String lower = block.toLowerCase(Locale.ROOT);
            boolean meter = lower.contains("peak detect") || lower.contains("pavucontrol")
                    || lower.contains("volume control");
            if (!meter && lower.contains("corked: no")) {
                return true;
            }
        }
        return false;
    }

    /** Runs a command and returns its combined output, or null if it is missing, hung or interrupted. */
    private static String run(String... command) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            p = pb.start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return null; // hung — never skip breaks
            }
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null; // tool not installed — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
