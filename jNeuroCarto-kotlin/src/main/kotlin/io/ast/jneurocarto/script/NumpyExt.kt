@file:JvmName("NumpyExt")

package io.ast.jneurocarto.script

infix fun IntArray.eq(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] == value }
}

infix fun IntArray.ne(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] != value }
}

infix fun IntArray.lt(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] < value }
}

infix fun IntArray.le(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] <= value }
}

infix fun IntArray.gt(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] > value }
}

infix fun IntArray.ge(value: Int): BooleanArray {
    return BooleanArray(this.size) { this[it] >= value }
}

infix fun DoubleArray.eq(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] == value }
}

infix fun DoubleArray.ne(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] != value }
}

infix fun DoubleArray.lt(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] < value }
}

infix fun DoubleArray.le(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] <= value }
}

infix fun DoubleArray.gt(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] > value }
}

infix fun DoubleArray.ge(value: Double): BooleanArray {
    return BooleanArray(this.size) { this[it] >= value }
}

operator fun IntArray.get(mask: BooleanArray): IntArray {
    if (this.size != mask.size) throw IllegalArgumentException()
    val ret = IntArray(this.size)
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) ret[ptr++] = this[i]
    }
    return ret.copyOfRange(0, ptr)
}

operator fun DoubleArray.get(mask: BooleanArray): DoubleArray {
    if (this.size != mask.size) throw IllegalArgumentException()
    val ret = DoubleArray(this.size)
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) ret[ptr++] = this[i]
    }
    return ret.copyOfRange(0, ptr)
}

operator fun IntArray.set(mask: BooleanArray, value: Int) {
    if (this.size != mask.size) throw IllegalArgumentException()
    for (i in 0 until size) {
        if (mask[i]) this[i] = value
    }
}

operator fun DoubleArray.set(mask: BooleanArray, value: Double) {
    if (this.size != mask.size) throw IllegalArgumentException()
    for (i in 0 until size) {
        if (mask[i]) this[i] = value
    }
}

operator fun IntArray.set(mask: BooleanArray, value: IntArray) {
    if (this.size != mask.size) throw IllegalArgumentException()
    if (mask.count { it } != value.size) throw IllegalArgumentException()
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) this[i] = value[ptr++]
    }

}

operator fun DoubleArray.set(mask: BooleanArray, value: DoubleArray) {
    if (this.size != mask.size) throw IllegalArgumentException()
    if (mask.count { it } != value.size) throw IllegalArgumentException()
    var ptr = 0
    for (i in 0 until size) {
        if (mask[i]) this[i] = value[ptr++]
    }
}

fun isnan(array: DoubleArray): BooleanArray {
    return BooleanArray(array.size) { java.lang.Double.isNaN(array[it]) }
}