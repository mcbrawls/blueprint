package net.mcbrawls.blueprint.structure

fun interface ProgressProvider {
    fun getProgress(): Float

    companion object {
        val COMPLETE: ProgressProvider = ProgressProvider { 1.0f }
    }
}
