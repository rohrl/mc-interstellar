package io.github.rohrl.interstellar.gravity;

import io.github.rohrl.interstellar.source.SourceInspector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Spatial lookup rebuilt only on source revisions; one dominant local field, no metric superposition. */
public final class GravitySources {
    private GravitySources() { }
    public static GravityConfig config=new GravityConfig();
    private record Tile(int x,int y,int z) { }
    private static final class Index {
        long revision=-1;
        final Map<Tile,List<GravityField>> tiles=new HashMap<>();
        final List<GravityField> all=new ArrayList<>();
    }
    private static final Map<ServerWorld,Index> indices=new IdentityHashMap<>();
    public static void register() {
        config=GravityConfig.load();GravityControl.register();GravityVisualPayload.register();ServerLifecycleEvents.SERVER_STOPPED.register(server->indices.clear());
    }
    private static int tile(double coordinate) {return (int)Math.floor(coordinate/16);}
    private static Index index(ServerWorld world) {
        var index=indices.computeIfAbsent(world,key->new Index());long revision=SourceInspector.revision(world);
        if(index.revision==revision)return index;
        index.revision=revision;index.tiles.clear();index.all.clear();
        for(var entry:SourceInspector.entries(world)) {
            if(!entry.ready())continue;
            var r=entry.result;var field=new GravityField(r.x(),r.y(),r.z(),r.count(),r.enclosingRadius(),r.blackHoleProxy()?r.schwarzschildRadius():0,config.strengthPerBlock);
            if(!field.supported())continue;
            index.all.add(field);double reach=field.reach();
            for(int x=tile(r.x()-reach);x<=tile(r.x()+reach);x++)
                for(int y=tile(r.y()-reach);y<=tile(r.y()+reach);y++)
                    for(int z=tile(r.z()-reach);z<=tile(r.z()+reach);z++)
                        index.tiles.computeIfAbsent(new Tile(x,y,z),key->new ArrayList<>()).add(field);
        }
        return index;
    }
    public static GravityField find(ServerWorld world,Vec3d start,Vec3d end) {
        if(!config.enabled||SourceInspector.legacy(world))return null;
        var index=index(world);if(index.all.isEmpty())return null;
        int x0=tile(Math.min(start.x,end.x)),x1=tile(Math.max(start.x,end.x));
        int y0=tile(Math.min(start.y,end.y)),y1=tile(Math.max(start.y,end.y));
        int z0=tile(Math.min(start.z,end.z)),z1=tile(Math.max(start.z,end.z));
        Collection<GravityField> candidates;
        if(x0==x1&&y0==y1&&z0==z1)candidates=index.tiles.getOrDefault(new Tile(x0,y0,z0),List.of());
        else if((double)(x1-x0+1)*(y1-y0+1)*(z1-z0+1)>64)candidates=index.all;
        else {
            var found=new HashSet<GravityField>();
            for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)
                found.addAll(index.tiles.getOrDefault(new Tile(x,y,z),List.of()));
            candidates=found;
        }
        GravityField best=null;double score=0;
        for(var field:candidates) {
            double distance=field.closestDistanceSquared(start.x,start.y,start.z,end.x,end.y,end.z);
            if(distance>field.reach()*field.reach())continue;
            double candidate=field.count()/(distance+field.bodyRadius()*field.bodyRadius());
            if(candidate>score) {score=candidate;best=field;}
        }
        return best;
    }
}
