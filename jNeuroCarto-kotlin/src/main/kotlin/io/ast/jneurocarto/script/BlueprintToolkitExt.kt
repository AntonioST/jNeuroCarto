@file:JvmName("BlueprintToolkitExt")

package io.ast.jneurocarto.script

import io.ast.jneurocarto.core.blueprint.BlueprintMask
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit.AreaThreshold

operator fun BlueprintToolkit<*>.set(mask: BooleanArray, cate: Int) {
    this.set(cate, BlueprintMask(mask))
}

operator fun BlueprintToolkit<*>.set(mask: BooleanArray, cate: IntArray) {
    this.from(cate, BlueprintMask(mask))
}

fun BlueprintToolkit<*>.fill(category: Int, threshold: IntRange) {
    fill(category, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.fill(blueprint: IntArray, threshold: IntRange): IntArray {
    return fill(blueprint, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.fill(blueprint: IntArray, category: Int, threshold: IntRange): IntArray {
    return fill(blueprint, category, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.extend(category: Int, step: Int, threshold: IntRange) {
    extend(category, step, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.extend(category: Int, step: Int, value: Int, threshold: IntRange) {
    extend(category, step, value, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.extend(blueprint: IntArray, category: Int, step: Int, threshold: IntRange): IntArray {
    return extend(blueprint, category, step, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.extend(blueprint: IntArray, category: Int, step: Int, value: Int, threshold: IntRange): IntArray {
    return extend(blueprint, category, step, value, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(category: Int, step: Int, threshold: IntRange) {
    reduce(category, step, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(category: Int, step: Int, value: Int, threshold: IntRange) {
    reduce(category, step, value, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(blueprint: IntArray, category: Int, step: Int, threshold: IntRange): IntArray {
    return reduce(blueprint, category, step, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(blueprint: IntArray, category: Int, step: Int, value: Int, threshold: IntRange): IntArray {
    return reduce(blueprint, category, step, value, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(category: Int, threshold: IntRange) {
    reduce(category, AreaThreshold(threshold.start, threshold.last))
}

fun BlueprintToolkit<*>.reduce(blueprint: IntArray, category: Int, threshold: IntRange): IntArray {
    return reduce(blueprint, category, AreaThreshold(threshold.start, threshold.last))
}