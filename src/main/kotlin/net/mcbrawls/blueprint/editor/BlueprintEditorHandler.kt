package net.mcbrawls.blueprint.editor

import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeKeys
import xyz.nucleoid.fantasy.Fantasy
import xyz.nucleoid.fantasy.RuntimeWorldConfig
import xyz.nucleoid.fantasy.RuntimeWorldHandle
import xyz.nucleoid.fantasy.util.VoidChunkGenerator

object BlueprintEditorHandler {
    private val handles: MutableMap<Identifier, RuntimeWorldHandle> = mutableMapOf()
    private val keys: MutableMap<RegistryKey<World>, Identifier> = mutableMapOf()

    /**
     * Opens a Blueprint editor environment.
     * @return whether the blueprint opened is new
     */
    internal fun open(
        server: MinecraftServer,
        blueprintId: Identifier,
        player: ServerPlayerEntity?
    ): Boolean {
        var new = false

        // create world
        val fantasy = Fantasy.get(server)
        val handle = handles[blueprintId] ?: run {
            val handle = fantasy.openTemporaryWorld(
                RuntimeWorldConfig()
                    .setGenerator(VoidChunkGenerator(server, BiomeKeys.THE_VOID))
                    .setWorldConstructor { server, key, config, _ -> BlueprintEditorWorld(blueprintId, server, key, config) }
            )

            // initialize world
            val world = handle.asWorld() as BlueprintEditorWorld
            new = world.initializeBlueprint()

            handle
        }

        // store
        handles[blueprintId] = handle

        val world = handle.asWorld()
        keys[world.registryKey] = blueprintId

        // teleport player
        player?.also { player ->
            val pos = BlueprintEditorWorld.BLUEPRINT_PLACEMENT_POS
            val vec = Vec3d.ofBottomCenter(pos)
            player.teleport(world, vec.x, vec.y, vec.z, 0.0f, 0.0f)

            player.changeGameMode(GameMode.SPECTATOR)
        }

        return new
    }

    internal fun close(world: BlueprintEditorWorld) {
        // get handle
        val key = world.registryKey
        val blueprintId = keys[key] ?: return
        val handle = handles[blueprintId] ?: return

        // delete world
        handle.delete()

        // remove keys
        keys.remove(key)
        handles.remove(blueprintId)
    }
}
