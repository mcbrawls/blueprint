package net.mcbrawls.blueprint.editor.gui

import eu.pb4.sgui.api.gui.AnvilInputGui
import net.minecraft.server.network.ServerPlayerEntity

/**
 * A gui to handle string input for region identifiers.
 */
class RegionIdInputGui(player: ServerPlayerEntity, val closeCallback: CloseCallback) : AnvilInputGui(player, true) {
    override fun onClose() {
        super.onClose()

        if (!closeCallback.onClose(this, input)) {
            setDefaultInputValue(input)
            open()
        }
    }

    fun interface CloseCallback {
        fun onClose(gui: RegionIdInputGui, input: String): Boolean
    }
}
