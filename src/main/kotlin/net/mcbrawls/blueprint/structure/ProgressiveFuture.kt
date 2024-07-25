package net.mcbrawls.blueprint.structure

import java.util.concurrent.CompletableFuture

data class ProgressiveFuture<T>(
    /**
     * The future.
     */
    val future: CompletableFuture<T>,

    /**
     * The progress provider of the future.
     */
    val progressProvider: ProgressProvider
)
