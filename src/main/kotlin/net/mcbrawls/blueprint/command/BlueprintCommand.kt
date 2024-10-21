package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import dev.andante.codex.encodeQuick
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.ModMetadata
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.BlueprintMod.MOD_NAME
import net.mcbrawls.blueprint.asExtremeties
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.blueprint.structure.Blueprint
import net.mcbrawls.blueprint.structure.BlueprintBlockEntity
import net.mcbrawls.blueprint.structure.PalettedState
import net.minecraft.block.BlockState
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import java.nio.file.Path

object BlueprintCommand {
    const val BLUEPRINT_KEY = "blueprint"
    const val POSITION_KEY = "position"
    const val START_POSITION_KEY = "start_position"
    const val END_POSITION_KEY = "end_position"

    private val INVALID_BLUEPRINT_EXCEPTION_TYPE = DynamicCommandExceptionType { id -> Text.literal("There is no blueprint with id \"$id\"") }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("blueprint")
                .executes(::execute)
                .requires { it.hasPermissionLevel(2) }
                .then(
                    literal("save")
                        .then(
                            argument(START_POSITION_KEY, BlockPosArgumentType.blockPos())
                                .then(
                                    argument(END_POSITION_KEY, BlockPosArgumentType.blockPos())
                                        .then(
                                            argument(BLUEPRINT_KEY, IdentifierArgumentType.identifier())
                                                .suggests { _, suggestions -> BlueprintManager.suggestBlueprints(suggestions) }
                                                .executes(::executeSave)
                                        )
                                )
                        )
                )
                .then(
                    literal("place")
                        .then(
                            argument(POSITION_KEY, BlockPosArgumentType.blockPos())
                                .then(
                                    argument(BLUEPRINT_KEY, IdentifierArgumentType.identifier())
                                        .suggests { _, suggestions -> BlueprintManager.suggestBlueprints(suggestions) }
                                        .executes(::executePlace)
                                )
                        )
                )
        )
    }

    private fun executeSave(context: CommandContext<ServerCommandSource>): Int {
        val world = context.source.world

        // gather arguments
        val blueprintId = IdentifierArgumentType.getIdentifier(context, BLUEPRINT_KEY)

        val inputStartPosition = BlockPosArgumentType.getLoadedBlockPos(context, START_POSITION_KEY)
        val inputEndPosition = BlockPosArgumentType.getLoadedBlockPos(context, END_POSITION_KEY)

        // order positions
        val (min, max) = inputStartPosition.asExtremeties(inputEndPosition)

        // save
        val pathString = Blueprint.save(world, min, max, blueprintId)

        // feedback
        context.source.sendFeedback({ Text.literal("Saved blueprint to \"$pathString\"") }, true)

        return 1
    }

    private fun executePlace(context: CommandContext<ServerCommandSource>): Int {
        val blueprintId = IdentifierArgumentType.getIdentifier(context, BLUEPRINT_KEY)
        val blueprint = BlueprintManager[blueprintId] ?: throw INVALID_BLUEPRINT_EXCEPTION_TYPE.create(blueprintId)

        val position = BlockPosArgumentType.getLoadedBlockPos(context, POSITION_KEY)

        val world = context.source.world
        blueprint.place(world, position)

        context.source.sendFeedback({ Text.literal("Placed blueprint at $position") }, true)

        return 1
    }

    private fun execute(context: CommandContext<ServerCommandSource>): Int {
        // retrieve version
        val loader = FabricLoader.getInstance()
        val container = loader.getModContainer(BlueprintMod.MOD_ID)
        val optionalVersion = container
            .map(ModContainer::getMetadata)
            .map(ModMetadata::getVersion)
            .map(Version::getFriendlyString)
        val version = optionalVersion.orElseGet { "Unknown" }

        // feedback version
        context.source.sendFeedback({ Text.literal("[$MOD_NAME] Version $version").formatted(Formatting.AQUA) }, false)
        return 1
    }
}
