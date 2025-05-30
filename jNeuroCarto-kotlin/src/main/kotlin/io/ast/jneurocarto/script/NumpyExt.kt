@file:JvmName("NumpyExt")

package io.ast.jneurocarto.script

import kotlin.math.pow
import kotlin.math.roundToInt
import io.ast.jneurocarto.core.blueprint.BlueprintMask

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
    return BlueprintMask.eq(this, value)
}

infix fun IntArray.ne(value: Int): BlueprintMask {
    return BlueprintMask.ne(this, value)
}

infix fun IntArray.lt(value: Int): BlueprintMask {
    return BlueprintMask.lt(this, value)
}

infix fun IntArray.le(value: Int): BlueprintMask {
    return BlueprintMask.le(this, value)
}

infix fun IntArray.gt(value: Int): BlueprintMask {
    return BlueprintMask.gt(this, value)
}

infix fun IntArray.ge(value: Int): BlueprintMask {
    return BlueprintMask.ge(this, value)
}

infix fun DoubleArray.eq(value: Double): BlueprintMask {
    return BlueprintMask.eq(this, value)
}

infix fun DoubleArray.ne(value: Double): BlueprintMask {
    return BlueprintMask.ne(this, value)
}

infix fun DoubleArray.lt(value: Double): BlueprintMask {
    return BlueprintMask.lt(this, value)
}

infix fun DoubleArray.le(value: Double): BlueprintMask {
    return BlueprintMask.le(this, value)
}

infix fun DoubleArray.gt(value: Double): BlueprintMask {
    return BlueprintMask.gt(this, value)
}

infix fun DoubleArray.ge(value: Double): BlueprintMask {
    return BlueprintMask.ge(this, value)
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
    return BlueprintMask.nan(array)
}

fun DoubleArray.nanmin(): Double {
    return asSequence().filter { !java.lang.Double.isNaN(it) }.minOrNull() ?: Double.NaN
}

fun DoubleArray.nanmax(): Double {
    return asSequence().filter { !java.lang.Double.isNaN(it) }.maxOrNull() ?: Double.NaN
}

fun DoubleArray.nanmean(): Double {
    var cnt = 0
    return asSequence().filter { !java.lang.Double.isNaN(it) }.sumOf {
        cnt++
        it
    } / cnt
}

fun Double.round(d: Int): Double {
    val p = 10.0.pow(d.toDouble())
    return (this * d).roundToInt().toDouble() / p
}