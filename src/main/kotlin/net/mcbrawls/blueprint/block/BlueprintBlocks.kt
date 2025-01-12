package net.mcbrawls.blueprint.block

import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.block.region.PointRegionBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object BlueprintBlocks {
    val POINT_REGION = register("point_region", AbstractBlock.Settings.create().dropsNothing(), ::PointRegionBlock)

    private fun register(id: String, settings: AbstractBlock.Settings, factory: (AbstractBlock.Settings) -> Block): Block {
        return register(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(BlueprintMod.MOD_ID, id)), settings, factory)
    }

    private fun register(key: RegistryKey<Block>, settings: AbstractBlock.Settings, factory: (AbstractBlock.Settings) -> Block): Block {
        val block = factory.invoke(settings.registryKey(key))
        return Registry.register(Registries.BLOCK, key, block)
    }
}
