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

    // ── Blockchain initialisation ──────────────────────────────────────────

    /** Sets the Android files directory for blockchain persistence. Call once at app start. */
    external fun initBlockchain(filesDir: String)

    // ── Blockchain + Proof-of-Work ─────────────────────────────────────────

    /** Records a calculation in the immutable blockchain ledger. Returns block hash. */
    external fun recordCalculation(expression: String, result: String): String

    /** Verifies the integrity of the entire blockchain. */
    external fun verifyChain(): Boolean

    /** Returns the number of blocks (including genesis). */
    external fun getChainLength(): Int

    /** Returns JSON info about a specific block. */
    external fun getBlockInfo(index: Int): String

    /** Returns JSON with detailed per-check verification results for a block. */
    external fun verifyBlockDetailed(index: Int): String

    /** Mines a proof-of-work block. Returns JSON with nonce and hash. */
    external fun proofOfWork(data: String): String

    // ── Post-Quantum Lattice Cryptography ─────────────────────────────────────

    /** LWE seal of data. Returns 12-char hex ciphertext. Deterministic. */
    external fun pqSeal(data: String): String

    /** Verifies an LWE seal against data. Returns true if authentic. */
    external fun pqVerify(data: String, seal: String): Boolean

    // ── Raytracer ──────────────────────────────────────────────────────────────

    /** Raytrace a 160×120 scene seeded by the result. Returns 76,800 RGBA bytes. */
    external fun renderScene(result: String): ByteArray

    // ── Lambda Calculus ──────────────────────────────────────────────────────

    /** Compute expression via Church-encoded lambda calculus with β-reduction.
     *  Returns JSON: {"result":"8","reductions":47,"church":"λf.λx.f(f(...))"} */
    external fun lambdaCompute(expression: String): String

    // ── Zero-Knowledge Proofs ────────────────────────────────────────────────

    /** Generate a Schnorr ZKP proving knowledge of the computation. Returns "y:R:s". */
    external fun zkpProve(expression: String, result: String): String

    /** Verify a Schnorr ZKP. Returns true if proof is valid. */
    external fun zkpVerify(expression: String, result: String, proof: String): Boolean

    // ── Genetic Algorithm ─────────────────────────────────────────────────────

    /** Evolve the result via genetic algorithm. Returns JSON with generation stats. */
    external fun geneticEvolve(expression: String, correctAnswer: Int): String

    // ── Monte Carlo Verification ──────────────────────────────────────────────

    /** Statistically verify arithmetic via Monte Carlo simulation. Returns JSON. */
    external fun monteCarloVerify(expression: String): String
}
