package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.world.ClientWorld;
import io.github.rohrl.interstellar.source.SourceState;
import org.lwjgl.glfw.GLFW;

/** Client-render-thread owner of the live preview. Vanilla simulation and input stay in control. */
public final class LiveTerrain {
    private static TerrainScreen renderer;
    private static int width,height;
    private static ClientWorld armedWorld;
    private LiveTerrain() { }
    static boolean active() {return armedWorld!=null;}
    static void toggle(MinecraftClient client) {
        if(active()) {stop();return;}
        if(client.world==null || client.currentScreen!=null)return;
        if(SelectedSource.state()==SourceState.NONE) {message(client,"Inspect a source before F10.");return;}
        armedWorld=client.world;
        synchronize(client);
    }
    // Ctrl avoids the crouch/descent side effect of holding Shift in a live world.
    static void benchmark() {if(renderer!=null)renderer.keyPressed(GLFW.GLFW_KEY_B,0,net.minecraft.client.gui.screen.Screen.hasControlDown()?GLFW.GLFW_MOD_SHIFT:0);}
    static void tick(MinecraftClient client) {synchronize(client);}
    private static void synchronize(MinecraftClient client) {
        if(!active())return;
        if(client.world!=armedWorld||SelectedSource.state()==SourceState.NONE) {stop();return;}
        var source=SelectedSource.current();
        if(renderer!=null&&renderer.selectedSource()==source)return;
        if(source!=null&&source.blackHoleProxy()) {
            if(renderer!=null) {
                renderer.adoptSource(source);
                Interstellar.LOGGER.info("Live source refreshed without terrain reload: N={}, r_s={}",source.count(),source.schwarzschildRadius());
                return;
            }
            renderer=new TerrainScreen(source,true);
            width=client.getWindow().getScaledWidth();height=client.getWindow().getScaledHeight();
            renderer.init(client,width,height);
            Interstellar.LOGGER.info("Live source adopted: N={}, r_s={}",source.count(),source.schwarzschildRadius());
            check(client);
        }
    }
    private static void releaseRenderer() {if(renderer!=null) {renderer.removed();renderer=null;}}
    static void stop() {releaseRenderer();armedWorld=null;}
    private static void message(MinecraftClient client,String message) {
        if(client.player!=null)client.player.sendMessage(Text.literal("Interstellar: "+message),true);
    }
    private static void check(MinecraftClient client) {
        if(renderer!=null && renderer.problem()!=null) {String problem=renderer.problem();stop();message(client,problem);}
    }
    public static void render(DrawContext context) {
        if(!active())return;
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null)return;
        try {
            synchronize(client);
            if(!active())return;
            if(renderer==null || SelectedSource.current()==null || !SelectedSource.current().blackHoleProxy()) {
                context.fill(6,6,Math.min(client.getWindow().getScaledWidth()-6,440),46,0xCD101824);
                context.drawTextWithShadow(client.textRenderer,"INTERSTELLAR | PAUSED - normal view | F10: off",12,12,0xFFFFD59A);
                String reason=SelectedSource.current()!=null?"Extended source: waiting for black-hole compactness":SelectedSource.state().message();
                context.drawTextWithShadow(client.textRenderer,reason,12,24,0xFFFFFFFF);
                context.drawTextWithShadow(client.textRenderer,"Source tracking active | Resumes automatically",12,36,0xFF88D8FF);
                return;
            }
            int w=client.getWindow().getScaledWidth(),h=client.getWindow().getScaledHeight();
            if(w!=width || h!=height) {width=w;height=h;renderer.resize(client,w,h);}
            renderer.render(context,0,0,0);
            check(client);
        } catch(RuntimeException failure) {
            Interstellar.LOGGER.error("Live terrain stopped",failure);stop();
            message(client,"Live terrain stopped after a rendering error; see log.");
        }
    }
}
