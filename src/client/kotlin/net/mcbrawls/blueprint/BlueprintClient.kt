package net.mcbrawls.blueprint

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.mcbrawls.blueprint.network.BlueprintConfigC2SPacket

object BlueprintClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register { handler, _, _ ->
            // send initial config packet to server
            val packet = BlueprintConfigC2SPacket(
                renderParticles = false
            )

            handler.sendPacket(ClientPlayNetworking.createC2SPacket(packet))
        }
    }
}
