package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.MinecraftClient;
import java.io.BufferedWriter;
import java.nio.file.*;
import java.util.Arrays;

/** Opt-in walking-refresh timings; no GPU query or readback is added. */
public final class RefreshProfile {
    public static final boolean ENABLED=System.getProperty("interstellar.refreshProfile")!=null;
    public static final int CAPTURE=0,PACK=1,TREE=2,PUBLISH=3,READ=4,TRANSFER=5,BLAS=6,WAIT=7;
    private static final long[] stages=new long[8];
    private static BufferedWriter output;
    private static BufferedWriter columns;
    private static final java.util.Map<Long,Long> sectionChanges=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long,Long> columnVersions=new java.util.HashMap<>();
    private static long previous,start,wall,frames;
    private static double interval;
    private static int queue,published,cancelled;
    private static boolean failed;
    private static java.util.Set<String> experiments;
    private static long nextControl;
    private static String previousControl="";
    private static long workCredit,captureLimit=5_000_000L;
    static long captureBudget(){return captureLimit;}
    public static void lightSection(int x,int y,int z) {
        if(ENABLED)sectionChanges.merge(net.minecraft.util.math.ChunkPos.toLong(x,z),1L<<(y+32),(a,b)->a|b);
    }
    static void column(long key,long version,boolean existing) {
        if(!ENABLED || failed)return;
        try {
            if(columns==null) {
                Path path=Path.of(System.getProperty("interstellar.refreshProfile")+".columns.csv");
                if(path.getParent()!=null)Files.createDirectories(path.getParent());
                columns=Files.newBufferedWriter(path);columns.write("wall,key,existing,content,sections\n");
                Runtime.getRuntime().addShutdownHook(new Thread(()->{try{columns.close();}catch(Exception ignored){}},"Interstellar column profile close"));
            }
            Long old=columnVersions.put(key,version),mask=sectionChanges.remove(key);
            columns.write(System.currentTimeMillis()+","+key+","+existing+","+(old==null||old!=version)+","+(mask==null?0:Long.bitCount(mask))+"\n");
            columns.flush();
        }catch(Exception e){failed=true;Interstellar.LOGGER.warn("Column profiling disabled",e);}
    }
    /** Developer-only A/B switches, read only when an explicit control path is supplied. */
    public static boolean experiment(String name,boolean fallback){return experiments==null?fallback:experiments.contains(name);}
    private RefreshProfile() {}
    public static long start(){return ENABLED?System.nanoTime():0;}
    public static void add(int stage,long nanos){if(ENABLED)stages[stage]+=nanos;}
    public static void end(int stage,long from){if(ENABLED)stages[stage]+=System.nanoTime()-from;}
    static void beginFrame() {
        if(!ENABLED)return;
        long now=System.nanoTime();interval=previous==0?0:(now-previous)/1e6;previous=now;start=now;wall=System.currentTimeMillis();
        if(now>=nextControl) {
            nextControl=now+1_000_000_000L;
            String control=System.getProperty("interstellar.refreshControl");
            if(control!=null)try {
                String text=Files.readString(Path.of(control)).trim();
                if(!text.equals(previousControl)) {
                    experiments=text.equals("default")?null:new java.util.HashSet<>(java.util.List.of(text.split("\\s+")));
                    previousControl=text;Interstellar.LOGGER.info("Refresh experiment: {}",text);
                }
            }catch(java.io.IOException ignored){}
        }
        if(experiment("budget",false) && MinecraftClient.getInstance().currentScreen==null) {
            long spent=stages[CAPTURE]+stages[PUBLISH]+stages[READ]+stages[TRANSFER]+stages[BLAS];
            if(!experiment("worker",false))spent+=stages[PACK]+stages[TREE];
            workCredit=Math.min(5_000_000L,workCredit+5_000_000L-spent);captureLimit=Math.max(0,workCredit);
        } else {workCredit=0;captureLimit=5_000_000L;}
        Arrays.fill(stages,0);published=cancelled=0;
    }
    static void published(){if(ENABLED)published++;}
    static void cancelled(){if(ENABLED)cancelled++;}
    static void queued(int count){if(ENABLED)queue=count;}
    static void endFrame() {
        if(!ENABLED || failed)return;
        long end=System.nanoTime();var client=MinecraftClient.getInstance();var pos=client.player.getPos();
        try {
            if(output==null) {
                Path path=Path.of(System.getProperty("interstellar.refreshProfile"));
                if(path.getParent()!=null)Files.createDirectories(path.getParent());
                output=Files.newBufferedWriter(path);
                output.write("wall,interval,render,capture,pack,tree,publish,read,transfer,blas,wait,queue,published,cancelled,x,y,z,screen\n");
                Runtime.getRuntime().addShutdownHook(new Thread(()->{try{output.close();}catch(Exception ignored){}},"Interstellar profile close"));
            }
            var line=new StringBuilder().append(wall).append(',').append(interval).append(',').append((end-start)/1e6);
            for(long stage:stages)line.append(',').append(stage/1e6);
            line.append(',').append(queue).append(',').append(published).append(',').append(cancelled)
                .append(',').append(pos.x).append(',').append(pos.y).append(',').append(pos.z).append(',')
                .append(client.currentScreen==null?"game":client.currentScreen.getClass().getSimpleName()).append('\n');
            output.write(line.toString());if(++frames%128==0)output.flush();
        } catch(Exception e){failed=true;Interstellar.LOGGER.warn("Refresh profiling disabled after output failure",e);}
    }
}
