package io.github.rohrl.interstellar.relativity;

import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import static net.minecraft.server.command.CommandManager.literal;

/** A normal synchronized status effect. It never changes movement or the server clock. */
public final class RelativisticPotion {
    private RelativisticPotion() {}
    public static final RegistryEntry<StatusEffect> EFFECT=Registry.registerReference(Registries.STATUS_EFFECT,
        Identifier.of(Interstellar.MOD_ID,"relativistic_sight"),new StatusEffect(StatusEffectCategory.BENEFICIAL,0x9474ED){});
    public static final RegistryEntry<Potion> POTION=Registry.registerReference(Registries.POTION,
        Identifier.of(Interstellar.MOD_ID,"relativistic_sight"),new Potion("relativistic_sight",new StatusEffectInstance(EFFECT,8*60*20,0,false,false,true)));
    public static ItemStack stack(){return PotionContentsComponent.createStack(Items.POTION,POTION);}
    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries->entries.add(stack()));
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder->builder.registerPotionRecipe(Potions.AWKWARD,Items.AMETHYST_SHARD,POTION));
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("interstellar")
            .then(literal("relativity").then(literal("potion").requires(s->s.hasPermissionLevel(2)).executes(context->{
                var player=context.getSource().getPlayerOrThrow();var stack=stack();
                if(!player.getInventory().insertStack(stack))player.dropItem(stack,false);
                player.sendMessage(Text.literal("Relativistic Sight: drink, wait for the view to prepare, then sprint. Normal movement speed. F4: Relativity."),false);return 1;
            })))));
    }
}
