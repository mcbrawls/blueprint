package net.mcbrawls.blueprint.anchor

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.andante.codex.ExtraCodecs
import dev.andante.codex.nullableFieldOf
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
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
    fun placeBlock(world: ServerWorld, state: BlockState, offset: BlockPos = BlockPos.ORIGIN) {
        val pos = BlockPos.ofFloored(position).add(offset)
        world.setBlockState(pos, state)
    }

    fun <T : Entity> placeEntity(world: ServerWorld, type: EntityType<T>, builder: (entity: T, data: String?) -> Unit = { _, _ -> }) {
        type.create(world, SpawnReason.CHUNK_GENERATION)?.also { entity ->
            entity.setPosition(position)
            entity.rotate(rotation.x, rotation.y)
            builder.invoke(entity, data)
            world.spawnEntity(entity)
        }
    }

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
