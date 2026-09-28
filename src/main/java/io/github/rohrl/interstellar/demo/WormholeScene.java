package io.github.rohrl.interstellar.demo;

import io.github.rohrl.interstellar.wormhole.WormholePair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import java.util.LinkedHashMap;
import java.util.Map;

/** Two visibly distinct, real block-built surroundings, 1145 blocks apart. */
final class WormholeScene {
    static Map<BlockPos,BlockState> blocks() {
        var blocks=new LinkedHashMap<BlockPos,BlockState>();
        for(int end=0;end<2;end++) {
            var c=BlockPos.ofFloored(WormholePair.centre(end));
            var ground=(end==0?Blocks.GRASS_BLOCK:Blocks.SMOOTH_SANDSTONE).getDefaultState();
            var accent=(end==0?Blocks.ORANGE_CONCRETE:Blocks.CYAN_CONCRETE).getDefaultState();
            var light=Blocks.SEA_LANTERN.getDefaultState();
            for(int x=-44;x<=44;x++)for(int z=-44;z<=44;z++) {
                var floor=ground;
                if(Math.abs(x)<=3)floor=((z/4)&1)==0?accent:Blocks.SMOOTH_QUARTZ.getDefaultState();
                if(Math.abs(x)==44||Math.abs(z)==44)floor=accent;
                blocks.put(c.add(x,-12,z),floor);
            }
            // Far striped wall, foreground pillars and separate thin surfaces make
            // finite-distance parallax, handedness and occlusion easy to inspect.
            for(int x=-28;x<=28;x++)for(int y=-11;y<=18;y++) {
                var colour=(x/4&1)==0?accent:Blocks.WHITE_CONCRETE.getDefaultState();
                blocks.put(c.add(x,y,36),colour);
            }
            for(int x:new int[]{-22,17})for(int y=-11;y<=10;y++) {
                blocks.put(c.add(x,y,-17),y==10?light:accent);
                blocks.put(c.add(x,y,22),y==10?light:Blocks.QUARTZ_PILLAR.getDefaultState());
            }
            for(int x=-23;x<=-17;x++)for(int y=-8;y<=8;y++)
                blocks.put(c.add(x,y,12),(end==0?Blocks.ORANGE_STAINED_GLASS:Blocks.CYAN_STAINED_GLASS).getDefaultState());
            // Discrete markers surround, without covering, the coordinate throat (r=8).
            for(int degrees=0;degrees<360;degrees+=15) {
                double angle=Math.toRadians(degrees);
                blocks.put(c.add((int)Math.round(11*Math.cos(angle)),(int)Math.round(11*Math.sin(angle)),0),light);
            }
            // An asymmetric letter-like landmark distinguishes rotated from mirrored views.
            for(int y=-11;y<=5;y++)blocks.put(c.add(28,y,-28),accent);
            for(int x=28;x<=35;x++) {
                blocks.put(c.add(x,5,-28),accent);blocks.put(c.add(x,-2,-28),accent);
            }
            blocks.put(c.add(9,-11,-27),Blocks.CHEST.getDefaultState());
        }
        return blocks;
    }
}
