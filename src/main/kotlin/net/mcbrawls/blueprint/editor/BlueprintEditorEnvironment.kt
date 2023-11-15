package net.mcbrawls.blueprint.editor

import dev.andante.bubble.world.BubbleWorld
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionOptions

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
    options: DimensionOptions
) : BubbleWorld(server, key, options) {
    companion object {
        val ROOT_POSITION = BlockPos(0, 200, 0)
    }
}
