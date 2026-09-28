package io.github.rohrl.interstellar.mixin.client;

import io.github.rohrl.interstellar.client.WormholeClient;
import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class WormholeCameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f horizontalPlane;
    @Shadow @Final private Vector3f verticalPlane;
    @Shadow @Final private Vector3f diagonalPlane;
    @Inject(method="setRotation",at=@At("TAIL"))
    private void interstellar$roll(float yaw,float pitch,CallbackInfo ci) {
        if((Object)this!=net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera())return;
        float roll=(float)WormholeClient.roll();if(roll==0)return;
        rotation.rotateZ(-roll);
        horizontalPlane.set(0,0,-1).rotate(rotation);
        verticalPlane.set(0,1,0).rotate(rotation);
        diagonalPlane.set(-1,0,0).rotate(rotation);
    }
}
