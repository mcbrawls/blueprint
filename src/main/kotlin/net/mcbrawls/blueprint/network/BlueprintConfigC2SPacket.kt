package net.mcbrawls.blueprint.network

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.player.BlueprintPlayerData
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier

/**
 * A packet sent by the client to modify its blueprint configuration.
 */
data class BlueprintConfigC2SPacket(
    /**
     * Whether the server should render blueprint particles.
     */
    val renderParticles: Boolean,
) : CustomPayload {
    constructor(buf: PacketByteBuf) : this(
        buf.readBoolean()
    )

    override fun getId(): Id<out CustomPayload> {
        return PACKET_ID
    }

    /**
     * Creates blueprint player data from this packet.
     */
    fun createBlueprintPlayerData(): BlueprintPlayerData {
        return BlueprintPlayerData(renderParticles)
    }

    companion object {
        val PACKET_ID: Id<BlueprintConfigC2SPacket> = Id(Identifier.of(BlueprintMod.MOD_ID, "config"))

        val CODEC: Codec<BlueprintConfigC2SPacket> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL.fieldOf("render_particles").forGetter(BlueprintConfigC2SPacket::renderParticles)
            ).apply(instance, ::BlueprintConfigC2SPacket)
        }

        val PACKET_CODEC: PacketCodec<ByteBuf, BlueprintConfigC2SPacket> = PacketCodecs.codec(CODEC)
    }
}
