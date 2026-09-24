package io.github.rohrl.interstellar.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.rohrl.interstellar.gravity.GravityVisualPayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.text.Text;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

/** Session controls for optional visuals and the server's current local gravity strength. */
final class WorldFeatures {
    static boolean body=true,weather=true,gravityEnabled;
    static double gravityStrength;
    private WorldFeatures() {}
    static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->gravityEnabled=false);
        ClientPlayNetworking.registerGlobalReceiver(GravityVisualPayload.ID,(p,context)-> {
            gravityEnabled=p.enabled() && Double.isFinite(p.strength()) && p.strength()>=0 && p.strength()<=.2;gravityStrength=p.strength();
        });
        // A distinct client root must not shadow the server's /interstellar commands.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(literal("interstellar-visuals")
                        .then(literal("body").then(argument("enabled",BoolArgumentType.bool()).executes(context->{
                            body=BoolArgumentType.getBool(context,"enabled");context.getSource().sendFeedback(Text.literal("Returning player-body images: "+body));return 1;
                        })))
                        .then(literal("weather").then(argument("enabled",BoolArgumentType.bool()).executes(context->{
                            weather=BoolArgumentType.getBool(context,"enabled");context.getSource().sendFeedback(Text.literal("Local foreground weather: "+weather));return 1;
                        })))));
    }
}
