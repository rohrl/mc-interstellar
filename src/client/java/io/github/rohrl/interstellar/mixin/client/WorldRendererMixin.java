package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.StreamingTerrain;
import io.github.rohrl.interstellar.client.LiveTerrain;
import io.github.rohrl.interstellar.client.LocalWeather;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
    @Redirect(method="renderSky",at=@At(value="INVOKE",target="Lnet/minecraft/client/world/ClientWorld;getStarBrightness(F)F"))
    private float interstellar$starBrightness(net.minecraft.client.world.ClientWorld world,float tickDelta) {
        // Vanilla uses brightness for both RGB and source alpha: sqrt(1.5) gives 50% more light.
        // This also applies to the native sky captures used by OpenGL and RTX lensing.
        return world.getStarBrightness(tickDelta)*1.2247449f;
    }
    @ModifyConstant(method="buildStarsBuffer",constant=@Constant(intValue=1500))
    private int interstellar$starDensity(int count) {return io.github.rohrl.interstellar.client.DiskAtmosphere.starAttempts(count);}
    @ModifyConstant(method="renderWeather",constant={@Constant(intValue=5),@Constant(intValue=10)})
    private int interstellar$weatherRange(int range) {return LocalWeather.drawing?3:range;}
    @Inject(method="drawEntityOutlinesFramebuffer",at=@At("HEAD"),cancellable=true)
    private void interstellar$outlines(CallbackInfo ci) {
        // These outlines follow straight vanilla rays. Do not overlay them at incorrect
        // positions on a curved scene; retain them while loading, paused or switched off.
        if(LiveTerrain.worldComposited())ci.cancel();
    }
    @Inject(method="scheduleChunkRender",at=@At("HEAD"))
    private void interstellar$dirty(int x,int y,int z,boolean important,CallbackInfo ci) {
        StreamingTerrain.lightingDirty(x,z);
    }
    @Inject(method="updateBlock",at=@At("HEAD"))
    private void interstellar$edited(net.minecraft.world.BlockView world,net.minecraft.util.math.BlockPos pos,
                                    net.minecraft.block.BlockState oldState,net.minecraft.block.BlockState newState,int flags,CallbackInfo ci) {
        if(oldState!=newState)StreamingTerrain.edited(world,pos);
    }
}
