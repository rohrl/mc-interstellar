package io.github.rohrl.interstellar.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.render.LightmapTextureManager;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("blockBreakingProgressions") Long2ObjectMap<SortedSet<BlockBreakingInfo>> interstellar$breaking();
    @Invoker("renderWeather") void interstellar$weather(LightmapTextureManager light,float delta,double x,double y,double z);
}
