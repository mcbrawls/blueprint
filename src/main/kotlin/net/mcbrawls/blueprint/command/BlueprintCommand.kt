package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.ModMetadata
import net.mcbrawls.blueprint.BlueprintMod
import net.mcbrawls.blueprint.BlueprintMod.MOD_NAME
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object BlueprintCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>, environment: RegistrationEnvironment) {
        val builder = literal("blueprint").executes(::execute)

        //

        dispatcher.register(builder)
    }

    private fun execute(context: CommandContext<ServerCommandSource>): Int {
        // retrieve version
        val loader = FabricLoader.getInstance()
        val container = loader.getModContainer(BlueprintMod.MOD_ID)
        val optionalVersion = container
            .map(ModContainer::getMetadata)
            .map(ModMetadata::getVersion)
            .map(Version::getFriendlyString)
        val version = optionalVersion.orElseGet { "Unknown" }

        // feedback version
        context.source.sendFeedback({ Text.literal("[$MOD_NAME] Version $version").formatted(Formatting.AQUA) }, false)
        return 1
    }
}
