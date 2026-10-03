package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.DiskAtmosphere;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
abstract class MinecraftMusicMixin {
    @Inject(method="getMusicType",at=@At("HEAD"),cancellable=true)
    private void interstellar$music(CallbackInfoReturnable<MusicSound> ci) {
        var music=DiskAtmosphere.music();if(music!=null)ci.setReturnValue(music);
    }
}
