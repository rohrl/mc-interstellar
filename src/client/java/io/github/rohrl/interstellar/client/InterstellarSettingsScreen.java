package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Live product controls; diagnostic and benchmark switches stay in the F9 lab. */
final class InterstellarSettingsScreen extends Screen {
    private TerrainOptions preferences;
    private String status="Changes apply immediately and are saved.";
    private int left,column,top;
    InterstellarSettingsScreen() {super(Text.literal("Interstellar settings"));}

    @Override protected void init() {
        preferences=LiveTerrain.preferences().features(WorldFeatures.weather,WorldFeatures.body);
        int total=Math.min(420,width-24);left=(width-total)/2;column=(total-8)/2;top=Math.max(32,(height-190)/2);
        button(0,0,"Resolution: "+(preferences.renderScale()==1?"100%":"50%"),
            "50% traces at half width and height. 100% gives finer detail and costs roughly four times as many rays.",()->
                quality(preferences.renderScale()==1?.5f:1,preferences.antialiasing(),preferences.finePaths(),preferences.preferRtx()));
        button(1,0,"Antialiasing: "+TerrainOptions.aaLabel(preferences.antialiasing()),
            "Off / Edge / 2x / 4x / 8x. More rays smooth silhouettes and fine detail. 4x traces twice as many rays as 2x; 8x traces four times as many. Both renderers support every mode.",()->
                quality(preferences.renderScale(),TerrainOptions.nextAa(preferences.antialiasing()),preferences.finePaths(),preferences.preferRtx()));
        button(0,1,"Smooth lighting: "+on(client.options.getAo().getValue()),
            "Minecraft's ambient occlusion. Changing this rebuilds captured terrain lighting; preparing the view can take a while.",()-> {
                client.options.getAo().setValue(!client.options.getAo().getValue());client.options.write();
                LiveTerrain.refreshLighting();status="Rebuilding terrain lighting...";clearAndInit();
            });
        button(1,1,"Fine light paths: "+on(preferences.finePaths()),
            "Smaller steps along curved light paths. More accurate near strong bends, with a performance cost.",()->
                quality(preferences.renderScale(),preferences.antialiasing(),!preferences.finePaths(),preferences.preferRtx()));
        var renderer=button(0,2,"Renderer: "+(WorldBackendBridge.ENABLED&&preferences.preferRtx()?"RTX (auto fallback)":"OpenGL"),
            WorldBackendBridge.ENABLED?"Prefer hardware ray queries or force OpenGL. RTX falls back if unavailable. Alt+F12 also switches.":
                "This is an OpenGL-only launch. Use Launch Interstellar RTX.cmd to make RTX available.",()->
                quality(preferences.renderScale(),preferences.antialiasing(),preferences.finePaths(),!preferences.preferRtx()));
        renderer.active=WorldBackendBridge.ENABLED;
        button(1,2,"Live lensing: "+on(LiveTerrain.active()),"F10 also toggles live lensing. Without a ready source, the normal world remains visible.",()-> {
            LiveTerrain.setEnabled(!LiveTerrain.active());clearAndInit();
        });
        button(0,3,"Weather: "+on(WorldFeatures.weather),"Local foreground rain and snow. This approximation does not bend precipitation around the source.",()-> {
            WorldFeatures.weather=!WorldFeatures.weather;save(preferences.features(WorldFeatures.weather,WorldFeatures.body));
        });
        button(1,3,"Returning body: "+on(WorldFeatures.body),"Experimental images of your actual body along returning light paths. Often small and distorted; adds rendering work.",()-> {
            WorldFeatures.body=!WorldFeatures.body;save(preferences.features(WorldFeatures.weather,WorldFeatures.body));
        });
        var upright=button(0,4,"Reset camera upright","Removes tilt from wormhole travel. Keeps position and viewing direction. R also resets it.",()-> {
            WormholeClient.resetOrientation();status="Camera reset requested.";
        });upright.active=WormholeClient.canResetOrientation();
        button(1,4,"Quality defaults","50% resolution, 2x AA, normal light-path steps. Other settings stay as selected.",()->
            quality(.5f,2,false,preferences.preferRtx()));
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width/2-70,top+145,140,20).build());
    }
    private ButtonWidget button(int x,int y,String text,String tip,Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(text),b->action.run())
            .dimensions(left+x*(column+8),top+y*24,column,20).tooltip(Tooltip.of(Text.literal(tip))).build());
    }
    private static String on(boolean value) {return value?"On":"Off";}
    private void quality(float scale,int aa,boolean fine,boolean rtx) {save(preferences.quality(scale,aa,fine,rtx));}
    private void save(TerrainOptions next) {
        try {
            next.save();LiveTerrain.applyPreferences(next);status="Changes applied and saved.";
            Interstellar.LOGGER.info("Interstellar settings saved: {}",next);
        } catch(Exception failure) {status="Could not save settings; see log.";Interstellar.LOGGER.error(status,failure);}
        clearAndInit();
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        super.render(context,mouseX,mouseY,delta);
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,top-22,0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,status,width/2,top+128,0xC8D8E8);
    }
    @Override public boolean shouldPause() {return false;}
}
