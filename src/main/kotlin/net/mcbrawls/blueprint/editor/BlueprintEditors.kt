package net.mcbrawls.blueprint.editor

import com.google.common.base.Preconditions
import dev.andante.bubble.BubbleManager
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.mcbrawls.blueprint.BlueprintMod
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier

/**
 * Manages all blueprint editor environments for a server.
 */
class BlueprintEditors private constructor(private val server: MinecraftServer) {
    /**
     * All active blueprint editor environments.
     */
    private val environments: MutableMap<Identifier, BlueprintEditorEnvironment> = mutableMapOf()

    fun tick() {
    }

    fun clean() {
        environments.values.forEach(BubbleManager.getOrCreate(server)::remove)
    }

    /**
     * Gets or creates a blueprint editor for the given blueprint id.
     * @return a blueprint editor environment
     * @throws IllegalStateException if a world exists for the given blueprint id but that world is not an environment
     */
    operator fun get(blueprintId: Identifier): BlueprintEditorEnvironment {
        val blueprintNamespace = blueprintId.namespace
        val blueprintPath = blueprintId.path
        val worldId = Identifier(BlueprintMod.MOD_ID, "blueprint/$blueprintNamespace/$blueprintPath")
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
            val bubbleManager = BubbleManager.getOrCreate(server)
            val editorEnvironment = bubbleManager.createAndInitialize(
                identifier = worldId,
                factory = { server, key, options -> BlueprintEditorEnvironment(blueprintId, server, key, options) }
            )

            environments[blueprintId] = editorEnvironment
            editorEnvironment
        }

        return editorEnvironment
    }

    /**
     * Gets the blueprint editor with the given id if present.
     * @return a nullable blueprint editor environment
     */
    fun getNullable(blueprintId: Identifier): BlueprintEditorEnvironment? {
        return environments[blueprintId]
    }

    /**
     * Removes a blueprint editor from the editor manager and removes the dimension.
     * @return whether the environment was removed
     */
    fun remove(environment: BlueprintEditorEnvironment): Boolean {
        return if (environments.remove(environment.blueprintId) != null) {
            val bubbleManager = BubbleManager.getOrCreate(server)
            bubbleManager.remove(environment)
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
