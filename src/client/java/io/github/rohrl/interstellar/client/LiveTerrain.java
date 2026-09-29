package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.glfw.GLFW;

/** Client-render-thread owner of the live preview. Vanilla simulation and input stay in control. */
public final class LiveTerrain {
    private static TerrainScreen renderer;
    private static int width,height;
    private static ClientWorld armedWorld;
    private static ClientWorld seenWorld;
    private static boolean worldComposited;
    private static boolean labOpen;
    private static boolean enteringWorld;
    private static TerrainOptions options=TerrainOptions.load();
    private LiveTerrain() { }
    static boolean active() {return armedWorld!=null;}
    static TerrainOptions preferences() {return renderer==null?options:renderer.preferences();}
    static boolean portalViews() {return active() && options.wormholes();}
    static void applyPreferences(TerrainOptions next) {
        options=next;
        if(renderer!=null)renderer.applyPreferences(next);
        if(!next.enabled())stop();
        else {armedWorld=MinecraftClient.getInstance().world;synchronize(MinecraftClient.getInstance());}
        if(!portalViews())WormholeClient.suspendView();
    }
    static void refreshLighting() {releaseRenderer();synchronize(MinecraftClient.getInstance());}
    static void wormholeChanged() {
        var client=MinecraftClient.getInstance();
        if(renderer!=null)renderer.wormholeChanged();
        synchronize(client);
    }
    static void setEnabled(boolean enabled) {
        var next=preferences().effects(enabled,options.massLensing(),options.wormholes(),options.statusHud());
        try {next.save();applyPreferences(next);}catch(Exception e){Interstellar.LOGGER.error("Cannot save world effects",e);}
    }
    static void toggle(MinecraftClient client) {
        if(client.world==null || client.currentScreen!=null)return;
        setEnabled(!active());
        message(client,"World effects "+(active()?"ON":"OFF")+" | F4: settings");
    }
    // Ctrl avoids the crouch/descent side effect of holding Shift in a live world.
    static void benchmark(int modifiers) {
        if(renderer==null && WormholeAppearance.pending() && modifiers==0){WormholeAppearance.benchmark();return;}
        if(renderer==null)return;
        if((modifiers&GLFW.GLFW_MOD_ALT)!=0) {
            if(!WorldBackendBridge.ENABLED) {
                var message="OpenGL-only launch. Enable RTX with: gradlew.bat runClient -PinterstellarRtx";
                MinecraftClient.getInstance().player.sendMessage(Text.literal(message),false);
                Interstellar.LOGGER.info(message);return;
            }
            if((modifiers&GLFW.GLFW_MOD_CONTROL)!=0)renderer.compareWorldBackend();else renderer.toggleWorldBackend();
        } else renderer.keyPressed(GLFW.GLFW_KEY_B,0,(modifiers&GLFW.GLFW_MOD_CONTROL)!=0?GLFW.GLFW_MOD_SHIFT:0);
    }
    static void tick(MinecraftClient client) {
        boolean lab=client.currentScreen instanceof TerrainScreen || client.currentScreen instanceof OpticalLabScreen;
        if(client.world!=seenWorld) {
            stop();seenWorld=client.world;enteringWorld=client.world!=null;
            options=TerrainOptions.load();if(options.enabled()&&!lab)armedWorld=client.world;
        }
        if(labOpen&&!lab&&options.enabled())armedWorld=client.world;
        labOpen=lab;
        synchronize(client);
    }
    private static void synchronize(MinecraftClient client) {
        if(!active())return;
        if(client.world!=armedWorld) {stop();return;}
        var source=options.massLensing()?SelectedSource.current():null;
        boolean portals=options.wormholes() && WormholeClient.nearby();
        // Keep the streamed cache even when no item or optical effect is active.
        if(renderer!=null && !renderer.resourcesCurrent())releaseRenderer();
        if(renderer!=null){renderer.sources(source,portals);return;}
        // The camera is valid only inside renderWorld, after vanilla has positioned it.
        if(!rendering || client.player==null)return;
        renderer=new TerrainScreen(source,true,portals);
        width=client.getWindow().getScaledWidth();height=client.getWindow().getScaledHeight();
        renderer.init(client,width,height);
        Interstellar.LOGGER.info("Gameplay view preparing: mass={}, wormholes={}",source==null?0:source.count(),portals);
        check(client);
    }
    private static void releaseRenderer() {RelativisticVision.prepared=false;if(renderer!=null) {WormholeClient.suspendView();options=renderer.preferences();renderer.removed();renderer=null;}}
    static void stop() {WormholeClient.suspendView();releaseRenderer();armedWorld=null;worldComposited=false;}
    private static boolean rendering;
    /** Replace the final vanilla loading screen, after its own readiness condition passes. */
    public static boolean beginWorldPreparation() {
        var client=MinecraftClient.getInstance();
        tick(client);
        if(!active() || client.world==null || client.player==null || !enteringWorld)return false;
        enteringWorld=false;
        client.setScreen(new WorldPreparationScreen(client.world));
        return true;
    }
    static String preparationSummary() {return renderer==null?"No cache":renderer.preparationSummary();}
    static int preparationPercent() {return renderer==null?0:renderer.preparationPercent();}
    static boolean preparationReady() {return renderer!=null && renderer.preparationReady();}
    public static boolean worldComposited() {return worldComposited;}
    private static void message(MinecraftClient client,String message) {
        if(client.player!=null)client.player.sendMessage(Text.literal("Interstellar: "+message),true);
    }
    private static void check(MinecraftClient client) {
        if(renderer!=null && renderer.problem()!=null) {String problem=renderer.problem();stop();message(client,problem);}
    }
    public static void renderWorld() {
        worldComposited=false;
        WormholeClient.renderingOptics=0;
        if(!active())return;
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null)return;
        if(client.currentScreen instanceof DownloadingTerrainScreen)return;
        try {
            rendering=true;
            synchronize(client);
            if(!active())return;
            if(renderer==null)return;
            int w=client.getWindow().getScaledWidth(),h=client.getWindow().getScaledHeight();
            if(w!=width || h!=height) {width=w;height=h;renderer.resize(client,w,h);}
            worldComposited=renderer.renderScene();
            check(client);
        } catch(RuntimeException failure) {
            Interstellar.LOGGER.error("Live terrain stopped",failure);stop();
            message(client,"Live terrain stopped after a rendering error; see log.");
        } finally {rendering=false;}
    }
    public static void renderHud(DrawContext context) {
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null || client.options.hudHidden || !options.statusHud())return;
        if(!active()) {
            context.drawTextWithShadow(client.textRenderer,"INTERSTELLAR | World effects OFF | F4: settings | F10: on",12,12,0xFFFFD59A);return;
        }
        if(renderer!=null) {renderer.renderHud(context);return;}
        context.fill(6,6,Math.min(client.getWindow().getScaledWidth()-6,440),46,0xCD101824);
        context.drawTextWithShadow(client.textRenderer,"INTERSTELLAR | World effects ON | F4: settings",12,12,0xFF88D8FF);
        context.drawTextWithShadow(client.textRenderer,SelectedSource.state().message(),12,24,0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer,"Place mass blocks or throw a Rift Pearl | Automatic activation",12,36,0xFF88D8FF);
    }
}
