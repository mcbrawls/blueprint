package net.mcbrawls.blueprint.editor

import net.mcbrawls.blueprint.region.serialization.SerializableRegion
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
import java.util.function.BooleanSupplier
import kotlin.math.max
import kotlin.math.min

class BlueprintEditorWorld(
    val blueprintId: Identifier,
    server: MinecraftServer,
    key: RegistryKey<World>,
    config: RuntimeWorldConfig,
) : RuntimeWorld(server, key, config, Style.TEMPORARY) {
    val sourceBlueprint: Blueprint? get() = BlueprintManager[blueprintId]

    private var minX = BLUEPRINT_PLACEMENT_POS.x
    private var minY = BLUEPRINT_PLACEMENT_POS.y
    private var minZ = BLUEPRINT_PLACEMENT_POS.z

    private var maxX = minX
    private var maxY = minY
    private var maxZ = minZ

    val minPos: BlockPos get() = BlockPos(minX, minY, minZ)
    val maxPos: BlockPos get() = BlockPos(maxX, maxY, maxZ)

    private val regions: MutableMap<String, SerializableRegion> = mutableMapOf()

    /**
     * Initializes this world with the blueprint id.
     * @return whether this created a new blueprint
     */
    internal fun initializeBlueprint(): Boolean {
        val blueprint = sourceBlueprint
        return if (blueprint != null) {
            val size = blueprint.size
            maxX += size.x
            maxY += size.y
            maxZ += size.z

            blueprint.place(this, BLUEPRINT_PLACEMENT_POS)
            blueprint.placeCreatorMarkers(this, BLUEPRINT_PLACEMENT_POS)
            regions.putAll(blueprint.regions)
            false
        } else {
            setBlockState(minPos, Blocks.STONE.defaultState)
            true
        }
    }

    override fun setBlockState(pos: BlockPos, state: BlockState, flags: Int, maxUpdateDepth: Int): Boolean {
        return if (super.setBlockState(pos, state, flags, maxUpdateDepth)) {
            if (!state.isAir) {
                updateExpectedSize(pos)
            }

            true
        } else {
            false
        }
    }

    override fun tick(shouldKeepTicking: BooleanSupplier) {
        super.tick(shouldKeepTicking)

        players.forEach { player ->
            updateExpectedSize(player.blockPos)
        }

        /*regions.forEach { _, region ->
            if (region is CuboidRegion) {
                val pos = region.rootPosition.add(Vec3d.of(BLUEPRINT_PLACEMENT_POS))
                spawnParticles(DustParticleEffect(0xFF0000, 1.0f), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0)
                val otherPos = region.rootPosition.add(region.size).add(Vec3d.of(BLUEPRINT_PLACEMENT_POS))
                spawnParticles(DustParticleEffect(0x0000FF, 1.0f), otherPos.x, otherPos.y, otherPos.z, 1, 0.0, 0.0, 0.0, 0.0)
            }
            if (region is SphericalRegion) {
                val pos = region.rootPosition.add(Vec3d.of(BLUEPRINT_PLACEMENT_POS))
                spawnParticles(DustParticleEffect(0xFF00FF, 1.0f), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }*/
    }

    private fun updateExpectedSize(pos: BlockPos) {
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

    /**
     * Calculates the total bounding box of the Blueprint within the world.
     * @return a pair of min/max positions
     */
    fun getRoughBlueprintBoundingBox(): Pair<BlockPos, BlockPos> {
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
    fun saveBlueprint(id: Identifier? = null): String {
        val (min, max) = getRoughBlueprintBoundingBox()
        val blueprint = Blueprint.save(this, min, max).let {
            it.copy(regions = it.regions + regions)
        }

        return BlueprintManager.saveGenerated(server, id ?: blueprintId, blueprint)
    }

    fun addRegion(id: String, region: SerializableRegion) {
        regions[id] = region
    }

    companion object {
        val BLUEPRINT_PLACEMENT_POS = BlockPos(0, 128, 0)
    }
}
