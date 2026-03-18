package edu.singaporetech.inf2007quiz01.operator

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Operator Strategy Engine — Grand Central Dispatch for Arithmetic.
 *
 * Receives an expression string, parses it with regex (because we learned
 * nothing from the Rust side), looks up the appropriate operator strategy
 * from the Hilt-injected map, and dispatches the computation.
 *
 * Call chain: ViewModel → Microservice → StrategyEngine → Operator → RustBridge → JNI → Rust
 * For computing: 2 + 2 = 4
 *
 * Design patterns used: Strategy, Dependency Injection, Service Locator (via map)
 * Lines of code to add two numbers: ~200 across 8 files
 */
@Singleton
class OperatorStrategyEngine @Inject constructor(
    private val operators: Map<String, @JvmSuppressWildcards ArithmeticOperator>
) {
    private val expressionRegex = Regex("""(-?\d+)([+\-*/])(-?\d+)""")

    fun compute(expression: String): String {
        val match = expressionRegex.find(expression) ?: return "Error"

        val num1 = match.groupValues[1].toIntOrNull() ?: return "Error"
        val op = match.groupValues[2]
        val num2 = match.groupValues[3].toIntOrNull() ?: return "Error"

        val operator = operators[op] ?: return "Error"

        // Division by zero check
        if (op == "/" && num2 == 0) return "Error"

        val result = operator.compute(num1, num2)

        // Check for division-by-zero sentinel from Rust
        if (result == Int.MIN_VALUE && op == "/") return "Error"

        return result.toString()
    }
}
