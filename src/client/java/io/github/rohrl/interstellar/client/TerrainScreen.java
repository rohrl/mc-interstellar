package io.github.rohrl.interstellar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.source.SourcePayload;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

/** Shared frozen/live scene-data preview. World geometry is never moved or destroyed. */
final class TerrainScreen extends Screen {
    private static final int MESH_VIEW_RANGE=256,VOXEL_VIEW_RANGE=128;
    private static ShaderProgram generalShader,meshShader,compactMeshShader,defaultMeshShader;
    private static ShaderProgram longMeshShader,longDefaultShader;
    private static ShaderProgram splitShader;
    private static ShaderProgram layoutShader,layoutDiagnosticShader,materialShader;
    private static ShaderProgram materialProbeShader,materialMaskedShader;
    private static ShaderProgram quadProbeShader,quadMaskedShader,quadMaterialShader,quadDiagnosticShader;
    private static final ShaderProgram[] movingShaders=new ShaderProgram[4];
    private static final ShaderProgram[] bodyShaders=new ShaderProgram[4];
    private static final ShaderProgram[] horizonShaders=new ShaderProgram[4];
    static void setHorizonShader(int pass,ShaderProgram program) {horizonShaders[pass]=program;resourceVersion++;}
    private boolean horizonView() {return !extendedSource() && camera.distanceTo(centre())<1.25*source.schwarzschildRadius();}
    static void setBodyShader(int pass,ShaderProgram program) {bodyShaders[pass]=program;resourceVersion++;}
    private boolean separateMoving=true;
    static void setMovingShader(int pass,ShaderProgram program) {movingShaders[pass]=program;resourceVersion++;}
    private boolean selectiveMaterials=true;
    private float orbitStep=.08f;
    private float curveFactor=4;
    private boolean fixedLayout=true;
    private boolean splitSamples=true;
    private TerrainSamples samples;
    private ShaderProgram shader;
    private boolean specialized=true;
    private boolean compactNodes=true;
    private boolean liveDefaults=true;
    private float meshStepLimit=16;
    private static int resourceVersion;
    private final int capturedVersion=resourceVersion;
    private boolean validate;
    private boolean curvedValidation;
    private String validationStatus="V: flat check | C: curved check (brief pause)";
    private SourcePayload source;
    private TerrainSnapshot snapshot, pending;
    private WorldMesh mesh;
    private WorldMesh moving;
    private boolean meshMode;
    private boolean streamedReference;
    private boolean meshEntities=true;
    private boolean meshClouds=true;
    private boolean meshCoverage=true;
    private final boolean live;
    private long publishedAt;
    private int generation;
    private SimpleFramebuffer target;
    private final GlowingOutline glowOutline=new GlowingOutline();
    private LabBenchmark benchmark;
    private boolean profileCounters;
    private int profileExperiment;
    private Vec3d camera;
    private float yaw,pitch;
    private boolean lensing=true, fine=false;
    private String error;
    private String paused;
    private final TerrainOptions options=TerrainOptions.load();
    private float scale=options.renderScale();
    private int antialiasing=options.antialiasing();
    private float emptyReach=1024;
    private boolean adaptivePath=true,fastBounds=true,fastFetch=true,emptyCells=true;
    private boolean hybrid=options.distantPrototype();
    private boolean faceLighting=true;
    private boolean smoothLighting=true;
    private final NativeSky nativeSky=new NativeSky();
    TerrainScreen(SourcePayload source) {this(source,false);}
    TerrainScreen(SourcePayload source,boolean live) {super(Text.literal("Interstellar terrain prototype"));this.source=source;this.live=live;}
    String problem() {return error;}
    SourcePayload selectedSource() {return source;}
    void adoptSource(SourcePayload next) {source=next;cancelBenchmark();}
    static void setShader(ShaderProgram program) {generalShader=program;resourceVersion++;}
    static void setMeshShader(ShaderProgram program) {meshShader=program;resourceVersion++;}
    static void setCompactMeshShader(ShaderProgram program) {compactMeshShader=program;resourceVersion++;}
    static boolean hasCompactShader() {return compactMeshShader!=null;}
    static void setDefaultMeshShader(ShaderProgram program) {defaultMeshShader=program;resourceVersion++;}
    static void setLongMeshShader(ShaderProgram program) {longMeshShader=program;resourceVersion++;}
    static void setLongDefaultShader(ShaderProgram program) {longDefaultShader=program;resourceVersion++;}
    static void setSplitShader(ShaderProgram program) {splitShader=program;resourceVersion++;}
    static void setLayoutShader(ShaderProgram program) {layoutShader=program;resourceVersion++;}
    static void setLayoutDiagnosticShader(ShaderProgram program) {layoutDiagnosticShader=program;resourceVersion++;}
    static void setMaterialShader(ShaderProgram program) {materialShader=program;resourceVersion++;}
    static void setMaterialProbeShader(ShaderProgram program) {materialProbeShader=program;resourceVersion++;}
    static void setMaterialMaskedShader(ShaderProgram program) {materialMaskedShader=program;resourceVersion++;}
    static void setQuadProbeShader(ShaderProgram program) {quadProbeShader=program;resourceVersion++;}
    static void setQuadMaskedShader(ShaderProgram program) {quadMaskedShader=program;resourceVersion++;}
    static void setQuadMaterialShader(ShaderProgram program) {quadMaterialShader=program;resourceVersion++;}
    static void setQuadDiagnosticShader(ShaderProgram program) {quadDiagnosticShader=program;resourceVersion++;}
    @Override protected void init() {
        if(snapshot!=null || error!=null) return;
        if(!options.enabled()) {error="Terrain preview disabled in interstellar-terrain.json";return;}
        if(source==null || source.count()<=0) {error="Wait for a nearby mass-block source, then reopen the terrain preview.";return;}
        camera=client.gameRenderer.getCamera().getPos();
        yaw=client.gameRenderer.getCamera().getYaw();pitch=client.gameRenderer.getCamera().getPitch();
        snapshot=new TerrainSnapshot(client.world,centre(),!live || hybrid);
        if(live) {
            meshMode=true;hybrid=true;
        }
        if(extendedSource()) {meshMode=true;hybrid=true;streamedReference=!live;}
        if(horizonView()) {meshMode=true;hybrid=true;streamedReference=!live;}
        if(!live && camera.distanceTo(centre())>MESH_VIEW_RANGE) {error="Move within "+MESH_VIEW_RANGE+" blocks of the source, then reopen the terrain preview.";}
        else if(!live && camera.distanceTo(centre())>VOXEL_VIEW_RANGE) {
            // Distant inspection must not fall back to the old bounded voxel/column representation.
            meshMode=true;streamedReference=true;hybrid=true;lensing=false;
            validationStatus="Native mesh required at this distance | P: pair | Space: lensing";
        }
    }
    private Vec3d centre() {return new Vec3d(source.x(),source.y(),source.z());}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        context.draw();
        renderScene();
        renderHud(context);
    }
    /** Composite only the world. Live mode calls this before vanilla's first-person pass. */
    boolean renderScene() {
        if(snapshot!=null && ((!live && SelectedSource.current()!=source) || client.world!=snapshot.world)) error="Source changed: reopen the frozen terrain preview when ready.";
        if(capturedVersion!=resourceVersion) error="Resources reloaded: reopen the terrain preview to refresh textures.";
        if(error==null && generalShader==null) error="Terrain shader unavailable: see game log.";
        if(error==null) {
            try {
                if(live) {
                    camera=client.gameRenderer.getCamera().getPos();
                    yaw=client.gameRenderer.getCamera().getYaw();pitch=client.gameRenderer.getCamera().getPitch();

                    String reason=camera.distanceTo(centre())>MESH_VIEW_RANGE?"Beyond "+MESH_VIEW_RANGE+"-block viewing range: move closer":null;
                    if(!java.util.Objects.equals(paused,reason)) {
                        paused=reason;cancelBenchmark();
                        if(pending!=null) {pending.close();pending=null;}
                        Interstellar.LOGGER.info("Live terrain {}",paused==null?"resumed":"paused: "+paused);
                    }
                    if(paused!=null)return false;
                }
                snapshot.advance();
                if((live || streamedReference) && mesh==null)mesh=new WorldMesh(client.world,snapshot.origin,net.minecraft.util.math.BlockPos.ofFloored(centre()),true);
                if(snapshot.ready() && publishedAt==0) {publishedAt=System.nanoTime();generation=1;}
                if(live && !meshMode && snapshot.ready() && !client.isPaused()) refresh();
                if(meshMode && mesh!=null)mesh.advance();
                if((live || streamedReference) && meshMode && mesh.ready()) {
                    if(moving==null)moving=mesh.movingScene();
                    if(!moving.ready() || live && !client.isPaused())moving.updateMoving();
                }
                if(snapshot.ready() && (!meshMode || mesh.ready())) {AppearanceCapture.finish(this);renderTerrain();return true;}
            } catch(RuntimeException failure) {error="Terrain preview failed: see game log.";Interstellar.LOGGER.error(error,failure);}
        }
        return false;
    }
    void renderHud(DrawContext context) {
        if(live) {
            if(paused!=null) {renderPaused(context);return;}
            context.fill(6,6,Math.min(width-6,410),46,0xCD101824);
            context.drawTextWithShadow(textRenderer,"INTERSTELLAR | Live camera | F10: off | F12: timing",12,12,0xFF88D8FF);
            String age=meshMode?mesh.viewStatus()+" | Mass blocks: "+source.count():"Preparing world view...";
            context.drawTextWithShadow(textRenderer,age,12,24,0xFFFFFFFF);
            String details=benchmark==null?String.format(Locale.ROOT,"%s | C %.2f | Range %.0f / %d | AA %s",source.blackHoleProxy()?"BH r="+String.format(Locale.ROOT,"%.2f",source.schwarzschildRadius()):"Extended mass",
                    source.schwarzschildRadius()/source.enclosingRadius(),camera.distanceTo(centre()),MESH_VIEW_RANGE,aaName()):benchmark.status().replace("B cancels","F12 cancels");
            if(source.enclosingRadius()>16 || source.blackHoleProxy()&&source.schwarzschildRadius()>12)details="Large source: optics only; entity gravity size limit";
            if(horizonView())details=camera.distanceTo(centre())<=source.schwarzschildRadius()?"Inside horizon | Blocks at normal positions for editing":"Near horizon | Transition to falling camera frame";
            context.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(details,Math.max(1,width-24)),12,36,0xFFFFD59A);
            return;
        }
        if(error==null && (!snapshot.ready() || meshMode && (mesh==null || !mesh.ready())))context.fill(0,0,width,height,0xFF101A28);
        if(error!=null) context.fill(0,0,width,height,0xFF201018);
        context.fill(6,6,Math.min(width-6,440),94,0xCD101824);
        context.drawTextWithShadow(textRenderer,"INTERSTELLAR | Minecraft terrain snapshot",12,12,0xFF88D8FF);
        context.drawTextWithShadow(textRenderer,error!=null?error:meshMode?mesh.status():snapshot.status(),12,24,0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,"Space: lensing "+(lensing?"ON":"OFF")+" | Arrows: look | L: aim at source",12,36,0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer,"Q: scale "+scale+" | J: path "+(fine?"fine":"standard")+" | A: AA "+aaName(),12,48,0xFFE0E8EF);
        context.drawTextWithShadow(textRenderer,benchmark==null?"B: benchmark terrain pass":benchmark.status(),12,60,0xFF88D8FF);
        context.drawTextWithShadow(textRenderer,validationStatus,12,72,0xFF88D8FF);
        context.drawTextWithShadow(textRenderer,meshMode?"E: mobs "+(meshEntities?"ON":"OFF")+" | K: native light "+(faceLighting?"ON":"OFF")+" | P: pair":
                "H: distant "+(hybrid?"ON":"OFF")+" | K: face "+(faceLighting?"ON":"OFF")+" | O: smooth "+(smoothLighting?"ON":"OFF")+" | P: pair",12,84,0xFFFFD59A);
        context.drawTextWithShadow(textRenderer,meshMode?"N: clouds "+(meshClouds?"ON":"OFF")+" | U: coverage "+(meshCoverage?"CAMERA":"OLD BOUNDS")+" | Native materials":"M: native mesh experiment | "+(hybrid?"Distant columns approximate":"Frozen cubes"),12,height-16,0xFFFFD59A);
    }
    private void renderPaused(DrawContext context) {
        context.fill(6,6,Math.min(width-6,440),46,0xCD101824);
        context.drawTextWithShadow(textRenderer,"INTERSTELLAR | PAUSED - normal view | F10: off",12,12,0xFFFFD59A);
        context.drawTextWithShadow(textRenderer,paused,12,24,0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,"Resumes automatically when back in range",12,36,0xFF88D8FF);
    }
    private void refresh() {
        if(pending==null && System.nanoTime()-publishedAt>=1_000_000_000L) pending=new TerrainSnapshot(client.world,centre(),hybrid);
        if(pending!=null) {
            pending.advance();
            if(pending.ready()) {
                snapshot.close();snapshot=pending;pending=null;
                publishedAt=System.nanoTime();generation++;
            }
        }
    }
    private void renderTerrain() {
        if(moving!=null)moving.movingLayout(useSeparateMoving());
        shader=currentShader();
        if(validate && meshMode) {
            validate=false;
            var diagnosticShader=useQuads()?(horizonView()?horizonShaders[3]:useSeparateMoving()?movingShaders[3]:quadDiagnosticShader):useLayoutShader()?layoutDiagnosticShader:useDefaultShader()?(useLongShader()?longMeshShader:compactMeshShader):shader;
            diagnosticShader.getUniformOrDefault("MeshStepLimit").set(meshStepLimit);
            Interstellar.LOGGER.info("Mesh fixture program: {}; step cap={}; angular cap={}",useQuads()?"quad diagnostic variant":useLayoutShader()?"streamed-layout diagnostic variant":useDefaultShader()?"compact diagnostic variant (live defaults fix Diagnostic=0)":programName(),useLongShader()?meshStepLimit:4,orbitStep);
            validationStatus=MeshValidation.run(diagnosticShader,()->drawQuad(1,1),orbitStep,curveFactor,useQuads(),useSeparateMoving(),horizonView());
        }
        if(hybrid)nativeSky.update(!meshMode || !meshClouds);
        int w=Math.max(1,Math.round(client.getWindow().getFramebufferWidth()*scale));
        int h=Math.max(1,Math.round(client.getWindow().getFramebufferHeight()*scale));
        if(target==null || target.textureWidth!=w || target.textureHeight!=h) {
            cancelBenchmark();if(target!=null)target.delete();target=new SimpleFramebuffer(w,h,false,false);
        }
        boolean split=useSplitShader();
        if(split && (samples==null || samples.width!=w || samples.height!=h)) {
            if(samples!=null)samples.close();samples=new TerrainSamples(w,h);
        }
        target.beginWrite(true);
        try {
            configureShader(w,h);
            var projection=WorldProjection.current();
            Vec3d forward=Vec3d.fromPolar(pitch,yaw),right=Vec3d.fromPolar(0,yaw+90);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();
            RenderSystem.setShader(()->shader);
            if(validate) {
                validate=false;
                Interstellar.LOGGER.info("Terrain validation observer: camera={}, source={}, yaw={}, pitch={}, pathStep={}",camera,centre(),yaw,pitch,fine?.225:.45);
                validationStatus=TerrainValidation.run(shader,snapshot,camera.subtract(Vec3d.of(snapshot.origin)),forward,right,right.crossProduct(forward),
                        projection,lensing,centre().subtract(Vec3d.of(snapshot.origin)),source.schwarzschildRadius(),curvedValidation,()->drawQuad(w,h));
            }
            if(benchmark!=null)benchmark.begin();
            if(split) {
                samples.begin(0);shader.getUniformOrDefault("SampleOffset").set(-.25f);drawQuad(w,h);
                if(benchmark!=null)benchmark.mark(0);
                samples.begin(1);shader.getUniformOrDefault("SampleOffset").set(.25f);drawQuad(w,h);
                if(benchmark!=null)benchmark.mark(1);
                int mask=0;
                if(useSelectiveMaterials()) {
                    mask=samples.copyMask();
                    if(benchmark!=null)benchmark.mark(2);
                    shader=useQuads()?(horizonView()?horizonShaders[1]:extendedSource()?bodyShaders[1]:useSeparateMoving()?movingShaders[1]:profileProgram(quadMaskedShader,1)):materialMaskedShader;configureShader(w,h);
                    shader.addSampler("PendingRays",mask);RenderSystem.setShader(()->shader);
                    samples.begin(0);shader.getUniformOrDefault("SampleOffset").set(-.25f);drawQuad(w,h);
                    if(benchmark!=null)benchmark.mark(3);
                    samples.begin(1);shader.getUniformOrDefault("SampleOffset").set(.25f);drawQuad(w,h);
                    if(benchmark!=null)benchmark.mark(4);
                } else if(benchmark!=null) {benchmark.mark(2);benchmark.mark(3);benchmark.mark(4);}
                if(profileCounters) {
                    profileCounters=false;
                    if(!useSelectiveMaterials())throw new IllegalStateException("Counters require the selective quad baseline");
                    TerrainProfile.capture(w,h,mask,useSeparateMoving(),program->{shader=program;configureShader(w,h);},()->drawQuad(w,h),
                        "separateMoving="+useSeparateMoving()+" camera="+camera+" yaw="+yaw+" pitch="+pitch+" logical="+w+"x"+h+" movingContents="+(moving==null?0:moving.profileMovingContents)+"; "+mesh.status());
                }
                samples.fold(target,()->drawQuad(w,h));
                if(benchmark!=null)benchmark.mark(5);
            } else {drawQuad(w,h);if(benchmark!=null)for(int i=0;i<6;i++)benchmark.mark(i);}
        } finally {
            client.getFramebuffer().beginWrite(true);
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
        if(antialiasing==0)target.draw(client.getWindow().getFramebufferWidth(),client.getWindow().getFramebufferHeight());
        else TerrainResolve.draw(target,client.getWindow().getFramebufferWidth(),client.getWindow().getFramebufferHeight(),antialiasing==1);
        var glow=meshMode?(moving!=null?moving.entities.glowing:mesh.entities==null?null:mesh.entities.glowing):null;
        var glowShader=GlowingOutline.rays[horizonView()?2:extendedSource()?1:0];
        if(glow!=null && glow.nodes>0 && meshEntities && glowShader!=null && GlowingOutline.edge!=null) {
            var previousShader=shader;
            try {
                shader=glowShader;
                glowOutline.render(w,h,()-> {
                    configureShader(w,h);
                    shader.getUniformOrDefault("MeshNodeCount").set(0f);
                    shader.getUniformOrDefault("MovingNodeCount").set((float)glow.nodes);
                    shader.getUniformOrDefault("CloudNodeCount").set(0f);
                    shader.getUniformOrDefault("MeshClouds").set(0f);
                    shader.getUniformOrDefault("Diagnostic").set(0f);
                    shader.addSampler("DistantAppearance",glow.vertices.texture);
                    shader.addSampler("CompactMovingNodes",glow.tree.texture);
                    RenderSystem.setShader(()->shader);drawQuad(w,h);
                });
            } finally {shader=previousShader;}
        }
        if(benchmark!=null)benchmark.end();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
    }
    /** Near observers magnify trajectory errors; this setting is constant for the whole image. */
    static void setPathQuality(ShaderProgram program,float angularCap,float curveFactor,double radiusRatio) {
        float t=(float)Math.clamp((radiusRatio-4)/2,0,1);
        t=t*t*(3-2*t);
        program.getUniformOrDefault("OrbitStep").set(.02f+(angularCap-.02f)*t);
        program.getUniformOrDefault("CurveFactor").set(1+(curveFactor-1)*t);
    }
    private void configureShader(int w,int h) {
        var projection=WorldProjection.current();
        shader.getUniformOrDefault("Viewport").set((float)w,(float)h);
        shader.getUniformOrDefault("ViewSlopes").set(projection.x(),projection.y(),projection.offsetX(),projection.offsetY());
        var fog=WorldFog.current();
        shader.getUniformOrDefault("TerrainFogRange").set(fog.start(),fog.end(),fog.cylindrical());
        shader.getUniformOrDefault("TerrainFogColour").set(fog.red(),fog.green(),fog.blue(),fog.alpha());
        setVector("Camera",camera.subtract(Vec3d.of(snapshot.origin)));
        setVector("Source",centre().subtract(Vec3d.of(snapshot.origin)));
        Vec3d forward=Vec3d.fromPolar(pitch,yaw),right=Vec3d.fromPolar(0,yaw+90);
        // Camera right follows increasing Minecraft yaw; right cross forward is up.
        setVector("Forward",forward);setVector("Right",right);setVector("Up",right.crossProduct(forward));
        shader.getUniformOrDefault("Radius").set((float)source.schwarzschildRadius());
        shader.getUniformOrDefault("BodyRadius").set(extendedSource()?(float)source.enclosingRadius():0f);
        shader.getUniformOrDefault("Lensing").set(lensing?1f:0f);
        shader.getUniformOrDefault("Hybrid").set(hybrid?1f:0f);
        shader.getUniformOrDefault("FaceLighting").set(faceLighting?1f:0f);
        shader.getUniformOrDefault("SmoothLighting").set(smoothLighting?1f:0f);
        shader.getUniformOrDefault("MeshMode").set(meshMode?1f:0f);
        shader.getUniformOrDefault("MeshEntities").set(meshEntities?1f:0f);
        shader.getUniformOrDefault("MeshClouds").set(meshClouds?1f:0f);
        shader.getUniformOrDefault("MeshCoverage").set(meshCoverage?1f:0f);
        int oldX=((net.minecraft.util.math.BlockPos.ofFloored(centre()).getX()>>4)-8)*16-snapshot.origin.getX();
        int oldZ=((net.minecraft.util.math.BlockPos.ofFloored(centre()).getZ()>>4)-8)*16-snapshot.origin.getZ();
        shader.getUniformOrDefault("OldMeshBounds").set((float)oldX,(float)oldZ,(float)oldX+256,(float)oldZ+256);
        shader.getUniformOrDefault("MeshExtent").set(meshMode?Math.max(mesh.extent,moving==null?0:moving.extent)+mesh.sourceShift(centre()):512f);
        shader.getUniformOrDefault("MovingNodeCount").set(!meshMode || moving==null?0f:(float)(useSeparateMoving()?moving.actorNodeCount:moving.nodeCount));
        shader.getUniformOrDefault("CloudNodeCount").set(!meshMode || moving==null || !useSeparateMoving()?0f:(float)moving.cloudNodeCount);
        shader.getUniformOrDefault("MeshNodeCount").set(meshMode?(float)mesh.nodeCount:0f);
        shader.getUniformOrDefault("DistantTop").set(snapshot.distant==null?-1024f:snapshot.distant.maxHeight);
        shader.getUniformOrDefault("FaceShades").set(client.world.getBrightness(net.minecraft.util.math.Direction.EAST,true),
                client.world.getBrightness(net.minecraft.util.math.Direction.SOUTH,true),client.world.getBrightness(net.minecraft.util.math.Direction.DOWN,true),
                client.world.getBrightness(net.minecraft.util.math.Direction.UP,true));
        shader.getUniformOrDefault("PathStep").set(fine?.225f:.45f);
        shader.getUniformOrDefault("MeshStepLimit").set(meshStepLimit);
        setPathQuality(shader,orbitStep,curveFactor,camera.distanceTo(centre())/source.schwarzschildRadius());
        shader.getUniformOrDefault("RaySamples").set(antialiasing==2?2f:antialiasing==4?4f:1f);
        shader.getUniformOrDefault("AdaptivePath").set(adaptivePath?1f:0f);
        shader.getUniformOrDefault("FastBounds").set(fastBounds?1f:0f);
        shader.getUniformOrDefault("FastFetch").set(fastFetch?1f:0f);
        shader.getUniformOrDefault("EmptyCells").set(emptyCells?1f:0f);
        shader.getUniformOrDefault("EmptyReach").set(emptyReach);
        shader.addSampler("Voxels",meshMode?mesh.triangleTexture:snapshot.voxelTexture);
        shader.addSampler("LocalLight",meshMode?(moving!=null?moving.entities.texture:mesh.entities.texture):snapshot.lightTexture);
        shader.addSampler("SmoothAtlas",snapshot.smoothLight.texture);
        shader.addSampler("LocalSmooth",snapshot.smoothTexture);
        shader.addSampler("DistantSmooth",snapshot.distant==null?snapshot.smoothTexture:snapshot.distant.smoothTexture);
        shader.addSampler("DistantLight",meshMode && moving!=null?moving.nodeTexture:snapshot.distant==null?snapshot.lightTexture:snapshot.distant.lightTexture);
        shader.addSampler("Distant",meshMode?(moving!=null?moving.clouds.texture:mesh.clouds.texture):snapshot.distant==null?snapshot.voxelTexture:snapshot.distant.texture);
        shader.addSampler("DistantAppearance",meshMode && moving!=null?moving.triangleTexture:snapshot.distant==null?snapshot.voxelTexture:snapshot.distant.appearanceTexture);
        shader.addSampler("SkyAtlas",hybrid?nativeSky.texture():snapshot.voxelTexture);
        shader.addSampler("Lightmap",((io.github.rohrl.interstellar.mixin.client.LightmapAccessor)client.gameRenderer.getLightmapTextureManager()).interstellar$texture().getGlId());
        shader.addSampler("Palette",meshMode?mesh.nodeTexture:snapshot.paletteTexture);
        shader.addSampler("CompactNodes",meshMode?mesh.compactNodeTexture:snapshot.paletteTexture);
        shader.addSampler("CompactMovingNodes",meshMode && moving!=null?moving.compactNodeTexture:snapshot.paletteTexture);
        shader.addSampler("Atlas",client.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getGlId());
    }
    static void drawQuad(int w,int h) {
        var buffer=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
        buffer.vertex(0,0,0);buffer.vertex(0,h,0);buffer.vertex(w,h,0);buffer.vertex(w,0,0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    private void setVector(String name,Vec3d value) {shader.getUniformOrDefault(name).set((float)value.x,(float)value.y,(float)value.z);}
    Vec3d appearanceCamera() {return camera;}
    float appearanceYaw() {return yaw;}
    float appearancePitch() {return pitch;}
    void checkAppearancePose(boolean vanillaReference) {
        if(live || snapshot==null || !snapshot.ready() || (meshMode && !mesh.ready()) || error!=null || shader==null || capturedVersion!=resourceVersion)
            throw new IllegalStateException("Wait for a ready frozen F9 snapshot");
        var actual=client.gameRenderer.getCamera();
        if(camera.squaredDistanceTo(actual.getPos())>1e-8 || vanillaReference && (Math.abs(yaw-actual.getYaw())>1e-4 || Math.abs(pitch-actual.getPitch())>1e-4))
            throw new IllegalStateException("F9 view rotated: reopen F9 at the desired player pose");
        if(client.world!=snapshot.world || SelectedSource.current()!=source)throw new IllegalStateException("Source/world changed");
    }
    String appearanceScene() {return (meshMode?mesh.status():snapshot.status())+"; mesh="+meshMode+"; streamedReference="+streamedReference+"; entities="+meshEntities+"; coverage="+meshCoverage+"; clouds="+meshClouds+"; distant="+hybrid+"; faceLight="+faceLighting+"; smoothLight="+smoothLighting+"; origin="+snapshot.origin;}
    void appearanceStatus(String text) {validationStatus=text;}
    void renderAppearanceCandidate() {
        boolean oldLensing=lensing,oldValidate=validate;float oldScale=scale;int oldAa=antialiasing;
        cancelBenchmark();
        try {lensing=false;scale=1;validate=false;antialiasing=0;renderTerrain();}
        finally {lensing=oldLensing;scale=oldScale;validate=oldValidate;antialiasing=oldAa;}
    }
    String qualitySettings() {return "AA="+aaName()+", scale="+scale+", lensing="+lensing+", fine="+fine+", adaptive="+adaptivePath+", fastBounds="+fastBounds+", fastFetch="+fastFetch+", emptyCells="+emptyCells+", emptyReach="+emptyReach+", angularCap="+orbitStep+", curveFactor="+curveFactor+", program="+programName();}
    private String aaName() {return antialiasing==0?"OFF":antialiasing==1?"EDGE":antialiasing==2?"2x":"4x reference";}
    void renderQuality(boolean reference) {
        int oldAa=antialiasing;float oldScale=scale,oldOrbit=orbitStep,oldCurve=curveFactor;boolean oldValidate=validate,oldFine=fine;
        cancelBenchmark();
        try {validate=false;if(reference){antialiasing=4;scale=1;fine=true;orbitStep=.02f;curveFactor=1;}renderTerrain();}
        finally {antialiasing=oldAa;scale=oldScale;validate=oldValidate;fine=oldFine;orbitStep=oldOrbit;curveFactor=oldCurve;}
    }
    void renderPathComparison(boolean reference) {
        boolean old=adaptivePath;cancelBenchmark();
        try {if(reference)adaptivePath=false;renderTerrain();}
        finally {adaptivePath=old;}
    }
    void renderBoundsComparison(boolean reference) {
        boolean old=fastBounds;cancelBenchmark();
        try {if(reference)fastBounds=false;renderTerrain();}
        finally {fastBounds=old;}
    }
    void renderCellComparison(boolean reference) {
        boolean old=emptyCells;cancelBenchmark();
        try {if(reference)emptyCells=false;renderTerrain();}
        finally {emptyCells=old;}
    }
    void renderReachComparison(boolean reference) {
        float old=emptyReach;cancelBenchmark();
        try {if(reference)emptyReach=16;renderTerrain();}
        finally {emptyReach=old;}
    }
    private boolean useMeshShader() {return meshMode && specialized && meshShader!=null;}
    private boolean useDefaultShader() {
        return liveDefaults && useMeshShader() && compactNodes && defaultMeshShader!=null && antialiasing==2 && lensing && hybrid
                && faceLighting && meshEntities && meshClouds && meshCoverage && adaptivePath && fastBounds && fastFetch && emptyCells && emptyReach==1024;
    }
    private boolean useLongShader() {return useMeshShader() && compactNodes && adaptivePath && meshStepLimit!=4 && longMeshShader!=null && longDefaultShader!=null;}
    private boolean useSplitShader() {return splitSamples && (!useQuads() || fixedLayout) && useDefaultShader() && useLongShader() && splitShader!=null
            && TerrainSamples.supported(Math.max(1,Math.round(client.getWindow().getFramebufferWidth()*scale)));}
    private ShaderProgram currentShader() {
        if(useQuads()) {
            if(horizonView())return horizonShaders[!useLayoutShader()?3:useMaterials() && !useSelectiveMaterials()?2:0];
            if(extendedSource())return bodyShaders[!useLayoutShader()?3:useMaterials() && !useSelectiveMaterials()?2:0];
            if(useSeparateMoving())return movingShaders[!useLayoutShader()?3:useMaterials() && !useSelectiveMaterials()?2:0];
            if(quadDiagnosticShader==null || quadProbeShader==null || quadMaterialShader==null || quadMaskedShader==null)
                throw new IllegalStateException("Quad terrain programs are unavailable");
            return useLayoutShader()?(useMaterials() && !useSelectiveMaterials()?profileProgram(quadMaterialShader,2):profileProgram(quadProbeShader,0)):quadDiagnosticShader;
        }
        if(useLayoutShader())return useMaterials()?(useSelectiveMaterials()?materialProbeShader:materialShader):orbitStep>.02f && materialProbeShader!=null?materialProbeShader:layoutShader;
        if(useSplitShader())return splitShader;
        if(!useMeshShader())return generalShader;
        if(useDefaultShader())return useLongShader()?longDefaultShader:defaultMeshShader;
        if(compactNodes && compactMeshShader!=null)return useLongShader()?longMeshShader:compactMeshShader;
        return meshShader;
    }
    private String programName() {
        if(horizonView())return "native-horizon-quads";
        if(useQuads())return (extendedSource()?"native-body-quads-":"native-quads-")+(!useLayoutShader()?"general-":useSelectiveMaterials()?"selective-":"full-")+meshStepLimit+"; separateMoving="+useSeparateMoving()+"; profileExperiment="+profileExperiment;
        if(useLayoutShader())return (useMaterials()?(useSelectiveMaterials()?"native-live-selective-materials-":"native-live-materials-"):orbitStep>.02f && materialProbeShader!=null?"native-live-curvature-probe-":"native-live-layout-")+meshStepLimit;
        if(useSplitShader())return "native-live-split-"+meshStepLimit;
        if(useDefaultShader())return useLongShader()?"native-live-steps-"+meshStepLimit:"native-live-defaults";
        return useMeshShader()?(compactNodes && compactMeshShader!=null?(useLongShader()?"native-compact-steps-"+meshStepLimit:"native-compact-nodes"):"native-mesh"):"general";
    }
    void renderStepLimitComparison(boolean reference) {
        float old=meshStepLimit;cancelBenchmark();
        try {if(reference)meshStepLimit=4;renderTerrain();}
        finally {meshStepLimit=old;}
    }
    void renderSplitComparison(boolean reference) {
        boolean old=splitSamples;cancelBenchmark();
        try {if(reference)splitSamples=false;renderTerrain();}
        finally {splitSamples=old;}
    }
    private boolean useLayoutShader() {return fixedLayout && mesh!=null && mesh.streamed() && useSplitShader() && layoutShader!=null && layoutDiagnosticShader!=null;}
    // Storage format is independent of AA, lensing and diagnostic quality switches.
    // Falling back to a triangle decoder would interpret quad offsets as triangle offsets.
    boolean useQuads() {return meshMode && mesh!=null && mesh.quadStorage();}
    private boolean useMaterials() {return materialShader!=null && (mesh.hasMaterials() || moving!=null && moving.hasMaterials());}
    private boolean useSelectiveMaterials() {return selectiveMaterials && useLayoutShader() && useMaterials() && materialProbeShader!=null && materialMaskedShader!=null;}
    void renderMaterialComparison(boolean reference) {
        boolean old=selectiveMaterials;cancelBenchmark();
        try {if(reference)selectiveMaterials=false;renderTerrain();}finally {selectiveMaterials=old;}
    }
    void renderOrbitComparison(boolean reference) {
        float old=orbitStep,oldCurve=curveFactor;cancelBenchmark();
        try {if(reference){orbitStep=.02f;curveFactor=1;}renderTerrain();}finally {orbitStep=old;curveFactor=oldCurve;}
    }
    private boolean extendedSource() {return source!=null&&!source.blackHoleProxy();}
    private boolean useSeparateMoving() {return useQuads()&&(extendedSource()||separateMoving&&profileExperiment==0);}
    void renderMovingComparison(boolean reference) {
        boolean old=separateMoving;cancelBenchmark();
        try {separateMoving=!reference;renderTerrain();}finally {separateMoving=old;}
    }
    void renderLayoutComparison(boolean reference) {
        boolean old=fixedLayout;cancelBenchmark();
        try {if(reference)fixedLayout=false;renderTerrain();}
        finally {fixedLayout=old;}
    }
    void renderDefaultsComparison(boolean reference) {
        boolean old=liveDefaults;cancelBenchmark();
        try {if(reference)liveDefaults=false;renderTerrain();}
        finally {liveDefaults=old;}
    }
    void renderCompactComparison(boolean reference) {
        boolean old=compactNodes;cancelBenchmark();
        try {if(reference)compactNodes=false;renderTerrain();}
        finally {compactNodes=old;}
    }
    void renderShaderComparison(boolean reference) {
        boolean old=specialized;cancelBenchmark();
        try {if(reference)specialized=false;renderTerrain();}
        finally {specialized=old;}
    }
    private void cancelBenchmark() {if(benchmark!=null) {benchmark.close();benchmark=null;}}
    private ShaderProgram profileProgram(ShaderProgram normal,int pass) {return TerrainProfile.ENABLED && profileExperiment>0?TerrainProfile.programs[profileExperiment][pass]:normal;}
    void renderFetchComparison(boolean reference) {
        boolean old=fastFetch;cancelBenchmark();
        try {if(reference)fastFetch=false;renderTerrain();}
        finally {fastFetch=old;}
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==GLFW.GLFW_KEY_W && !live && useQuads()) {
            cancelBenchmark();
            if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,15);else separateMoving=!separateMoving;
            validationStatus="W: separate moving trees "+(separateMoving?"ON":"OFF")+" | Shift+W: compare trees";return true;
        }
        if(TerrainProfile.ENABLED && !live && key==GLFW.GLFW_KEY_BACKSLASH && (modifiers&GLFW.GLFW_MOD_SHIFT)!=0 && moving!=null) {
            cancelBenchmark();profileExperiment=0;moving.profileMovingContents=(moving.profileMovingContents+1)%3;moving.updateMoving();
            Interstellar.LOGGER.info("Profile moving contents: {} (0=normal, 1=actors only, 2=clouds only); {}",moving.profileMovingContents,moving.status());return true;
        }
        if(TerrainProfile.ENABLED && key==GLFW.GLFW_KEY_B && (modifiers&GLFW.GLFW_MOD_CONTROL)!=0 && useQuads() && useSelectiveMaterials()) {
            cancelBenchmark();profileExperiment=0;profileCounters=true;return true;
        }
        if(TerrainProfile.ENABLED && key==GLFW.GLFW_KEY_BACKSLASH && useQuads()) {
            cancelBenchmark();profileExperiment=(profileExperiment+1)%3;
            Interstellar.LOGGER.info("Profile experiment selected: {} (0=normal, 1=no moving tree, 2=no lightmap reads)",profileExperiment);return true;
        }
        if(key==GLFW.GLFW_KEY_B && snapshot!=null && snapshot.ready() && error==null && paused==null && target!=null) {
            if(benchmark!=null)cancelBenchmark();
            else benchmark=new LabBenchmark(String.format(Locale.ROOT,"TERRAIN %dx%d, scale=%.2f, r/rs=%.5f, lensing=%s, fine=%s, snapshot=%s, hybrid="+hybrid+", live="+live+", mesh="+meshMode+", entities="+meshEntities+", nativeLight="+faceLighting+", coverage="+meshCoverage+", clouds="+meshClouds,
                    target.textureWidth,target.textureHeight,scale,camera.distanceTo(centre())/source.schwarzschildRadius(),lensing,fine,meshMode?mesh.status():snapshot.status())+"; yaw="+yaw+"; pitch="+pitch+"; AA="+aaName()+"; adaptive="+adaptivePath+"; fastBounds="+fastBounds+"; fastFetch="+fastFetch+"; emptyCells="+emptyCells+"; emptyReach="+emptyReach+"; program="+programName()+"; movingContents="+(moving==null?0:moving.profileMovingContents)+"; angularCap="+orbitStep+"; curveFactor="+curveFactor+"; includes resolve",TerrainProfile.ENABLED && (modifiers&GLFW.GLFW_MOD_SHIFT)!=0);
            return true;
        }
        cancelBenchmark();
        switch(key) {
            case GLFW.GLFW_KEY_N -> {if(meshMode)meshClouds=!meshClouds;}
            case GLFW.GLFW_KEY_U -> {if(meshMode)meshCoverage=!meshCoverage;}
            case GLFW.GLFW_KEY_E -> {if(meshMode)meshEntities=!meshEntities;}
            case GLFW.GLFW_KEY_M -> {
                if(horizonView()) {validationStatus="Near-horizon view requires the native streamed mesh";return true;}
                if(!live && snapshot!=null && error==null) {
                    boolean nextStream=(modifiers&GLFW.GLFW_MOD_SHIFT)!=0;
                    boolean replace=mesh!=null && nextStream!=streamedReference;
                    if(replace) {mesh.close();mesh=null;if(moving!=null){moving.close();moving=null;}}
                    meshMode=replace || !meshMode;
                    if(!meshMode && camera.distanceTo(centre())>VOXEL_VIEW_RANGE) {
                        meshMode=true;validationStatus="Native mesh required beyond "+VOXEL_VIEW_RANGE+" blocks";return true;
                    }
                    if(meshMode) {
                        streamedReference=nextStream;hybrid=true;lensing=false;
                        if(mesh==null)try {mesh=new WorldMesh(client.world,snapshot.origin,net.minecraft.util.math.BlockPos.ofFloored(centre()),streamedReference);}
                        catch(RuntimeException failure) {error="Terrain preview allocation failed: see game log.";Interstellar.LOGGER.error(error,failure);}
                    }
                    validationStatus=meshMode?"P: vanilla/mesh pair | C: independent mesh fixture":"V: flat check | C: curved check (brief pause)";
                    validate=false;
                }
            }
            case GLFW.GLFW_KEY_P -> {AppearanceCapture.request(this,(modifiers&(GLFW.GLFW_MOD_ALT|GLFW.GLFW_MOD_CONTROL))==(GLFW.GLFW_MOD_ALT|GLFW.GLFW_MOD_CONTROL)?6:(modifiers&(GLFW.GLFW_MOD_ALT|GLFW.GLFW_MOD_SHIFT))==(GLFW.GLFW_MOD_ALT|GLFW.GLFW_MOD_SHIFT)?5:(modifiers&GLFW.GLFW_MOD_ALT)!=0?4:(modifiers&(GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SHIFT))==(GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SHIFT)?3:(modifiers&GLFW.GLFW_MOD_CONTROL)!=0?2:(modifiers&GLFW.GLFW_MOD_SHIFT)!=0?1:0);validationStatus="Capturing same-frame comparison...";}
            case GLFW.GLFW_KEY_S -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,7);else specialized=!specialized;validationStatus="S: program "+programName()+" | Shift+S: compare programs";}
            case GLFW.GLFW_KEY_F -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,8);else compactNodes=!compactNodes;validationStatus="F: program "+programName()+(useQuads()?" | Shift+F: compare general quad program":" | Shift+F: compare node layouts");}
            case GLFW.GLFW_KEY_D -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,9);else liveDefaults=!liveDefaults;validationStatus="D: program "+programName()+" | Shift+D: compare default settings";}
            case GLFW.GLFW_KEY_I -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)emptyReach=emptyReach==16?1024:16;else emptyCells=!emptyCells;validationStatus="I: cache "+emptyCells+" | Shift+I reach "+emptyReach+" | Ctrl+Alt+P: compare reach";}
            case GLFW.GLFW_KEY_R -> {fastFetch=!fastFetch;validationStatus="R: fast mesh addressing "+(fastFetch?"ON":"OFF")+" | Alt+P: compare addressing";}
            case GLFW.GLFW_KEY_T -> {fastBounds=!fastBounds;validationStatus="T: fast bounds "+(fastBounds?"ON":"OFF")+" | Ctrl+Shift+P: compare bounds";}
            case GLFW.GLFW_KEY_A -> antialiasing=(antialiasing+1)%3;
            case GLFW.GLFW_KEY_G -> {adaptivePath=!adaptivePath;validationStatus="G: adaptive paths "+(adaptivePath?"ON":"OFF")+" | Ctrl+P: compare old path";}
            case GLFW.GLFW_KEY_H -> {if(!meshMode)hybrid=!hybrid;}
            case GLFW.GLFW_KEY_K -> faceLighting=!faceLighting;
            case GLFW.GLFW_KEY_O -> smoothLighting=!smoothLighting;
            case GLFW.GLFW_KEY_Z -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,13);else selectiveMaterials=!selectiveMaterials;validationStatus="Z: selective materials "+(selectiveMaterials?"ON":"OFF")+" | Shift+Z: compare full material pass";}
            case GLFW.GLFW_KEY_LEFT_BRACKET -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,14);else orbitStep=orbitStep==.02f?.04f:orbitStep==.04f?.08f:.02f;validationStatus="[: angular cap "+orbitStep+" | Shift+[: compare .02";}
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> {curveFactor=curveFactor==1?2:curveFactor==2?4:1;validationStatus="]: chord tolerance multiplier "+curveFactor;}
            case GLFW.GLFW_KEY_V -> {
                if(meshMode) {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,10);else meshStepLimit=meshStepLimit==4?16:4;validationStatus="V: step cap "+meshStepLimit+" | Shift+V: compare four-block cap";}
                else {validate=true;curvedValidation=false;}
            }
            case GLFW.GLFW_KEY_C -> {validate=true;curvedValidation=true;}
            case GLFW.GLFW_KEY_Y -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,12);else fixedLayout=!fixedLayout;validationStatus=useQuads()?"Y: optimized quad program "+(fixedLayout?"ON":"OFF")+" | Shift+Y: compare general quad program":"Y: fixed texture layouts "+(fixedLayout?"ON":"OFF")+" | Shift+Y: compare dynamic layout";}
            case GLFW.GLFW_KEY_X -> {if((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)AppearanceCapture.request(this,11);else splitSamples=!splitSamples;validationStatus="X: split AA "+(splitSamples?"ON":"OFF")+" | Shift+X: compare serial AA";}
            case GLFW.GLFW_KEY_SPACE -> lensing=!lensing;
            case GLFW.GLFW_KEY_Q -> scale=scale==.5f?1f:.5f;
            case GLFW.GLFW_KEY_J -> fine=!fine;
            case GLFW.GLFW_KEY_LEFT -> yaw-=5;
            case GLFW.GLFW_KEY_RIGHT -> yaw+=5;
            case GLFW.GLFW_KEY_UP -> pitch=Math.max(-89,pitch-5);
            case GLFW.GLFW_KEY_DOWN -> pitch=Math.min(89,pitch+5);
            case GLFW.GLFW_KEY_L -> {if(camera!=null) {var d=centre().subtract(camera);yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));}}
            default -> {return super.keyPressed(key,scan,modifiers);}
        }
        return true;
    }
    @Override public void removed() {cancelBenchmark();glowOutline.close();nativeSky.close();if(moving!=null)moving.close();if(mesh!=null)mesh.close();if(snapshot!=null)snapshot.close();if(pending!=null)pending.close();if(target!=null)target.delete();if(samples!=null)samples.close();}
    @Override public boolean shouldPause() {return true;}
}
