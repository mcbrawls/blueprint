package net.mcbrawls.blueprint.anchor

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.andante.codex.ExtraCodecs
import dev.andante.codex.nullableFieldOf
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d

/**
 * An entity-like point within a blueprint that can hold custom data.
 */
data class Anchor(
    val position: Vec3d,
    val rotation: Vec2f,
    val data: String? = null,
) {
    companion object {
        val CODEC: Codec<Anchor> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("position").forGetter(Anchor::position),
                ExtraCodecs.VEC_2F.fieldOf("rotation").forGetter(Anchor::rotation),
                Codec.STRING.nullableFieldOf("data").forGetter(Anchor::data),
            ).apply(instance, ::Anchor)
        }
    }
}
