package net.mcbrawls.blueprint.network

import net.fabricmc.fabric.api.networking.v1.FabricPacket
import net.fabricmc.fabric.api.networking.v1.PacketType
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.player.BlueprintPlayerData
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

/**
 * A packet sent by the client to modify its blueprint configuration.
 */
data class BlueprintConfigC2SPacket(
    /**
     * Whether the server should render blueprint particles.
     */
    val renderParticles: Boolean,
) : FabricPacket {
    constructor(buf: PacketByteBuf) : this(
        buf.readBoolean()
    )

    override fun write(buf: PacketByteBuf) {
        buf.writeBoolean(renderParticles)
    }

    override fun getType(): PacketType<*> {
        return TYPE
    }

    /**
     * Creates blueprint player data from this packet.
     */
    fun createBlueprintPlayerData(): BlueprintPlayerData {
        return BlueprintPlayerData(renderParticles)
    }

    companion object {
        /**
         * The type of the config packet.
         */
        val TYPE: PacketType<BlueprintConfigC2SPacket> = PacketType.create(
            Identifier(BlueprintMod.MOD_ID, "config"),
            ::BlueprintConfigC2SPacket
        )
    }
}
