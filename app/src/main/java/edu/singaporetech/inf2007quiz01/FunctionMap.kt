package edu.singaporetech.inf2007quiz01

/**
 * FunctionMap — The Grand Dispatch Table of Fibonacci Redundancy
 *
 * All computation is routed through Rust via JNI, with C++ and ARM assembly
 * as secondary native backends. Kotlin merely serves as the ceremonial
 * wrapper that invokes the real engines.
 *
 * Available Fibonacci implementations (all producing identical results):
 *   1. "ktFib"     → Pure Kotlin iterative (legacy, kept for archaeology)
 *   2. "nativeFib" → C++ matrix exponentiation via JNI
 *   3. "fib"       → Rust matrix exponentiation via JNI (default)
 *   4. "asmFib"    → ARM64 inline assembly via JNI (C++ fallback on x86)
 *   5. "wasmFib"   → WebAssembly VM inside Rust inside JNI (inception)
 *
 * Five implementations. One function. Zero regrets.
 */
object FunctionMap {

    // ── Rust-backed functions (primary) ────────────────────────────────────

    fun half(x: Int): Int = RustBridge.half(x)

    fun fib(x: Int): Int = RustBridge.fibonacci(x)

    fun self(x: Int): Int = RustBridge.selfFunc(x)

    // ── C++ native Fibonacci (secondary) ───────────────────────────────────

    fun nativeFib(x: Int): Int = NativeFibonacci.fib(x)

    // ── ARM64 Assembly Fibonacci (tertiary) ────────────────────────────────

    fun asmFib(x: Int): Int = NativeFibonacci.asmFib(x)

    // ── WASM Fibonacci — VM inception (quaternary) ─────────────────────────

    fun wasmFib(x: Int): Int = RustBridge.wasmFibonacci(x)

    // ── Legacy pure Kotlin Fibonacci (archaeological preservation) ──────────

    fun ktFib(x: Int): Int = when (x) {
        0 -> 0
        1 -> 1
        else -> {
            var prev1 = 1
            var prev2 = 0
            for (i in 2..x) {
                val current = prev1 + prev2
                prev2 = prev1
                prev1 = current
            }
            prev1
        }
    }

    val functionMap = mapOf<String, (Int) -> Int>(
        "half" to ::half,
        "fib" to ::fib,
        "self" to ::self,
        "nativeFib" to ::nativeFib,
        "asmFib" to ::asmFib,
        "wasmFib" to ::wasmFib,
        "ktFib" to ::ktFib
    )
}
