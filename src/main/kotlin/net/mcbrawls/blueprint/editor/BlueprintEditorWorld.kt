package net.mcbrawls.blueprint.editor

import net.fabricmc.fabric.api.networking.v1.PlayerLookup.world
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import xyz.nucleoid.fantasy.RuntimeWorld
import xyz.nucleoid.fantasy.RuntimeWorldConfig
import kotlin.math.max
import kotlin.math.min

class BlueprintEditorWorld(
    val blueprintId: Identifier,
    server: MinecraftServer,
    key: RegistryKey<World>,
    config: RuntimeWorldConfig
) : RuntimeWorld(server, key, config, Style.TEMPORARY) {
    private var minX = BLUEPRINT_PLACEMENT_POS.x
    private var minY = BLUEPRINT_PLACEMENT_POS.y
    private var minZ = BLUEPRINT_PLACEMENT_POS.z

    private var maxX = minX
    private var maxY = minY
    private var maxZ = minZ

    val minPos: BlockPos get() = BlockPos(minX, minY, minZ)
    val maxPos: BlockPos get() = BlockPos(maxX, maxY, maxZ)

    /**
     * Initializes this world with the blueprint id.
     * @return whether this created a new blueprint
     */
    internal fun initializeBlueprint(): Boolean {
        val blueprint = BlueprintManager[blueprintId]
        return if (blueprint != null) {
            val size = blueprint.size
            maxX += size.x
            maxY += size.y
            maxZ += size.z

            blueprint.place(this, BLUEPRINT_PLACEMENT_POS)
            false
        } else {
            setBlockState(minPos, Blocks.STONE.defaultState)
            true
        }
    }

    override fun setBlockState(pos: BlockPos, state: BlockState, flags: Int, maxUpdateDepth: Int): Boolean {
        return if (super.setBlockState(pos, state, flags, maxUpdateDepth)) {
            if (!state.isAir) {
                pos.x.also {
                    minX = min(minX, it)
                    maxX = max(maxX, it)
                }

                pos.y.also {
                    minY = min(minY, it)
                    maxY = max(maxY, it)
                }

                pos.z.also {
                    minZ = min(minZ, it)
                    maxZ = max(maxZ, it)
                }
            }

            true
        } else {
            false
        }
    }

    /**
     * Calculates the total bounding box of the Blueprint within the world.
     * @return a pair of min/max positions
     */
    fun getBlueprintBoundingBox(): Pair<BlockPos, BlockPos> {
        var furthestMin = BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
        var furthestMax = BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE)

        BlockPos.iterate(minPos, maxPos).forEach { pos ->
            val state = getBlockState(pos)

            if (!state.isAir) {
                furthestMin = BlockPos(
                    minOf(furthestMin.x, pos.x),
                    minOf(furthestMin.y, pos.y),
                    minOf(furthestMin.z, pos.z),
                )

                furthestMax = BlockPos(
                    maxOf(furthestMax.x, pos.x),
                    maxOf(furthestMax.y, pos.y),
                    maxOf(furthestMax.z, pos.z),
                )
            }
        }

        return Pair(furthestMin, furthestMax)
    }

    /**
     * Saves the built Blueprint to disk.
     */
    fun saveBlueprint(): String {
        val (min, max) = getBlueprintBoundingBox()
        return Blueprint.save(this, min, max, blueprintId)
    }

    companion object {
        val BLUEPRINT_PLACEMENT_POS = BlockPos(0, 128, 0)
    }
}
