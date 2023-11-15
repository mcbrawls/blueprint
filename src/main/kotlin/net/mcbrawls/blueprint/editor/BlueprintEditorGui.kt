package net.mcbrawls.blueprint.editor

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.GuiHelpers
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.HotbarGui
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class BlueprintEditorGui(player: ServerPlayerEntity) : HotbarGui(player) {
    init {
        setSlot(0, ARROW_ELEMENT)
        setSlot(8, CLOSE_ELEMENT)
    }

    override fun click(index: Int, type: ClickType, action: SlotActionType): Boolean {
        if (type.numKey || type == ClickType.OFFHAND_SWAP || index == -999) {
            GuiHelpers.sendPlayerScreenHandler(player)
            return false
        }

        return super.click(index, type, action)
    }

    override fun canPlayerClose(): Boolean {
        return false
    }

    companion object {
        val ARROW_ELEMENT: GuiElementBuilder = GuiElementBuilder(Items.ARROW).setName(Text.literal("Back"))
            .setCallback { _, _, _, gui ->
                gui.close()
            }

        val CLOSE_ELEMENT: GuiElementBuilder = GuiElementBuilder(Items.BARRIER).setName(Text.literal("Close Blueprint"))
            .setCallback { _, _, _, gui ->
                val player = gui.player
                val world = player.world
                if (world is BlueprintEditorEnvironment) {
                    val server = player.server
                    val editors = BlueprintEditors[server]
                    editors.remove(world)
                }
            }
    }
}
