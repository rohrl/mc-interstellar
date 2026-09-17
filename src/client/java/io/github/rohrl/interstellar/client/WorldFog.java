package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.FogShape;

/** Read terrain fog before vanilla clears it for the HUD stage. */
record WorldFog(float start,float end,float cylindrical,float red,float green,float blue,float alpha) {
    private static WorldFog current=new WorldFog(1e6f,1e6f,0,0,0,0,1);
    static void register() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            float[] colour=RenderSystem.getShaderFogColor();
            current=new WorldFog(RenderSystem.getShaderFogStart(),RenderSystem.getShaderFogEnd(),
                    RenderSystem.getShaderFogShape()==FogShape.CYLINDER?1:0,colour[0],colour[1],colour[2],colour[3]);
        });
    }
    static WorldFog current() {return current;}
}
