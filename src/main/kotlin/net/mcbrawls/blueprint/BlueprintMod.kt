package net.mcbrawls.blueprint

import com.mojang.serialization.JsonOps
import dev.andante.codex.encodeQuick
import net.fabricmc.api.ModInitializer
import net.mcbrawls.blueprint.region.PointRegion
import net.minecraft.util.math.BlockPos
import org.slf4j.LoggerFactory

object BlueprintMod : ModInitializer {
    const val MOD_ID = "blueprint"
    const val MOD_NAME = "Blueprint"

    private val logger = LoggerFactory.getLogger(MOD_NAME)

	override fun onInitialize() {
		logger.info("Initializing $MOD_NAME")

        val json = CompoundRegion.CODEC.encodeQuick(JsonOps.INSTANCE, CompoundRegion(listOf(PointRegion(BlockPos.ORIGIN), CompoundRegion(listOf(PointRegion(BlockPos(10, 2, 3)))))))
        println(json)
	}
}
