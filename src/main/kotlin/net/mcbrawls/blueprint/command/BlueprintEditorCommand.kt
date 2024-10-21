package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.mcbrawls.blueprint.editor.BlueprintEditorHandler
import net.mcbrawls.blueprint.editor.BlueprintEditorWorld
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object BlueprintEditorCommand {
    const val BLUEPRINT_KEY = "blueprint"
    private val NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION = SimpleCommandExceptionType(Text.literal("You are not in a Blueprint editor."))
    private val IN_BLUEPRINT_EDITOR_WORLD_EXCEPTION = SimpleCommandExceptionType(Text.literal("You are already in a Blueprint editor. Use /blueprint-editor close to leave."))

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("blueprint-editor")
                .requires { it.hasPermissionLevel(2) }
                .then(
                    literal("open")
                        .then(
                            argument(BLUEPRINT_KEY, IdentifierArgumentType.identifier())
                                .suggests { _, suggestions -> BlueprintManager.suggestBlueprints(suggestions) }
                                .executes(::executeOpen)
                        )
                )
                .then(
                    literal("close")
                        .executes(::executeClose)
                )
                .then(
                    literal("save")
                        .executes(::executeSave)
                )
        )
    }

    private fun executeOpen(context: CommandContext<ServerCommandSource>): Int {
        val blueprintId = IdentifierArgumentType.getIdentifier(context, BLUEPRINT_KEY)

        val source = context.source
        val server = source.server

        if (source.world is BlueprintEditorWorld) {
            throw IN_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        } else {
            if (BlueprintEditorHandler.open(server, blueprintId, source.player)) {
                source.sendFeedback({ Text.literal("Opened Blueprint editor for new blueprint \"$blueprintId\"") }, true)
            } else {
                source.sendFeedback({ Text.literal("Opened Blueprint editor for \"$blueprintId\"") }, true)
            }
        }

        return 1
    }

    private fun executeClose(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        BlueprintEditorHandler.close(world)

        return 1
    }

    private fun executeSave(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        val pathString = world.saveBlueprint()
        source.sendFeedback({ Text.literal("Saved editor blueprint: \"$pathString\"") }, true)
        return 1
    }
}
