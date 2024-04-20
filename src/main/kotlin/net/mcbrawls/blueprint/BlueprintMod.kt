package net.mcbrawls.blueprint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.mcbrawls.blueprint.command.BlueprintCommand
import net.mcbrawls.blueprint.editor.BlueprintEditors
import net.mcbrawls.blueprint.network.BlueprintConfigC2SPacket
import net.mcbrawls.blueprint.player.BlueprintPlayerData.Companion.blueprintData
import net.mcbrawls.blueprint.resource.BlueprintManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory

object BlueprintMod : ModInitializer {
    const val MOD_ID = "blueprint"
    const val MOD_NAME = "Blueprint"

    val logger = LoggerFactory.getLogger(MOD_NAME)

    private var isPlayerDataSavingEnabled = false

    override fun onInitialize() {
        logger.info("Initializing $MOD_NAME")

        // initialize classes
        BlueprintEditors

        // register config packet receiver
        PayloadTypeRegistry.playC2S().register(BlueprintConfigC2SPacket.PACKET_ID, BlueprintConfigC2SPacket.PACKET_CODEC)

        ServerPlayNetworking.registerGlobalReceiver(BlueprintConfigC2SPacket.PACKET_ID) { packet, context ->
            val player = context.player()
            player.blueprintData = packet.createBlueprintPlayerData()

            val playerName = player.gameProfile.name
            logger.info("Received blueprint config from player: $playerName")
        }

        // register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BlueprintCommand.register(dispatcher)
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

/**
 * Compares this block position with another and returns an ordered pair.
 */
fun BlockPos.asExtremeties(other: BlockPos): Pair<BlockPos, BlockPos> {
    val box = Box(Vec3d.of(this), Vec3d.of(other))
    val min = BlockPos.ofFloored(box.minX, box.minY, box.minZ)
    val max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
    return min to max
}
