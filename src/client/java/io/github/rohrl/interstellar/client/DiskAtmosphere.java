package io.github.rohrl.interstellar.client;

import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.mixin.client.WorldRendererAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import java.nio.file.*;

/** Optional local ambience; no particles, server simulation or extra optical rays. */
public final class DiskAtmosphere {
    record Options(int stars,boolean music,boolean gas) {
        Options {if(stars<1||stars>3)throw new IllegalArgumentException("Star density must be 1–3");}
    }
    private static final MusicSound[] SCORES={
        new MusicSound(SoundEvents.MUSIC_NETHER_BASALT_DELTAS,100,1200,true),
        new MusicSound(SoundEvents.MUSIC_NETHER_CRIMSON_FOREST,100,1200,true),
        new MusicSound(SoundEvents.MUSIC_END,100,1200,true)
    };
    private static int scoreIndex=-1;
    private static MusicSound score;
    private static Options options=load();
    private static boolean near;
    private static net.minecraft.client.world.ClientWorld world;
    static Options options(){return options;}
    public static int starAttempts(int vanilla){return vanilla*options.stars();}
    public static MusicSound music(){return near?score:null;}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("interstellar-atmosphere.json");}
    private static Options load() {
        try {if(Files.exists(path()))return java.util.Objects.requireNonNull(new GsonBuilder().create().fromJson(Files.readString(path()),Options.class));}
        catch(Exception failure){Interstellar.LOGGER.warn("Cannot load atmosphere settings; using defaults",failure);}
        return new Options(2,true,true);
    }
    static void apply(Options next) throws java.io.IOException {
        Files.writeString(path(),new GsonBuilder().setPrettyPrinting().create().toJson(next)+"\n");
        boolean stars=next.stars()!=options.stars();options=next;
        var client=MinecraftClient.getInstance();
        if(stars){((WorldRendererAccessor)client.worldRenderer).interstellar$stars();NativeSky.invalidate();}
        tick(client);
    }
    static void tick(MinecraftClient client) {
        if(world!=client.world){if(near)client.getMusicTracker().stop(score);near=false;world=client.world;}
        var source=SelectedSource.current();
        boolean next=options.music()&&client.world!=null&&client.player!=null&&LiveTerrain.worldComposited()
            &&LiveTerrain.preferences().massLensing()&&source!=null&&source.blackHoleProxy();
        if(next) {
            double range=Math.clamp(source.schwarzschildRadius()*8,24,192)*(near?1.25:1);
            next=client.player.getPos().squaredDistanceTo(source.x(),source.y(),source.z())<range*range;
        }
        if(next!=near) {
            near=next;
            if(near){
                // Choose once per approach, not each getMusicType call. Avoid repeating the last selection.
                int choice=java.util.concurrent.ThreadLocalRandom.current().nextInt(SCORES.length-(scoreIndex<0?0:1));
                if(scoreIndex>=0&&choice>=scoreIndex)choice++;
                scoreIndex=choice;score=SCORES[choice];
                client.getMusicTracker().stop();client.getMusicTracker().play(score);
            }
            else client.getMusicTracker().stop(score);
            Interstellar.LOGGER.info("BH ambience music {}",near?"started":"stopped");
        }
    }
    static float gasOpacity() {
        var client=MinecraftClient.getInstance();var source=SelectedSource.current();
        if(!options.gas()||!LiveTerrain.active()||!LiveTerrain.preferences().massLensing()
                ||client.world==null||client.player==null||client.currentScreen!=null||source==null)return 0;
        double rs=AccretionDiskVisuals.radius(source);if(rs<=0)return 0;
        double tilt=Math.toRadians(AccretionDiskVisuals.options().tilt());
        var axis=new Vec3d(Math.sin(tilt),Math.cos(tilt),0);
        var p=client.gameRenderer.getCamera().getPos().subtract(source.x(),source.y(),source.z());
        double height=p.dotProduct(axis),r=Math.sqrt(Math.max(0,p.lengthSquared()-height*height))/rs;
        double thickness=Math.clamp(rs*.045,.2,3),vertical=1-Math.abs(height)/thickness;
        double edge=Math.min((r-3)/.2,(AccretionDiskVisuals.options().outerRadius()-r)/.3);
        if(vertical<=0||edge<=0)return 0;
        double time=AccretionDiskVisuals.options().animation()
            ?(client.world.getTime()+client.getRenderTickCounter().getTickDelta(false))/20.0:0;
        double angle=Math.atan2(-p.z,p.x*Math.cos(tilt)-p.y*Math.sin(tilt));
        double travel=time*60/rs/Math.sqrt(2*r*r*r),phase=angle-travel,phase2=angle-travel*.97;
        double clouds=noise(Math.cos(phase)*r*9,Math.sin(phase)*r*9);
        double filaments=noise(Math.cos(phase2)*r*21+8,Math.sin(phase2)*r*21-5);
        return (float)(Math.min(1,edge)*vertical*vertical*(.025+.155*clouds*clouds)*(.7+.3*filaments));
    }
    private static double noise(double x,double y) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double a=x-ix,b=y-iy;
        a=a*a*(3-2*a);b=b*b*(3-2*b);
        return (hash(ix,iy)*(1-a)+hash(ix+1,iy)*a)*(1-b)+(hash(ix,iy+1)*(1-a)+hash(ix+1,iy+1)*a)*b;
    }
    private static double hash(int x,int y){int h=x*1597334677^y*381201581;h=(h^(h>>>16))*224682251;return (h>>>8)/16777216.0;}
}
