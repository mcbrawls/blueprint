package net.mcbrawls.blueprint.item

import eu.pb4.polymer.core.api.item.PolymerBlockItem
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object BlueprintItems {
    val POINT_REGION = register("point_region") { settings -> PolymerBlockItem(BlueprintBlocks.POINT_REGION, settings, Items.YELLOW_WOOL) }

    private fun register(id: String, settings: Item.Settings = Item.Settings(), factory: (Item.Settings) -> Item): Item {
        return register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(BlueprintMod.MOD_ID, id)), settings, factory)
    }

    private fun register(key: RegistryKey<Item>, settings: Item.Settings, factory: (Item.Settings) -> Item): Item {
        val block = factory.invoke(settings.registryKey(key))
        return Registry.register(Registries.ITEM, key, block)
    }
}
