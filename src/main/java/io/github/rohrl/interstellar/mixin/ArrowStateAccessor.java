package io.github.rohrl.interstellar.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentProjectileEntity.class)
public interface ArrowStateAccessor {
    @Accessor("inGround") boolean interstellar$inGround();
}
