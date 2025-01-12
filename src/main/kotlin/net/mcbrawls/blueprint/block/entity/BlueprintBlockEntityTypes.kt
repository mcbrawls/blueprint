package net.mcbrawls.blueprint.block.entity

import eu.pb4.polymer.core.api.block.PolymerBlockUtils
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object BlueprintBlockEntityTypes {
    val REGION_ID = register("region_id", FabricBlockEntityTypeBuilder.create(::RegionIdBlockEntity, BlueprintBlocks.POINT_REGION))

    private fun <T : BlockEntity> register(id: String, builder: FabricBlockEntityTypeBuilder<T>): BlockEntityType<T> {
        val type = builder.build()
        PolymerBlockUtils.registerBlockEntity(type)
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(BlueprintMod.MOD_ID, id), type);
    }
}
