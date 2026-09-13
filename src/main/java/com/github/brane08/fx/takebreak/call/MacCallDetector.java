package com.github.brane08.fx.takebreak.call;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public final class MacCallDetector implements CallDetector {

    private static final long TIMEOUT_SECONDS = 2;
    private static final double CPU_THRESHOLD_PERCENT = 3.0;

    @Override
    public boolean isCallActive() {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "-A", "-o", "%cpu,comm=");
            pb.redirectErrorStream(true);
            p = pb.start();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Thread reader = startDrainThread(p.getInputStream(), buffer);

            boolean exited = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reader.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            if (!exited) {
                return false; // ps hung — never skip breaks
            }

            String out = buffer.toString();
            for (String line : out.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int split = trimmed.indexOf(' ');
                if (split < 0) {
                    continue;
                }
                String cpuToken = trimmed.substring(0, split);
                String comm = trimmed.substring(split + 1).trim();
                if (!comm.contains("VDCAssistant") && !comm.contains("AppleCameraAssistant")) {
                    continue;
                }
                try {
                    double cpu = Double.parseDouble(cpuToken);
                    if (cpu > CPU_THRESHOLD_PERCENT) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // malformed line — skip, not a match
                }
            }
            return false;
        } catch (Exception e) {
            return false; // detection unavailable — never skip breaks
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }

    private static Thread startDrainThread(InputStream in, ByteArrayOutputStream buffer) {
        Thread t = new Thread(() -> {
            try {
                in.transferTo(buffer);
            } catch (Exception e) {
                // stream closed/interrupted — buffer holds whatever was read so far
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
