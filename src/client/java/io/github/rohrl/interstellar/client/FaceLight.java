package io.github.rohrl.interstellar.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

/** Three sky/block light bytes fit exactly in one float's 24-bit integer precision. */
final class FaceLight {
    private static final Direction[] FACES=Direction.values();
    static int capture(ClientWorld world,BlockPos pos,BlockState state,int group,boolean column) {
        int packed=0;
        for(int i=0;i<3;i++) {
            var face=FACES[group*3+i];
            var surface=pos;var surfaceState=state;
            // A distant column's horizontal sides represent the block under its cap.
            if(column && face.getAxis()!=Direction.Axis.Y && !world.getBlockState(pos.down()).isAir()) {
                surface=pos.down();surfaceState=world.getBlockState(surface);
            }
            boolean insetTop=face==Direction.UP && surfaceState.isOf(Blocks.SNOW) && surfaceState.get(SnowBlock.LAYERS)<8;
            var sample=insetTop?surface:surface.offset(face);
            int sky=world.getLightLevel(LightType.SKY,sample);
            int block=Math.max(surfaceState.getLuminance(),world.getLightLevel(LightType.BLOCK,sample));
            packed|=(sky*16+block)<<(i*8);
        }
        return packed;
    }
    private FaceLight() {}
}
