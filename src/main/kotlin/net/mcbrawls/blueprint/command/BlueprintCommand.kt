package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.ModMetadata
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.BlueprintMod.MOD_NAME
import net.mcbrawls.blueprint.editor.BlueprintEditorEnvironment
import net.mcbrawls.blueprint.editor.BlueprintEditorGui
import net.mcbrawls.blueprint.editor.BlueprintEditors
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.mcbrawls.sgui.openGui
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d

object BlueprintCommand {
    const val BLUEPRINT_KEY = "blueprint"

    private val NOT_IN_EDITOR_ENVIRONMENT_EXCEPTION_TYPE = SimpleCommandExceptionType { "This command must be run in an editor environment" }
    private val ALREADY_IN_EDITOR_ENVIRONMENT_EXCEPTION_TYPE = SimpleCommandExceptionType { "You are already in this editor environment" }
    private val INVALID_BLUEPRINT_EXCEPTION_TYPE = DynamicCommandExceptionType { id -> Text.literal("There is no blueprint with id \"$id\"") }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val builder = literal("blueprint").executes(::execute)

        builder.then(
            literal("editor")
                .then(
                    literal("open")
                        .then(
                            argument(BLUEPRINT_KEY, IdentifierArgumentType.identifier())
                                .suggests { _, suggestions -> BlueprintManager.suggestBlueprints(suggestions) }
                                .executes(::executeEditorOpen)
                        )
                )
                .then(
                    literal("close")
                        .requires(::isEditorEnvironment)
                        .executes(::executeEditorClose)
                )
                .then(
                    literal("toolset")
                        .requires(::isEditorEnvironment)
                        .executes(::executeEditorToolset)
                )
        )

        dispatcher.register(builder)
    }

    private fun isEditorEnvironment(source: ServerCommandSource): Boolean {
        return source.world is BlueprintEditorEnvironment
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

    private fun executeEditorOpen(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.playerOrThrow

        val blueprintId = IdentifierArgumentType.getIdentifier(context, BLUEPRINT_KEY)
        val environmentManager = BlueprintEditors[source.server]
        val environment = environmentManager[blueprintId]

        if (player.world !== environment) {
            player.teleport(environment, Vec3d.ofBottomCenter(BlueprintEditorEnvironment.ROOT_POSITION), Vec2f.ZERO)
            source.sendFeedback({ Text.literal("Opened editor environment \"$blueprintId\"") }, false)
        } else {
            throw ALREADY_IN_EDITOR_ENVIRONMENT_EXCEPTION_TYPE.create()
        }

        return 1
    }

    private fun executeEditorClose(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val server = source.server

        val world = source.world
        if (world !is BlueprintEditorEnvironment) {
            throw NOT_IN_EDITOR_ENVIRONMENT_EXCEPTION_TYPE.create()
        }

        val blueprintId = world.blueprintId
        val editors = BlueprintEditors[server]
        val editorEnvironment = editors.getNullable(blueprintId)
            ?: throw IllegalStateException("Editor environment was somehow null")

        editors.remove(editorEnvironment)
        source.sendFeedback({ Text.literal("Closed editor environment \"$blueprintId\"") }, false)

        return 1
    }

    private fun executeEditorToolset(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.playerOrThrow
        player.openGui(::BlueprintEditorGui)
        return 1
    }
}
