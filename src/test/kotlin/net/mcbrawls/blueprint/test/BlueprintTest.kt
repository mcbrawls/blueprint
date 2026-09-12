package net.mcbrawls.blueprint.test

import dev.andante.codex.encodeQuick
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.region.CuboidRegion
import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.blueprint.structure.BlockStore
import net.mcbrawls.blueprint.structure.Blueprint
import net.mcbrawls.blueprint.structure.BlueprintBatch
import net.mcbrawls.blueprint.structure.ProgressProvider
import net.minecraft.block.Blocks
import net.minecraft.nbt.NbtOps
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d

object BlueprintTest : ModInitializer {
    private var displayedProgress: ProgressProvider? = null

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            PlacementVerify.register(dispatcher)

            dispatcher.register(
                CommandManager.literal("blueprint-test")
                    .executes { context ->
                        runCatching {
                            val b = Blueprint(
                                emptyList(),
                                BlockStore.EMPTY,
                                emptyMap(),
                                mapOf(
                                    "point" to PointRegion(Vec3d.ZERO),
                                    "cuboid" to CuboidRegion(Vec3d.ZERO, Vec3d.ZERO)
                                ),
                                listOf(
                                    "test" to Anchor(Vec3d(10.0, 2.0, 1.0), Vec2f(90.0f, 0.0f), "Custom data, anything here!"),
                                )
                            )
                            println(Blueprint.CODEC.encodeQuick(NbtOps.INSTANCE, b))

                            //

                            val pos = context.source.position
                            val blockPos = BlockPos.ofFloored(pos)
                            val (future, progress) = BlueprintManager[Identifier.of(
                                BlueprintMod.MOD_ID,
                                "test"
                            )]!!.placeWithProgress(context.source.world, blockPos)

                            if (displayedProgress == null) {
                                displayedProgress = progress
                            }
                            future.thenAccept { blueprint ->
                                if (displayedProgress === progress) {
                                    displayedProgress = null
                                }

                                println(blueprint.blueprint.size)
                            }
                        }.exceptionOrNull()?.printStackTrace()
                        1
                    }
            )

            dispatcher.register(
                CommandManager.literal("blueprint-test-batch")
                    .executes { context ->
                        runCatching {
                            val pos = context.source.position
                            val blockPos = BlockPos.ofFloored(pos)
                            val (future, progress) = BlueprintBatch.place(context.source.world, setOf(
                                BlueprintBatch.Entry(Identifier.of("blueprint", "anchor"), blockPos),
                                BlueprintBatch.Entry(Identifier.of("blueprint", "block_entity_test"), blockPos.add(10, 0, 0)),
                                BlueprintBatch.Entry(Identifier.of("blueprint", "wow"), blockPos.add(20, 0, 0)),
                                BlueprintBatch.Entry(Identifier.of("blueprint", "test"), blockPos.add(30, 0, 0)) { state ->
                                    if (state.block == Blocks.STONE) {
                                        Blocks.WHITE_WOOL.defaultState
                                    } else {
                                        state
                                    }
                                },
                            ))

                            if (displayedProgress == null) {
                                displayedProgress = progress
                            }
                            future.thenAccept { blueprint ->
                                if (displayedProgress === progress) {
                                    displayedProgress = null
                                }

                                println(blueprint)
                            }
                        }.exceptionOrNull()?.printStackTrace()
                        1
                    }
            )
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.playerManager.playerList.forEach { player ->
                player.sendMessage(Text.literal("${displayedProgress?.getProgress()?.times(100)?.toInt()}%"), true)
            }
        }
    }
}
