package net.mcbrawls.blueprint.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos

data class BlueprintBlockEntity(
    /**
     * The offset position from the root of the blueprint placement.
     */
    val blockPos: BlockPos,

    /**
     * The block entity NBT.
     */
    val nbt: NbtCompound
) {
    override fun toString(): String {
        return "BlueprintBlockEntity[$blockPos, $nbt]"
    }

    companion object {
        /**
         * The codec of a blueprint block entity.
         */
        val CODEC: Codec<BlueprintBlockEntity> = RecordCodecBuilder.create { instance ->
            instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(BlueprintBlockEntity::blockPos),
                NbtCompound.CODEC.fieldOf("nbt").forGetter(BlueprintBlockEntity::nbt)
            ).apply(instance, ::BlueprintBlockEntity)
        }
    }
}
