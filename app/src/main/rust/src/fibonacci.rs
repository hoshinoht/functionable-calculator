/// Enterprise Fibonacci Computation Module (Rust Edition)
///
/// This is the THIRD Fibonacci implementation in this project:
///   1. Pure Kotlin iterative (FunctionMap.fastfibonacci)
///   2. C++ matrix exponentiation via JNI (fibonacci.cpp)
///   3. Rust matrix exponentiation via JNI (you are here)
///
/// All three produce identical results. All three are necessary.
/// The redundancy ensures Five Nines (99.999%) Fibonacci availability.

/// 2x2 matrix for Fibonacci computation.
/// Yes, we are using linear algebra for what is fundamentally addition.
#[derive(Clone, Copy)]
struct Matrix2x2 {
    m: [[i64; 2]; 2],
}

impl Matrix2x2 {
    fn identity() -> Self {
        Matrix2x2 {
            m: [[1, 0], [0, 1]],
        }
    }

    fn fib_base() -> Self {
        Matrix2x2 {
            m: [[1, 1], [1, 0]],
        }
    }

    fn multiply(&self, other: &Matrix2x2) -> Matrix2x2 {
        Matrix2x2 {
            m: [
                [
                    self.m[0][0] * other.m[0][0] + self.m[0][1] * other.m[1][0],
                    self.m[0][0] * other.m[0][1] + self.m[0][1] * other.m[1][1],
                ],
                [
                    self.m[1][0] * other.m[0][0] + self.m[1][1] * other.m[1][0],
                    self.m[1][0] * other.m[0][1] + self.m[1][1] * other.m[1][1],
                ],
            ],
        }
    }
}

/// O(log n) matrix exponentiation — the Rust edition.
fn mat_pow(mut base: Matrix2x2, mut n: i32) -> Matrix2x2 {
    let mut result = Matrix2x2::identity();
    while n > 0 {
        if n & 1 == 1 {
            result = result.multiply(&base);
        }
        base = base.multiply(&base);
        n >>= 1;
    }
    result
}

/// Computes the nth Fibonacci number using matrix exponentiation in Rust.
///
/// For n=0, returns 0.
/// For n=1, returns 1.
/// For n>=2, invokes a full matrix exponentiation pipeline
/// that is hilariously overkill for the input sizes we actually see.
pub fn fibonacci(n: i32) -> i32 {
    if n <= 0 {
        return 0;
    }
    if n == 1 {
        return 1;
    }

    let result = mat_pow(Matrix2x2::fib_base(), n - 1);
    result.m[0][0] as i32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_fib_base_cases() {
        assert_eq!(fibonacci(0), 0);
        assert_eq!(fibonacci(1), 1);
    }

    #[test]
    fn test_fib_small() {
        assert_eq!(fibonacci(10), 55);
        assert_eq!(fibonacci(20), 6765);
    }

    #[test]
    fn test_fib_44() {
        // This is tested by the grading APK
        assert_eq!(fibonacci(44), 701408733);
    }
}
