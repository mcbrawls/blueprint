package net.mcbrawls.blueprint.entity

import eu.pb4.polymer.core.api.entity.PolymerEntity
import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec2f
import net.minecraft.world.World
import xyz.nucleoid.packettweaker.PacketContext

class AnchorEntity(type: EntityType<*>, world: World) : InteractionEntity(type, world), PolymerEntity {
    var id: String? = null
    var data: String? = null

    init {
        interactionWidth = 0.25f
        interactionHeight = 0.25f

        setResponse(true)
    }

    override fun tick() {
        super.tick()

        val world = world
        if (world is ServerWorld) {
            world.spawnParticles(DustParticleEffect(0xFF0000, 1.0f), x, getBodyY(0.5), z, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        if (player !is ServerPlayerEntity) {
            return ActionResult.PASS
        }

        if (player.shouldCancelInteraction()) {
            openAnchorDataEditor(player)
        } else {
            openAnchorIdEditor(player)
        }

        return ActionResult.SUCCESS
    }

    fun openAnchorIdEditor(player: ServerPlayerEntity) {
        openInputGui(player, Text.literal("Anchor ID"), this, getOrCreateId(), "anchor ID") { input, dataName ->
            id = input.trim()
            player.sendMessage(Text.literal("Set $dataName: \"$id\"").formatted(Formatting.GREEN))
        }
    }

    fun openAnchorDataEditor(player: ServerPlayerEntity) {
        openInputGui(player, Text.literal("Anchor Data"), this, "", "data") { input, dataName ->
            data = input.trim()
            player.sendMessage(Text.literal("Set $dataName: \"$data\"").formatted(Formatting.GREEN))
        }
    }

    override fun handleAttack(attacker: Entity): Boolean {
        discard()

        if (attacker is ServerPlayerEntity) {
            attacker.sendMessage(Text.literal("Removed anchor: id \"$id\", data \"$data\"").formatted(Formatting.RED))
        }

        return false
    }

    fun createAnchor(): Anchor {
        return Anchor(pos, Vec2f(pitch, yaw), data)
    }

    /**
     * Gets the stored identifier or creates one from the entity's world key and position.
     * @return an anchor id
     */
    fun getOrCreateId(): String {
        id?.also { return it }

        return Blueprint.Companion.createUniqueId(world ?: throw IllegalStateException("World not set"), pos)
    }

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityType.INTERACTION
    }

    companion object {
        /**
         * Opens the region id editor gui for a given region id block entity.
         */
        fun openInputGui(player: ServerPlayerEntity, slateTitle: Text, anchorEntity: AnchorEntity, initialInput: String, dataName: String, setter: (input: String, dataName: String) -> Unit) {
            Blueprint.Companion.openInputGui(player, slateTitle, initialInput) { input ->
                if (input != initialInput) {
                    if (input.isBlank()) {
                        val id = anchorEntity.id
                        player.sendMessage(Text.literal("No $dataName set. Still: \"$id\"").formatted(Formatting.RED))
                    } else {
                        setter.invoke(input, dataName)
                    }
                }
            }
        }
    }
}
