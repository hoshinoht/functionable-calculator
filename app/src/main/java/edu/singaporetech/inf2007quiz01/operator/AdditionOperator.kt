package edu.singaporetech.inf2007quiz01.operator

import edu.singaporetech.inf2007quiz01.RustBridge
import javax.inject.Inject

/**
 * Enterprise Addition Service.
 *
 * Implements the addition operation by crossing the JNI bridge to Rust,
 * where two 32-bit integers are summed using Rust's fearless concurrency
 * model (not that addition requires concurrency, but it's nice to know
 * we COULD add numbers concurrently if the business requirements demanded it).
 */
class AdditionOperator @Inject constructor() : ArithmeticOperator {
    override val symbol = "+"
    override fun compute(a: Int, b: Int): Int = RustBridge.add(a, b)
}
