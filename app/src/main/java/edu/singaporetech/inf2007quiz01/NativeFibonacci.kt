package edu.singaporetech.inf2007quiz01

/**
 * JNI Bridge to the C++ Native Fibonacci Computation Engine.
 *
 * Provides two C++ fibonacci implementations:
 *   - nativeFib: C++ matrix exponentiation (implementation #2)
 *   - asmFib: ARM64 inline assembly (implementation #4, falls back to C++ on x86)
 */
object NativeFibonacci {

    init {
        System.loadLibrary("fibonacci")
    }

    /** C++ matrix exponentiation fibonacci (#2) */
    external fun nativeFib(n: Int): Int

    /** ARM64 assembly fibonacci (#4, C++ fallback on x86) */
    external fun asmFib(n: Int): Int

    /** Kotlin-callable wrapper matching (Int) -> Int for FunctionMap */
    fun fib(x: Int): Int = when (x) {
        0 -> 0
        1 -> 1
        else -> nativeFib(x)
    }
}
