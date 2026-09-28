package io.github.rohrl.interstellar.wormhole;

import io.github.rohrl.interstellar.science.EllisWormhole;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

/** The v1 exhibit has one fixed pair. Only this dimension retains remote chunks. */
public final class WormholePair {
    public static final RegistryKey<World> WORLD=RegistryKey.of(RegistryKeys.WORLD,Identifier.of("interstellar","wormholes"));
    public static final EllisWormhole METRIC=new EllisWormhole(16);
    public static final Vec3d A=new Vec3d(8,96,8),B=new Vec3d(1032,112,520);
    // Both exhibits fit within 3 chunks of their centre. Two extra rings provide
    // native neighbour geometry/lighting and a safe arrival area: 242 chunks total.
    public static final int CHUNK_RADIUS=5;
    public static final List<ChunkPos> CHUNKS=chunks();
    private WormholePair() {}
    public static boolean active(World world) {return world!=null && world.getRegistryKey().equals(WORLD);}
    public static Vec3d centre(int end) {return end==0?A:B;}
    public static int nearest(Vec3d p) {return p.squaredDistanceTo(A)<=p.squaredDistanceTo(B)?0:1;}
    public static boolean contains(int x,int z) {
        return contains(A,x,z)||contains(B,x,z);
    }
    private static boolean contains(Vec3d centre,int x,int z) {
        return Math.abs((long)x-((int)centre.x>>4))<=CHUNK_RADIUS && Math.abs((long)z-((int)centre.z>>4))<=CHUNK_RADIUS;
    }
    private static List<ChunkPos> chunks() {
        var result=new ArrayList<ChunkPos>();
        // Interleave equally distant rings at both ends, preparing the two mouths together.
        for(int ring=0;ring<=CHUNK_RADIUS;ring++)for(int end=0;end<2;end++)
            for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++)if(Math.max(Math.abs(x),Math.abs(z))==ring)
                result.add(new ChunkPos(((int)centre(end).x>>4)+x,((int)centre(end).z>>4)+z));
        return List.copyOf(result);
    }
    public static Vec3d transfer(int from,Vec3d position) {
        var v=position.subtract(centre(from));var p=METRIC.transfer(new double[]{v.x,v.y,v.z});
        return centre(1-from).add(p[0],p[1],p[2]);
    }
    public static Vec3d transferVector(int from,Vec3d position,Vec3d vector) {
        var v=position.subtract(centre(from));var p=METRIC.transferVector(new double[]{v.x,v.y,v.z},new double[]{vector.x,vector.y,vector.z});
        return new Vec3d(p[0],p[1],p[2]);
    }
}
