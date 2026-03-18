/// Genetic Algorithm Arithmetic Solver
///
/// Because computing 3+5=8 directly is pedestrian. Real engineers spawn
/// a population of 200 random integers, subject them to Darwinian selection
/// pressure, crossbreed their bit patterns, and wait for evolution to
/// converge on the answer that any pocket calculator could produce instantly.
///
/// Pipeline:
///   "3+5" → seed PRNG from SHA-256(expression)
///         → spawn 200 random i32 candidates
///         → evaluate fitness |candidate - correct_answer|
///         → tournament selection (k=3, survival of the fittest integer)
///         → single-point crossover on 32-bit representation
///         → bit-flip mutation (~5% rate, because cosmic rays)
///         → repeat up to 50 generations
///         → declare victory when fitness reaches 0
///         → return the answer you already knew
///
/// Average convergence: ~15 generations for single-digit results.
/// Worst case: 50 generations of futile evolution, still gets the right answer
/// most of the time. Natural selection finds a way.
///
/// Computational complexity: O(generations × population_size) to compute
/// what O(1) arithmetic could have done. But evolution is beautiful.

use sha2::{Digest, Sha256};

// ── Deterministic PRNG ──────────────────────────────────────────────────────

/// Xorshift64 — the fruit fly of pseudorandom number generators.
/// Fast, simple, deterministic. Perfect for evolving calculator results.
struct Xorshift64 {
    state: u64,
}

impl Xorshift64 {
    fn from_expression(expression: &str) -> Self {
        let hash = Sha256::digest(expression.as_bytes());
        let seed = u64::from_le_bytes(hash[0..8].try_into().unwrap());
        // Ensure non-zero state (xorshift64 fixpoint at 0)
        Xorshift64 {
            state: if seed == 0 { 0xDEAD_BEEF_CAFE_BABE } else { seed },
        }
    }

    fn next(&mut self) -> u64 {
        let mut x = self.state;
        x ^= x << 13;
        x ^= x >> 7;
        x ^= x << 17;
        self.state = x;
        x
    }

    /// Random i32 in a range centred around `centre` with spread `radius`.
    /// Gives the initial population a sporting chance.
    fn next_i32_near(&mut self, centre: i32, radius: i32) -> i32 {
        let raw = self.next();
        let offset = (raw % (2 * radius as u64 + 1)) as i32 - radius;
        centre.wrapping_add(offset)
    }
}

// ── Fitness ─────────────────────────────────────────────────────────────────

/// Fitness function: absolute distance from the correct answer.
/// Lower is better. Zero means the organism has evolved to be... correct.
/// Darwin would be proud. Or confused. Probably confused.
#[inline]
fn fitness(candidate: i32, target: i32) -> u64 {
    (candidate as i64 - target as i64).unsigned_abs()
}

// ── Genetic Operators ───────────────────────────────────────────────────────

/// Tournament selection: pick k=3 random individuals, return the fittest.
/// Like a reality TV show, but for 32-bit integers.
fn tournament_select(population: &[i32], target: i32, rng: &mut Xorshift64) -> i32 {
    let mut best = population[(rng.next() % population.len() as u64) as usize];
    let mut best_fit = fitness(best, target);

    for _ in 1..3 {
        let idx = (rng.next() % population.len() as u64) as usize;
        let f = fitness(population[idx], target);
        if f < best_fit {
            best = population[idx];
            best_fit = f;
        }
    }
    best
}

/// Single-point crossover on the 32-bit representation.
/// Picks a random crossover point [1, 31] and swaps the lower bits.
/// Two parents enter, two children leave. Circle of life.
fn crossover(parent_a: i32, parent_b: i32, rng: &mut Xorshift64) -> (i32, i32) {
    let point = (rng.next() % 31 + 1) as u32; // 1..=31
    let mask = (1u32 << point).wrapping_sub(1); // lower `point` bits
    let a = parent_a as u32;
    let b = parent_b as u32;

    let child1 = (a & !mask) | (b & mask);
    let child2 = (b & !mask) | (a & mask);

    (child1 as i32, child2 as i32)
}

/// Bit-flip mutation at ~5% per bit.
/// Each of 32 bits has a 5% chance of flipping, because genetic diversity
/// is important — even in a population of integers.
fn mutate(individual: i32, rng: &mut Xorshift64) -> i32 {
    let mut bits = individual as u32;
    for bit_pos in 0..32u32 {
        // 5% ≈ 1/20. We use modulo 20 for simplicity.
        if rng.next() % 20 == 0 {
            bits ^= 1 << bit_pos;
        }
    }
    bits as i32
}

// ── Evolution Engine ────────────────────────────────────────────────────────

const POPULATION_SIZE: usize = 200;
const MAX_GENERATIONS: usize = 50;

/// Run the genetic algorithm and return (best_individual, generations_elapsed).
fn evolve(target: i32, rng: &mut Xorshift64) -> (i32, usize) {
    // Spawn initial population near the target (±500) for plausible convergence
    let mut population: Vec<i32> = (0..POPULATION_SIZE)
        .map(|_| rng.next_i32_near(target, 500))
        .collect();

    for gen in 0..MAX_GENERATIONS {
        // Check for a perfect specimen
        if let Some(&winner) = population.iter().find(|&&c| fitness(c, target) == 0) {
            return (winner, gen + 1);
        }

        // Breed next generation
        let mut next_gen = Vec::with_capacity(POPULATION_SIZE);

        // Elitism: carry forward the best individual (survival of the fittest,
        // taken literally)
        let elite = *population
            .iter()
            .min_by_key(|&&c| fitness(c, target))
            .unwrap();
        next_gen.push(elite);

        while next_gen.len() < POPULATION_SIZE {
            let parent_a = tournament_select(&population, target, rng);
            let parent_b = tournament_select(&population, target, rng);

            let (child1, child2) = crossover(parent_a, parent_b, rng);
            let child1 = mutate(child1, rng);
            let child2 = mutate(child2, rng);

            next_gen.push(child1);
            if next_gen.len() < POPULATION_SIZE {
                next_gen.push(child2);
            }
        }

        population = next_gen;
    }

    // Return best after exhausting generations (evolution doesn't always converge,
    // much like group projects)
    let best = *population
        .iter()
        .min_by_key(|&&c| fitness(c, target))
        .unwrap();
    (best, MAX_GENERATIONS)
}

// ── Public API ──────────────────────────────────────────────────────────────

/// Evolve an arithmetic result using a genetic algorithm.
///
/// Takes an expression string (for PRNG seeding) and the correct answer,
/// then simulates Darwinian evolution on a population of random integers
/// until one of them accidentally equals the answer.
///
/// Returns JSON:
/// ```json
/// {"evolved_result":8,"generations":17,"population_size":200,"final_fitness":0,"converged":true}
/// ```
///
/// The `converged` field indicates whether evolution found a perfect
/// specimen (fitness 0) or gave up after 50 generations of fruitless
/// natural selection. In the latter case, `evolved_result` is the
/// least-wrong individual — evolution's participation trophy.
pub fn evolve_result(expression: &str, correct_answer: i32) -> String {
    let mut rng = Xorshift64::from_expression(expression);
    let (result, generations) = evolve(correct_answer, &mut rng);
    let final_fitness = fitness(result, correct_answer);
    let converged = final_fitness == 0;

    format!(
        "{{\"evolved_result\":{},\"generations\":{},\"population_size\":{},\"final_fitness\":{},\"converged\":{}}}",
        result, generations, POPULATION_SIZE, final_fitness, converged
    )
}

// ── Tests ───────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_evolve_simple_addition() {
        let json = evolve_result("3+5", 8);
        assert!(json.contains("\"evolved_result\":8"));
        assert!(json.contains("\"converged\":true"));
        assert!(json.contains("\"final_fitness\":0"));
    }

    #[test]
    fn test_evolve_negative_target() {
        let json = evolve_result("5-12", -7);
        assert!(json.contains("\"evolved_result\":-7"));
        assert!(json.contains("\"converged\":true"));
    }

    #[test]
    fn test_evolve_zero() {
        let json = evolve_result("0*999", 0);
        assert!(json.contains("\"evolved_result\":0"));
        assert!(json.contains("\"converged\":true"));
    }

    #[test]
    fn test_deterministic() {
        let r1 = evolve_result("42*1", 42);
        let r2 = evolve_result("42*1", 42);
        assert_eq!(r1, r2, "Same expression must produce identical evolution");
    }

    #[test]
    fn test_different_expressions_different_seeds() {
        let r1 = evolve_result("3+5", 8);
        let r2 = evolve_result("4+4", 8);
        // Both should converge but may take different generation counts
        assert!(r1.contains("\"converged\":true"));
        assert!(r2.contains("\"converged\":true"));
    }

    #[test]
    fn test_population_size_in_output() {
        let json = evolve_result("1+1", 2);
        assert!(json.contains("\"population_size\":200"));
    }

    #[test]
    fn test_fitness_function() {
        assert_eq!(fitness(8, 8), 0);
        assert_eq!(fitness(10, 8), 2);
        assert_eq!(fitness(5, 8), 3);
        assert_eq!(fitness(-3, 5), 8);
    }

    #[test]
    fn test_xorshift_deterministic() {
        let mut rng1 = Xorshift64::from_expression("test");
        let mut rng2 = Xorshift64::from_expression("test");
        for _ in 0..100 {
            assert_eq!(rng1.next(), rng2.next());
        }
    }

    #[test]
    fn test_xorshift_different_seeds() {
        let mut rng1 = Xorshift64::from_expression("alpha");
        let mut rng2 = Xorshift64::from_expression("beta");
        // Extremely unlikely to produce the same sequence
        let different = (0..10).any(|_| rng1.next() != rng2.next());
        assert!(different);
    }
}
