package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.StreamingTerrain;
import io.github.rohrl.interstellar.client.LiveTerrain;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
    @Inject(method="drawEntityOutlinesFramebuffer",at=@At("HEAD"),cancellable=true)
    private void interstellar$outlines(CallbackInfo ci) {
        // These outlines follow straight vanilla rays. Do not overlay them at incorrect
        // positions on a curved scene; retain them while loading, paused or switched off.
        if(LiveTerrain.worldComposited())ci.cancel();
    }
    @Inject(method="scheduleChunkRender",at=@At("HEAD"))
    private void interstellar$dirty(int x,int y,int z,boolean important,CallbackInfo ci) {
        StreamingTerrain.dirty(x,z);
    }
}
