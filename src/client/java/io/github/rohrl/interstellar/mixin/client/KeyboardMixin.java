package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.InterstellarClient;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {
    @Inject(method="onKey",at=@At("HEAD"),cancellable=true)
    private void interstellar$timing(long window,int key,int scan,int action,int modifiers,CallbackInfo ci) {
        if(InterstellarClient.handleTimingKey(window,key,scan,action,modifiers))ci.cancel();
    }
}
