package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.mixin.client.CloudRendererAccessor;
import io.github.rohrl.interstellar.mixin.client.RenderLayerAccessor;
import io.github.rohrl.interstellar.mixin.client.RenderPhasesAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.nio.ByteOrder;

/** Capture vanilla's frozen cloud faces/UV/colour, preserving their native camera-relative transform. */
final class CloudMesh {
    int texture,triangles;
    void capture(WorldMesh mesh,BlockPos origin) {
        var client=MinecraftClient.getInstance();
        var mode=client.options.getCloudRenderModeValue();
        float height=client.world.getDimensionEffects().getCloudsHeight();
        if(mode==CloudRenderMode.OFF || Float.isNaN(height))return;
        var renderer=(CloudRendererAccessor)client.worldRenderer;
        var camera=client.gameRenderer.getCamera().getPos();
        float delta=client.getRenderTickCounter().getTickDelta(false);
        // Match the pinned native caller's arithmetic, including float rounding and periodic wrapping.
        double wind=(renderer.interstellar$ticks()+delta)*.03f;
        double x=(camera.x+wind)/12, y=height-(float)camera.y+.33f, z=camera.z/12+.33f;
        x-=Math.floor(x/2048)*2048;z-=Math.floor(z/2048)*2048;
        float fx=(float)(x-Math.floor(x)),fy=(float)(y/4-Math.floor(y/4))*4,fz=(float)(z-Math.floor(z));
        var phases=(RenderPhasesAccessor)(Object)((RenderLayerAccessor)RenderLayer.getFastClouds()).interstellar$phases();
        float kind=phases.interstellar$cull()==RenderPhase.DISABLE_CULLING?6:5;
        try(var built=renderer.interstellar$buildClouds(Tessellator.getInstance(),x,y,z,renderer.interstellar$cloudColour())) {
            var parameters=built.getDrawParameters();var format=parameters.format();
            if(parameters.mode()!=VertexFormat.DrawMode.QUADS || parameters.vertexCount()%4!=0 || parameters.vertexCount()>100_000)
                throw new IllegalStateException("Unexpected or excessive native cloud geometry");
            var data=built.getBuffer().order(ByteOrder.nativeOrder());
            int stride=format.getVertexSizeByte(),position=format.getOffset(VertexFormatElement.POSITION);
            int uv=format.getOffset(VertexFormatElement.UV_0),colour=format.getOffset(VertexFormatElement.COLOR);
            float[] quad=new float[48];
            for(int first=0;first<parameters.vertexCount();first+=4) {
                for(int v=0;v<4;v++) {
                    int src=(first+v)*stride,dst=v*12;
                    quad[dst]=(float)(camera.x-origin.getX())+(data.getFloat(src+position)-fx)*12;
                    quad[dst+1]=(float)(camera.y-origin.getY())+data.getFloat(src+position+4)+fy;
                    quad[dst+2]=(float)(camera.z-origin.getZ())+(data.getFloat(src+position+8)-fz)*12;
                    quad[dst+3]=kind;
                    quad[dst+4]=data.getFloat(src+uv);quad[dst+5]=data.getFloat(src+uv+4);
                    for(int c=0;c<4;c++)quad[dst+8+c]=(data.get(src+colour+c)&255)/255f;
                }
                mesh.entityQuad(quad,false);triangles+=2;
            }
        }
        texture=client.getTextureManager().getTexture(Identifier.ofVanilla("textures/environment/clouds.png")).getGlId();
        Interstellar.LOGGER.info("Cloud mesh: {} triangles, mode={}, cloud height={}, cull={}",triangles,mode,height,kind==5);
    }
}
