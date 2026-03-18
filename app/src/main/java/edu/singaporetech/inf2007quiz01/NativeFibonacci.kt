package edu.singaporetech.inf2007quiz01

/**
 * JNI Bridge to the Enterprise-Grade Native Fibonacci Computation Engine.
 *
 * This class exists because pure Kotlin cannot achieve the raw computational
 * throughput required for enterprise-grade Fibonacci sequences. By dropping
 * down to C++ with O(log n) matrix exponentiation, we achieve mass_of_electron%
 * faster computation on inputs where the JNI overhead is 10000x the actual work.
 *
 * Usage: NativeFibonacci.fib(5) // Crosses 3 language boundaries for this
 */
object NativeFibonacci {

    init {
        System.loadLibrary("fibonacci")
    }

    /**
     * Computes the nth Fibonacci number via C++ matrix exponentiation.
     * Crosses the JNI bridge, invokes BLAS-adjacent linear algebra,
     * and returns a number you could have computed on your fingers.
     */
    external fun nativeFib(n: Int): Int

    /**
     * Kotlin-callable wrapper that matches the (Int) -> Int signature
     * expected by FunctionMap.
     */
    fun fib(x: Int): Int = when (x) {
        0 -> 0
        1 -> 1
        else -> nativeFib(x)
    }
}
