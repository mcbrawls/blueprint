package net.mcbrawls.blueprint.editor.block

import net.mcbrawls.blueprint.BlueprintMod
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object BlueprintEditorBlocks {
    val POINT_REGION = register("point_region", PointRegionBlock(AbstractBlock.Settings.create().dropsNothing()))

    private fun register(id: String, block: Block): Block {
        return Registry.register(Registries.BLOCK, Identifier.of(BlueprintMod.MOD_ID, id), block)
    }
}
