package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.relativity.RelativisticPotion;
import io.github.rohrl.interstellar.science.SprintObserver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import java.util.Locale;

/** Client-only prescribed observer. Potion availability is synchronized by vanilla. */
final class RelativisticVision {
    private static RelativityOptions options=RelativityOptions.load();
    private static final SprintObserver envelope=new SprintObserver();
    private static ClientWorld world;
    private static Vec3d previousPosition,direction=new Vec3d(0,0,1),previousDirection=direction;
    private static double previousBeta;
    private static boolean running;
    static boolean prepared;
    private RelativisticVision(){}
    static RelativityOptions options(){return options;}
    static boolean potion(){var p=MinecraftClient.getInstance().player;return p!=null&&p.hasStatusEffect(RelativisticPotion.EFFECT);}
    static boolean wanted(){return options.enabled()&&(options.aberration()||options.colour()!=0||options.brightness())&&potion();}
    static boolean visible(){return envelope.beta()>1e-6;}
    static void apply(RelativityOptions next) throws java.io.IOException {
        next.save();options=next;
        if(!wanted()){envelope.reset();previousBeta=0;}
        Interstellar.LOGGER.info("Relativity settings saved: {}",next);
    }
    static void tick(MinecraftClient client) {
        if(client.world!=world){world=client.world;envelope.reset();previousBeta=0;previousPosition=null;prepared=false;options=RelativityOptions.load();}
        var p=client.player;if(p==null)return;
        if(client.isPaused()||client.currentScreen instanceof TerrainScreen||client.currentScreen instanceof OpticalLabScreen)return;
        var pos=p.getPos();var movement=previousPosition==null?Vec3d.ZERO:pos.subtract(previousPosition);previousPosition=pos;
        previousBeta=envelope.beta();previousDirection=direction;
        double length=movement.horizontalLength();
        // A portal transports velocity; its coordinate displacement is not running.
        if(movement.lengthSquared()>16){movement=p.getVelocity();length=movement.horizontalLength();}
        boolean wasRunning=running;
        running=wanted()&&LiveTerrain.active()&&prepared&&p.isSprinting()&&!p.getAbilities().flying&&!p.isFallFlying()&&!p.isSwimming()&&length>.015;
        if(running) {
            var target=new Vec3d(movement.x,0,movement.z).normalize();
            direction=previousBeta==0||direction.dotProduct(target)<-.8?target:direction.lerp(target,.45).normalize();
        }
        envelope.advance(running,.05,options.cap(),options.rampSeconds());
        if(!wanted()||!LiveTerrain.active()){envelope.reset();previousBeta=0;}
        if(wasRunning!=running)Interstellar.LOGGER.info("Relativistic sprint {}: beta={}, direction={}, prepared={}",running?"started":"stopped",envelope.beta(),direction,prepared);
        if(previousBeta<options.cap() && envelope.beta()>=options.cap())Interstellar.LOGGER.info("Relativistic sprint reached {}c",options.cap());
    }
    static Vec3d velocity(boolean live) {
        if(!wanted()||!LiveTerrain.preferences().enabled())return Vec3d.ZERO;
        var client=MinecraftClient.getInstance();float t=live?client.getRenderTickCounter().getTickDelta(false):1;
        double beta=previousBeta+(envelope.beta()-previousBeta)*t;
        var aim=previousDirection.dotProduct(direction)<-.8?direction:previousDirection.lerp(direction,t).normalize();
        return aim.multiply(beta);
    }
    static void hud(DrawContext context) {
        var client=MinecraftClient.getInstance();if(client.player==null||client.options.hudHidden||!potion())return;
        String text;
        if(!options.enabled()||!LiveTerrain.active())text="Relativistic sight: paused | F4";
        else if(!wanted())text="Relativistic sight: all effects off | F4";
        else if(!prepared)text="Relativistic sight: preparing world view...";
        else if(visible())text=String.format(Locale.ROOT,"Relativistic sight  %.2fc / %.2fc%s",envelope.beta(),options.cap(),running?"":"  slowing");
        else text="Relativistic sight  0.00c | Sprint on foot to charge";
        int x=8,y=context.getScaledWindowHeight()-66,w=Math.min(context.getScaledWindowWidth()-16,client.textRenderer.getWidth(text)+12);
        context.fill(x,y,x+w,y+24,0xBD101824);
        context.drawTextWithShadow(client.textRenderer,client.textRenderer.trimToWidth(text,w-12),x+6,y+5,0xFFC7B4FF);
        context.fill(x+6,y+18,x+w-6,y+20,0xFF44425B);
        context.fill(x+6,y+18,x+6+(int)((w-12)*Math.min(1,envelope.beta()/options.cap())),y+20,0xFFB49AFF);
    }
}
