@file:JvmName("BlueprintToolkitExt")

package io.ast.jneurocarto.script

import io.ast.jneurocarto.core.ProbeDescription
import io.ast.jneurocarto.core.blueprint.BlueprintMask
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit.AreaChange
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit.AreaThreshold

operator fun BlueprintToolkit<*>.set(mask: BlueprintMask, cate: Int) {
    this.set(cate, mask)
}

operator fun BlueprintToolkit<*>.set(mask: BlueprintMask, cate: IntArray) {
    this.from(cate, mask)
}

fun BlueprintToolkit<*>.fill(category: Int, threshold: IntRange) {
    fill(category, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.fill(blueprint: IntArray, threshold: IntRange): IntArray {
    return fill(blueprint, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.fill(blueprint: IntArray, category: Int, threshold: IntRange): IntArray {
    return fill(blueprint, category, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.extend(
    category: Int,
    step: Int,
    value: Int = category,
    threshold: IntRange
) {
    extend(category, AreaChange(step), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.extend(
    blueprint: IntArray,
    category: Int,
    step: Int,
    value: Int = category,
    threshold: IntRange
): IntArray {
    return extend(blueprint, category, AreaChange(step), value, AreaThreshold(threshold))
}


fun BlueprintToolkit<*>.extend(
    category: Int,
    y: Pair<Int, Int>,
    x: Pair<Int, Int> = 0 to 0,
    value: Int = category,
    threshold: IntRange
) {
    extend(category, AreaChange(x, y), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.extend(
    blueprint: IntArray,
    category: Int,
    y: Pair<Int, Int>,
    x: Pair<Int, Int> = 0 to 0,
    value: Int = category,
    threshold: IntRange
): IntArray {
    return extend(blueprint, category, AreaChange(x, y), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(
    category: Int,
    step: Int,
    value: Int = ProbeDescription.CATE_UNSET,
    threshold: IntRange
) {
    reduce(category, AreaChange(step), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(
    blueprint: IntArray,
    category: Int,
    step: Int,
    value: Int = ProbeDescription.CATE_UNSET,
    threshold: IntRange
): IntArray {
    return reduce(blueprint, category, AreaChange(step), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(
    category: Int,
    y: Pair<Int, Int>,
    x: Pair<Int, Int> = 0 to 0,
    value: Int = ProbeDescription.CATE_UNSET,
    threshold: IntRange
) {
    return reduce(category, AreaChange(x, y), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(
    blueprint: IntArray,
    category: Int,
    y: Pair<Int, Int>,
    x: Pair<Int, Int> = 0 to 0,
    value: Int = ProbeDescription.CATE_UNSET,
    threshold: IntRange
): IntArray {
    return reduce(blueprint, category, AreaChange(x, y), value, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(category: Int, threshold: IntRange) {
    reduce(category, AreaThreshold(threshold))
}

fun BlueprintToolkit<*>.reduce(blueprint: IntArray, category: Int, threshold: IntRange): IntArray {
    return reduce(blueprint, category, AreaThreshold(threshold))
}

private inline fun AreaThreshold(range: IntRange) = AreaThreshold(range.start, range.last)
private inline fun AreaChange(x: Pair<Int, Int>, y: Pair<Int, Int>) = AreaChange(x.first, x.second, y.first, y.second)