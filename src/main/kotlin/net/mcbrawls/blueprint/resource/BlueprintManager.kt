package net.mcbrawls.blueprint.resource

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object BlueprintManager : SimpleResourceReloadListener<Map<Identifier, Blueprint>> {
    /**
     * The identifier of the blueprint resource listener.
     */
    val RESOURCE_ID = Identifier.of(BlueprintMod.MOD_ID, "blueprints")

    /**
     * The resource finder for blueprint nbt files.
     */
    val FINDER = ResourceFinder("blueprints", ".nbt")

    val LOGGER = LoggerFactory.getLogger("Blueprint Manager")

    /**
     * The blueprints currently loaded in the game instance.
     */
    private val blueprints: MutableMap<Identifier, Blueprint> = mutableMapOf()

    /**
     * Gets the blueprint with the given id.
     * @return an optional blueprint
     */
    operator fun get(id: Identifier): Blueprint? {
        return blueprints[id]
    }

    /**
     * Suggests all loaded blueprints to the suggestions builder.
     * @return a suggestions future
     */
    fun suggestBlueprints(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        blueprints.keys.map(Identifier::toString).forEach(builder::suggest)
        return builder.buildFuture()
    }

    override fun load(
        manager: ResourceManager,
        profiler: Profiler,
        executor: Executor
    ): CompletableFuture<Map<Identifier, Blueprint>> {
        return CompletableFuture.supplyAsync {
            // find all resources
            val resources: Map<Identifier, Resource> = FINDER.findResources(manager)

            // load blueprint data and build map
            buildMap {
                resources.forEach { (location, resource) ->
                    // read raw blueprint nbt data
                    val blueprintNbt = try {
                        resource.inputStream.use { NbtIo.readCompressed(it, NbtSizeTracker.ofUnlimitedBytes()) }
                    } catch (exception: Exception) {
                        LOGGER.error("Could not load blueprint: $location", exception)
                        return@forEach
                    }

                    // attempt decode blueprint
                    val blueprintDataResult = Blueprint.CODEC.decode(NbtOps.INSTANCE, blueprintNbt)

                    // log error if present
                    val optionalResult = blueprintDataResult.resultOrPartial(LOGGER::error)

                    // put blueprint if present
                    val loadedLocation = FINDER.toResourceId(location)
                    optionalResult.ifPresent { result -> this[loadedLocation] = result.first }
                }
            }
        }
    }

    override fun apply(
        data: Map<Identifier, Blueprint>,
        manager: ResourceManager,
        profiler: Profiler,
        executor: Executor
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            // consume resultant data
            blueprints.clear()
            blueprints.putAll(data)
        }
    }

    override fun getFabricId(): Identifier {
        return RESOURCE_ID
    }
}
