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
    /**
     * The registry key for serializable region types.
     */
    val REGISTRY_KEY: RegistryKey<Registry<SerializableRegionType>> =
        RegistryKey.ofRegistry(Identifier(BlueprintMod.MOD_ID, "serializable_region_types"))

    /**
     * The registry of serializable region types.
     */
    val REGISTRY: Registry<SerializableRegionType> = SimpleRegistry(REGISTRY_KEY, Lifecycle.stable())

    val POINT = register("point", SerializableRegionType(PointRegion.CODEC))
    val CUBOID = register("cuboid", SerializableRegionType(CuboidRegion.CODEC))
    val SPHERE = register("sphere", SerializableRegionType(SphericalRegion.CODEC))

    private fun register(id: String, type: SerializableRegionType): SerializableRegionType {
        return Registry.register(REGISTRY, Identifier(BlueprintMod.MOD_ID, id), type)
    }
}
