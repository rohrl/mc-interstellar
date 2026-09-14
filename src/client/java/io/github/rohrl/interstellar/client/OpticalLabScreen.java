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

/** Controlled exterior sky scene, not Minecraft terrain and not a horizon-crossing camera. */
public final class OpticalLabScreen extends Screen {
    private static ShaderProgram shader;
    private float radius = 8;
    private boolean lensing = true;
    private boolean grid = true;
    private boolean aligned = true;

    public OpticalLabScreen() { super(Text.literal("Interstellar optical lab")); }
    public static void setShader(ShaderProgram loaded) { shader = loaded; }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.draw();
        if (shader != null) {
            shader.getUniformOrDefault("Viewport").set((float) width, (float) height);
            shader.getUniformOrDefault("CameraRadius").set(radius);
            shader.getUniformOrDefault("Lensing").set(lensing ? 1f : 0f);
            shader.getUniformOrDefault("Grid").set(grid ? 1f : 0f);
            shader.getUniformOrDefault("Aligned").set(aligned ? 1f : 0f);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            buffer.vertex(0, 0, 0);
            buffer.vertex(0, height, 0);
            buffer.vertex(width, height, 0);
            buffer.vertex(width, 0, 0);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            context.fill(0, 0, width, height, 0xFF180C20);
        }
        context.fill(6, 6, Math.min(width - 6, 402), 68, 0xB0101824);
        context.drawTextWithShadow(textRenderer, "INTERSTELLAR | Exterior optical lab", 12, 12, 0xFF88D8FF);
        context.drawTextWithShadow(textRenderer, String.format(Locale.ROOT,
                "Static observer r/r_s %.2f | Lensing %s", radius, lensing ? "ON" : "OFF"), 12, 24, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Up/Down: distance | Space: lensing | G: grid", 12, 36, 0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer, "A: source alignment | R: reset | Esc: return", 12, 48, 0xFFE0E8EF);
        String footer = shader == null ? "Shader unavailable: check the game log"
                : "Test sky, not terrain | Exterior only | Magenta = unresolved ray";
        context.drawTextWithShadow(textRenderer, footer, 12, height - 16, 0xFFFFD59A);
        // This screen owns its background; vanilla Screen.render would blur the finished lab.
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_SPACE -> lensing = !lensing;
            case GLFW.GLFW_KEY_G -> grid = !grid;
            case GLFW.GLFW_KEY_A -> aligned = !aligned;
            case GLFW.GLFW_KEY_UP -> radius = Math.max(1.05f, radius / 1.2f);
            case GLFW.GLFW_KEY_DOWN -> radius = Math.min(64f, radius * 1.2f);
            case GLFW.GLFW_KEY_R -> { radius = 8; lensing = true; grid = true; aligned = true; }
            default -> { return super.keyPressed(key, scanCode, modifiers); }
        }
        return true;
    }
    @Override public boolean shouldPause() { return true; }
}
