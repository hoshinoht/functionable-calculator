package edu.singaporetech.inf2007quiz01

/**
 * JNI Bridge to the CalcuLux Rust Core Engine.
 *
 * This object provides the sacred gateway between Kotlin's managed comfort
 * and Rust's blazing, unsafe, zero-cost-abstraction world. Every method call
 * crosses the JNI bridge, adding negligible latency that we will nonetheless
 * document extensively.
 *
 * The Rust core handles:
 * - Arithmetic computation (full expression parsing AND individual operators)
 * - Fibonacci via matrix exponentiation (the 3rd implementation)
 * - Fibonacci via embedded WebAssembly VM (the 5th implementation)
 * - Blockchain-verified calculation history (SHA-256 chained audit trail)
 * - Proof-of-Work mining (because your calculator needs to mine blocks)
 */
object RustBridge {

    init {
        System.loadLibrary("calculux_core")
    }

    // ── Full Expression Arithmetic ─────────────────────────────────────────

    /** Parses "11+2" and returns "13". In Rust. Via JNI. For basic math. */
    external fun computeExpression(expression: String): String

    // ── Individual Operator Strategy Endpoints ─────────────────────────────

    /** Addition. In Rust. Via JNI. For the Strategy Pattern. */
    external fun add(a: Int, b: Int): Int

    /** Subtraction. In Rust. Via JNI. For the Strategy Pattern. */
    external fun subtract(a: Int, b: Int): Int

    /** Multiplication. In Rust. Via JNI. For the Strategy Pattern. */
    external fun multiply(a: Int, b: Int): Int

    /** Division. Returns Int.MIN_VALUE for division by zero. */
    external fun divide(a: Int, b: Int): Int

    // ── Fibonacci (two Rust implementations) ───────────────────────────────

    /** O(log n) matrix exponentiation fibonacci. The Rust edition (#3). */
    external fun fibonacci(n: Int): Int

    /** Fibonacci via embedded WASM VM inside Rust inside JNI (#5). */
    external fun wasmFibonacci(n: Int): Int

    // ── FunctionMap helpers ────────────────────────────────────────────────

    /** Integer division by 2. In Rust. Because memory safety matters. */
    external fun half(x: Int): Int

    /** sqrt(x)*sqrt(x) repeated 1 billion times. Now blazingly fast. */
    external fun selfFunc(x: Int): Int

    // ── Blockchain + Proof-of-Work ─────────────────────────────────────────

    /** Records a calculation in the immutable blockchain ledger. Returns block hash. */
    external fun recordCalculation(expression: String, result: String): String

    /** Verifies the integrity of the entire blockchain. */
    external fun verifyChain(): Boolean

    /** Returns the number of blocks (including genesis). */
    external fun getChainLength(): Int

    /** Returns JSON info about a specific block. */
    external fun getBlockInfo(index: Int): String

    /** Mines a proof-of-work block. Returns JSON with nonce and hash. */
    external fun proofOfWork(data: String): String
}
