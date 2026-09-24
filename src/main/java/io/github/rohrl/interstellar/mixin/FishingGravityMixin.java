package io.github.rohrl.interstellar.mixin;

import io.github.rohrl.interstellar.gravity.GravityField;
import io.github.rohrl.interstellar.gravity.GravityMotion;
import io.github.rohrl.interstellar.gravity.GravitySources;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
abstract class FishingGravityMixin {
    @Unique private GravityField interstellar$field;
    @Unique private Vec3d interstellar$start;
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void interstellar$pull(CallbackInfo ci) {
        interstellar$field=null;
        var self=(FishingBobberEntity)(Object)this;
        if(!(self.getWorld() instanceof ServerWorld world) || self.getHookedEntity()!=null)return;
        var start=self.getPos();var field=GravitySources.find(world,start,start.add(self.getVelocity()));
        if(field==null)return;
        if(GravityMotion.captured(field,start,start)) {self.discard();ci.cancel();return;}
        interstellar$field=field;interstellar$start=start;
        var a=field.acceleration(start.x,start.y,start.z);
        self.setVelocity(self.getVelocity().add(a.x(),a.y(),a.z()));self.velocityModified=true;
        // Keep native buoyancy, fishing, hook ownership and reel-in logic on their
        // ordinary tick. The swept check below captures contacts after native movement.
    }
    @Inject(method="tick",at=@At("RETURN"))
    private void interstellar$capture(CallbackInfo ci) {
        var self=(FishingBobberEntity)(Object)this;
        if(interstellar$field!=null && !self.isRemoved() && GravityMotion.captured(interstellar$field,interstellar$start,self.getPos()))self.discard();
        interstellar$field=null;
    }
}
