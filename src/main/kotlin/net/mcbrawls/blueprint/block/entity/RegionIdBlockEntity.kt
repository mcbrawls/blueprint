package net.mcbrawls.blueprint.block.entity

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.math.BlockPos
import java.nio.charset.StandardCharsets
import java.util.UUID

class RegionIdBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(BlueprintBlockEntityTypes.REGION_ID, pos, state) {
    var id: String? = null

    /**
     * Gets the stored identifier or creates one from the block entity's world key and position.
     * @return a region id
     */
    fun getOrCreateRegionId(): String {
        // return stored
        id?.also { return it }

        // create custom
        val world = world ?: throw IllegalStateException("World not set")
        val key = world.registryKey
        val worldId = key.value

        val data = worldId.toString() + pos.toShortString()
        val uuid = UUID.nameUUIDFromBytes(data.toByteArray(StandardCharsets.UTF_8))

        return uuid.toString()
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        id?.also { id -> nbt.putString(REGION_ID_KEY, id) }
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)

        if (nbt.contains(REGION_ID_KEY, NbtElement.STRING_TYPE.toInt())) {
            id = nbt.getString(REGION_ID_KEY)
        }
    }

    companion object {
        const val REGION_ID_KEY = "region_id"
    }
}
