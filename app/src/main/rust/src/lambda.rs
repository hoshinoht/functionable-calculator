/// Lambda Calculus Evaluator — Church Numeral Arithmetic
///
/// Because computing 3+5 via direct addition is pedestrian. Real engineers
/// encode integers as higher-order functions, apply combinators, and
/// beta-reduce until a Church numeral emerges from the λ-soup.
///
/// Pipeline:
///   "3+5" → Church(3) ADD Church(5)
///       → (λm.λn.λf.λx. m f (n f x)) (λf.λx. f(f(f x))) (λf.λx. f(f(f(f(f x)))))
///       → ... N β-reductions later ...
///       → λf.λx. f(f(f(f(f(f(f(f x))))))))
///       → 8
///
/// Subtraction uses Church predecessor, which is O(n) β-reductions per
/// decrement. SUB(20, 15) involves ~1,200 β-reductions. For subtraction.
/// Of single-digit numbers. In a calculator.

use std::collections::HashSet;
use std::sync::atomic::{AtomicUsize, Ordering};

// ── AST ──────────────────────────────────────────────────────────────────────

#[derive(Clone, Debug, PartialEq)]
pub enum Term {
    Var(String),
    Abs(String, Box<Term>),
    App(Box<Term>, Box<Term>),
}

impl Term {
    fn var(name: &str) -> Self {
        Term::Var(name.to_string())
    }
    fn abs(param: &str, body: Term) -> Self {
        Term::Abs(param.to_string(), Box::new(body))
    }
    fn app(f: Term, x: Term) -> Self {
        Term::App(Box::new(f), Box::new(x))
    }
}

// ── Church Numeral Encoding ──────────────────────────────────────────────────

/// Encode n as λf.λx. f(f(...(f x)...)) with n applications of f.
fn church(n: u32) -> Term {
    let mut body = Term::var("x");
    for _ in 0..n {
        body = Term::app(Term::var("f"), body);
    }
    Term::abs("f", Term::abs("x", body))
}

/// ADD = λm.λn.λf.λx. m f (n f x)
fn church_add() -> Term {
    Term::abs("m", Term::abs("n", Term::abs("f", Term::abs("x",
        Term::app(
            Term::app(Term::var("m"), Term::var("f")),
            Term::app(
                Term::app(Term::var("n"), Term::var("f")),
                Term::var("x"),
            ),
        ),
    ))))
}

/// MUL = λm.λn.λf.λx. m (n f) x
fn church_mul() -> Term {
    Term::abs("m", Term::abs("n", Term::abs("f", Term::abs("x",
        Term::app(
            Term::app(
                Term::var("m"),
                Term::app(Term::var("n"), Term::var("f")),
            ),
            Term::var("x"),
        ),
    ))))
}

/// PRED = λn.λf.λx. n (λg.λh. h (g f)) (λu. x) (λv. v)
///
/// The most cursed combinator in Church arithmetic.
/// Each application performs O(n) β-reductions just to subtract 1.
fn church_pred() -> Term {
    Term::abs("n", Term::abs("f", Term::abs("x",
        Term::app(
            Term::app(
                Term::app(
                    Term::var("n"),
                    Term::abs("g", Term::abs("h",
                        Term::app(
                            Term::var("h"),
                            Term::app(Term::var("g"), Term::var("f")),
                        ),
                    )),
                ),
                Term::abs("u", Term::var("x")),
            ),
            Term::abs("v", Term::var("v")),
        ),
    )))
}

/// SUB = λm.λn. n PRED m
///
/// Applies PRED n times to m. Each PRED is O(m) reductions.
/// Total: O(n × m) β-reductions. For subtraction.
fn church_sub() -> Term {
    Term::abs("m", Term::abs("n",
        Term::app(
            Term::app(Term::var("n"), church_pred()),
            Term::var("m"),
        ),
    ))
}

// ── Variable Utilities ───────────────────────────────────────────────────────

fn free_vars(term: &Term) -> HashSet<String> {
    match term {
        Term::Var(name) => {
            let mut s = HashSet::new();
            s.insert(name.clone());
            s
        }
        Term::Abs(param, body) => {
            let mut s = free_vars(body);
            s.remove(param);
            s
        }
        Term::App(f, x) => {
            let mut s = free_vars(f);
            s.extend(free_vars(x));
            s
        }
    }
}

fn all_vars(term: &Term) -> HashSet<String> {
    match term {
        Term::Var(name) => {
            let mut s = HashSet::new();
            s.insert(name.clone());
            s
        }
        Term::Abs(param, body) => {
            let mut s = all_vars(body);
            s.insert(param.clone());
            s
        }
        Term::App(f, x) => {
            let mut s = all_vars(f);
            s.extend(all_vars(x));
            s
        }
    }
}

fn term_size(term: &Term) -> usize {
    match term {
        Term::Var(_) => 1,
        Term::Abs(_, body) => 1 + term_size(body),
        Term::App(f, x) => 1 + term_size(f) + term_size(x),
    }
}

/// Global counter for fresh variable generation.
/// Monotonically increasing across all lambda computations.
/// Thread-safe because enterprise calculators are concurrent.
static FRESH_COUNTER: AtomicUsize = AtomicUsize::new(0);

fn fresh_var(base: &str, avoid: &HashSet<String>) -> String {
    loop {
        let n = FRESH_COUNTER.fetch_add(1, Ordering::Relaxed);
        let name = format!("{}′{}", base, n);
        if !avoid.contains(&name) {
            return name;
        }
    }
}

// ── Capture-Avoiding Substitution ────────────────────────────────────────────

/// term[var := replacement]
///
/// Performs alpha-renaming when necessary to avoid variable capture.
/// Every alpha-rename generates a fresh variable with a prime (′) suffix,
/// contributing to the term's aesthetic complexity.
fn substitute(term: &Term, var: &str, replacement: &Term) -> Term {
    match term {
        Term::Var(name) => {
            if name == var {
                replacement.clone()
            } else {
                term.clone()
            }
        }
        Term::App(f, x) => Term::App(
            Box::new(substitute(f, var, replacement)),
            Box::new(substitute(x, var, replacement)),
        ),
        Term::Abs(param, body) => {
            if param == var {
                term.clone()
            } else if free_vars(replacement).contains(param) {
                let mut avoid = all_vars(term);
                avoid.extend(all_vars(replacement));
                avoid.insert(var.to_string());
                let fresh = fresh_var(param, &avoid);
                let renamed = substitute(body, param, &Term::Var(fresh.clone()));
                Term::Abs(fresh, Box::new(substitute(&renamed, var, replacement)))
            } else {
                Term::Abs(param.clone(), Box::new(substitute(body, var, replacement)))
            }
        }
    }
}

// ── Beta Reduction ───────────────────────────────────────────────────────────

/// Single-step normal-order (leftmost-outermost) β-reduction.
fn beta_step(term: &Term) -> Option<Term> {
    match term {
        Term::App(f, x) => {
            if let Term::Abs(param, body) = f.as_ref() {
                Some(substitute(body, param, x))
            } else if let Some(f2) = beta_step(f) {
                Some(Term::App(Box::new(f2), x.clone()))
            } else {
                beta_step(x).map(|x2| Term::App(f.clone(), Box::new(x2)))
            }
        }
        Term::Abs(param, body) => {
            beta_step(body).map(|b| Term::Abs(param.clone(), Box::new(b)))
        }
        Term::Var(_) => None,
    }
}

/// Fully β-reduce a term, returning (normal_form, reduction_count).
///
/// Aborts if:
///   - max_steps exceeded (divergent term)
///   - term size exceeds 50,000 nodes (space explosion)
fn beta_reduce(term: &Term, max_steps: usize) -> (Term, usize) {
    let mut current = term.clone();
    let mut steps = 0;
    while steps < max_steps {
        if term_size(&current) > 50_000 {
            break;
        }
        match beta_step(&current) {
            Some(next) => {
                current = next;
                steps += 1;
            }
            None => break,
        }
    }
    (current, steps)
}

// ── Church Numeral Decoding ──────────────────────────────────────────────────

/// Decode a Church numeral λf.λx. f^n(x) back to an integer.
fn decode_church(term: &Term) -> Option<i32> {
    if let Term::Abs(f, inner) = term {
        if let Term::Abs(x, body) = inner.as_ref() {
            return count_apps(body, f, x);
        }
    }
    None
}

fn count_apps(term: &Term, f: &str, x: &str) -> Option<i32> {
    match term {
        Term::Var(name) if name == x => Some(0),
        Term::App(func, arg) => {
            if let Term::Var(name) = func.as_ref() {
                if name == f {
                    return count_apps(arg, f, x).map(|n| n + 1);
                }
            }
            None
        }
        _ => None,
    }
}

// ── Pretty Printing ──────────────────────────────────────────────────────────

fn display_term(term: &Term) -> String {
    match term {
        Term::Var(name) => name.clone(),
        Term::Abs(param, body) => format!("λ{}.{}", param, display_term(body)),
        Term::App(f, x) => {
            let f_str = if matches!(f.as_ref(), Term::Abs(_, _)) {
                format!("({})", display_term(f))
            } else {
                display_term(f)
            };
            let x_str = if matches!(x.as_ref(), Term::Var(_)) {
                display_term(x)
            } else {
                format!("({})", display_term(x))
            };
            format!("{} {}", f_str, x_str)
        }
    }
}

fn truncate(s: &str, max: usize) -> String {
    if s.chars().count() <= max {
        s.to_string()
    } else {
        let truncated: String = s.chars().take(max).collect();
        format!("{}…", truncated)
    }
}

// ── Expression Parser ────────────────────────────────────────────────────────

fn parse_expression(expr: &str) -> Option<(u32, char, u32)> {
    for op in ['+', '-', '*', '/'] {
        if let Some(pos) = expr.rfind(op) {
            if pos == 0 {
                continue;
            }
            if let (Ok(a), Ok(b)) = (
                expr[..pos].trim().parse::<u32>(),
                expr[pos + 1..].trim().parse::<u32>(),
            ) {
                return Some((a, op, b));
            }
        }
    }
    None
}

// ── Public API ───────────────────────────────────────────────────────────────

/// Parse an arithmetic expression, compute it via Church-encoded lambda
/// calculus with full β-reduction, and return a JSON result.
///
/// Returns JSON: {"result":"8","reductions":47,"church":"λf.λx.f(f(f(f(f(f(f(f x)))))))"}
///
/// For division: returns a snarky error because Church division requires
/// the Y combinator and nobody has time for that.
pub fn lambda_compute(expression: &str) -> String {
    let (a, op, b) = match parse_expression(expression) {
        Some(v) => v,
        None => {
            return format!(
                "{{\"error\":\"Cannot λ-encode '{}' — only A op B supported\",\"reductions\":0}}",
                expression
            );
        }
    };

    if op == '/' {
        return "{\"error\":\"Division requires μ-recursion. Even Church gave up.\",\"reductions\":0}".to_string();
    }

    // Cap at 30 to prevent term explosion
    if a > 30 || b > 30 {
        return format!(
            "{{\"error\":\"Church({}) is {} nested lambdas. The JVM has limits.\",\"reductions\":0}}",
            a.max(b),
            a.max(b)
        );
    }

    let op_term = match op {
        '+' => church_add(),
        '-' => church_sub(),
        '*' => church_mul(),
        _ => unreachable!(),
    };

    let term = Term::app(Term::app(op_term, church(a)), church(b));
    let (reduced, steps) = beta_reduce(&term, 5000);
    let result = decode_church(&reduced);

    let result_str = match result {
        Some(n) => n.to_string(),
        None => "?".to_string(),
    };

    let church_display = truncate(&display_term(&reduced), 100);

    // Escape any quotes in the church display for JSON safety
    let church_escaped = church_display.replace('\\', "\\\\").replace('"', "\\\"");

    format!(
        "{{\"result\":\"{}\",\"reductions\":{},\"church\":\"{}\"}}",
        result_str, steps, church_escaped
    )
}

// ── Tests ────────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_church_roundtrip() {
        for n in 0..=10 {
            assert_eq!(decode_church(&church(n)), Some(n as i32));
        }
    }

    #[test]
    fn test_addition() {
        let term = Term::app(Term::app(church_add(), church(3)), church(5));
        let (reduced, steps) = beta_reduce(&term, 500);
        assert_eq!(decode_church(&reduced), Some(8));
        assert!(steps > 0, "Expected β-reductions, got 0");
    }

    #[test]
    fn test_multiplication() {
        let term = Term::app(Term::app(church_mul(), church(3)), church(4));
        let (reduced, steps) = beta_reduce(&term, 500);
        assert_eq!(decode_church(&reduced), Some(12));
        assert!(steps > 0);
    }

    #[test]
    fn test_subtraction() {
        let term = Term::app(Term::app(church_sub(), church(7)), church(3));
        let (reduced, steps) = beta_reduce(&term, 5000);
        assert_eq!(decode_church(&reduced), Some(4));
        assert!(steps > 10, "Subtraction should involve many reductions");
    }

    #[test]
    fn test_subtraction_underflow_is_zero() {
        // Church numerals can't represent negatives: 3 - 7 = 0
        let term = Term::app(Term::app(church_sub(), church(3)), church(7));
        let (reduced, steps) = beta_reduce(&term, 5000);
        assert_eq!(decode_church(&reduced), Some(0));
        assert!(steps > 0);
    }

    #[test]
    fn test_lambda_compute_add() {
        let result = lambda_compute("3+5");
        assert!(result.contains("\"result\":\"8\""));
        assert!(result.contains("\"reductions\":"));
    }

    #[test]
    fn test_lambda_compute_mul() {
        let result = lambda_compute("4*5");
        assert!(result.contains("\"result\":\"20\""));
    }

    #[test]
    fn test_lambda_compute_div_refuses() {
        let result = lambda_compute("10/2");
        assert!(result.contains("error"));
        assert!(result.contains("μ-recursion"));
    }
}
