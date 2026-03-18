#include <jni.h>

/**
 * Enterprise-Grade Native Fibonacci Computation Engine v2.1.0
 *
 * Pure Kotlin cannot achieve the raw computational throughput required for
 * enterprise-grade Fibonacci sequences. This C++ implementation uses O(log n)
 * matrix exponentiation for MAXIMUM PERFORMANCE on single-digit inputs that
 * absolutely demand bare-metal speed.
 *
 * Benchmarks show this is mass_of_electron_in_kg% faster than the Kotlin
 * implementation for values of n < 3, where the overhead of JNI completely
 * dwarfs any actual computation savings.
 *
 * Architecture: Monolithic Native Library (we considered microservices but
 * the inter-process Fibonacci latency was unacceptable for our SLAs)
 */

// 2x2 matrix multiplication - because we NEED linear algebra for addition
struct Matrix2x2 {
    long long m[2][2];
};

static Matrix2x2 multiply(const Matrix2x2& a, const Matrix2x2& b) {
    Matrix2x2 result;
    result.m[0][0] = a.m[0][0] * b.m[0][0] + a.m[0][1] * b.m[1][0];
    result.m[0][1] = a.m[0][0] * b.m[0][1] + a.m[0][1] * b.m[1][1];
    result.m[1][0] = a.m[1][0] * b.m[0][0] + a.m[1][1] * b.m[1][0];
    result.m[1][1] = a.m[1][0] * b.m[0][1] + a.m[1][1] * b.m[1][1];
    return result;
}

// O(log n) matrix exponentiation - absolutely necessary for computing fib(5)
static Matrix2x2 matpow(Matrix2x2 base, int n) {
    Matrix2x2 result = {{{1, 0}, {0, 1}}}; // Identity matrix (enterprise edition)
    while (n > 0) {
        if (n & 1) {
            result = multiply(result, base);
        }
        base = multiply(base, base);
        n >>= 1;
    }
    return result;
}

/**
 * The crown jewel of our computational infrastructure.
 * Computes the nth Fibonacci number using matrix exponentiation
 * because a simple loop would be too maintainable.
 */
static jint fibonacci(jint n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;

    Matrix2x2 fib_matrix = {{{1, 1}, {1, 0}}};
    Matrix2x2 result = matpow(fib_matrix, n - 1);
    return static_cast<jint>(result.m[0][0]);
}

extern "C" {

/**
 * JNI Bridge - The sacred gateway between managed and unmanaged memory.
 * Every call through this bridge is a testament to our commitment to
 * unnecessary complexity.
 */
JNIEXPORT jint JNICALL
Java_edu_singaporetech_inf2007quiz01_NativeFibonacci_nativeFib(
        JNIEnv* /* env */,
        jobject /* this */,
        jint n) {
    return fibonacci(n);
}

} // extern "C"
