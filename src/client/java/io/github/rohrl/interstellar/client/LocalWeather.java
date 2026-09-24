package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.mixin.client.WorldRendererAccessor;
import net.minecraft.client.MinecraftClient;
import org.joml.Quaternionf;

/** Cheap foreground approximation: native precipitation within three columns of
 * the camera, with native biome/roof checks and straight-world depth occlusion. */
public final class LocalWeather {
    public static boolean drawing;
    private LocalWeather() {}
    public static void render() {
        var client=MinecraftClient.getInstance();float delta=client.getRenderTickCounter().getTickDelta(false);
        if(!WorldFeatures.weather || client.world==null || client.world.getRainGradient(delta)<=0)return;
        var camera=client.gameRenderer.getCamera();var pos=camera.getPos();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();
        try {
            drawing=true;view.mul(new org.joml.Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf())));RenderSystem.applyModelViewMatrix();
            ((WorldRendererAccessor)client.worldRenderer).interstellar$weather(client.gameRenderer.getLightmapTextureManager(),delta,pos.x,pos.y,pos.z);
        } finally {
            drawing=false;view.popMatrix();RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
    }
}
