package net.mcbrawls.blueprint.region.serialization

import com.mojang.serialization.Lifecycle
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.region.CuboidRegion
import net.mcbrawls.blueprint.region.PointRegion
import net.mcbrawls.blueprint.region.SphericalRegion
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.SimpleRegistry
import net.minecraft.util.Identifier

object SerializableRegionTypes {
    val REGISTRY_ID = Identifier.of(BlueprintMod.MOD_ID, "serializable_region_types")

    /**
     * The registry key for serializable region types.
     */
    val REGISTRY_KEY: RegistryKey<Registry<SerializableRegion.Type>> = RegistryKey.ofRegistry(REGISTRY_ID)

    /**
     * The registry of serializable region types.
     */
    val REGISTRY: Registry<SerializableRegion.Type> = SimpleRegistry(REGISTRY_KEY, Lifecycle.stable())

    /**
     * A region defined by a single point in the world.
     * Useful for cases such as respawn or chest positions.
     */
    val POINT = register("point", SerializableRegion.Type(PointRegion.CODEC))

    /**
     * A region defined by a cuboid.
     */
    val CUBOID = register("cuboid", SerializableRegion.Type(CuboidRegion.CODEC))

    /**
     * A region defined by a sphere.
     */
    val SPHERE = register("sphere", SerializableRegion.Type(SphericalRegion.CODEC))

    private fun register(id: String, type: SerializableRegion.Type): SerializableRegion.Type {
        return Registry.register(REGISTRY, Identifier.of(BlueprintMod.MOD_ID, id), type)
    }
}
