package net.mcbrawls.blueprint.key

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.mcbrawls.blueprint.BlueprintMod
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

object BlueprintKeyBindings {
    val CATEGORY = KeyBinding.Category.create(Identifier.of(BlueprintMod.MOD_ID, "category"))

    val TOGGLE_ANCHOR_VISUALISATION = registerKey("toggle_anchor_visualisation", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N)

    private fun registerKey(id: String, type: InputUtil.Type, key: Int): KeyBinding {
        val modId = BlueprintMod.MOD_ID
        val translationKey = "$modId.key.$id"
        val keyBinding = KeyBinding(translationKey, type, key, CATEGORY)
        return KeyBindingHelper.registerKeyBinding(keyBinding)
    }
}
