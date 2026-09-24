package io.github.rohrl.interstellar.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.rohrl.interstellar.client.StringGravity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingBobberEntityRenderer.class)
abstract class FishingLineGravityMixin {
    @WrapMethod(method="render(Lnet/minecraft/entity/projectile/FishingBobberEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    private void interstellar$line(FishingBobberEntity entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider consumers,int light,Operation<Void> original) {
        var previous=StringGravity.current;var owner=entity.getPlayerOwner();var base=entity.getLerpedPos(delta);
        StringGravity.current=owner==null?null:StringGravity.context(base,owner.getEyePos().distanceTo(base));
        try {original.call(entity,yaw,delta,matrices,consumers,light);}finally {StringGravity.current=previous;}
    }
    @WrapOperation(method="renderFishingLine",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/VertexConsumer;vertex(Lnet/minecraft/client/util/math/MatrixStack$Entry;FFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer interstellar$bend(VertexConsumer out,MatrixStack.Entry matrix,float x,float y,float z,Operation<VertexConsumer> original,@Local(argsOnly=true,ordinal=3) float along) {
        var d=StringGravity.offset(x,y,z,along);
        return original.call(out,matrix,x+(float)d.x,y+(float)d.y,z+(float)d.z);
    }
}
