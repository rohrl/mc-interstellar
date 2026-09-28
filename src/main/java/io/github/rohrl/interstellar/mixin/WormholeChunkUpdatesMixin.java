package io.github.rohrl.interstellar.mixin;

import io.github.rohrl.interstellar.wormhole.WormholeChunks;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkManager.class)
abstract class WormholeChunkUpdatesMixin {
    @Shadow @Final private ServerWorld world;
    @Inject(method="markForUpdate",at=@At("HEAD"))
    private void interstellar$block(BlockPos pos,CallbackInfo ci) {WormholeChunks.dirty(world,pos.getX()>>4,pos.getZ()>>4);}
    @Inject(method="onLightUpdate",at=@At("HEAD"))
    private void interstellar$light(LightType type,ChunkSectionPos pos,CallbackInfo ci) {WormholeChunks.dirty(world,pos.getSectionX(),pos.getSectionZ());}
}
