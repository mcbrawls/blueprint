package net.mcbrawls.blueprint.block.entity

import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class RegionIdBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(BlueprintBlockEntityTypes.REGION_ID, pos, state) {
    var id: String? = null

    /**
     * Gets the stored identifier or creates one from the block entity's world key and position.
     * @return a region id
     */
    fun getOrCreateId(): String {
        id?.also { return it }

        return Blueprint.createUniqueId(world ?: throw IllegalStateException("World not set"), Vec3d.of(pos))
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        id?.also { nbt.putString(REGION_ID_KEY, it) }
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        nbt.getString(REGION_ID_KEY).ifPresent { id = it }
    }

    companion object {
        const val REGION_ID_KEY = "region_id"
    }
}
