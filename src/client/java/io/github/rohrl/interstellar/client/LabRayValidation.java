package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.science.FreeFallRay;
import io.github.rohrl.interstellar.science.Schwarzschild;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

/** Explicit, one-shot blocking diagnostic; never used during normal rendering or timing. */
final class LabRayValidation {
    private static final int WIDTH = 128, HEIGHT = 72;

    static String run(ShaderProgram shader, float radius, boolean lensing, boolean falling, boolean lookBack, double aspect, Runnable draw) {
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousRenderbuffer = GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int framebuffer = GL30.glGenFramebuffers(), color = GL30.glGenRenderbuffers();
        FloatBuffer pixels = BufferUtils.createFloatBuffer(WIDTH * HEIGHT * 4);
        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, color);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_RGBA32F, WIDTH, HEIGHT);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, color);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Floating point validation framebuffer incomplete");
            GL11.glViewport(0, 0, WIDTH, HEIGHT);
            shader.getUniformOrDefault("Diagnostic").set(1f);
            draw.run();
            GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_FLOAT, pixels);
        } finally {
            shader.getUniformOrDefault("Diagnostic").set(0f);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, previousRenderbuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL30.glDeleteRenderbuffers(color);
            GL30.glDeleteFramebuffers(framebuffer);
        }
        int mismatch = 0, analyticMismatch = 0, unresolved = 0, count = 0, invalid = 0;
        double[] errors = new double[WIDTH * HEIGHT];
        double shadow = falling ? Double.NaN : new Schwarzschild(1).staticShadowHalfAngle(radius);
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            double dx = ((x + .5) / WIDTH * 2 - 1) * .7002075382 * aspect;
            double dy = ((y + .5) / HEIGHT * 2 - 1) * .7002075382;
            double mu = (lookBack ? 1 : -1) / Math.sqrt(1 + dx * dx + dy * dy);
                        int expected;
            double cpuAngle;
            if (lensing) {
                double flow = 1 / Math.sqrt(radius);
                var cpu = FreeFallRay.trace(radius, falling ? mu : (mu-flow)/(1-flow*mu), 1e-10);
                expected = switch (cpu.outcome()) { case DARK -> 0; case SKY -> 1; case UNRESOLVED -> 2; };
                cpuAngle = cpu.angle();
            } else {
                expected = 1;
                cpuAngle = Math.acos(mu);
            }
            int offset = (y * WIDTH + x) * 4;
            float cosine = pixels.get(offset), sine = pixels.get(offset + 1), status = pixels.get(offset + 2);
            if (!Float.isFinite(cosine) || !Float.isFinite(sine) || !Float.isFinite(status)
                    || pixels.get(offset + 3) != 1f || (status != 0 && status != 1 && status != 2)) {
                invalid++; continue;
            }

            if (status != expected) mismatch++;
            if (status == 2) unresolved++;
            boolean analyticCapture = lensing && Math.acos(-mu) < shadow;
            if (!falling && (status == 0) != analyticCapture) analyticMismatch++;
            if (status == 1 && expected == 1) {
                double difference = Math.atan2(sine, cosine) - cpuAngle;
                errors[count++] = Math.abs(Math.atan2(Math.sin(difference), Math.cos(difference)));
            }
        }
        Arrays.sort(errors, 0, count);
        double p95 = count == 0 ? Double.NaN : errors[(int) Math.ceil(count * .95) - 1];
        double maximum = count == 0 ? Double.NaN : errors[count - 1];
        Interstellar.LOGGER.info("GPU ray check: {}x{}, aspect={}, r/rs={}, lensing={}, falling={}, lookBack={}; invalid={}, CPU outcome mismatches={}, analytic capture mismatches={}, unresolved={}, escaped compared={}; angular p95={} max={} rad; CPU independent PG Dormand-Prince tol=1e-10; analytic capture count only applies to static mode",
                WIDTH, HEIGHT, aspect, radius, lensing, falling, lookBack, invalid, mismatch, analyticMismatch, unresolved, count, p95, maximum);
        if (count == 0) return "Ray check: " + (mismatch + invalid) + " mismatches | no sky rays in this view";
        return String.format(Locale.ROOT, "Ray check: %d mismatches | max %.3g rad | logged",
                mismatch + invalid + analyticMismatch, maximum);
    }
}
