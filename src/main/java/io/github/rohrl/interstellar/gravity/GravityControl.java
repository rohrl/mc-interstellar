package io.github.rohrl.interstellar.gravity;

import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.Locale;
import static net.minecraft.server.command.CommandManager.*;

/** Runtime controls and opt-in timings; no clocks/readback in the ordinary physics path. */
public final class GravityControl {
    private GravityControl() { }
    private static boolean measuring;
    private static ServerWorld world;
    private static long since;
    private static final long[] nanos=new long[3],maximum=new long[3],calls=new long[3];
    public static long begin() {return measuring?System.nanoTime():0;}
    public static void end(int kind,long started) {
        if(started==0)return;long elapsed=System.nanoTime()-started;nanos[kind]+=elapsed;maximum[kind]=Math.max(maximum[kind],elapsed);calls[kind]++;
    }
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{measuring=false;world=null;});
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("interstellar")
                .then(literal("gravity").requires(source->source.hasPermissionLevel(2))
                        .then(literal("status").executes(context->{
                            context.getSource().sendFeedback(()->Text.literal("Entity gravity "+GravitySources.config.enabled+", capture "+GravitySources.config.capture
                                    +", strength/block "+GravitySources.config.strengthPerBlock+"; body radius limit16, horizon limit12; legacy exhibit excluded"),false);return 1;
                        }))
                        .then(literal("enabled").then(argument("value",BoolArgumentType.bool()).executes(context->{
                            GravitySources.config.enabled=BoolArgumentType.getBool(context,"value");
                            context.getSource().sendFeedback(()->Text.literal("Entity gravity: "+GravitySources.config.enabled+" (this server session)"),true);return 1;
                        })))
                        .then(literal("capture").then(argument("value",BoolArgumentType.bool()).executes(context->{
                            GravitySources.config.capture=BoolArgumentType.getBool(context,"value");
                            context.getSource().sendFeedback(()->Text.literal("Horizon capture: "+GravitySources.config.capture+" (this server session)"),true);return 1;
                        })))
                        .then(literal("profile")
                                .then(literal("start").executes(context->{
                                    world=context.getSource().getWorld();since=world.getTime();
                                    Arrays.fill(nanos,0);Arrays.fill(maximum,0);Arrays.fill(calls,0);measuring=true;
                                    context.getSource().sendFeedback(()->Text.literal("Gravity timing started; use /interstellar gravity profile stop"),false);return 1;
                                }))
                                .then(literal("stop").executes(context->{
                                    if(!measuring) {context.getSource().sendFeedback(()->Text.literal("No gravity timing session"),false);return 0;}
                                    measuring=false;long ticks=Math.max(1,world.getTime()-since);
                                    String[] names={"source","mob movement","projectile tick"};
                                    for(int i=0;i<3;i++) {
                                        long divisor=i==0?Math.max(1,calls[i]):ticks;
                                        String report=String.format(Locale.ROOT,"Gravity profile enabled=%s capture=%s | %s: worldTicks=%d calls=%d total=%.3fms mean/%s=%.4fms max/call=%.4fms",
                                                GravitySources.config.enabled,GravitySources.config.capture,names[i],ticks,calls[i],nanos[i]/1e6,i==0?"serverCallback":"worldTick",nanos[i]/1e6/divisor,maximum[i]/1e6);
                                        Interstellar.LOGGER.info(report);context.getSource().sendFeedback(()->Text.literal(report),false);
                                    }
                                    world=null;return 1;
                                }))))));
    }
}
