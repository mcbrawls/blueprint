package net.mcbrawls.blueprint.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource

object BlueprintCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>, environment: RegistrationEnvironment) {
        val builder = literal("blueprint")

        //

        dispatcher.register(builder)
    }
}
