package net.mcbrawls.blueprint.test

import dev.andante.audience.AudienceInitializer.server
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.blueprint.structure.ProgressProvider
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object BlueprintTest : ModInitializer {
    private var displayedProgress: ProgressProvider? = null

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("blueprint-test")
                    .executes { context ->
                        val pos = context.source.position
                        val blockPos = BlockPos.ofFloored(pos)
                        val (future, progress) = BlueprintManager[Identifier.of(BlueprintMod.MOD_ID, "test")]!!.placeWithProgress(context.source.world, blockPos)
                        if (displayedProgress == null) {
                            displayedProgress = progress
                        }
                        future.thenRun {
                            if (displayedProgress === progress) {
                                displayedProgress = null
                            }
                        }
                        1
                    }
            )
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.playerManager.playerList.forEach { player ->
                player.actionBar(Text.literal("${displayedProgress?.getProgress()}"))
            }
        }
    }
}
