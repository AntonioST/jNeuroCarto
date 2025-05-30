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

operator fun BlueprintMask.minus(other: BlueprintMask): BlueprintMask {
    return this.diff(other)
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
    return mask.squeeze(this)
}

operator fun DoubleArray.get(mask: BlueprintMask): DoubleArray {
    return mask.squeeze(this)
}

operator fun IntArray.set(mask: BlueprintMask, value: Int) {
    mask.fill(this, value)
}

operator fun DoubleArray.set(mask: BlueprintMask, value: Double) {
    mask.fill(this, value)
}

operator fun IntArray.set(mask: BlueprintMask, value: IntArray) {
    mask.where(this, value, null)
}

operator fun DoubleArray.set(mask: BlueprintMask, value: DoubleArray) {
    mask.where(this, value, null)
}

fun isnan(array: DoubleArray): BlueprintMask {
    return BlueprintMask.nan(array)
}

fun DoubleArray.nanmin(): Double {
    return BlueprintMask.notNan(this).fold(this, Math::min).orElse(Double.NaN)
}

fun DoubleArray.nanmax(): Double {
    return BlueprintMask.notNan(this).fold(this, Math::max).orElse(Double.NaN)
}

fun DoubleArray.nanmean(): Double {
    val mask = BlueprintMask.notNan(this)
    val count = mask.count()
    if (count == 0) return Double.NaN
    return mask.fold(this, 0.0, Double::plus) / count
}

fun Double.round(d: Int): Double {
    val p = 10.0.pow(d.toDouble())
    return (this * d).roundToInt().toDouble() / p
}