package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.config.CalibrationSettings;
import io.github.rohrl.interstellar.science.Schwarzschild;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InterstellarClient implements ClientModInitializer {
    private CalibrationSettings settings;
    private boolean showHud;
    private Vec3d referenceCentre;
    private ClientWorld previousWorld;

    @Override
    public void onInitializeClient() {
        settings = CalibrationConfig.load();
        showHud = settings.hudEnabled();
        KeyBinding toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6,
                "key.categories.interstellar"));
        KeyBinding placeReference = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar.place_reference", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7,
                "key.categories.interstellar"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (previousWorld != client.world) {
                referenceCentre = null;
                previousWorld = client.world;
            }
            while (toggleHud.wasPressed()) {
                showHud = !showHud;
            }
            while (placeReference.wasPressed()) {
                if (client.player != null && client.world != null) {
                    var camera = client.gameRenderer.getCamera();
                    Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
                    referenceCentre = camera.getPos().add(forward.multiply(settings.sourceDistance()));
                    client.player.sendMessage(Text.literal(
                            "Interstellar: virtual reference placed; no mass or lensing is active."), true);
                }
            }
        });
        HudRenderCallback.EVENT.register((context, tickCounter) -> drawHud(context));
    }

    private void drawHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!showHud || client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }
        Schwarzschild source = new Schwarzschild(settings.schwarzschildRadius());
        List<String> lines = new ArrayList<>();
        lines.add("INTERSTELLAR | Calibration");
        lines.add("Optics not implemented | F6 HUD | F7 reference");
        lines.add(String.format(Locale.ROOT, "r_s %.2f | photon sphere %.2f | ISCO %.2f blocks",
                source.horizonRadius(), source.photonSphereRadius(),
                source.innermostStableCircularOrbitRadius()));
        if (referenceCentre == null) {
            lines.add("Press F7 to set a virtual centre ahead of the camera.");
        } else {
            double distance = client.gameRenderer.getCamera().getPos().distanceTo(referenceCentre);
            lines.add(String.format(Locale.ROOT, "Coordinate r/r_s %.3f | r %.2f blocks",
                    distance / source.horizonRadius(), distance));
            lines.add(distance <= source.horizonRadius()
                    ? "Inside reference radius (measurement only)"
                    : "Outside reference radius (measurement only)");
        }
        int width = lines.stream().mapToInt(client.textRenderer::getWidth).max().orElse(0) + 16;
        context.fill(6, 6, 6 + width, 16 + lines.size() * 12, 0xC0101824);
        for (int index = 0; index < lines.size(); index++) {
            context.drawTextWithShadow(client.textRenderer, lines.get(index), 14, 12 + index * 12,
                    index == 0 ? 0xFF88D8FF : 0xFFE0E8EF);
        }
    }
}
