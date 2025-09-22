package net.mcbrawls.blueprint.resource

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.andante.codex.encodeQuick
import kotlinx.io.IOException
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.BiFunction
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.jvm.optionals.getOrNull

object BlueprintManager : SimpleResourceReloadListener<Map<Identifier, Blueprint>> {
    /**
     * The identifier of the blueprint resource listener.
     */
    val resourceId: Identifier = Identifier.of(BlueprintMod.MOD_ID, "blueprints")

    /**
     * The resource finder for blueprint nbt files.
     */
    val finder: ResourceFinder = ResourceFinder("blueprints", ".nbt")

    val logger: Logger = LoggerFactory.getLogger("Blueprint Manager")

    private var activeLevelStorageSession: LevelStorage.Session? = null

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

    /**
     * Saves a generated blueprint to the disk.
     * @return the generated nbt
     */
    @Throws(IOException::class)
    fun save(file: File, blueprint: Blueprint): NbtCompound {
        val nbt = Blueprint.CODEC.encodeQuick(NbtOps.INSTANCE, blueprint) as? NbtCompound
            ?: throw IllegalStateException("Nbt was not expected compound type")

        file.parentFile.mkdirs()
        NbtIo.writeCompressed(nbt, file.outputStream())

        return nbt
    }

    /**
     * Saves a generated blueprint to the disk and loads it.
     * @return the relative path of the blueprint
     */
    @Throws(IOException::class)
    fun saveGenerated(server: MinecraftServer, blueprintId: Identifier, blueprint: Blueprint): String {
        // save blueprint
        val blueprintNamespace = blueprintId.namespace
        val blueprintPath = blueprintId.path

        val session = server.session
        val generatedDirectory = session.getDirectory(WorldSavePath.GENERATED)
        val path = generatedDirectory.resolve("$blueprintNamespace/blueprints/$blueprintPath.nbt")

        save(path.toFile(), blueprint)

        // load blueprint
        blueprints[blueprintId] = blueprint

        val relativePath = path
            .relativeTo(generatedDirectory)
            .pathString
            .replace(File.separatorChar, '/')

        return relativePath
    }

    @OptIn(ExperimentalPathApi::class)
    override fun load(
        manager: ResourceManager,
        executor: Executor
    ): CompletableFuture<Map<Identifier, Blueprint>> {
        return CompletableFuture.supplyAsync {
            // find all resources
            val resources: Map<Identifier, Resource> = finder.findResources(manager)

            // read raw blueprint nbt data
            val blueprintData = resources.mapValues { (identifier, resource) ->
                val result = readInputEither(resource.inputStream)

                result.exceptionOrNull()?.also { exception ->
                    logger.error("Could not load blueprint: $identifier", exception)
                }

                result.getOrNull()
            }.toMutableMap()

            // add extra (generated)
            activeLevelStorageSession?.also { session ->
                val generatedPath = session.getDirectory(WorldSavePath.GENERATED)
                if (generatedPath.isDirectory()) {
                    // collect all namespaces (direct children of the generated folder)
                    val namespaces = generatedPath
                        .listDirectoryEntries()
                        .map(Path::toFile)
                        .filter(File::isDirectory)
                        .map(File::getName)

                    // scan all namespaces
                    namespaces.forEach { namespace ->
                        // resolve blueprint folder for namespace
                        val blueprintFolderPath = generatedPath
                            .resolve(namespace)
                            .resolve(finder.directoryName)
                            .toAbsolutePath()

                        // walk namespaced blueprint folder
                        blueprintFolderPath.walk().forEach { path ->
                            val fullPath = path.toAbsolutePath()
                            val extension = ".${fullPath.extension}"
                            if (extension == finder.fileExtension) {
                                val file = fullPath.toFile()

                                // parse nbt
                               readInputEither(file.inputStream()).getOrNull()?.also { nbt ->
                                    // calculate path and store
                                    val relativePath = fullPath
                                        .relativeTo(blueprintFolderPath.parent)
                                        .pathString
                                        .replace(File.separatorChar, '/')

                                    val identifier = Identifier.of(namespace, relativePath)
                                    blueprintData[identifier] = nbt
                                }
                            }
                        }
                    }
                }
            }

            // decode nbt
            val blueprintResults = blueprintData.mapValues { (_, blueprintNbt) ->
                if (blueprintNbt == null) {
                    Optional.empty()
                } else {
                    // attempt decode blueprint
                    val blueprintDataResult = Blueprint.CODEC.decode(NbtOps.INSTANCE, blueprintNbt)

                    // log error if present
                    blueprintDataResult.resultOrPartial(logger::error)
                }
            }

            // compile blueprints
            val loadedBlueprints: Map<Identifier, Blueprint> = blueprintResults
                .mapNotNull { (location, optionalResult) ->
                    // put blueprint if present
                    val result = optionalResult.getOrNull()
                    if (result != null) {
                        val loadedLocation = finder.toResourceId(location)
                        loadedLocation to result.first
                    } else {
                        null
                    }
                }
                .toMap()

            loadedBlueprints
        }
    }

    private fun readInput(bytes: ByteArray, reader: BiFunction<InputStream, NbtSizeTracker, NbtElement>): Result<NbtElement> {
        return runCatching {
            ByteArrayInputStream(bytes.copyOf()).use {
                reader.apply(it, NbtSizeTracker.ofUnlimitedBytes())
            }
        }
    }

    private fun readInputEither(stream: InputStream): Result<NbtElement> {
        val bytes = stream.readBytes()
        val result = readInput(bytes, NbtIo::readCompressed).recoverCatching {
            readInput(bytes) { s, t ->
                NbtIo.read(DataInputStream(s), t)
            }.getOrThrow()
        }

        return result
    }

    override fun apply(
        data: Map<Identifier, Blueprint>,
        manager: ResourceManager,
        executor: Executor
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            // consume resultant data
            blueprints.clear()
            blueprints.putAll(data)
        }
    }

    fun onSessionChange(session: LevelStorage.Session) {
        blueprints.clear()
        activeLevelStorageSession = session
    }

    override fun getFabricId(): Identifier {
        return resourceId
    }
}
