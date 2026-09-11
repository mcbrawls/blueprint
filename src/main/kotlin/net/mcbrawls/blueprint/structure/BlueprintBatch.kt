package net.mcbrawls.blueprint.structure

import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

object BlueprintBatch {
    fun place(world: ServerWorld, entries: Set<Entry>): ProgressiveFuture<BatchResult> {
        val progress = AtomicReference(0.0f)

        val blueprints = entries.associateWith {
            val id = it.id
            BlueprintManager[id] ?: throw IllegalArgumentException("No blueprint found: $id")
        }

        val totalBlocks = blueprints.values.sumOf(Blueprint::totalBlocks)

        val future: CompletableFuture<BatchResult> = CompletableFuture.supplyAsync {
            val placedBlueprints = blueprints.map { (entry, blueprint) ->
                val pos = entry.pos

                synchronized(world) {
                    var i = 0
                    val blockPos = BlockPos.Mutable()
                    blueprint.forEach { x, y, z, state, blockEntityNbt ->
                        Blueprint.placePosition(world, blockPos.set(pos.x + x, pos.y + y, pos.z + z), state, blockEntityNbt, entry.processor)
                        progress.set(++i / totalBlocks.toFloat())
                    }
                }

                PlacedBlueprint(blueprint, pos)
            }

            BatchResult(placedBlueprints.toSet())
        }

        return ProgressiveFuture(future, ProgressProvider(progress::get))
    }

    data class Entry(
        val id: Identifier,
        val pos: BlockPos,
        val processor: BlockStateProcessor? = null
    )

    class BatchResult(val placedBlueprints: Set<PlacedBlueprint>) {
        fun getAnchors(id: String): List<Anchor> {
            return placedBlueprints.flatMap { it.getAnchors(id) }
        }

        fun getUniqueAnchor(id: String): Anchor? {
            return placedBlueprints.firstNotNullOfOrNull { it.getUniqueAnchor(id) }
        }
    }
}
