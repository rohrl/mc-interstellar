package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Client-render-thread owner of the live preview. Vanilla simulation and input stay in control. */
public final class LiveTerrain {
    private static TerrainScreen renderer;
    private static int width,height;
    private LiveTerrain() { }
    static boolean active() {return renderer!=null;}
    static void toggle(MinecraftClient client) {
        if(active()) {stop();return;}
        if(client.world==null || client.currentScreen!=null)return;
        renderer=new TerrainScreen(SelectedSource.current(),true);
        width=client.getWindow().getScaledWidth();height=client.getWindow().getScaledHeight();
        renderer.init(client,width,height);
        check(client);
    }
    static void benchmark() {if(renderer!=null)renderer.keyPressed(GLFW.GLFW_KEY_B,0,0);}
    static void tick(MinecraftClient client) {
        if(renderer!=null && (client.world==null || SelectedSource.current()!=renderer.selectedSource())) {
            stop();message(client,"Source selection changed; inspect again before F10.");
        }
    }
    static void stop() {if(renderer!=null) {renderer.removed();renderer=null;}}
    private static void message(MinecraftClient client,String message) {
        if(client.player!=null)client.player.sendMessage(Text.literal("Interstellar: "+message),true);
    }
    private static void check(MinecraftClient client) {
        if(renderer!=null && renderer.problem()!=null) {String problem=renderer.problem();stop();message(client,problem);}
    }
    public static void render(DrawContext context) {
        if(renderer==null)return;
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null)return;
        try {
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
