package io.github.rohrl.interstellar.mixin.client;

import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface CloudRendererAccessor {
    @Accessor("ticks") int interstellar$ticks();
    @Accessor("lastCloudsColor") Vec3d interstellar$cloudColour();
    @Invoker("buildCloudsBuffer") BuiltBuffer interstellar$buildClouds(Tessellator tessellator,double x,double y,double z,Vec3d colour);
}
