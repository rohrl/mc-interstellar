package io.github.rohrl.interstellar.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.rohrl.interstellar.client.StringGravity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
abstract class LeashGravityMixin {
    @WrapMethod(method="renderLeash")
    private void interstellar$leash(Entity entity,float delta,MatrixStack matrices,VertexConsumerProvider consumers,Entity holder,Operation<Void> original) {
        var previous=StringGravity.current;
        var offset=entity.getLeashOffset(delta);double yaw=Math.toRadians(entity.lerpYaw(delta))+Math.PI/2;
        var base=entity.getLerpedPos(delta).add(Math.cos(yaw)*offset.z+Math.sin(yaw)*offset.x,offset.y,Math.sin(yaw)*offset.z-Math.cos(yaw)*offset.x);
        StringGravity.current=StringGravity.context(base,holder.getLeashPos(delta).distanceTo(base));
        try {original.call(entity,delta,matrices,consumers,holder);}finally {StringGravity.current=previous;}
    }
    @WrapOperation(method="renderLeashSegment",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/VertexConsumer;vertex(Lorg/joml/Matrix4f;FFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer interstellar$bend(VertexConsumer out,Matrix4f matrix,float x,float y,float z,Operation<VertexConsumer> original,@Local(argsOnly=true,ordinal=4) int segment) {
        Vec3d d=StringGravity.offset(x,y,z,segment/24.0);
        return original.call(out,matrix,x+(float)d.x,y+(float)d.y,z+(float)d.z);
    }
}
