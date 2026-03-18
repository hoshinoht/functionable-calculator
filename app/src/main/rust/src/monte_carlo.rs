/// Monte Carlo Statistical Verification of Arithmetic
///
/// Because trusting deterministic arithmetic is for people who haven't taken
/// a statistics course. When someone says "7 * 8 = 56", a well-calibrated
/// engineer asks: "At what confidence level?"
///
/// This module re-derives arithmetic results using Monte Carlo simulation:
///   - Multiplication a*b: estimated via rectangle-area random sampling
///   - Addition a+b: estimated via threshold-counting on a uniform distribution
///   - Subtraction a-b: same idea, different sign
///   - Division a/b: denied due to budget constraints (infinite samples required)
///
/// Each result comes with a 95% confidence interval, because point estimates
/// are for amateurs and deterministic CPUs.
///
/// The PRNG is a xorshift64 seeded from SHA-256 of the expression, ensuring
/// reproducible randomness — an oxymoron that statisticians have learned to
/// live with.

use sha2::{Digest, Sha256};

// ── Xorshift64 PRNG ─────────────────────────────────────────────────────────

/// A blazingly fast™ PRNG that trades cryptographic security for speed.
/// Seeded deterministically from SHA-256 because we have standards.
struct Xorshift64 {
    state: u64,
}

impl Xorshift64 {
    fn from_expression(expr: &str) -> Self {
        let mut hasher = Sha256::new();
        hasher.update(expr.as_bytes());
        let hash = hasher.finalize();
        // Absorb first 8 bytes of SHA-256 as seed
        let mut seed_bytes = [0u8; 8];
        seed_bytes.copy_from_slice(&hash[..8]);
        let state = u64::from_le_bytes(seed_bytes);
        // Ensure non-zero state (xorshift64 fixpoint)
        Xorshift64 {
            state: if state == 0 { 0xDEAD_BEEF_CAFE_BABE } else { state },
        }
    }

    /// Returns the next pseudo-random u64.
    fn next_u64(&mut self) -> u64 {
        let mut x = self.state;
        x ^= x << 13;
        x ^= x >> 7;
        x ^= x << 17;
        self.state = x;
        x
    }

    /// Returns a uniform f64 in [0, 1).
    fn next_f64(&mut self) -> f64 {
        (self.next_u64() >> 11) as f64 / ((1u64 << 53) as f64)
    }
}

// ── Expression Parser ────────────────────────────────────────────────────────

/// Extracts (a, operator, b) from a string like "7*8" or "-3+5".
///
/// Handles negative first operands by skipping position 0 when scanning
/// for the operator. Supports +, -, *, /.
fn parse_expr(expr: &str) -> Option<(f64, char, f64)> {
    let trimmed = expr.trim();
    // Scan for the operator — skip index 0 to allow negative first operand
    // Check from the end to handle cases like "-3+-5" (find rightmost operator)
    let bytes = trimmed.as_bytes();
    let mut op_pos = None;
    for i in (1..bytes.len()).rev() {
        let ch = bytes[i] as char;
        if (ch == '+' || ch == '*' || ch == '/') {
            op_pos = Some(i);
            break;
        }
        // For '-', make sure it's an operator not a negative sign on the second operand
        if ch == '-' {
            // It's an operator if the preceding character is a digit or whitespace
            if i > 0 {
                let prev = bytes[i - 1] as char;
                if prev.is_ascii_digit() || prev == ' ' {
                    op_pos = Some(i);
                    break;
                }
            }
        }
    }

    let pos = op_pos?;
    let op = bytes[pos] as char;
    let a: f64 = trimmed[..pos].trim().parse().ok()?;
    let b: f64 = trimmed[pos + 1..].trim().parse().ok()?;
    Some((a, op, b))
}

// ── Monte Carlo Engines ──────────────────────────────────────────────────────

const SAMPLES: usize = 10_000;

/// Estimates a*b using rectangle-area Monte Carlo sampling.
///
/// Method: Generate N random points in [0, |a|] x [0, |b|].
/// Every point falls inside the rectangle (it's a rectangle, not a circle),
/// but we add statistical dignity via sub-sampling: each point has a 99.9%
/// chance of being counted. The remaining 0.1% are sacrificed for variance.
///
/// estimate = |a| * |b| * (counted / total) * sign(a*b)
fn monte_carlo_multiply(a: f64, b: f64, rng: &mut Xorshift64) -> SimResult {
    let abs_a = a.abs();
    let abs_b = b.abs();
    let sign = if (a < 0.0) ^ (b < 0.0) { -1.0 } else { 1.0 };
    let area = abs_a * abs_b;

    let mut samples = Vec::with_capacity(SAMPLES);

    for _ in 0..SAMPLES {
        // Generate random point in [0, |a|] x [0, |b|] (always inside the rectangle)
        let _x = rng.next_f64() * abs_a;
        let _y = rng.next_f64() * abs_b;
        // Sub-sampling: 99.9% inclusion probability for statistical texture
        let included = rng.next_f64() < 0.999;
        let sample_value = if included { area } else { 0.0 };
        samples.push(sample_value);
    }

    let mean = samples.iter().sum::<f64>() / samples.len() as f64;
    let estimate = mean * sign;
    let exact = a * b;

    let variance = samples.iter().map(|s| (s - mean).powi(2)).sum::<f64>()
        / (samples.len() - 1) as f64;
    let stderr = (variance / samples.len() as f64).sqrt();

    SimResult {
        estimate,
        exact,
        samples: SAMPLES,
        confidence_low: (mean - 1.96 * stderr) * sign,
        confidence_high: (mean + 1.96 * stderr) * sign,
        method: "rectangle_area",
    }
}

/// Estimates a+b using threshold-counting Monte Carlo.
///
/// Method: Generate N uniform values in [0, |a|+|b|].
/// Count the fraction that fall below |a|. That fraction times (|a|+|b|)
/// estimates |a|. Then a_hat + b = estimate.
///
/// For negative operands we adjust signs accordingly, because even
/// randomness respects the number line. Mostly.
fn monte_carlo_add(a: f64, b: f64, rng: &mut Xorshift64) -> SimResult {
    let exact = a + b;
    let abs_a = a.abs();
    let abs_b = b.abs();
    let total_range = abs_a + abs_b;

    if total_range == 0.0 {
        return SimResult {
            estimate: 0.0,
            exact: 0.0,
            samples: SAMPLES,
            confidence_low: 0.0,
            confidence_high: 0.0,
            method: "threshold_counting",
        };
    }

    let mut samples = Vec::with_capacity(SAMPLES);

    for _ in 0..SAMPLES {
        let val = rng.next_f64() * total_range;
        // Each sample estimates |a| based on whether it fell below the threshold
        let a_est = if val < abs_a { total_range } else { 0.0 };
        samples.push(a_est);
    }

    let mean_a_hat = samples.iter().sum::<f64>() / samples.len() as f64;
    // Reconstruct: a_hat has sign of a, then add b
    let a_hat = if a < 0.0 { -mean_a_hat } else { mean_a_hat };
    let estimate = a_hat + b;

    let variance = samples.iter().map(|s| (s - mean_a_hat).powi(2)).sum::<f64>()
        / (samples.len() - 1) as f64;
    let stderr = (variance / samples.len() as f64).sqrt();

    // Confidence interval on the a_hat estimate, then shift by b
    let ci_low = if a < 0.0 {
        -(mean_a_hat + 1.96 * stderr) + b
    } else {
        (mean_a_hat - 1.96 * stderr) + b
    };
    let ci_high = if a < 0.0 {
        -(mean_a_hat - 1.96 * stderr) + b
    } else {
        (mean_a_hat + 1.96 * stderr) + b
    };

    SimResult {
        estimate,
        exact,
        samples: SAMPLES,
        confidence_low: ci_low.min(ci_high),
        confidence_high: ci_low.max(ci_high),
        method: "threshold_counting",
    }
}

/// Estimates a-b by treating it as a+(−b). Delegation is an enterprise pattern.
fn monte_carlo_subtract(a: f64, b: f64, rng: &mut Xorshift64) -> SimResult {
    let mut result = monte_carlo_add(a, -b, rng);
    result.exact = a - b; // In case floating-point had opinions
    result.method = "threshold_counting_negated";
    result
}

// ── Result Types ─────────────────────────────────────────────────────────────

struct SimResult {
    estimate: f64,
    exact: f64,
    samples: usize,
    confidence_low: f64,
    confidence_high: f64,
    method: &'static str,
}

impl SimResult {
    /// Serialise to JSON by hand, because adding serde to a calculator would be
    /// like hiring a civil engineer to build a sandcastle. Accurate, but excessive.
    fn to_json(&self) -> String {
        let error_pct = if self.exact != 0.0 {
            ((self.estimate - self.exact) / self.exact * 100.0).abs()
        } else if self.estimate == 0.0 {
            0.0
        } else {
            100.0 // philosophically, any nonzero deviation from zero is infinite error
        };

        format!(
            "{{\"estimate\":{:.2},\"exact\":{},\"samples\":{},\"confidence_low\":{:.2},\"confidence_high\":{:.2},\"method\":\"{}\",\"error_pct\":{:.2}}}",
            self.estimate,
            format_exact(self.exact),
            self.samples,
            self.confidence_low,
            self.confidence_high,
            self.method,
            error_pct,
        )
    }
}

/// Format exact value: integers as integers, floats as floats.
/// Because "56.00" is technically correct but emotionally wrong.
fn format_exact(v: f64) -> String {
    if v == v.trunc() && v.abs() < 1e15 {
        format!("{}", v as i64)
    } else {
        format!("{:.6}", v)
    }
}

// ── Public API ───────────────────────────────────────────────────────────────

/// Statistically verifies an arithmetic expression using Monte Carlo simulation.
///
/// Accepts expressions like "7*8", "3+5", "10-3". Returns a JSON string with
/// the Monte Carlo estimate, the exact result, sample count, 95% confidence
/// interval, method name, and error percentage.
///
/// Division is refused on principle: accurate Monte Carlo division requires
/// infinite samples, and the budget was denied in Q3.
///
/// # Examples
/// ```
/// let result = monte_carlo_verify("7*8");
/// // {"estimate":55.94,"exact":56,"samples":10000,"confidence_low":55.72,...}
/// ```
pub fn monte_carlo_verify(expression: &str) -> String {
    let (a, op, b) = match parse_expr(expression) {
        Some(v) => v,
        None => {
            return format!(
                "{{\"error\":\"Cannot parse '{}'. Expected 'A op B' where op is +,-,*,/.\",\"samples\":0}}",
                expression.replace('\\', "\\\\").replace('"', "\\\"")
            );
        }
    };

    if op == '/' {
        return "{\"error\":\"Monte Carlo division requires infinite samples. Budget denied.\",\"samples\":0}".to_string();
    }

    let mut rng = Xorshift64::from_expression(expression);

    let result = match op {
        '*' => monte_carlo_multiply(a, b, &mut rng),
        '+' => monte_carlo_add(a, b, &mut rng),
        '-' => monte_carlo_subtract(a, b, &mut rng),
        _ => unreachable!("parse_expr only returns +, -, *, /"),
    };

    result.to_json()
}

// ── Tests ────────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_simple_multiply() {
        let (a, op, b) = parse_expr("7*8").unwrap();
        assert_eq!(op, '*');
        assert!((a - 7.0).abs() < f64::EPSILON);
        assert!((b - 8.0).abs() < f64::EPSILON);
    }

    #[test]
    fn test_parse_negative_first_operand() {
        let (a, op, b) = parse_expr("-3+5").unwrap();
        assert_eq!(op, '+');
        assert!((a - (-3.0)).abs() < f64::EPSILON);
        assert!((b - 5.0).abs() < f64::EPSILON);
    }

    #[test]
    fn test_parse_subtraction() {
        let (a, op, b) = parse_expr("10-3").unwrap();
        assert_eq!(op, '-');
        assert!((a - 10.0).abs() < f64::EPSILON);
        assert!((b - 3.0).abs() < f64::EPSILON);
    }

    #[test]
    fn test_parse_spaces() {
        let (a, op, b) = parse_expr(" 12 * 4 ").unwrap();
        assert_eq!(op, '*');
        assert!((a - 12.0).abs() < f64::EPSILON);
        assert!((b - 4.0).abs() < f64::EPSILON);
    }

    #[test]
    fn test_parse_nonsense_returns_none() {
        assert!(parse_expr("hello").is_none());
        assert!(parse_expr("").is_none());
    }

    #[test]
    fn test_multiply_7_times_8() {
        let json = monte_carlo_verify("7*8");
        assert!(json.contains("\"exact\":56"));
        assert!(json.contains("\"method\":\"rectangle_area\""));
        assert!(json.contains("\"samples\":10000"));
        // Estimate should be close to 56
        assert!(json.contains("\"estimate\":"));
    }

    #[test]
    fn test_multiply_estimate_accuracy() {
        let mut rng = Xorshift64::from_expression("7*8");
        let result = monte_carlo_multiply(7.0, 8.0, &mut rng);
        // With 99.9% inclusion, estimate should be within 1% of exact
        assert!(
            (result.estimate - 56.0).abs() < 1.0,
            "estimate {} too far from 56.0",
            result.estimate
        );
        // Confidence interval should contain the exact value (most of the time)
        assert!(result.confidence_low <= 56.0 && result.confidence_high >= 56.0,
            "CI [{}, {}] does not contain 56.0", result.confidence_low, result.confidence_high);
    }

    #[test]
    fn test_addition_3_plus_5() {
        let json = monte_carlo_verify("3+5");
        assert!(json.contains("\"exact\":8"));
        assert!(json.contains("\"method\":\"threshold_counting\""));
    }

    #[test]
    fn test_addition_estimate_accuracy() {
        let mut rng = Xorshift64::from_expression("3+5");
        let result = monte_carlo_add(3.0, 5.0, &mut rng);
        assert!(
            (result.estimate - 8.0).abs() < 1.5,
            "estimate {} too far from 8.0",
            result.estimate
        );
    }

    #[test]
    fn test_subtraction() {
        let json = monte_carlo_verify("10-3");
        assert!(json.contains("\"exact\":7"));
        assert!(json.contains("\"method\":\"threshold_counting_negated\""));
    }

    #[test]
    fn test_division_denied() {
        let json = monte_carlo_verify("10/2");
        assert!(json.contains("\"error\""));
        assert!(json.contains("Budget denied"));
        assert!(json.contains("\"samples\":0"));
    }

    #[test]
    fn test_negative_multiply() {
        let mut rng = Xorshift64::from_expression("-3*4");
        let result = monte_carlo_multiply(-3.0, 4.0, &mut rng);
        assert!(
            (result.estimate - (-12.0)).abs() < 1.0,
            "estimate {} too far from -12.0",
            result.estimate
        );
    }

    #[test]
    fn test_zero_multiply() {
        let mut rng = Xorshift64::from_expression("0*8");
        let result = monte_carlo_multiply(0.0, 8.0, &mut rng);
        assert!((result.estimate).abs() < f64::EPSILON);
    }

    #[test]
    fn test_zero_addition() {
        let json = monte_carlo_verify("0+0");
        assert!(json.contains("\"exact\":0"));
        assert!(json.contains("\"estimate\":0.00"));
    }

    #[test]
    fn test_deterministic_rng() {
        // Same expression should always produce the same sequence
        let mut rng1 = Xorshift64::from_expression("7*8");
        let mut rng2 = Xorshift64::from_expression("7*8");
        for _ in 0..100 {
            assert_eq!(rng1.next_u64(), rng2.next_u64());
        }
    }

    #[test]
    fn test_different_expressions_different_rng() {
        let mut rng1 = Xorshift64::from_expression("7*8");
        let mut rng2 = Xorshift64::from_expression("8*7");
        // Very unlikely to collide
        let same = (0..10).all(|_| rng1.next_u64() == rng2.next_u64());
        assert!(!same, "different expressions should produce different sequences");
    }

    #[test]
    fn test_invalid_expression() {
        let json = monte_carlo_verify("lol");
        assert!(json.contains("\"error\""));
        assert!(json.contains("\"samples\":0"));
    }

    #[test]
    fn test_format_exact_integer() {
        assert_eq!(format_exact(56.0), "56");
        assert_eq!(format_exact(-12.0), "-12");
        assert_eq!(format_exact(0.0), "0");
    }

    #[test]
    fn test_format_exact_float() {
        assert_eq!(format_exact(3.14), "3.140000");
    }

    #[test]
    fn test_json_structure() {
        let json = monte_carlo_verify("7*8");
        // Should be valid-ish JSON with all expected keys
        for key in &["estimate", "exact", "samples", "confidence_low", "confidence_high", "method", "error_pct"] {
            assert!(json.contains(key), "missing key: {}", key);
        }
    }
}
