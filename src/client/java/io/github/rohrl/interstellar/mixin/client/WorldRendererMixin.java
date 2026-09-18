package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.StreamingTerrain;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
    @Inject(method="scheduleChunkRender",at=@At("HEAD"))
    private void interstellar$dirty(int x,int y,int z,boolean important,CallbackInfo ci) {
        StreamingTerrain.dirty(x,z);
    }
}
