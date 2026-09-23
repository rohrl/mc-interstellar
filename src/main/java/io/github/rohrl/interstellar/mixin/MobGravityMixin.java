package io.github.rohrl.interstellar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.rohrl.interstellar.gravity.GravityMotion;
import io.github.rohrl.interstellar.gravity.GravityControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
abstract class MobGravityMixin {
    @Unique private long interstellar$gravityTick=Long.MIN_VALUE;
    @WrapMethod(method="move")
    private void interstellar$move(MovementType kind,Vec3d movement,Operation<Void> original) {
        Entity self=(Entity)(Object)this;
        if(kind!=MovementType.SELF||!(self instanceof MobEntity mob)||!(self.getWorld() instanceof ServerWorld world)
                ||interstellar$gravityTick==world.getTime()) {original.call(kind,movement);return;}
        // Hook physical movement so flying/swimming travel overrides also retain their AI/contacts.
        interstellar$gravityTick=world.getTime();
        long started=GravityControl.begin();
        try {GravityMotion.mob(mob,movement,delta->original.call(kind,delta));}
        finally {GravityControl.end(1,started);}
    }
}
