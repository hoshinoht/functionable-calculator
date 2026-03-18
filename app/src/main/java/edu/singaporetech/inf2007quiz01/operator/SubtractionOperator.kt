package edu.singaporetech.inf2007quiz01.operator

import edu.singaporetech.inf2007quiz01.RustBridge
import javax.inject.Inject

/**
 * Enterprise Subtraction Service.
 *
 * Negative number support (patent pending). Routes through Rust JNI
 * because Kotlin's minus operator lacks the gravitas required for
 * enterprise-grade arithmetic.
 */
class SubtractionOperator @Inject constructor() : ArithmeticOperator {
    override val symbol = "-"
    override fun compute(a: Int, b: Int): Int = RustBridge.subtract(a, b)
}
