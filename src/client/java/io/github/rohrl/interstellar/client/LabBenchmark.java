package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import java.util.Arrays;
import java.util.Locale;

/** Opt-in asynchronous timestamp sampling. Never waits for the GPU or changes the FPS cap. */
final class LabBenchmark implements AutoCloseable {
    private final int[] start = new int[8], end = new int[8];
    private final boolean[] pending = new boolean[8];
    private final double[] gpu = new double[300], frame = new double[300];
    private int slot, samples, warmup = 120, active = -1;
    private long previous;
    private final String scene;
    private boolean closed;
    private String status = "Warming up GPU benchmark...";

    LabBenchmark(String scene) {
        this.scene = scene;
        if (!GL.getCapabilities().OpenGL33 && !GL.getCapabilities().GL_ARB_timer_query) {
            closed = true;
            status = "GPU timestamps unavailable (timer query unsupported)";
            return;
        }
        for (int i = 0; i < 8; i++) { start[i] = GL15.glGenQueries(); end[i] = GL15.glGenQueries(); }
        Interstellar.LOGGER.info("Optical benchmark started: {}; GPU {}; driver {}", scene,
                GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));
    }

    void begin() {
        if (closed) return;
        long now = System.nanoTime();
        if (warmup-- > 0) { previous = now; return; }
        double interval = (now - previous) / 1e6;
        previous = now;
        if (pending[slot]) {
            if (GL15.glGetQueryObjecti(end[slot], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) return;
            gpu[samples] = (GL33.glGetQueryObjectui64(end[slot], GL15.GL_QUERY_RESULT)
                    - GL33.glGetQueryObjectui64(start[slot], GL15.GL_QUERY_RESULT)) / 1e6;
            frame[samples++] = interval;
            pending[slot] = false;
            status = "GPU benchmark " + samples + "/300 (B cancels)";
            if (samples == gpu.length) {
                Arrays.sort(gpu); Arrays.sort(frame);
                status = String.format(Locale.ROOT, "Optical GPU p50 %.2f / p95 %.2f ms | logged", gpu[149], gpu[284]);
                Interstellar.LOGGER.info("Optical benchmark completed: {}; 120 warmup frames; 300 samples; GPU pass p50={} p95={} p99={} ms; sampled frame intervals p50={} p95={} p99={} ms (includes cap/vsync; not full-frame GPU time)",
                        scene, gpu[149], gpu[284], gpu[296], frame[149], frame[284], frame[296]);
                close();
                return;
            }
        }
        active = slot;
        GL33.glQueryCounter(start[active], GL33.GL_TIMESTAMP);
    }

    void end() {
        if (active < 0) return;
        GL33.glQueryCounter(end[active], GL33.GL_TIMESTAMP);
        pending[active] = true;
        slot = (active + 1) % 8;
        active = -1;
    }

    String status() { return status; }
    @Override public void close() {
        if (closed) return;
        for (int i = 0; i < 8; i++) { GL15.glDeleteQueries(start[i]); GL15.glDeleteQueries(end[i]); }
        closed = true;
    }
}
