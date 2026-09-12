package net.mcbrawls.blueprint.test

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.argument.IdentifierArgumentType
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

/**
 * Places a blueprint and reads every one of its blocks back from the world, to catch placements which only partly
 * land. Direct chunk section writes have no vanilla bookkeeping to fall back on, so this is the check that a placement
 * path actually wrote what it was asked to.
 */
object PlacementVerify {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("blueprint-verify")
                .then(
                    CommandManager.argument("id", IdentifierArgumentType.identifier())
                        .executes { context ->
                            val id = IdentifierArgumentType.getIdentifier(context, "id")
                            verify(context.source, id)
                            1
                        }
                )
                .executes { context ->
                    verify(context.source, Identifier.of("blueprint", "test"))
                    1
                }
        )
    }

    private fun verify(source: ServerCommandSource, id: Identifier) {
        val blueprint = BlueprintManager[id]
        if (blueprint == null) {
            source.sendError(Text.literal("No blueprint found: $id"))
            return
        }

        val world = source.world
        val origin = BlockPos.ofFloored(source.position)

        val started = System.nanoTime()
        blueprint.place(world, origin)
        val took = (System.nanoTime() - started) / 1_000_000

        val mismatches = countMismatches(blueprint, world, origin)
        val message = if (mismatches == 0) {
            "Placed and verified all ${blueprint.totalBlocks} blocks of $id in ${took}ms"
        } else {
            "Placed $id in ${took}ms but $mismatches of ${blueprint.totalBlocks} blocks did not land"
        }

        source.sendFeedback({ Text.literal(message) }, false)
    }

    private fun countMismatches(blueprint: Blueprint, world: ServerWorld, origin: BlockPos): Int {
        var mismatches = 0
        val pos = BlockPos.Mutable()

        blueprint.forEach { x, y, z, state, _ ->
            pos.set(origin.x + x, origin.y + y, origin.z + z)
            if (world.getBlockState(pos) !== state) {
                mismatches++
            }
        }

        return mismatches
    }
}
