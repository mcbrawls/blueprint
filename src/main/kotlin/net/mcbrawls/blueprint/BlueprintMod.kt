package net.mcbrawls.blueprint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.mcbrawls.blueprint.command.BlueprintCommand
import net.mcbrawls.blueprint.network.BlueprintConfigC2SPacket
import net.mcbrawls.blueprint.player.BlueprintPlayerData.Companion.blueprintData
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.resource.ResourceType
import org.slf4j.LoggerFactory

object BlueprintMod : ModInitializer {
    const val MOD_ID = "blueprint"
    const val MOD_NAME = "Blueprint"

    val logger = LoggerFactory.getLogger(MOD_NAME)

    private var isPlayerDataSavingEnabled = false

    override fun onInitialize() {
        logger.info("Initializing $MOD_NAME")

        // register config packet receiver
        ServerPlayNetworking.registerGlobalReceiver(BlueprintConfigC2SPacket.TYPE) { packet, player, _ ->
            player.blueprintData = packet.createBlueprintPlayerData()
        }

        // register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, environment ->
            BlueprintCommand.register(dispatcher, environment)
        }

        // register resource listener
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(BlueprintManager)
    }

    /**
     * Enables whether blueprint data, such as user configuration, is saved to the player's data file.
     */
    fun enablePlayerDataSaving() {
        isPlayerDataSavingEnabled = true
    }

    /**
     * Whether blueprint data is saved to players' data files.
     */
    fun isPlayerDataSavingEnabled(): Boolean {
        return isPlayerDataSavingEnabled
    }
}
