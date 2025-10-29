package net.mcbrawls.blueprint.item

import eu.pb4.polymer.core.api.item.PolymerItem
import net.mcbrawls.blueprint.editor.BlueprintEditorWorld
import net.mcbrawls.blueprint.entity.BlueprintEntityTypes
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import xyz.nucleoid.packettweaker.PacketContext

class AnchorItem(settings: Settings) : Item(settings), PolymerItem {
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        if (world !is BlueprintEditorWorld) {
            return ActionResult.PASS
        }

        val player = context.player
        if (player !is ServerPlayerEntity) {
            return ActionResult.PASS
        }

        BlueprintEntityTypes.ANCHOR.create(world, SpawnReason.SPAWN_ITEM_USE)?.also { entity ->
            entity.setPosition(context.hitPos)
            entity.rotate(context.playerYaw, false, 0.0f, false)
            world.spawnEntity(entity)
            entity.openAnchorIdEditor(player)

            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.STICK
    }

    override fun getPolymerItemModel(stack: ItemStack, context: PacketContext): Identifier? {
        return null
    }
}
