package edu.singaporetech.inf2007quiz01.operator

/**
 * Strategy Pattern Interface for Arithmetic Operations.
 *
 * Because calling RustBridge.add(a, b) directly would be too simple.
 * Each operator is a Hilt-injectable dependency with its own class,
 * interface implementation, and module binding. This is how enterprise
 * software computes 2 + 2.
 *
 * Design patterns used: Strategy, Factory (via Hilt), Dependency Injection
 * Design patterns needed: zero
 */
interface ArithmeticOperator {
    /** The operator symbol ("+", "-", "*", "/") */
    val symbol: String

    /** Compute the operation. Each implementation crosses the JNI bridge to Rust. */
    fun compute(a: Int, b: Int): Int
}
