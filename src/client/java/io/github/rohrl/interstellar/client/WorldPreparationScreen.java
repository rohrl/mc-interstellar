package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

/** Final loading phase. Rendering and chunk delivery continue behind the opaque screen. */
final class WorldPreparationScreen extends Screen {
    private final ClientWorld world;
    private final long started=System.nanoTime();
    private float initialHealth;
    WorldPreparationScreen(ClientWorld world) {super(Text.literal("Preparing Interstellar world view"));this.world=world;Interstellar.LOGGER.info("World preparation loading screen started: {}",world.getRegistryKey().getValue());}
    @Override protected void init() {
        if(initialHealth==0 && client.player!=null)initialHealth=client.player.getHealth();
        addDrawableChild(ButtonWidget.builder(Text.literal("Continue in background"),button->close())
            .dimensions(width/2-110,height/2+42,220,20).build());
    }
    @Override public boolean shouldPause() {return false;}
    @Override public void tick() {
        if(client.world!=world || client.player==null)return;
        // Do not hide danger behind a client-only loading screen (including multiplayer).
        if(!LiveTerrain.active() || LiveTerrain.preparationReady() || client.player.getHealth()<initialHealth || client.player.isDead())close();
    }
    @Override public void close() {
        Interstellar.LOGGER.info("World preparation loading screen closed: ready={}, progress={}%, elapsed={} ms; {}",
            LiveTerrain.preparationReady(),LiveTerrain.preparationPercent(),(System.nanoTime()-started)/1e6,LiveTerrain.preparationSummary());
        client.getNarratorManager().narrate(Text.translatable("narrator.ready_to_play"));
        super.close();
    }
    @Override public void renderBackground(DrawContext context,int mouseX,int mouseY,float delta) {
        context.fill(0,0,width,height,0xFF101824);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        super.render(context,mouseX,mouseY,delta);
        int progress=LiveTerrain.preparationPercent();
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-54,0xFF88D8FF);
        context.drawCenteredTextWithShadow(textRenderer,progress+"%",width/2,height/2-32,0xFFFFFFFF);
        context.fill(width/2-120,height/2-12,width/2+120,height/2-6,0xFF293B50);
        context.fill(width/2-120,height/2-12,width/2-120+progress*240/100,height/2-6,0xFF88D8FF);
        context.drawCenteredTextWithShadow(textRenderer,"Preparing the nearby world for lenses and portals",width/2,height/2+8,0xFFE0E8EF);
        context.drawCenteredTextWithShadow(textRenderer,"Distant destinations can open later while you play",width/2,height/2+21,0xFFA6B7C9);
    }
}
