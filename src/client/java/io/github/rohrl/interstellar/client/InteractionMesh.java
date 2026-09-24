package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.mixin.client.WorldRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Small native interaction overlays, captured into the same curved scene as their blocks. */
final class InteractionMesh {
    private InteractionMesh() {}
    static void capture(EntityMesh target,BlockPos origin) {
        var client=MinecraftClient.getInstance();var world=client.world;
        var camera=client.gameRenderer.getCamera().getPos();
        if(!client.options.hudHidden && client.crosshairTarget instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK) {
            var pos=hit.getBlockPos();var state=world.getBlockState(pos);
            if(!state.isAir() && world.getWorldBorder().contains(pos)) {
                var out=target.solidColour();
                float radius=(float)Math.max(.002,Math.min(.015,camera.distanceTo(Vec3d.ofCenter(pos))*.0009));
                state.getOutlineShape(world,pos,ShapeContext.of(client.player)).forEachEdge((ax,ay,az,bx,by,bz)->
                    line(out,new Vec3d(pos.getX()-origin.getX()+ax,pos.getY()-origin.getY()+ay,pos.getZ()-origin.getZ()+az),
                            new Vec3d(pos.getX()-origin.getX()+bx,pos.getY()-origin.getY()+by,pos.getZ()-origin.getZ()+bz),radius,0,0,0,102));
            }
        }
        var matrices=new MatrixStack();
        for(var entry:((WorldRendererAccessor)client.worldRenderer).interstellar$breaking().long2ObjectEntrySet()) {
            var pos=BlockPos.fromLong(entry.getLongKey());var stages=entry.getValue();
            if(stages.isEmpty() || camera.squaredDistanceTo(Vec3d.of(pos))>1024)continue;
            int stage=stages.last().getStage();if(stage<0 || stage>=10)continue;
            matrices.push();
            try {
                matrices.translate(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ());
                var out=new OverlayVertexConsumer(target.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)),matrices.peek(),1);
                client.getBlockRenderManager().renderDamage(world.getBlockState(pos),pos,world,matrices,out);
            } finally {matrices.pop();}
        }
    }
    static void line(VertexConsumer out,Vec3d a,Vec3d b,double radius,int r,int g,int blue,int alpha) {
        Vec3d direction=b.subtract(a);if(direction.lengthSquared()<1e-16)return;
        Vec3d u=direction.crossProduct(Math.abs(direction.y)<.9*direction.length()?new Vec3d(0,1,0):new Vec3d(1,0,0)).normalize().multiply(radius);
        Vec3d v=direction.normalize().crossProduct(u);
        ribbon(out,a,b,u,r,g,blue,alpha);ribbon(out,a,b,v,r,g,blue,alpha);
    }
    private static void ribbon(VertexConsumer out,Vec3d a,Vec3d b,Vec3d side,int r,int g,int blue,int alpha) {
        for(var p:new Vec3d[]{a.subtract(side),b.subtract(side),b.add(side),a.add(side)})
            out.vertex((float)p.x,(float)p.y,(float)p.z).color(r,g,blue,alpha).texture(0,0).light(240,240);
    }
}
