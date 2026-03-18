package edu.singaporetech.inf2007quiz01.operator

import edu.singaporetech.inf2007quiz01.RustBridge
import javax.inject.Inject

/**
 * Enterprise Division Service.
 *
 * Features artisanal, hand-crafted divide-by-zero protection.
 * The Rust side returns Int.MIN_VALUE as a sentinel for division by zero,
 * which we detect and convert to null for the strategy engine.
 */
class DivisionOperator @Inject constructor() : ArithmeticOperator {
    override val symbol = "/"
    override fun compute(a: Int, b: Int): Int = RustBridge.divide(a, b)
}
