package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.wormhole.WormholePair;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Closed-mouth markers before local optics are available, plus the crosshair status. */
final class WormholeAppearance {
    private static final float[] SPHERE=sphere();
    private static final Matrix4f IDENTITY=new Matrix4f();
    private static LabBenchmark benchmark;
    private WormholeAppearance() {}
    static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context->{
            if(!pending())return;
            var camera=context.camera().getPos();
            var mouths=WormholePair.layout(context.world()).mouths();
            if(mouths.stream().noneMatch(centre->camera.squaredDistanceTo(centre)<256*256))return;
            var shader=RenderSystem.getShader();
            boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND);
            if(benchmark!=null)benchmark.begin();
            try {
                RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.disableCull();RenderSystem.disableBlend();
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                var buffer=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
                for(var centre:mouths)emit(buffer,context.matrixStack().peek().getPositionMatrix(),camera,centre);
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } finally {
                if(cull)RenderSystem.enableCull();if(blend)RenderSystem.enableBlend();
                RenderSystem.setShader(()->shader);
                if(benchmark!=null)benchmark.end();
            }
        });
        HudRenderCallback.EVENT.register((context,ticks)->hud(context));
    }
    static void changed() {if(benchmark!=null){benchmark.close();benchmark=null;}}
    static void benchmark() {
        if(benchmark!=null){changed();return;}
        benchmark=new LabBenchmark("Closed wormhole mouth, native sphere draw only");
    }
    static boolean pending() {
        return WormholePair.present(MinecraftClient.getInstance().world) && !WormholeClient.passageOpen();
    }
    static void capture(EntityMesh target,net.minecraft.util.math.BlockPos origin) {
        if(!pending() || WormholeClient.renderingOptics==2)return;
        var client=MinecraftClient.getInstance();var mouths=WormholePair.layout(client.world).mouths();
        int active=WormholePair.nearest(client.world,client.gameRenderer.getCamera().getPos());
        for(int i=0;i<mouths.size();i++)if(WormholeClient.renderingOptics!=1 || i!=active)
            emit(target.solidColour(),IDENTITY,Vec3d.of(origin),mouths.get(i));
    }
    private static void emit(VertexConsumer out,Matrix4f matrix,Vec3d origin,Vec3d centre) {
        var client=MinecraftClient.getInstance();
        var offset=centre.subtract(origin);var view=client.gameRenderer.getCamera().getPos().subtract(centre);
        double radius=WormholeClient.closedRadius();
        for(int i=0;i<SPHERE.length;i+=3) {
            float x=SPHERE[i],y=SPHERE[i+1],z=SPHERE[i+2];
            double vx=view.x-radius*x,vy=view.y-radius*y,vz=view.z-radius*z;
            double facing=Math.abs((x*vx+y*vy+z*vz)/Math.max(1e-6,Math.sqrt(vx*vx+vy*vy+vz*vz)));
            double rim=Math.pow(Math.max(0,1-facing),8);
            out.vertex(matrix,(float)(offset.x+x*radius),(float)(offset.y+y*radius),(float)(offset.z+z*radius))
                .color((int)(3+69*rim),(int)(5+205*rim),(int)(12+225*rim),255);
        }
    }
    private static float[] sphere() {
        int latitudes=32,longitudes=64;var result=new float[latitudes*longitudes*12];int n=0;
        for(int y=0;y<latitudes;y++)for(int x=0;x<longitudes;x++)for(int corner=0;corner<4;corner++) {
            double latitude=Math.PI*(y+(corner>=2?1:0))/latitudes;
            double longitude=2*Math.PI*(x+(corner==1||corner==2?1:0))/longitudes;
            result[n++]=(float)(Math.sin(latitude)*Math.cos(longitude));result[n++]=(float)Math.cos(latitude);
            result[n++]=(float)(Math.sin(latitude)*Math.sin(longitude));
        }
        return result;
    }
    private static void hud(DrawContext context) {
        var client=MinecraftClient.getInstance();var world=client.world;
        if(world==null || client.player==null || client.options.hudHidden || client.currentScreen!=null)return;
        var layout=WormholePair.layout(world);
        if(!layout.dimension().equals(world.getRegistryKey()) || layout.mouths().isEmpty())return;
        var camera=client.gameRenderer.getCamera();var eye=camera.getPos();var direction=new Vec3d(camera.getHorizontalPlane());
        double nearest=96,radius=WormholeClient.passageOpen()?WormholePair.METRIC.mouthRadius():WormholeClient.closedRadius();
        for(var centre:layout.mouths()) {
            var offset=eye.subtract(centre);double b=offset.dotProduct(direction),c=offset.lengthSquared()-radius*radius;
            double discriminant=b*b-c;if(discriminant<0)continue;
            double distance=c<=0?0:-b-Math.sqrt(discriminant);
            if(distance>=0 && distance<nearest)nearest=distance;
        }
        if(nearest>=96)return;
        var surface=eye.add(direction.multiply(nearest));
        if(nearest>0 && world.raycast(new RaycastContext(eye,surface,RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,client.player)).getType()!=HitResult.Type.MISS)return;
        int x=context.getScaledWindowWidth()/2,y=context.getScaledWindowHeight()/2+18;
        boolean closed=layout.mouths().size()==1;
        var title=Text.translatable(closed?"message.interstellar.wormhole_closed":WormholeClient.passageOpen()?
            "message.interstellar.wormhole_open":"message.interstellar.wormhole_preparing",WormholeClient.opening.percent());
        context.drawCenteredTextWithShadow(client.textRenderer,title,x,y,0xFF9EEEF5);
        if(closed)context.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("message.interstellar.wormhole_place_second"),x,y+12,0xFFFFFFFF);
        else if(WormholeClient.passageOpen() && eye.squaredDistanceTo(WormholePair.centre(world,WormholePair.nearest(world,eye)))<radius*radius)
            context.drawCenteredTextWithShadow(client.textRenderer,Text.translatable("message.interstellar.wormhole_step_out"),x,y+12,0xFFFFFFFF);
    }
}
