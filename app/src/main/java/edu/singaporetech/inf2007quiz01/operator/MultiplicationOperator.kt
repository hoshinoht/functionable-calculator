package edu.singaporetech.inf2007quiz01.operator

import edu.singaporetech.inf2007quiz01.RustBridge
import javax.inject.Inject

/**
 * Enterprise Multiplication Service.
 *
 * O(1) complexity achieved through the revolutionary strategy of
 * calling a single Rust function via JNI. The Strategy Pattern adds
 * exactly zero value here, but the architecture diagram looks impressive.
 */
class MultiplicationOperator @Inject constructor() : ArithmeticOperator {
    override val symbol = "*"
    override fun compute(a: Int, b: Int): Int = RustBridge.multiply(a, b)
}
