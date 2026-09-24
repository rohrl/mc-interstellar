package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.LiveTerrain;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    // World-only composite: let vanilla draw hands, item animations and camera overlays once,
    // with its own projection/depth clear, then apply post-processing and HUD as usual.
    @Inject(method="renderWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",shift=At.Shift.AFTER))
    private void interstellar$world(RenderTickCounter ticks,CallbackInfo ci) {
        LiveTerrain.renderWorld();
    }
}
