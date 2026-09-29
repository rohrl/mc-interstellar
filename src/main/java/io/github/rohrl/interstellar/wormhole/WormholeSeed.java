package io.github.rohrl.interstellar.wormhole;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A reusable, bounded-range thrown anchor. Collision chooses the surface, never the camera. */
public final class WormholeSeed extends Item {
    public static final WormholeSeed ITEM=new WormholeSeed();
    public static final EntityType<SeedEntity> ENTITY=EntityType.Builder.<SeedEntity>create(SeedEntity::new,SpawnGroup.MISC)
        .disableSaving().dimensions(.25f,.25f).maxTrackingRange(4).trackingTickInterval(10).build("interstellar:wormhole_seed");
    private WormholeSeed(){super(new Settings().maxCount(1).rarity(Rarity.EPIC));}
    public static void register() {
        var id=Identifier.of("interstellar","wormhole_seed");Registry.register(Registries.ITEM,id,ITEM);Registry.register(Registries.ENTITY_TYPE,id,ENTITY);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries->entries.add(ITEM));
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        var stack=user.getStackInHand(hand);user.getItemCooldownManager().set(this,20);
        if(user.isSneaking()) {
            if(user instanceof ServerPlayerEntity player)WormholeState.clear(player);
            return TypedActionResult.success(stack,world.isClient);
        }
        world.playSound(null,user.getX(),user.getY(),user.getZ(),SoundEvents.ENTITY_SNOWBALL_THROW,SoundCategory.PLAYERS,.7f,.6f);
        if(!world.isClient) {
            var seed=new SeedEntity(ENTITY,world);seed.setOwner(user);seed.setPosition(user.getEyePos().add(0,-.1,0));seed.setItem(stack);
            seed.setVelocity(user,user.getPitch(),user.getYaw(),0,1.8f,0);world.spawnEntity(seed);
        }
        user.incrementStat(net.minecraft.stat.Stats.USED.getOrCreateStat(this));return TypedActionResult.success(stack,world.isClient);
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,java.util.List<Text> tooltip,net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal("Wormhole anchor — throw onto a distant surface."));
        tooltip.add(Text.literal("First end stays closed until you place the second."));
        tooltip.add(Text.literal("Later throws move the oldest end."));
        tooltip.add(Text.literal("Reusable. Aim far away; mouth radius: 8 blocks."));
        tooltip.add(Text.literal("Sneak + use closes the pair."));
        tooltip.add(Text.literal("Throwing in another dimension starts a new pair."));
    }
    public static final class SeedEntity extends ThrownItemEntity {
        public SeedEntity(EntityType<? extends SeedEntity> type,World world){super(type,world);}
        @Override protected Item getDefaultItem(){return ITEM;}
        @Override public void tick() {
            super.tick();if(!getWorld().isClient && age>80){fail("The seed expired before landing. Aim at a surface.");discard();}
        }
        private void fail(String message){if(getOwner() instanceof ServerPlayerEntity p)p.sendMessage(Text.literal("Wormhole: "+message),true);}
        @Override protected void onCollision(HitResult hit) {
            super.onCollision(hit);if(getWorld().isClient || isRemoved())return;
            if(hit instanceof BlockHitResult block && getOwner() instanceof ServerPlayerEntity player && player.getServerWorld()==getWorld()) {
                Vec3d centre=block.getPos().add(Vec3d.of(block.getSide().getVector()).multiply(WormholePair.METRIC.mouthRadius()+1));
                WormholeState.place(player,centre);
            } else fail("Aim at a block surface, clear of mobs.");
            discard();
        }
    }
}
