package net.mcbrawls.blueprint.player

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Blueprint data attached to the player entity.
 */
data class BlueprintPlayerData(
    /**
     * Whether the server should render blueprint particles.
     */
    var renderParticles: Boolean,
) {
    companion object {
        /**
         * The codec for blueprint player data.
         */
        val CODEC: Codec<BlueprintPlayerData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.BOOL
                    .fieldOf("render_particles")
                    .forGetter(BlueprintPlayerData::renderParticles)
            ).apply(instance, ::BlueprintPlayerData)
        }

        /**
         * The blueprint player data for this player.
         */
        var ServerPlayerEntity.blueprintData: BlueprintPlayerData?
            get() {
                val that = this as BlueprintPlayerAccessor
                return that.blueprintPlayerData
            }

            set(value) {
                val that = this as BlueprintPlayerAccessor
                that.blueprintPlayerData = value
            }
    }
}
