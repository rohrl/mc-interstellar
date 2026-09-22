package io.github.rohrl.interstellar.demo;

import io.github.rohrl.interstellar.source.SourceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned, deterministic exhibit; only built in the dedicated demo dimension. */
final class DemoScene {
    static final BlockPos ANCHOR=new BlockPos(0,80,0);
    static Map<BlockPos,BlockState> blocks() {
        var blocks=new LinkedHashMap<BlockPos,BlockState>();
        fill(blocks,-40,64,-68,44,64,44,Blocks.GRASS_BLOCK.getDefaultState());
        // Visible route and a stable landing/viewing platform.
        fill(blocks,-2,65,-64,5,65,-16,Blocks.SMOOTH_STONE.getDefaultState());
        fill(blocks,-8,65,-58,11,65,-50,Blocks.SMOOTH_STONE.getDefaultState());
        fill(blocks,0,80,0,3,83,3,SourceBlocks.MASS_BLOCK.getDefaultState());
        var colours=new BlockState[]{Blocks.RED_CONCRETE.getDefaultState(),Blocks.ORANGE_CONCRETE.getDefaultState(),Blocks.YELLOW_CONCRETE.getDefaultState(),Blocks.LIME_CONCRETE.getDefaultState(),Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(),Blocks.BLUE_CONCRETE.getDefaultState(),Blocks.PURPLE_CONCRETE.getDefaultState()};
        for(int stripe=0;stripe<7;stripe++)fill(blocks,-26+stripe*8,65,28,-19+stripe*8,100,28,colours[stripe]);
        // A foreground pillar, thin slabs, native stair models and a stepped terrain mound.
        fill(blocks,15,65,-14,16,88,-13,Blocks.QUARTZ_BLOCK.getDefaultState());
        for(int step=0;step<7;step++)fill(blocks,-31+step,65+step,5+step,-13-step,65+step,23-step,Blocks.GRASS_BLOCK.getDefaultState());
        fill(blocks,22,65,-32,32,65,-25,Blocks.STONE_SLAB.getDefaultState());
        for(int x=23;x<31;x++)blocks.put(new BlockPos(x,65,-24),Blocks.OAK_STAIRS.getDefaultState());
        fill(blocks,-28,65,-28,-28,71,-28,Blocks.OAK_LOG.getDefaultState());
        fill(blocks,-30,72,-30,-26,74,-26,Blocks.OAK_LEAVES.getDefaultState().with(net.minecraft.state.property.Properties.PERSISTENT,true));
        return blocks;
    }
    private static void fill(Map<BlockPos,BlockState> blocks,int x0,int y0,int z0,int x1,int y1,int z1,BlockState block) {
        for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)blocks.put(new BlockPos(x,y,z),block);
    }
}
