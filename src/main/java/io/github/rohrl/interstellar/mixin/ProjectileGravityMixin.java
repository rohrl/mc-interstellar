package io.github.rohrl.interstellar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.rohrl.interstellar.gravity.ProjectileStep;
import io.github.rohrl.interstellar.gravity.GravityControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import java.util.function.Predicate;

@Mixin({PersistentProjectileEntity.class,ThrownEntity.class})
abstract class ProjectileGravityMixin {
    @Unique private final ProjectileStep interstellar$step=new ProjectileStep();
    @Unique private boolean interstellar$flightStep() {
        // onBlockHit replaces velocity with the displacement to impact. Vanilla then moves
        // to that contact (minus its 0.05-block offset); scaling it again leaves arrows in air.
        return interstellar$step.active&&(!((Object)this instanceof ArrowStateAccessor arrow)||!arrow.interstellar$inGround());
    }
    @WrapMethod(method="tick")
    private void interstellar$tick(Operation<Void> original) {
        Entity self=(Entity)(Object)this;
        if(!(self instanceof ArrowEntity||self instanceof SpectralArrowEntity||self instanceof TridentEntity||self instanceof ThrownEntity)) {original.call();return;}
        long started=self.getWorld().isClient?0:GravityControl.begin();
        try {interstellar$step.run(self,()->original.call(),()->self instanceof ArrowStateAccessor arrow&&arrow.interstellar$inGround());}
        finally {GravityControl.end(2,started);}
    }
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/entity/projectile/ProjectileEntity;tick()V"))
    private void interstellar$baseTick(@Coerce ProjectileEntity self,Operation<Void> original) {
        if(!interstellar$step.active||interstellar$step.index==0)original.call(self);
    }
    @WrapOperation(method="tick",at={
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/PersistentProjectileEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;"),
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/thrown/ThrownEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;")})
    private Vec3d interstellar$displacement(@Coerce Entity self,Operation<Vec3d> original) {
        Vec3d velocity=original.call(self);return interstellar$flightStep()?velocity.multiply(interstellar$step.dt):velocity;
    }
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/util/math/Vec3d;multiply(D)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d interstellar$drag(Vec3d velocity,double drag,Operation<Vec3d> original) {
        return original.call(velocity,interstellar$flightStep()?Math.pow(drag,interstellar$step.dt):drag);
    }
    @WrapOperation(method="tick",at={
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"),
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/thrown/ThrownEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V")})
    private void interstellar$velocity(@Coerce Entity self,Vec3d velocity,Operation<Void> original) {
        original.call(self,interstellar$flightStep()?velocity.multiply(1/interstellar$step.dt):velocity);
    }
    @WrapOperation(method="tick",at={
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/PersistentProjectileEntity;applyGravity()V"),
            @At(value="INVOKE",target="Lnet/minecraft/entity/projectile/thrown/ThrownEntity;applyGravity()V")})
    private void interstellar$ordinaryGravity(@Coerce Entity self,Operation<Void> original) {
        if(!interstellar$flightStep())original.call(self);
        else self.setVelocity(self.getVelocity().add(0,-self.getFinalGravity()*interstellar$step.dt,0));
    }
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"),require=0)
    private HitResult interstellar$thrownCollision(Entity self,Predicate<Entity> canHit,Operation<HitResult> original) {
        if(!interstellar$step.active)return original.call(self,canHit);
        Vec3d velocity=self.getVelocity();self.setVelocity(velocity.multiply(interstellar$step.dt));
        try {return original.call(self,canHit);}finally {self.setVelocity(velocity);}
    }
}
