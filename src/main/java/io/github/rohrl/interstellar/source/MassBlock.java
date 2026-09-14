package io.github.rohrl.interstellar.source;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MassBlock extends Block {
    public static final MapCodec<MassBlock> CODEC = createCodec(MassBlock::new);
    public MassBlock(Settings settings) { super(settings); }
    @Override public MapCodec<MassBlock> getCodec() { return CODEC; }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                          PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity user)
            SourceInspector.request(server,user,pos);
        return ActionResult.SUCCESS;
    }
    @Override protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState old, boolean notify) {
        super.onBlockAdded(state,world,pos,old,notify);
        if (!old.isOf(this) && world instanceof ServerWorld server) SourceInspector.changed(server);
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState next, boolean moved) {
        if (!next.isOf(this) && world instanceof ServerWorld server) SourceInspector.changed(server);
        super.onStateReplaced(state,world,pos,next,moved);
    }
}
