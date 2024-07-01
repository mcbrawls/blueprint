package net.mcbrawls.blueprint.editor

import com.google.common.base.Preconditions
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.mcbrawls.blueprint.BlueprintMod
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.world.GameRules
import net.minecraft.world.biome.BiomeKeys
import xyz.nucleoid.fantasy.Fantasy
import xyz.nucleoid.fantasy.RuntimeWorldConfig
import xyz.nucleoid.fantasy.RuntimeWorldHandle
import xyz.nucleoid.fantasy.util.VoidChunkGenerator

/**
 * Manages all blueprint editor environments for a server.
 */
class BlueprintEditors private constructor(private val server: MinecraftServer) {
    /**
     * All active blueprint editor environments.
     */
    private val environments: MutableMap<Identifier, RuntimeWorldHandle> = mutableMapOf()

    fun tick() {
    }

    fun clean() {
        environments.values.forEach(RuntimeWorldHandle::delete)
    }

    /**
     * Gets or creates a blueprint editor for the given blueprint id.
     * @return a blueprint editor environment
     * @throws IllegalStateException if a world exists for the given blueprint id but that world is not an environment
     */
    operator fun get(blueprintId: Identifier): BlueprintEditorEnvironment {
        val blueprintNamespace = blueprintId.namespace
        val blueprintPath = blueprintId.path
        val worldId = Identifier.of(BlueprintMod.MOD_ID, "blueprint/$blueprintNamespace/$blueprintPath")
        val worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId)

        val environmentWorld = server.getWorld(worldKey)

        // get or create environment
        val editorEnvironment = if (environmentWorld != null) {
            if (environmentWorld is BlueprintEditorEnvironment) {
                environmentWorld
            } else {
                throw IllegalStateException("World existed for blueprint but was not environment: $blueprintId")
            }
        } else {
            val fantasy = Fantasy.get(server)
            val handle = fantasy.openTemporaryWorld(
                worldId,
                RuntimeWorldConfig()
                    .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
                    .setGenerator(VoidChunkGenerator(server.registryManager.get(RegistryKeys.BIOME), BiomeKeys.THE_VOID))
                    .setWorldConstructor { server, key, config, style ->
                        BlueprintEditorEnvironment(
                            blueprintId,
                            server,
                            key,
                            config,
                            style
                        )
                    }
            )

            environments[blueprintId] = handle

            val editorEnvironment = handle.asWorld() as BlueprintEditorEnvironment
            editorEnvironment
        }

        return editorEnvironment
    }

    /**
     * Gets the blueprint editor with the given id if present.
     * @return a nullable blueprint editor environment
     */
    fun getNullable(blueprintId: Identifier): BlueprintEditorEnvironment? {
        return environments[blueprintId]?.asWorld() as? BlueprintEditorEnvironment
    }

    /**
     * Removes a blueprint editor from the editor manager and removes the dimension.
     * @return whether the environment was removed
     */
    fun remove(environment: BlueprintEditorEnvironment): Boolean {
        val handle = environments.remove(environment.blueprintId)
        return if (handle != null) {
            handle.delete()
            true
        } else {
            false
        }
    }

    companion object {
        /**
         * All blueprint editor managers.
         */
        private val EDITORS = mutableMapOf<MinecraftServer, BlueprintEditors>()

        init {
            // register server events
            ServerTickEvents.START_SERVER_TICK.register { server -> EDITORS[server]?.tick() }
            ServerLifecycleEvents.SERVER_STOPPING.register { server -> EDITORS[server]?.clean() }

            ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, origin, destination ->
                // send command tree on editor world change
                if (origin is BlueprintEditorEnvironment || destination is BlueprintEditorEnvironment) {
                    player.server.commandManager.sendCommandTree(player)
                }
            }
        }

        /**
         * Gets an environment manager for the server or creates one if not present.
         */
        operator fun get(server: MinecraftServer): BlueprintEditors {
            Preconditions.checkState(server.isOnThread, "Cannot create blueprint editor manager off-thread")
            return EDITORS.computeIfAbsent(server, ::BlueprintEditors)
        }

        /**
         * Clears the environment manager for the given server.
         */
        fun clear(server: MinecraftServer): BlueprintEditors? {
            EDITORS[server]?.let(BlueprintEditors::clean)
            return EDITORS.remove(server)
        }
    }
}
