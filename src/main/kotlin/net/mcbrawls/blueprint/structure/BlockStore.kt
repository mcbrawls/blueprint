package net.mcbrawls.blueprint.structure

import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import net.minecraft.util.math.Vec3i
import java.util.Arrays
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * The blocks of a blueprint: an offset position and a palette index for each block, packed into primitive arrays.
 *
 * Positions are stored relative to [min] with only as many bits per axis as the extent needs, in an [IntArray] when all
 * three axes fit in 32 bits and a [LongArray] otherwise. Palette indexes are unsigned shorts. Real blueprints cost
 * 6 bytes per block this way, where a list of per-block position objects costs about 52.
 *
 * Immutable. Blocks keep the order they were added in, so placement writes stay as spatially coherent as the source.
 */
class BlockStore private constructor(
    /**
     * The number of blocks.
     */
    val count: Int,

    minX: Int,
    minY: Int,
    minZ: Int,

    maxX: Int,
    maxY: Int,
    maxZ: Int,

    /**
     * The largest palette index any block references, or -1 when there are no blocks.
     */
    val maxPaletteIndex: Int,

    bitsY: Int,
    private val bitsZ: Int,
    private val packsIntoInts: Boolean,
    private val intPositions: IntArray,
    private val longPositions: LongArray,
    private val paletteIndexes: ShortArray,
) {
    /**
     * The smallest x, y and z over all blocks, or zero when there are none.
     */
    val min: Vec3i = Vec3i(minX, minY, minZ)

    /**
     * The largest x, y and z over all blocks, or zero when there are none.
     */
    val max: Vec3i = Vec3i(maxX, maxY, maxZ)

    /**
     * The number of blocks this store spans on each axis, or zero when there are none.
     */
    val size: Vec3i = if (count == 0) Vec3i.ZERO else Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)

    private val shiftX = bitsY + bitsZ
    private val maskY = (1L shl bitsY) - 1
    private val maskZ = (1L shl bitsZ) - 1

    fun isEmpty(): Boolean {
        return count == 0
    }

    fun x(index: Int): Int {
        return xOf(packedAt(index))
    }

    fun y(index: Int): Int {
        return yOf(packedAt(index))
    }

    fun z(index: Int): Int {
        return zOf(packedAt(index))
    }

    fun paletteIndex(index: Int): Int {
        return paletteIndexes[index].toInt() and 0xFFFF
    }

    /**
     * Performs the given action for every block's offset position and palette index, in the order they were added.
     */
    inline fun forEach(action: (x: Int, y: Int, z: Int, paletteIndex: Int) -> Unit) {
        for (index in 0 until count) {
            val packed = packedAt(index)
            action(xOf(packed), yOf(packed), zOf(packed), paletteIndex(index))
        }
    }

    /**
     * The blocks as a flat array of (x, y, z, palette index) quadruples; the inverse of [fromQuads].
     * @return a new array
     */
    fun toQuads(): IntArray {
        val quads = IntArray(Math.multiplyExact(count, QUAD))
        var base = 0
        forEach { x, y, z, paletteIndex ->
            quads[base] = x
            quads[base + 1] = y
            quads[base + 2] = z
            quads[base + 3] = paletteIndex
            base += QUAD
        }
        return quads
    }

    @PublishedApi
    internal fun packedAt(index: Int): Long {
        return if (packsIntoInts) intPositions[index].toLong() and 0xFFFFFFFFL else longPositions[index]
    }

    @PublishedApi
    internal fun xOf(packed: Long): Int {
        return (packed ushr shiftX).toInt() + min.x
    }

    @PublishedApi
    internal fun yOf(packed: Long): Int {
        return ((packed ushr bitsZ) and maskY).toInt() + min.y
    }

    @PublishedApi
    internal fun zOf(packed: Long): Int {
        return (packed and maskZ).toInt() + min.z
    }

    /**
     * Stores are equal when they hold the same blocks in the same order.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is BlockStore) {
            return false
        }

        return count == other.count &&
            min == other.min &&
            max == other.max &&
            intPositions.contentEquals(other.intPositions) &&
            longPositions.contentEquals(other.longPositions) &&
            paletteIndexes.contentEquals(other.paletteIndexes)
    }

    override fun hashCode(): Int {
        var result = count
        result = 31 * result + min.hashCode()
        result = 31 * result + max.hashCode()
        result = 31 * result + intPositions.contentHashCode()
        result = 31 * result + longPositions.contentHashCode()
        return 31 * result + paletteIndexes.contentHashCode()
    }

    override fun toString(): String {
        return "BlockStore[$count blocks, ${size.x}x${size.y}x${size.z}]"
    }

    /**
     * Collects blocks, in order, for a [BlockStore].
     */
    class Builder(expectedCount: Int = 16) {
        private var quads = IntArray(expectedCount.coerceAtLeast(1) * QUAD)
        private var length = 0

        /**
         * The number of blocks added so far.
         */
        val count: Int get() = length / QUAD

        /**
         * Adds a block at offset position ([x], [y], [z]) which uses the palette entry at [paletteIndex].
         */
        fun add(x: Int, y: Int, z: Int, paletteIndex: Int): Builder {
            require(paletteIndex in 0 until MAX_PALETTE_SIZE) {
                "Palette index $paletteIndex is outside 0..${MAX_PALETTE_SIZE - 1}"
            }

            if (length == quads.size) {
                quads = quads.copyOf(quads.size * 2)
            }

            quads[length] = x
            quads[length + 1] = y
            quads[length + 2] = z
            quads[length + 3] = paletteIndex
            length += QUAD

            return this
        }

        fun build(): BlockStore {
            return pack(quads, length)
        }
    }

    companion object {
        /**
         * Palette indexes are stored as unsigned shorts, so a palette can hold at most this many entries.
         */
        const val MAX_PALETTE_SIZE: Int = 1 shl 16

        private const val QUAD = 4
        private const val BLOCKS_KEY = "blocks"
        private const val LEGACY_BLOCKS_KEY = "block_states"

        /**
         * A store holding no blocks.
         */
        val EMPTY: BlockStore = BlockStore(0, 0, 0, 0, 0, 0, 0, -1, 0, 0, true, IntArray(0), LongArray(0), ShortArray(0))

        inline fun build(expectedCount: Int = 16, block: Builder.() -> Unit): BlockStore {
            return Builder(expectedCount).apply(block).build()
        }

        /**
         * Packs a flat array of (x, y, z, palette index) quadruples.
         * @return a new block store
         * @throws IllegalArgumentException if the array is not whole quadruples, a palette index is outside
         * 0 until [MAX_PALETTE_SIZE], or the extent needs 64 or more bits to pack
         */
        fun fromQuads(quads: IntArray): BlockStore {
            return pack(quads, quads.size)
        }

        /**
         * The `blocks` field: a flat int array of (x, y, z, palette index) quadruples. Decoding also accepts the legacy
         * `block_states` list of `{offset: [x, y, z], index}` compounds, so older files still load and switch to the
         * flat form the next time they are saved.
         */
        val MAP_CODEC: MapCodec<BlockStore> = object : MapCodec<BlockStore>() {
            override fun <T> keys(ops: DynamicOps<T>): Stream<T> {
                return Stream.of(ops.createString(BLOCKS_KEY), ops.createString(LEGACY_BLOCKS_KEY))
            }

            override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<BlockStore> {
                input.get(BLOCKS_KEY)?.also { tag ->
                    return ops.getIntStream(tag).flatMap { stream -> tryPack { fromQuads(stream.toArray()) } }
                }

                input.get(LEGACY_BLOCKS_KEY)?.also { tag ->
                    return decodeLegacy(ops, tag)
                }

                return DataResult.error { "Missing field \"$BLOCKS_KEY\"" }
            }

            override fun <T> encode(input: BlockStore, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T> {
                return prefix.add(BLOCKS_KEY, ops.createIntList(Arrays.stream(input.toQuads())))
            }

            override fun toString(): String {
                return "BlockStore"
            }
        }

        private fun <T> decodeLegacy(ops: DynamicOps<T>, list: T): DataResult<BlockStore> {
            return ops.getList(list).flatMap { elements ->
                val builder = Builder()
                var error: String? = null

                elements.accept(
                    Consumer { element ->
                        if (error != null) {
                            return@Consumer
                        }

                        val state = ops.getMap(element).result().orElse(null)
                        val offset = state?.get("offset")?.let { ops.getIntStream(it).result().orElse(null)?.toArray() }
                        val index = state?.get("index")?.let { ops.getNumberValue(it).result().orElse(null)?.toInt() }

                        if (offset == null || offset.size != 3 || index == null || index !in 0 until MAX_PALETTE_SIZE) {
                            error = "Invalid $LEGACY_BLOCKS_KEY entry: $element"
                        } else {
                            builder.add(offset[0], offset[1], offset[2], index)
                        }
                    }
                )

                val message = error
                if (message != null) DataResult.error<BlockStore> { message } else tryPack(builder::build)
            }
        }

        private inline fun tryPack(pack: () -> BlockStore): DataResult<BlockStore> {
            return try {
                DataResult.success(pack())
            } catch (exception: IllegalArgumentException) {
                DataResult.error { "Invalid blocks: ${exception.message}" }
            }
        }

        private fun pack(quads: IntArray, length: Int): BlockStore {
            require(length % QUAD == 0) { "Blocks must be (x, y, z, palette index) quadruples, got $length ints" }

            val count = length / QUAD
            if (count == 0) {
                return EMPTY
            }

            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var minZ = Int.MAX_VALUE

            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE
            var maxZ = Int.MIN_VALUE

            var maxPaletteIndex = -1

            for (base in 0 until length step QUAD) {
                val x = quads[base]
                val y = quads[base + 1]
                val z = quads[base + 2]
                val paletteIndex = quads[base + 3]

                require(paletteIndex in 0 until MAX_PALETTE_SIZE) {
                    "Palette index $paletteIndex of block ${base / QUAD} is outside 0..${MAX_PALETTE_SIZE - 1}"
                }

                if (x < minX) minX = x
                if (x > maxX) maxX = x

                if (y < minY) minY = y
                if (y > maxY) maxY = y

                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z

                if (paletteIndex > maxPaletteIndex) maxPaletteIndex = paletteIndex
            }

            val bitsX = bitsFor(minX, maxX)
            val bitsY = bitsFor(minY, maxY)
            val bitsZ = bitsFor(minZ, maxZ)
            require(bitsX + bitsY + bitsZ < Long.SIZE_BITS) {
                "Block extent from ($minX, $minY, $minZ) to ($maxX, $maxY, $maxZ) is too large to pack"
            }

            val packsIntoInts = bitsX + bitsY + bitsZ <= Int.SIZE_BITS
            val intPositions = IntArray(if (packsIntoInts) count else 0)
            val longPositions = LongArray(if (packsIntoInts) 0 else count)
            val paletteIndexes = ShortArray(count)

            for (index in 0 until count) {
                val base = index * QUAD
                val dx = quads[base].toLong() - minX
                val dy = quads[base + 1].toLong() - minY
                val dz = quads[base + 2].toLong() - minZ

                val packed = (dx shl (bitsY + bitsZ)) or (dy shl bitsZ) or dz
                if (packsIntoInts) {
                    intPositions[index] = packed.toInt()
                } else {
                    longPositions[index] = packed
                }

                paletteIndexes[index] = quads[base + 3].toShort()
            }

            return BlockStore(
                count,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                maxPaletteIndex,
                bitsY, bitsZ, packsIntoInts,
                intPositions, longPositions, paletteIndexes,
            )
        }

        /**
         * The number of bits needed to hold every offset from [min] to [max].
         */
        private fun bitsFor(min: Int, max: Int): Int {
            return Long.SIZE_BITS - java.lang.Long.numberOfLeadingZeros(max.toLong() - min)
        }
    }
}
