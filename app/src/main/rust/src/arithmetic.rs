/// Enterprise Arithmetic Computation Module
///
/// Because basic math operations needed their own module with proper
/// separation of concerns. Each function is O(1) but we document them
/// as if they were solving NP-hard problems.

use regex::Regex;

/// Parses an arithmetic expression like "11+2" and computes the result.
/// Uses regex because we needed at least one dependency for parsing
/// what is fundamentally "number operator number".
pub fn compute_expression(expression: &str) -> String {
    let re = match Regex::new(r"(-?\d+)([+\-*/])(-?\d+)") {
        Ok(r) => r,
        Err(_) => return "Error".to_string(),
    };

    let caps = match re.captures(expression) {
        Some(c) => c,
        None => return "Error".to_string(),
    };

    let num1: i64 = match caps[1].parse() {
        Ok(n) => n,
        Err(_) => return "Error".to_string(),
    };

    let op = &caps[2];

    let num2: i64 = match caps[3].parse() {
        Ok(n) => n,
        Err(_) => return "Error".to_string(),
    };

    let result = match op {
        "+" => num1 + num2,
        "-" => num1 - num2,
        "*" => num1 * num2,
        "/" => {
            if num2 == 0 {
                return "Error".to_string();
            }
            num1 / num2
        }
        _ => return "Error".to_string(),
    };

    result.to_string()
}

// ─── Individual Operator Strategy Endpoints ────────────────────────────────
// Each arithmetic operation gets its own function because the Strategy Pattern
// demands it. These are called individually by the Hilt-injected operator
// implementations on the Kotlin side. Maximum indirection achieved.

pub fn add(a: i32, b: i32) -> i32 {
    (a as i64 + b as i64) as i32
}

pub fn subtract(a: i32, b: i32) -> i32 {
    (a as i64 - b as i64) as i32
}

pub fn multiply(a: i32, b: i32) -> i32 {
    (a as i64 * b as i64) as i32
}

/// Returns i32::MIN as sentinel for division by zero.
pub fn divide(a: i32, b: i32) -> i32 {
    if b == 0 {
        return i32::MIN; // Sentinel value for division by zero
    }
    a / b
}

/// Enterprise-grade halving operation.
/// Integer division by 2, but in Rust for memory safety.
pub fn half(x: i32) -> i32 {
    x / 2
}

/// The "self" function: computes sqrt(x)*sqrt(x) one billion times.
/// Now in Rust, so it's a FAST billion unnecessary operations.
pub fn self_func(x: i32) -> i32 {
    let mut y = x as f64;
    for _ in 0..1_000_000_000 {
        y = y.sqrt() * y.sqrt();
    }
    y as i32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_addition() {
        assert_eq!(compute_expression("11+2"), "13");
    }

    #[test]
    fn test_subtraction() {
        assert_eq!(compute_expression("13-3"), "10");
    }

    #[test]
    fn test_multiplication() {
        assert_eq!(compute_expression("10*11"), "110");
    }

    #[test]
    fn test_division() {
        assert_eq!(compute_expression("110/5"), "22");
    }

    #[test]
    fn test_division_by_zero() {
        assert_eq!(compute_expression("5/0"), "Error");
    }

    #[test]
    fn test_negative_first_operand() {
        assert_eq!(compute_expression("-5+3"), "-2");
    }

    #[test]
    fn test_half() {
        assert_eq!(half(10), 5);
        assert_eq!(half(7), 3);
    }
}
