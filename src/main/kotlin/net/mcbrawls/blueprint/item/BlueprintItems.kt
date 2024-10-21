package net.mcbrawls.blueprint.item

import eu.pb4.polymer.core.api.item.PolymerBlockItem
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object BlueprintItems {
    val POINT_REGION = register("point_region", PolymerBlockItem(BlueprintBlocks.POINT_REGION, Item.Settings(), Items.YELLOW_WOOL))

    private fun register(id: String, block: Item): Item {
        return Registry.register(Registries.ITEM, Identifier.of(BlueprintMod.MOD_ID, id), block)
    }
}
