package io.github.rohrl.interstellar.source;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SourceBlocks {
    private SourceBlocks() { }
    public static final MassBlock MASS_BLOCK = new MassBlock(AbstractBlock.Settings.create().strength(4,1200));
    public static void register() {
        Identifier id=Identifier.of("interstellar","mass_block");
        Registry.register(Registries.BLOCK,id,MASS_BLOCK);
        var item=Registry.register(Registries.ITEM,id,new BlockItem(MASS_BLOCK,new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries->entries.add(item));
        SourceInspector.register();
    }
}
