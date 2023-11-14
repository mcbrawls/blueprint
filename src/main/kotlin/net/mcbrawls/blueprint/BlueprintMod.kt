package net.mcbrawls.blueprint

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object BlueprintMod : ModInitializer {
    const val MOD_ID = "blueprint"
    const val MOD_NAME = "Blueprint"

    val logger = LoggerFactory.getLogger(MOD_NAME)

	override fun onInitialize() {
		logger.info("Initializing $MOD_NAME")
	}
}
