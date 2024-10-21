package net.mcbrawls.blueprint.editor.block

import net.mcbrawls.blueprint.block.entity.RegionIdBlockEntity
import net.mcbrawls.blueprint.editor.gui.RegionIdInputGui
import net.mcbrawls.blueprint.region.serialization.SerializableRegion
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class RegionBlock(settings: Settings) : BlockWithEntity(settings) {
    abstract fun saveRegion(
        world: World,
        pos: BlockPos,
        relativePos: BlockPos,
        blockEntity: RegionIdBlockEntity,
    ) : SerializableRegion

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {
        super.onPlaced(world, pos, state, placer, itemStack)

        if (placer is ServerPlayerEntity) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is RegionIdBlockEntity) {
                openRegionIdEditorGui(placer, blockEntity)
            }
        }
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        if (player is ServerPlayerEntity) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is RegionIdBlockEntity) {
                if (player.isSneaky) {
                    val id = blockEntity.id
                    player.sendMessage(Text.literal("Region ID: \"$id\""), true)
                } else {
                    openRegionIdEditorGui(player, blockEntity)
                }

                return ActionResult.SUCCESS
            }
        }

        return ActionResult.PASS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RegionIdBlockEntity(pos, state)
    }

    companion object {
        /**
         * Tries to save a region from the given information.
         * @return whether the provided values give a serializable block position
         */
        fun trySaveRegion(
            world: ServerWorld,
            pos: BlockPos,
            relativePos: BlockPos,
            state: BlockState,
            regions: MutableMap<String, SerializableRegion>
        ): Boolean {
            val block = state.block
            if (block is RegionBlock) {
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is RegionIdBlockEntity) {
                    val regionId = blockEntity.getOrCreateRegionId()
                    val region = block.saveRegion(world, pos, relativePos, blockEntity)
                    regions[regionId] = region
                }

                return true
            }

            return false
        }

        /**
         * Opens the region id editor gui for a given region id block entity.
         */
        fun openRegionIdEditorGui(player: ServerPlayerEntity, blockEntity: RegionIdBlockEntity) {
            val regionId = blockEntity.getOrCreateRegionId()

            val gui = RegionIdInputGui(player) { gui, input ->
                if (input != regionId) {
                    if (input.isBlank()) {
                        val id = blockEntity.id
                        gui.player.sendMessage(Text.literal("No region ID set. Still: \"$id\"").formatted(Formatting.RED))
                    } else {
                        val processedInput = input.trim()
                        blockEntity.id = processedInput
                        gui.player.sendMessage(Text.literal("Set region ID: \"$processedInput\"").formatted(Formatting.GREEN))
                    }
                }

                true
            }

            gui.setDefaultInputValue(regionId)

            gui.open()
        }
    }
}
