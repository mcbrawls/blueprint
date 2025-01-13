package net.mcbrawls.blueprint.entity

import eu.pb4.polymer.core.api.entity.PolymerEntity
import net.mcbrawls.blueprint.anchor.Anchor
import net.mcbrawls.blueprint.editor.BlueprintEditorWorld
import net.mcbrawls.blueprint.structure.Blueprint
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import xyz.nucleoid.packettweaker.PacketContext

class AnchorEntity(type: EntityType<*>, world: World) : InteractionEntity(type, world), PolymerEntity {
    var anchorId: String? = null
    var data: String? = null

    init {
        val dimensions = getDimensions(EntityPose.STANDING)
        interactionWidth = dimensions.width / 2
        interactionHeight = dimensions.height / 2

        setResponse(true)
    }

    override fun tick() {
        if (world !is BlueprintEditorWorld) {
            discard()
            return
        }

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
        openInputGui(player, Text.literal("Set Anchor ID").formatted(Formatting.BOLD), getOrCreateId(), "anchor ID") { input, dataName ->
            anchorId = input.trim()
            player.sendMessage(Text.literal("Set $dataName: \"$anchorId\"").formatted(Formatting.GREEN))
        }
    }

    fun openAnchorDataEditor(player: ServerPlayerEntity) {
        openInputGui(player, Text.literal("Set Anchor Data").formatted(Formatting.BOLD), data ?: "", "data", canBeBlank = true) { input, dataName ->
            data = input.trim()
            player.sendMessage(Text.literal("Set $dataName: \"$data\"").formatted(Formatting.GREEN))
        }
    }

    override fun handleAttack(attacker: Entity): Boolean {
        discard()

        if (attacker is ServerPlayerEntity) {
            attacker.sendMessage(Text.literal("Removed anchor: id \"$anchorId\", data \"$data\"").formatted(Formatting.RED))
        }

        return false
    }

    fun createAnchor(root: BlockPos): Anchor {
        return Anchor(pos.subtract(Vec3d.of(root)), Vec2f(yaw, pitch), data)
    }

    /**
     * Gets the stored identifier or creates one from the entity's world key and position.
     * @return an anchor id
     */
    fun getOrCreateId(): String {
        anchorId?.also { return it }

        return Blueprint.Companion.createUniqueId(world ?: throw IllegalStateException("World not set"), pos)
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)

        anchorId?.also { nbt.putString(ANCHOR_ID_KEY, it) }
        data?.also { nbt.putString(DATA_KEY, it) }
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)

        if (nbt.contains(ANCHOR_ID_KEY, NbtElement.STRING_TYPE.toInt())) {
            anchorId = nbt.getString(ANCHOR_ID_KEY)
        }

        if (nbt.contains(DATA_KEY, NbtElement.STRING_TYPE.toInt())) {
            data = nbt.getString(DATA_KEY)
        }
    }

    override fun getPolymerEntityType(context: PacketContext): EntityType<*> {
        return EntityType.INTERACTION
    }

    companion object {
        const val ANCHOR_ID_KEY = "anchor_id"
        const val DATA_KEY = "data"

        /**
         * Opens the region id editor gui for a given region id block entity.
         */
        fun openInputGui(player: ServerPlayerEntity, slateTitle: Text, initialInput: String, dataName: String, canBeBlank: Boolean = false, setter: (input: String, dataName: String) -> Unit) {
            Blueprint.Companion.openInputGui(player, slateTitle, initialInput) { input ->
                if (input != initialInput) {
                    if (canBeBlank && input.isBlank()) {
                        player.sendMessage(Text.literal("No $dataName set. Still: \"$initialInput\"").formatted(Formatting.RED))
                    } else {
                        setter.invoke(input, dataName)
                    }
                }
            }
        }
    }
}
