package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.LiveTerrain;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTerrainScreen.class)
abstract class DownloadingTerrainMixin {
    @Inject(method="close",at=@At("HEAD"),cancellable=true)
    private void interstellar$prepare(CallbackInfo ci) {
        if(LiveTerrain.beginWorldPreparation())ci.cancel();
    }
}
