package net.mcbrawls.blueprint.block.entity

import eu.pb4.polymer.core.api.block.PolymerBlockUtils
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.datafixer.TypeReferences
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.Util

object BlueprintBlockEntityTypes {
    val REGION_ID = register("region_id", BlockEntityType.Builder.create(::RegionIdBlockEntity, BlueprintBlocks.POINT_REGION))

    private fun <T : BlockEntity> register(id: String, builder: BlockEntityType.Builder<T>): BlockEntityType<T> {
        val identifier = Identifier.of(BlueprintMod.MOD_ID, id)
        val datafixType = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, identifier.toString());
        val type = builder.build(datafixType)
        PolymerBlockUtils.registerBlockEntity(type)
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, identifier, type);
    }
}
