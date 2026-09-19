package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.config.CalibrationSettings;
import io.github.rohrl.interstellar.science.Schwarzschild;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
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
        SelectedSource.register();
        StreamingTerrain.register();
        AppearanceCapture.register();
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(Identifier.of("interstellar", "terrain"), VertexFormats.POSITION, TerrainScreen::setShader));
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(Identifier.of("interstellar", "terrain_resolve"), VertexFormats.POSITION, TerrainResolve::setShader));
        KeyBinding terrain = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interstellar.terrain", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, "key.categories.interstellar"));
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                Identifier.of("interstellar", "optical_lab"), VertexFormats.POSITION, OpticalLabScreen::setShader));
        KeyBinding opticalLab = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interstellar.optical_lab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.interstellar"));
        KeyBinding liveTerrain = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interstellar.live_terrain", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, "key.categories.interstellar"));
        KeyBinding liveTiming = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interstellar.live_timing", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.interstellar"));
        settings = CalibrationConfig.load();
        showHud = settings.hudEnabled();
        KeyBinding toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6,
                "key.categories.interstellar"));
        KeyBinding placeReference = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar.place_reference", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7,
                "key.categories.interstellar"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LiveTerrain.tick(client);
            while(liveTerrain.wasPressed()) LiveTerrain.toggle(client);
            while(liveTiming.wasPressed()) LiveTerrain.benchmark();
            while (terrain.wasPressed()) { LiveTerrain.stop(); if (client.world != null) client.setScreen(new TerrainScreen(SelectedSource.current())); }
            while (opticalLab.wasPressed()) {
                LiveTerrain.stop();
                if (client.world != null) client.setScreen(new OpticalLabScreen());
            }
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
        if (LiveTerrain.active() || !showHud || client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }
        Schwarzschild source = new Schwarzschild(settings.schwarzschildRadius());
        List<String> lines = new ArrayList<>();
        lines.add("INTERSTELLAR | Calibration");
        lines.add("F10 live | F9 snapshot | F8 sky | F6 HUD");
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
        var selected=SelectedSource.current();
        if (selected != null) {
            lines.add(String.format(Locale.ROOT,"Selected N=%d | r_s %.3f | %s", selected.count(),
                    selected.schwarzschildRadius(), selected.blackHoleProxy()?"F8 then S: source lab":"extended source"));
        } else if(SelectedSource.state()!=io.github.rohrl.interstellar.source.SourceState.NONE) {
            lines.add(SelectedSource.state().message());
        }
        int width = lines.stream().mapToInt(client.textRenderer::getWidth).max().orElse(0) + 16;
        context.fill(6, 6, 6 + width, 16 + lines.size() * 12, 0xC0101824);
        for (int index = 0; index < lines.size(); index++) {
            context.drawTextWithShadow(client.textRenderer, lines.get(index), 14, 12 + index * 12,
                    index == 0 ? 0xFF88D8FF : 0xFFE0E8EF);
        }
    }
}
