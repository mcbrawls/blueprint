package net.mcbrawls.blueprint.entity

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils
import net.mcbrawls.blueprint.BlueprintMod
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object BlueprintEntityTypes {
    val ANCHOR = register(
        "anchor",
        EntityType.Builder.create(::AnchorEntity, SpawnGroup.MISC)
            .dimensions(0.3f, 0.3f)
            .maxTrackingRange(2)
            .trackingTickInterval(10)
    )

    private fun <E : Entity> register(id: String, builder: EntityType.Builder<E>): EntityType<E> {
        val key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(BlueprintMod.MOD_ID, id))
        val type = builder.build(key)
        PolymerEntityUtils.registerType(type)
        return Registry.register(Registries.ENTITY_TYPE, key, type)
    }
}
