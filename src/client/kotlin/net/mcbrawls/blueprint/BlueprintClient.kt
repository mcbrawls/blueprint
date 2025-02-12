package net.mcbrawls.blueprint

import net.fabricmc.api.ClientModInitializer
import net.mcbrawls.blueprint.key.BlueprintKeyBindings

object BlueprintClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlueprintKeyBindings
    }
}
