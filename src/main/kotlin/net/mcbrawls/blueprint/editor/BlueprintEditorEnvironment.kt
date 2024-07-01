package net.mcbrawls.blueprint.editor

import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import xyz.nucleoid.fantasy.RuntimeWorld
import xyz.nucleoid.fantasy.RuntimeWorldConfig

/**
 * A custom world for managing blueprints.
 */
class BlueprintEditorEnvironment(
    /**
     * The blueprint to manage.
     */
    val blueprintId: Identifier,

    server: MinecraftServer,
    key: RegistryKey<World>,
    config: RuntimeWorldConfig,
    style: Style
) : RuntimeWorld(server, key, config, style) {
    companion object {
        val ROOT_POSITION = BlockPos(0, 200, 0)
    }
}
