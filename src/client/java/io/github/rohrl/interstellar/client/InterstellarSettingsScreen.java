package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.config.AccretionSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** One settings entry point for ordinary gameplay, appearance and diagnostics. */
final class InterstellarSettingsScreen extends Screen {
    private TerrainOptions preferences;
    private String status="Visual settings are saved. Gravity controls affect this server session.";
    private int left,column,top,page;
    private boolean lastGravity,lastCapture;
    private double lastStrength;
    InterstellarSettingsScreen() {super(Text.literal("Interstellar settings"));}
    @Override protected void init() {
        preferences=LiveTerrain.preferences().features(WorldFeatures.weather,WorldFeatures.body);
        lastGravity=WorldFeatures.gravityEnabled;lastCapture=WorldFeatures.gravityCapture;lastStrength=WorldFeatures.gravityStrength;
        int total=Math.min(420,width-24);left=(width-total)/2;column=(total-8)/2;top=Math.max(38,(height-210)/2);
        String[] tabs={"Gameplay","Graphics","Disk","Ambience","Relativity","Tools"};int tabWidth=(total-20)/6;
        for(int i=0;i<6;i++) {final int p=i;
            var tab=addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]),b->{page=p;clearAndInit();})
                .dimensions(left+i*(tabWidth+4),top,tabWidth,20).build());tab.active=i!=page;
        }
        if(page==0)gameplay();else if(page==1)graphics();else if(page==2)disk();else if(page==3)ambience();else if(page==4)relativity();else tools();
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width/2-70,top+154,140,20).build());
    }
    private void ambience() {
        var s=DiskAtmosphere.options();
        button(0,0,"Stars: "+s.stars()+"x","Vanilla star field density: 1x / 2x / 3x. Applies to normal and lensed skies. Default 2x.",()->atmosphere(new DiskAtmosphere.Options(s.stars()%3+1,s.music(),s.gas())));
        button(1,0,"BH music: "+on(s.music()),"Choose eerie Minecraft Nether/End music at random when approaching a black hole. Respects Music volume; leaving restores ordinary music scheduling.",()->atmosphere(new DiskAtmosphere.Options(s.stars(),!s.music(),s.gas())));
        button(0,1,"Disk gas veil: "+on(s.gas()),"Gentle irregular warm haze only while skimming the visible disk. Maximum 18% opacity; no damage or simulated gas volume.",()->atmosphere(new DiskAtmosphere.Options(s.stars(),s.music(),!s.gas())));
    }
    private void atmosphere(DiskAtmosphere.Options next) {
        try {DiskAtmosphere.apply(next);status="Ambience settings applied and saved.";}
        catch(Exception failure){status="Could not save ambience settings; see log.";Interstellar.LOGGER.error(status,failure);}
        clearAndInit();
    }
    private void gameplay() {
        button(0,0,"World effects: "+on(LiveTerrain.active()),"Master visual switch; F10 does the same. Enabled by default in every world. Server gravity has its own controls below.",()->
            save(preferences.effects(!LiveTerrain.active(),preferences.massLensing(),preferences.wormholes(),preferences.statusHud())));
        button(1,0,"Mass lensing: "+on(preferences.massLensing()),"Render the automatically tracked mass cluster, including weak lensing and black holes. Works together with portals. Does not change entity gravity.",()->
            save(preferences.effects(preferences.enabled(),!preferences.massLensing(),preferences.wormholes(),preferences.statusHud())));
        button(0,1,"Portal views: "+on(preferences.wormholes()),"Render and prepare wormhole passages. Off keeps visible markers and suspends your travel; the pair stays placed.",()->
            save(preferences.effects(preferences.enabled(),preferences.massLensing(),!preferences.wormholes(),preferences.statusHud())));
        button(1,1,"Status overlay: "+on(preferences.statusHud()),"Show which effects are active and whether terrain or portals are preparing.",()->
            save(preferences.effects(preferences.enabled(),preferences.massLensing(),preferences.wormholes(),!preferences.statusHud())));
        serverButton(0,2,"Entity gravity: "+on(lastGravity),"Pull mobs, projectiles and supported tethered entities. Affects all players in this server session; requires operator permission.","gravity enabled "+!lastGravity);
        serverButton(1,2,"Horizon capture: "+on(lastCapture),"Allow a black hole to consume nearby mobs/projectiles. Server-wide session setting; independent of visual effects.","gravity capture "+!lastCapture);
        int strength=(int)Math.round(lastStrength/.2*100);
        double next=lastStrength<.049?.05:lastStrength<.099?.1:lastStrength<.199?.2:.05;
        serverButton(0,3,"Gravity strength: "+strength+"%","Cycle 25%, 50%, 100% of the current strong-gravity default. Does not change mass or optical lensing. Server session only.","gravity strength "+next);
        upright(1,3);
    }
    private void graphics() {
        button(0,0,"Resolution: "+(preferences.renderScale()==1?"100%":"50%"),"50% uses half width and height. 100% gives finer detail and traces four times as many pixels.",()->
            quality(preferences.renderScale()==1?.5f:1,preferences.antialiasing(),preferences.finePaths(),preferences.preferRtx()));
        button(1,0,"Antialiasing: "+TerrainOptions.aaLabel(preferences.antialiasing()),"Off / Edge / 2x / 4x / 8x. Every mode works with RTX and OpenGL; more samples cost more GPU time.",()->
            quality(preferences.renderScale(),TerrainOptions.nextAa(preferences.antialiasing()),preferences.finePaths(),preferences.preferRtx()));
        button(0,1,"Fine light paths: "+on(preferences.finePaths()),"Smaller steps along curved light paths. More accuracy at a performance cost.",()->
            quality(preferences.renderScale(),preferences.antialiasing(),!preferences.finePaths(),preferences.preferRtx()));
        button(1,1,"Smooth lighting: "+on(client.options.getAo().getValue()),"Minecraft ambient occlusion. Rebuilds captured lighting; preparation can take a while.",()-> {
            client.options.getAo().setValue(!client.options.getAo().getValue());client.options.write();LiveTerrain.refreshLighting();status="Rebuilding terrain lighting...";clearAndInit();
        });
        var renderer=button(0,2,"Renderer: "+(WorldBackendBridge.ENABLED&&preferences.preferRtx()?"RTX (auto fallback)":"OpenGL"),
            WorldBackendBridge.ENABLED?"Prefer RTX or force OpenGL for all effects. Alt+F12 also switches.":"Use Launch Interstellar RTX.cmd to make RTX available.",()->
                quality(preferences.renderScale(),preferences.antialiasing(),preferences.finePaths(),!preferences.preferRtx()));renderer.active=WorldBackendBridge.ENABLED;
        button(1,2,"Weather: "+on(WorldFeatures.weather),"Local foreground rain/snow. Precipitation is not lensed.",()-> {
            WorldFeatures.weather=!WorldFeatures.weather;save(preferences.features(WorldFeatures.weather,WorldFeatures.body));
        });
        button(0,3,"Returning body: "+on(WorldFeatures.body),"Experimental images of your actual body along returning rays; often very small. Adds rendering work.",()-> {
            WorldFeatures.body=!WorldFeatures.body;save(preferences.features(WorldFeatures.weather,WorldFeatures.body));
        });
        button(1,3,"Quality defaults","50% resolution, 2x AA, normal path steps. Keeps feature switches.",()->quality(.5f,2,false,preferences.preferRtx()));
    }
    private void disk() {
        var s=AccretionDiskVisuals.options();
        button(0,0,"Accretion: "+s.modeName(),"Off / Auto: large black holes / All black holes. Requires mass lensing. A supplied gas disk is assumed; size alone does not create one in nature.",()->disk(new AccretionSettings((s.mode()+1)%3,s.animation(),s.brightness(),s.outerRadius(),s.tilt(),s.threshold(),s.glow())));
        button(1,0,"Animation: "+on(s.animation()),"Orbiting bright filaments evolve and stretch. Inner orbits move faster. Turbulence is an appearance model.",()->disk(new AccretionSettings(s.mode(),!s.animation(),s.brightness(),s.outerRadius(),s.tilt(),s.threshold(),s.glow())));
        button(0,1,"Brightness: "+(int)(s.brightness()*100)+"%","Cycle 50%, 100%, 200%, 400% exposure. The new 100% equals the original 200%. The gas emits its own light; this does not add terrain lighting or heat damage.",()->disk(new AccretionSettings(s.mode(),s.animation(),s.brightness()<1?1:s.brightness()<2?2:s.brightness()<4?4:.5f,s.outerRadius(),s.tilt(),s.threshold(),s.glow())));
        button(1,1,"Disk size: "+(int)s.outerRadius()+" horizon radii","Outer radius: 6 / 10 / 16 times the horizon radius. The stable disk starts at 3 horizon radii. Larger disks cover more of the view.",()->disk(new AccretionSettings(s.mode(),s.animation(),s.brightness(),s.outerRadius()<10?10:s.outerRadius()<16?16:6,s.tilt(),s.threshold(),s.glow())));
        button(0,2,"Tilt: "+(int)s.tilt()+" degrees","Tilt from horizontal: 0 / 15 / 30 / 60 / 90 degrees. Applies to the mass disk, including its lensed images.",()->disk(new AccretionSettings(s.mode(),s.animation(),s.brightness(),s.outerRadius(),s.tilt()<15?15:s.tilt()<30?30:s.tilt()<60?60:s.tilt()<90?90:0,s.threshold(),s.glow())));
        button(1,2,"Auto: horizon "+(int)(s.threshold()*2)+"+ blocks","Auto requires a horizon diameter of at least 16 / 32 / 64 blocks. Default 32; compact 7x7x7 and larger masses meet it under normal calibration.",()->disk(new AccretionSettings(s.mode(),s.animation(),s.brightness(),s.outerRadius(),s.tilt(),s.threshold()<16?16:s.threshold()<32?32:8,s.glow())));
        button(0,3,"Disk defaults","Auto for large black holes, animated, 100% brightness, radius 10 horizons, tilt 15 degrees.",()->disk(AccretionSettings.defaults()));
        button(1,3,"Glow: "+(s.glow()==0?"Off":s.glow()<1?"Soft":s.glow()<2?"Strong":"Intense"),"Cinematic camera bloom from visible disk light only. Off / Soft / Strong / Intense. Does not illuminate terrain or alter light paths.",()->disk(new AccretionSettings(s.mode(),s.animation(),s.brightness(),s.outerRadius(),s.tilt(),s.threshold(),s.glow()==0?.6f:s.glow()<1?1.2f:s.glow()<2?2.4f:0)));
    }
    private void disk(AccretionSettings next) {
        try {AccretionDiskVisuals.apply(next);status="Disk settings applied and saved.";}
        catch(Exception e){status="Could not save disk settings; see log.";Interstellar.LOGGER.error(status,e);}
        clearAndInit();
    }
    private void tools() {
        upright(0,0);
        button(1,0,"Measure / stop FPS","F12 timing: 120 warmup and 300 measured frames. Press again to stop.",()->{close();LiveTerrain.benchmark(0);});
        button(0,1,"Rebuild world lighting","Refresh captured geometry and lighting. Preparation takes time.",()->{LiveTerrain.refreshLighting();status="Rebuilding world view...";});
        button(1,1,"Automatic mass selection","Remove any manually pinned inspection target. The strongest nearby cluster is selected automatically.",()->client.player.networkHandler.sendChatCommand("interstellar source auto"));
        button(0,2,"Frozen inspection (F9)","Development view and numerical checks. Esc returns to gameplay and restores your visual settings.",()-> {
            LiveTerrain.stop();client.setScreen(io.github.rohrl.interstellar.wormhole.WormholePair.active(client.world)?TerrainScreen.wormhole(false):new TerrainScreen(SelectedSource.current()));
        });
        button(1,2,"Optical lab (F8)","Separate educational sky/reference laboratory. Esc returns to gameplay and restores your visual settings.",()->{LiveTerrain.stop();client.setScreen(new OpticalLabScreen());});
    }
    private void relativity() {
        var s=RelativisticVision.options();
        button(0,0,"Potion visuals: "+on(s.enabled()),"Drink Relativistic Sight, then walk on foot in any direction. Normal movement speed. F10 is still the master visual switch.",()->sr(new RelativityOptions(!s.enabled(),s.aberration(),s.colour(),s.brightness(),s.cap(),s.rampSeconds())));
        button(1,0,"Aberration: "+on(s.aberration()),"Relativistic changes in viewing direction. Follow actual horizontal travel, even when looking sideways or walking backwards. Independent of colour and brightness.",()->sr(new RelativityOptions(s.enabled(),!s.aberration(),s.colour(),s.brightness(),s.cap(),s.rampSeconds())));
        button(0,1,"Doppler colour: "+s.colourName(),"Off / Gentle / Full shift. Gentle compresses frequency shifts. Assumed infrared/ultraviolet light and a faint exposure floor keep extreme views readable; material spectra are approximate.",()->sr(new RelativityOptions(s.enabled(),s.aberration(),(s.colour()+1)%3,s.brightness(),s.cap(),s.rampSeconds())));
        button(1,1,"Brightness / dimming: "+on(s.brightness()),"Directional brightening/dimming from the Doppler factor, with compressed exposure so the view stays playable. Independent of colour and aberration.",()->sr(new RelativityOptions(s.enabled(),s.aberration(),s.colour(),!s.brightness(),s.cap(),s.rampSeconds())));
        button(0,2,"Speed cap: "+String.format(java.util.Locale.ROOT,"%.2fc",s.cap()),"Cycle 0.50c / 0.90c / 0.99c. This is the simulated observer speed, not blocks travelled per second.",()->sr(new RelativityOptions(s.enabled(),s.aberration(),s.colour(),s.brightness(),s.cap()<.5?.5:s.cap()<.9?.9:s.cap()<.99?.99:.5,s.rampSeconds())));
        button(1,2,"Walk ramp: "+s.rampSeconds()+" seconds","Cycle 10 / 15 / 25 seconds of uninterrupted walking to reach the cap. Sideways/backwards movement and jumping continue; stopping or a wall releases the effect in at most a third of a second.",()->sr(new RelativityOptions(s.enabled(),s.aberration(),s.colour(),s.brightness(),s.cap(),s.rampSeconds()<10?10:s.rampSeconds()<15?15:s.rampSeconds()<25?25:10)));
        serverButton(0,3,"Get sight potion","Operator convenience. Also available in Creative Food & Drinks. Survival: Awkward Potion + Amethyst Shard. Lasts 2 minutes; milk removes it.","relativity potion");
        button(1,3,"Relativity defaults","All three effects on, gentle colours, 0.99c cap and 15-second ramp. Does not change graphics quality or activate the potion.",()->sr(RelativityOptions.defaults()));
    }
    private void sr(RelativityOptions next) {
        try {RelativisticVision.apply(next);status="Relativity controls saved. Movement speed stays normal.";}
        catch(Exception e){status="Could not save relativity controls; see log.";Interstellar.LOGGER.error(status,e);}
        clearAndInit();
    }
    private void upright(int x,int y) {
        var b=button(x,y,"Reset camera upright","Clear wormhole tilt, preserving position and aim. R is the dedicated shortcut.",()->{WormholeClient.resetOrientation();status="Camera reset requested.";});b.active=WormholeClient.canResetOrientation();
    }
    private void serverButton(int x,int y,String label,String tip,String command) {
        var b=button(x,y,label,tip,()->{client.player.networkHandler.sendChatCommand("interstellar "+command);status="Server setting requested.";});
        b.active=client.player!=null && client.player.hasPermissionLevel(2);
    }
    private ButtonWidget button(int x,int y,String text,String tip,Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(text),b->action.run()).dimensions(left+x*(column+8),top+30+y*24,column,20).tooltip(Tooltip.of(Text.literal(tip))).build());
    }
    @Override public void tick() {
        if(lastGravity!=WorldFeatures.gravityEnabled || lastCapture!=WorldFeatures.gravityCapture || lastStrength!=WorldFeatures.gravityStrength)clearAndInit();
    }
    private static String on(boolean value) {return value?"On":"Off";}
    private void quality(float scale,int aa,boolean fine,boolean rtx) {save(preferences.quality(scale,aa,fine,rtx));}
    private void save(TerrainOptions next) {
        try {next.save();LiveTerrain.applyPreferences(next);status="Visual settings applied and saved.";Interstellar.LOGGER.info("Interstellar settings saved: {}",next);}
        catch(Exception failure){status="Could not save settings; see log.";Interstellar.LOGGER.error(status,failure);}
        clearAndInit();
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        super.render(context,mouseX,mouseY,delta);
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,top-22,0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(status,width-16),width/2,top+134,0xC8D8E8);
    }
    @Override public boolean shouldPause() {return false;}
}
