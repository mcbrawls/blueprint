package net.mcbrawls.blueprint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.mcbrawls.blueprint.block.BlueprintBlocks
import net.mcbrawls.blueprint.block.entity.BlueprintBlockEntityTypes
import net.mcbrawls.blueprint.command.BlueprintCommand
import net.mcbrawls.blueprint.command.BlueprintEditorCommand
import net.mcbrawls.blueprint.entity.BlueprintEntityTypes
import net.mcbrawls.blueprint.item.BlueprintItems
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object BlueprintMod : ModInitializer {
    const val MOD_ID = "blueprint"
    const val MOD_NAME = "Blueprint"

    val logger: Logger = LoggerFactory.getLogger(MOD_NAME)

    override fun onInitialize() {
        logger.info("Initializing $MOD_NAME")

        BlueprintBlocks
        BlueprintItems
        BlueprintBlockEntityTypes
        BlueprintEntityTypes

        // register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BlueprintCommand.register(dispatcher)
            BlueprintEditorCommand.register(dispatcher)
        }

        // register resource listener
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(BlueprintManager)
    }
}

/**
 * Compares this block position with another and returns an ordered pair.
 */
fun BlockPos.asExtremeties(other: BlockPos): Pair<BlockPos, BlockPos> {
    val box = Box(Vec3d.of(this), Vec3d.of(other))
    val min = BlockPos.ofFloored(box.minX, box.minY, box.minZ)
    val max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
    return min to max
}
