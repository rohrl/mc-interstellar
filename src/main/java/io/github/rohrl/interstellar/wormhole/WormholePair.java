package io.github.rohrl.interstellar.wormhole;

import io.github.rohrl.interstellar.science.EllisWormhole;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.*;

/** One server-owned pair, with an independent immutable copy in each client world. */
public final class WormholePair {
    public static final RegistryKey<World> WORLD=RegistryKey.of(RegistryKeys.WORLD,Identifier.of("interstellar","wormholes"));
    public static final EllisWormhole METRIC=new EllisWormhole(16);
    public static final Vec3d A=new Vec3d(8,96,8),B=new Vec3d(1032,112,520);
    // Both exhibits fit within 3 chunks of their centre. Two extra rings provide
    // native neighbour geometry/lighting and a safe arrival area: 242 chunks total.
    public static final int CHUNK_RADIUS=5;
    private record ClientLayout(World world,Layout layout) {}
    private static volatile ClientLayout client=new ClientLayout(null,null);
    public record Layout(RegistryKey<World> dimension,List<Vec3d> mouths,long revision,boolean demo,List<ChunkPos> chunks) {
        public Layout(RegistryKey<World> dimension,List<Vec3d> mouths,long revision,boolean demo) {
            this(dimension,List.copyOf(mouths),revision,demo,regions(mouths));
        }
        public boolean active(World world) {return world!=null && mouths.size()==2 && world.getRegistryKey().equals(dimension);}
        public boolean contains(int x,int z) {return mouths.size()==2 && (WormholePair.contains(mouths.get(0),x,z)||WormholePair.contains(mouths.get(1),x,z));}
    }
    public static final Layout EMPTY=new Layout(World.OVERWORLD,List.of(),0,false);
    private WormholePair() {}
    public static Layout layout(World world) {
        if(world==null)return EMPTY;
        if(!world.isClient)return WormholeState.get(world.getServer()).layout;
        var copy=client;return copy.world==world?copy.layout:EMPTY;
    }
    public static void clientLayout(World world,Layout layout) {client=new ClientLayout(world,layout);}
    public static boolean active(World world) {return layout(world).active(world);}
    public static List<ChunkPos> chunks(World world) {return layout(world).chunks();}
    public static Vec3d centre(World world,int end) {return layout(world).mouths().get(end);}
    public static int nearest(World world,Vec3d p) {return p.squaredDistanceTo(centre(world,0))<=p.squaredDistanceTo(centre(world,1))?0:1;}
    public static boolean contains(World world,int x,int z) {return active(world)&&layout(world).contains(x,z);}
    private static boolean contains(Vec3d centre,int x,int z) {
        return Math.abs((long)x-((int)Math.floor(centre.x)>>4))<=CHUNK_RADIUS && Math.abs((long)z-((int)Math.floor(centre.z)>>4))<=CHUNK_RADIUS;
    }
    private static List<ChunkPos> regions(List<Vec3d> mouths) {
        if(mouths.size()!=2)return List.of();
        var result=new LinkedHashSet<ChunkPos>();
        // Interleave equally distant rings at both ends, preparing the two mouths together.
        for(int ring=0;ring<=CHUNK_RADIUS;ring++)for(int end=0;end<2;end++)
            for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++)if(Math.max(Math.abs(x),Math.abs(z))==ring)
                result.add(new ChunkPos(((int)Math.floor(mouths.get(end).x)>>4)+x,((int)Math.floor(mouths.get(end).z)>>4)+z));
        return List.copyOf(result);
    }
    public static Vec3d transfer(World world,int from,Vec3d position) {
        var v=position.subtract(centre(world,from));var p=METRIC.transfer(new double[]{v.x,v.y,v.z});
        return centre(world,1-from).add(p[0],p[1],p[2]);
    }
    public static Vec3d transferVector(World world,int from,Vec3d position,Vec3d vector) {
        var v=position.subtract(centre(world,from));var p=METRIC.transferVector(new double[]{v.x,v.y,v.z},new double[]{vector.x,vector.y,vector.z});
        return new Vec3d(p[0],p[1],p[2]);
    }
}
