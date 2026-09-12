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
            // palettes are processed off the server thread; only the writes themselves need to be on it
            val palettes = blueprints.mapValues { (entry, blueprint) -> blueprint.processedPalette(entry.processor) }
            var i = 0

            // one placement session for the whole batch, so entries sharing a chunk cost one flush between them
            BulkPlacement.onServerThread(world) {
                BulkPlacement(world).use { placement ->
                    blueprints.forEach { (entry, blueprint) ->
                        val pos = entry.pos

                        blueprint.forEach(palettes.getValue(entry)) { x, y, z, state, blockEntityNbt ->
                            placement.setBlock(pos.x + x, pos.y + y, pos.z + z, state, blockEntityNbt)
                            progress.set(++i / totalBlocks.toFloat())
                        }
                    }
                }
            }

            BatchResult(blueprints.map { (entry, blueprint) -> PlacedBlueprint(blueprint, entry.pos) }.toSet())
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
