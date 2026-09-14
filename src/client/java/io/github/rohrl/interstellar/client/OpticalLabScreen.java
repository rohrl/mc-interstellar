package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import io.github.rohrl.interstellar.config.OpticalSettings;

/** Controlled sky at infinity with static and freely falling observer frames. */
public final class OpticalLabScreen extends Screen {
    private static ShaderProgram shader;
    private final OpticalSettings settings = OpticalConfig.load();
    private OpticalSettings.Quality quality;
    private LabBenchmark benchmark;
    private boolean validateRays;
    private String validationStatus = "V: check GPU rays (brief pause)";
    private float radius = 8;
    private boolean falling, playing, lookBack;
    private long previousFrame;
    private boolean lensing = true;
    private boolean grid = true;
    private boolean aligned = true;

    public OpticalLabScreen() { super(Text.literal("Interstellar optical lab")); resetSettings(); }
    public static void setShader(ShaderProgram loaded) { shader = loaded; }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double seconds = previousFrame == 0 ? 0 : Math.min(.1, (now-previousFrame)/1e9);
        previousFrame = now;
        if (playing) {
            radius = (float) Math.pow(Math.max(Math.pow(.35, 1.5), Math.pow(radius, 1.5)-1.5*seconds*settings.playbackRate()), 2.0/3);
            if (radius <= .35001f) playing = false;
        }
        context.draw();
        if (shader != null) {
            shader.getUniformOrDefault("Viewport").set((float) client.getWindow().getFramebufferWidth(), (float) client.getWindow().getFramebufferHeight());
            shader.getUniformOrDefault("CameraRadius").set(radius);
            shader.getUniformOrDefault("IntegrationStep").set(quality.step());
            shader.getUniformOrDefault("Lensing").set(lensing ? 1f : 0f);
            shader.getUniformOrDefault("Grid").set(grid ? 1f : 0f);
            shader.getUniformOrDefault("Aligned").set(aligned ? 1f : 0f);
            shader.getUniformOrDefault("Falling").set(falling ? 1f : 0f);
            shader.getUniformOrDefault("LookBack").set(lookBack ? 1f : 0f);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            if (validateRays) {
                validateRays = false;
                io.github.rohrl.interstellar.Interstellar.LOGGER.info("Ray diagnostic quality={}, step={}", quality, quality.step());
                validationStatus = LabRayValidation.run(shader, radius, lensing, falling, lookBack, (double) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight(), this::drawQuad);
            }
            if (benchmark != null) benchmark.begin();
            drawQuad();
            if (benchmark != null) benchmark.end();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            context.fill(0, 0, width, height, 0xFF180C20);
        }
        context.fill(6, 6, Math.min(width - 6, 402), 68, 0xB0101824);
        context.drawTextWithShadow(textRenderer, "INTERSTELLAR | Optical lab", 12, 12, 0xFF88D8FF);
        context.drawTextWithShadow(textRenderer, String.format(Locale.ROOT,
                "%s r/r_s %.3f | %s", falling ? "Free fall" : "Static", radius, playing ? "PLAYING" : "PAUSED"), 12, 24, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Up/Down: distance | Space: lensing | G: grid", 12, 36, 0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer, "A: source alignment | R: reset | Esc: return", 12, 48, 0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer, benchmark == null ? "B / click here: benchmark GPU pass" : benchmark.status(), 12, 72, 0xFF88D8FF);
        context.drawTextWithShadow(textRenderer, validationStatus, 12, 84, 0xFF88D8FF);
        context.drawTextWithShadow(textRenderer, "F: observer | T: fall | L: look back | H: horizon", 12, 96, 0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer, lensing ? "Lensing ON" : "Lensing OFF (flat comparison)", 12, 108, 0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer, radius == 1 ? "At the event horizon" : radius < 1 ?
                (radius <= .35001f ? "Inside horizon | Tour stops here (r/r_s=0.35)" : "Inside the event horizon") : "Outside the event horizon", 12, 120, 0xFFFFD59A);
        context.drawTextWithShadow(textRenderer, "Q: quality " + quality + " | Playback x" + settings.playbackRate(), 12, 132, 0xFFE0E8EF);
        String footer = shader == null ? "Shader unavailable: check the game log"
                : "Illustrative sky | Magenta: unresolved ray";
        context.drawTextWithShadow(textRenderer, footer, 12, height - 16, 0xFFFFD59A);
        // This screen owns its background; vanilla Screen.render would blur the finished lab.
    }

    private void resetSettings() {
        radius=(float)settings.startRadius(); quality=settings.quality();
        lensing=settings.lensing(); grid=settings.grid(); aligned=settings.aligned();
        falling=settings.falling(); lookBack=settings.lookBack(); playing=false;
    }
    private void drawQuad() {
        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(0, 0, 0);
        buffer.vertex(0, client.getWindow().getFramebufferHeight(), 0);
        buffer.vertex(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), 0);
        buffer.vertex(client.getWindow().getFramebufferWidth(), 0, 0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= 12 && x < width - 12 && y >= 68 && y < 84) return keyPressed(GLFW.GLFW_KEY_B, 0, 0);
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_B || key == GLFW.GLFW_KEY_V || key == GLFW.GLFW_KEY_F ||
                key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_H || key == GLFW.GLFW_KEY_Q) playing = false;
        if (key != GLFW.GLFW_KEY_V && key != GLFW.GLFW_KEY_B) validationStatus = "V: check GPU rays (brief pause)";
        if (key == GLFW.GLFW_KEY_B) {
            if (benchmark != null) { benchmark.close(); benchmark = null; }
            else if (shader != null) benchmark = new LabBenchmark(String.format(Locale.ROOT,
                    "%dx%d, r/rs=%.4f, lensing=%s, grid=%s, aligned=%s, falling=%s, lookBack=%s, quality=%s",
                    client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), radius, lensing, grid, aligned, falling, lookBack, quality));
            return true;
        }
        if (benchmark != null) { benchmark.close(); benchmark = null; }
        switch (key) {
            case GLFW.GLFW_KEY_V -> validateRays = true;
            case GLFW.GLFW_KEY_Q -> quality = quality.next();
            case GLFW.GLFW_KEY_F -> { falling = !falling; radius = Math.max(falling ? .35f : 1.05f, radius); }
            case GLFW.GLFW_KEY_T -> { falling = true; playing = !playing; previousFrame = System.nanoTime(); }
            case GLFW.GLFW_KEY_L -> lookBack = !lookBack;
            case GLFW.GLFW_KEY_H -> { falling = true; radius = 1; }
            case GLFW.GLFW_KEY_SPACE -> lensing = !lensing;
            case GLFW.GLFW_KEY_G -> grid = !grid;
            case GLFW.GLFW_KEY_A -> aligned = !aligned;
            case GLFW.GLFW_KEY_UP -> radius = Math.max(falling ? .35f : 1.05f, radius / 1.2f);
            case GLFW.GLFW_KEY_DOWN -> radius = Math.min(64f, radius * 1.2f);
            case GLFW.GLFW_KEY_R -> resetSettings();
            default -> { return super.keyPressed(key, scanCode, modifiers); }
        }
        return true;
    }
    @Override public void removed() { if (benchmark != null) { benchmark.close(); benchmark = null; } }
    @Override protected void init() { if (benchmark != null) { benchmark.close(); benchmark = null; } validationStatus = "V: check GPU rays (brief pause)"; previousFrame = 0; }
    @Override public boolean shouldPause() { return true; }
}
