package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.mcbrawls.blueprint.editor.BlueprintEditorHandler
import net.mcbrawls.blueprint.editor.BlueprintEditorWorld
import net.mcbrawls.blueprint.region.CuboidRegion
import net.mcbrawls.blueprint.region.SphericalRegion
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.command.DefaultPermissions
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.command.permission.PermissionCheck
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

object BlueprintEditorCommand {
    const val BLUEPRINT_KEY = "blueprint"
    const val BLUEPRINT_ID_KEY = "blueprint_id"
    const val NAME_KEY = "name"
    const val POS_KEY = "pos"
    const val FIRST_KEY = "first"
    const val SECOND_KEY = "second"
    const val RADIUS_KEY = "radius"

    private val NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION = SimpleCommandExceptionType(Text.literal("You are not in a Blueprint editor."))
    private val IN_BLUEPRINT_EDITOR_WORLD_EXCEPTION = SimpleCommandExceptionType(Text.literal("You are already in a Blueprint editor. Use /blueprint-editor close to leave."))

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("blueprint-editor")
                .requires(CommandManager.requirePermissionLevel(PermissionCheck.Require(DefaultPermissions.GAMEMASTERS)))
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
                        .then(
                            argument(BLUEPRINT_ID_KEY, IdentifierArgumentType.identifier())
                                .suggests { _, suggestions -> BlueprintManager.suggestBlueprints(suggestions) }
                                .executes(::executeSave)
                        )
                )
                .then(
                    literal("region")
                        .then(
                            argument(NAME_KEY, StringArgumentType.word())
                                .then(
                                    literal("cuboid")
                                        .then(
                                            argument(FIRST_KEY, Vec3ArgumentType.vec3())
                                                .then(
                                                    argument(SECOND_KEY, Vec3ArgumentType.vec3())
                                                        .executes(::executeCuboidRegion)
                                                )
                                        )
                                )
                                .then(
                                    literal("sphere")
                                        .then(
                                            argument(POS_KEY, Vec3ArgumentType.vec3())
                                                .then(
                                                    argument(RADIUS_KEY, DoubleArgumentType.doubleArg(0.0))
                                                        .executes(::executeSphericalRegion)
                                                )
                                        )
                                )
                        )
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

    private fun executeCuboidRegion(context: CommandContext<ServerCommandSource>): Int {
        val regionId = StringArgumentType.getString(context, NAME_KEY)
        val firstPos = Vec3ArgumentType.getVec3(context, FIRST_KEY)
        val secondPos = Vec3ArgumentType.getVec3(context, SECOND_KEY)

        val min = minVec(firstPos, secondPos)
        val max = maxVec(firstPos, secondPos)
        val size = max.subtract(min)

        val rootPos = min.subtract(Vec3d.of(BlueprintEditorWorld.BLUEPRINT_PLACEMENT_POS))
        val region = CuboidRegion(rootPos, size)

        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        world.addRegion(regionId, region)

        source.sendFeedback({ Text.literal("Marked region \"$regionId\" at relative $rootPos (size $size)") }, true)
        return 1
    }

    private fun executeSphericalRegion(context: CommandContext<ServerCommandSource>): Int {
        val regionId = StringArgumentType.getString(context, NAME_KEY)
        val pos = Vec3ArgumentType.getVec3(context, POS_KEY)
        val radius = DoubleArgumentType.getDouble(context, RADIUS_KEY)

        val rootPos = pos.subtract(Vec3d.of(BlueprintEditorWorld.BLUEPRINT_PLACEMENT_POS))
        val region = SphericalRegion(rootPos, radius)

        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        world.addRegion(regionId, region)

        source.sendFeedback({ Text.literal("Marked region \"$regionId\" at relative $rootPos (radius $radius)") }, true)
        return 1
    }

    private fun executeClose(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        BlueprintEditorHandler.close(world)

        return 1
    }

    private fun executeSave(context: CommandContext<ServerCommandSource>): Int {
        val customBlueprintId = runCatching {
            IdentifierArgumentType.getIdentifier(context, BLUEPRINT_ID_KEY)
        }.getOrNull()

        val source = context.source
        val world = source.world as? BlueprintEditorWorld ?: throw NOT_BLUEPRINT_EDITOR_WORLD_EXCEPTION.create()
        if (customBlueprintId != null) {
            val pathString = world.saveBlueprint(source.server, customBlueprintId)
            source.sendFeedback({ Text.literal("Saved editor blueprint as: \"$pathString\"") }, true)
        } else {
            val pathString = world.saveBlueprint(source.server)
            source.sendFeedback({ Text.literal("Saved editor blueprint: \"$pathString\"") }, true)
        }

        return 1
    }

    fun minVec(a: Vec3d, b: Vec3d): Vec3d {
        return Vec3d(
            min(a.getX(), b.getX()),
            min(a.getY(), b.getY()),
            min(a.getZ(), b.getZ())
        )
    }

    fun maxVec(a: Vec3d, b: Vec3d): Vec3d {
        return Vec3d(
            max(a.getX(), b.getX()),
            max(a.getY(), b.getY()),
            max(a.getZ(), b.getZ())
        )
    }
}
