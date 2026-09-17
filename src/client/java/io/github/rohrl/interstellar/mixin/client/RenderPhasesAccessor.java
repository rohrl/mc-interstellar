package io.github.rohrl.interstellar.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public interface RenderPhasesAccessor {
    @Accessor("texture") RenderPhase.TextureBase interstellar$texture();
    @Accessor("program") RenderPhase.ShaderProgram interstellar$program();
    @Accessor("cull") RenderPhase.Cull interstellar$cull();
}
