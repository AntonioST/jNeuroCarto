@file:JvmName("NumpyExt")

package io.ast.jneurocarto.script

import java.util.function.Function
import io.ast.jneurocarto.core.blueprint.BlueprintMask

private fun newBlueprintMask(length: Int, init: Function<Int, Boolean>): BlueprintMask {
    return BlueprintMask(length).apply {
        for (i in 0 until length) {
            set(i, init.apply(i))
        }
    }
}

operator fun BlueprintMask.get(i: Int): Boolean = test(i)

operator fun BlueprintMask.not(): BlueprintMask {
    return this.not()
}

infix fun BlueprintMask.and(other: BlueprintMask): BlueprintMask {
    return this.and(other)
}

infix fun BlueprintMask.or(other: BlueprintMask): BlueprintMask {
    return this.or(other)
}

infix fun BlueprintMask.xor(other: BlueprintMask): BlueprintMask {
    return this.xor(other)
}

infix fun IntArray.eq(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] == value }
}

infix fun IntArray.ne(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] != value }
}

infix fun IntArray.lt(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] < value }
}

infix fun IntArray.le(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] <= value }
}

infix fun IntArray.gt(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] > value }
}

infix fun IntArray.ge(value: Int): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] >= value }
}

infix fun DoubleArray.eq(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] == value }
}

infix fun DoubleArray.ne(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] != value }
}

infix fun DoubleArray.lt(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] < value }
}

infix fun DoubleArray.le(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] <= value }
}

infix fun DoubleArray.gt(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] > value }
}

infix fun DoubleArray.ge(value: Double): BlueprintMask {
    return newBlueprintMask(this.size) { this[it] >= value }
}

operator fun IntArray.get(mask: BlueprintMask): IntArray {
    if (this.size != mask.length) throw IllegalArgumentException()
    val ret = IntArray(this.size)
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) ret[ptr++] = this[i]
    }
    return ret.copyOfRange(0, ptr)
}

operator fun DoubleArray.get(mask: BlueprintMask): DoubleArray {
    if (this.size != mask.length) throw IllegalArgumentException()
    val ret = DoubleArray(this.size)
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) ret[ptr++] = this[i]
    }
    return ret.copyOfRange(0, ptr)
}

operator fun IntArray.set(mask: BlueprintMask, value: Int) {
    if (this.size != mask.length) throw IllegalArgumentException()
    for (i in 0 until size) {
        if (mask[i]) this[i] = value
    }
}

operator fun DoubleArray.set(mask: BlueprintMask, value: Double) {
    if (this.size != mask.length) throw IllegalArgumentException()
    for (i in 0 until size) {
        if (mask[i]) this[i] = value
    }
}

operator fun IntArray.set(mask: BlueprintMask, value: IntArray) {
    if (this.size != mask.length) throw IllegalArgumentException()
    if (mask.count() != value.size) throw IllegalArgumentException()
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) this[i] = value[ptr++]
    }
}

operator fun DoubleArray.set(mask: BlueprintMask, value: DoubleArray) {
    if (this.size != mask.length) throw IllegalArgumentException()
    if (mask.count() != value.size) throw IllegalArgumentException()
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) this[i] = value[ptr++]
    }
}

fun isnan(array: DoubleArray): BlueprintMask {
    return newBlueprintMask(array.size) { java.lang.Double.isNaN(array[it]) }
}