package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.LiveTerrain;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void interstellar$terrain(DrawContext context,RenderTickCounter ticks,CallbackInfo ci) {
        LiveTerrain.renderHud(context);
    }
}
