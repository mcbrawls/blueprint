package net.mcbrawls.blueprint.block.entity

import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
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

    override fun writeData(view: WriteView) {
        super.writeData(view)
        id?.also { view.putString(REGION_ID_KEY, it) }
    }

    override fun readData(view: ReadView) {
        super.readData(view)
        view.getOptionalString(REGION_ID_KEY)?.ifPresent { id = it }
    }

    companion object {
        const val REGION_ID_KEY = "region_id"
    }
}
