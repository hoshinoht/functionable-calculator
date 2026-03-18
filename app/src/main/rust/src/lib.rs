/// CalcuLux Core — The Rust Heart of the Enterprise Calculator
///
/// This is where Kotlin comes to die and be reborn as unsafe extern "system" fn.
/// Every JNI call crosses the sacred bridge between managed and unmanaged memory,
/// adding approximately 0.001ms of latency that we will claim is "negligible"
/// in our performance benchmarks.
///
/// Module layout:
///   - arithmetic:      Basic math operations (+, -, *, /) and operator strategy endpoints
///   - fibonacci:       O(log n) matrix exponentiation (Rust edition)
///   - wasm_fibonacci:  Fibonacci via embedded WebAssembly VM (inception edition)
///   - blockchain:      SHA-256 chained history + proof-of-work mining
///   - lambda:          Church numeral arithmetic via β-reduction
///   - monte_carlo:     Statistical verification of arithmetic via MC simulation
///   - zkp:             Schnorr zero-knowledge proofs (Fiat-Shamir)

mod arithmetic;
mod blockchain;
mod fibonacci;
mod genetic;
mod lambda;
mod monte_carlo;
mod pq_crypto;
mod raytracer;
mod wasm_fibonacci;
mod zkp;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jstring};
use jni::JNIEnv;

// ═══════════════════════════════════════════════════════════════════════════════
// ARITHMETIC — Full Expression Parser
// ═══════════════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_computeExpression(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
) -> jstring {
    let expr: String = match env.get_string(&expression) {
        Ok(s) => s.into(),
        Err(_) => {
            return env
                .new_string("Error")
                .expect("Failed to create string")
                .into_raw();
        }
    };
    let result = arithmetic::compute_expression(&expr);
    env.new_string(&result)
        .expect("Failed to create result string")
        .into_raw()
}

// ═══════════════════════════════════════════════════════════════════════════════
// OPERATOR STRATEGY ENDPOINTS — Individual ops for the Hilt DI Strategy Pattern
// ═══════════════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_add(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    arithmetic::add(a, b)
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_subtract(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    arithmetic::subtract(a, b)
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_multiply(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    arithmetic::multiply(a, b)
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_divide(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    arithmetic::divide(a, b)
}

// ═══════════════════════════════════════════════════════════════════════════════
// FIBONACCI — Three implementations because redundancy is reliability
// ═══════════════════════════════════════════════════════════════════════════════

/// Rust matrix exponentiation fibonacci (implementation #3)
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_fibonacci(
    _env: JNIEnv,
    _class: JClass,
    n: jint,
) -> jint {
    fibonacci::fibonacci(n)
}

/// WASM fibonacci — runs a WebAssembly VM inside Rust inside JNI (implementation #5)
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_wasmFibonacci(
    _env: JNIEnv,
    _class: JClass,
    n: jint,
) -> jint {
    wasm_fibonacci::fibonacci(n)
}

// ═══════════════════════════════════════════════════════════════════════════════
// FUNCTIONMAP HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_half(
    _env: JNIEnv,
    _class: JClass,
    x: jint,
) -> jint {
    arithmetic::half(x)
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_selfFunc(
    _env: JNIEnv,
    _class: JClass,
    x: jint,
) -> jint {
    arithmetic::self_func(x)
}

// ═══════════════════════════════════════════════════════════════════════════════
// BLOCKCHAIN + PROOF-OF-WORK MINING
// ═══════════════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_recordCalculation(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
    result: JString,
) -> jstring {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let res: String = env
        .get_string(&result)
        .map(|s| s.into())
        .unwrap_or_default();
    let hash = blockchain::record_calculation(&expr, &res);
    env.new_string(&hash)
        .expect("Failed to create hash string")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_verifyChain(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if blockchain::verify_chain() {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_getChainLength(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    blockchain::get_chain_length()
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_getBlockInfo(
    env: JNIEnv,
    _class: JClass,
    index: jint,
) -> jstring {
    let info = blockchain::get_block_info(index);
    env.new_string(&info)
        .expect("Failed to create block info string")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_verifyBlockDetailed(
    env: JNIEnv,
    _class: JClass,
    index: jint,
) -> jstring {
    let info = blockchain::verify_block_detailed(index);
    env.new_string(&info)
        .expect("Failed to create block verification string")
        .into_raw()
}

/// Proof-of-Work mining. Finds a nonce where SHA-256 has a leading zero.
/// Returns JSON with nonce, hash, and iteration count.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_proofOfWork(
    mut env: JNIEnv,
    _class: JClass,
    data: JString,
) -> jstring {
    let data_str: String = env
        .get_string(&data)
        .map(|s| s.into())
        .unwrap_or_default();
    let result = blockchain::proof_of_work(&data_str);
    env.new_string(&result)
        .expect("Failed to create PoW result string")
        .into_raw()
}

// ═══════════════════════════════════════════════════════════════════════════════
// BLOCKCHAIN INITIALISATION — Tell Rust where to persist the ledger
// ═══════════════════════════════════════════════════════════════════════════════

/// Sets the Android files directory so the blockchain can persist to disk.
/// Call once at app start with `context.filesDir.absolutePath`. Idempotent.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_initBlockchain(
    mut env: JNIEnv,
    _class: JClass,
    files_dir: JString,
) {
    let dir: String = env
        .get_string(&files_dir)
        .map(|s| s.into())
        .unwrap_or_default();
    blockchain::init_storage(&dir);
}

// ═══════════════════════════════════════════════════════════════════════════════
// POST-QUANTUM LATTICE CRYPTOGRAPHY — Because SHA-256 is so classically secure
// ═══════════════════════════════════════════════════════════════════════════════

/// Produces a compact LWE post-quantum seal for the given data string.
/// Uses the global enterprise keypair (n=16, q=3329, η=2).
/// Returns a 12-character hex string representing the LWE ciphertext.
/// Deterministic: seal(data) == seal(data) always. Tamper-evident by construction.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_pqSeal(
    mut env: JNIEnv,
    _class: JClass,
    data: JString,
) -> jstring {
    let data_str: String = env
        .get_string(&data)
        .map(|s| s.into())
        .unwrap_or_default();
    let seal = pq_crypto::seal_data(&data_str);
    env.new_string(&seal)
        .expect("Failed to create PQ seal string")
        .into_raw()
}

/// Verifies that the given seal was produced from the given data.
/// Returns 1 if authentic, 0 if tampered.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_pqVerify(
    mut env: JNIEnv,
    _class: JClass,
    data: JString,
    seal: JString,
) -> jboolean {
    let data_str: String = env.get_string(&data).map(|s| s.into()).unwrap_or_default();
    let seal_str: String = env.get_string(&seal).map(|s| s.into()).unwrap_or_default();
    if pq_crypto::verify_seal(&data_str, &seal_str) { 1 } else { 0 }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RAYTRACER — Software CPU Raytracing Because Why Not
// ═══════════════════════════════════════════════════════════════════════════════

/// Renders a 160×120 scene seeded by the calculator result.
/// Returns 76,800 RGBA bytes (R, G, B, A per pixel, A always 255).
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_renderScene(
    mut env: JNIEnv,
    _class: JClass,
    result: JString,
) -> jbyteArray {
    let seed_str: String = env
        .get_string(&result)
        .map(|s| s.into())
        .unwrap_or_default();
    let seed: f32 = seed_str.parse().unwrap_or(0.0);
    let pixels = raytracer::render_scene(seed);
    env.byte_array_from_slice(&pixels).unwrap().into_raw()
}

// ═══════════════════════════════════════════════════════════════════════════════
// LAMBDA CALCULUS — Church Numeral Arithmetic via β-Reduction
// ═══════════════════════════════════════════════════════════════════════════════

/// Computes an arithmetic expression via Church-encoded lambda calculus.
/// "3+5" → Church(3) ADD Church(5) → N β-reductions → Church(8) → "8"
/// Returns JSON: {"result":"8","reductions":47,"church":"λf.λx.f(f(...))"}
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_lambdaCompute(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
) -> jstring {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let result = lambda::lambda_compute(&expr);
    env.new_string(&result)
        .expect("Failed to create lambda result string")
        .into_raw()
}

// ═══════════════════════════════════════════════════════════════════════════════
// ZERO-KNOWLEDGE PROOFS — Schnorr Sigma Protocol (Fiat-Shamir)
// ═══════════════════════════════════════════════════════════════════════════════

/// Generates a non-interactive Schnorr ZKP for a calculation.
/// Proves knowledge of the computation without revealing operands.
/// Returns proof string "y:R:s".
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_zkpProve(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
    result: JString,
) -> jstring {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let res: String = env
        .get_string(&result)
        .map(|s| s.into())
        .unwrap_or_default();
    let proof = zkp::generate_proof(&expr, &res);
    env.new_string(&proof)
        .expect("Failed to create ZKP proof string")
        .into_raw()
}

/// Verifies a Schnorr ZKP for a calculation.
/// Returns 1 if the proof is valid, 0 if tampered or invalid.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_zkpVerify(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
    result: JString,
    proof: JString,
) -> jboolean {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let res: String = env
        .get_string(&result)
        .map(|s| s.into())
        .unwrap_or_default();
    let proof_str: String = env
        .get_string(&proof)
        .map(|s| s.into())
        .unwrap_or_default();
    if zkp::verify_proof(&expr, &res, &proof_str) {
        1
    } else {
        0
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GENETIC ALGORITHM — Natural Selection for Arithmetic
// ═══════════════════════════════════════════════════════════════════════════════

/// Evolves the correct answer via genetic algorithm.
/// Returns JSON: {"evolved_result":8,"generations":17,"population_size":200,...}
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_geneticEvolve(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
    correct_answer: jint,
) -> jstring {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let result = genetic::evolve_result(&expr, correct_answer);
    env.new_string(&result)
        .expect("Failed to create genetic result string")
        .into_raw()
}

// ═══════════════════════════════════════════════════════════════════════════════
// MONTE CARLO — Statistical Verification of Deterministic Arithmetic
// ═══════════════════════════════════════════════════════════════════════════════

/// Verifies an arithmetic expression via Monte Carlo simulation.
/// Returns JSON with estimate, samples, confidence interval, and error %.
#[no_mangle]
pub extern "system" fn Java_edu_singaporetech_inf2007quiz01_RustBridge_monteCarloVerify(
    mut env: JNIEnv,
    _class: JClass,
    expression: JString,
) -> jstring {
    let expr: String = env
        .get_string(&expression)
        .map(|s| s.into())
        .unwrap_or_default();
    let result = monte_carlo::monte_carlo_verify(&expr);
    env.new_string(&result)
        .expect("Failed to create Monte Carlo result string")
        .into_raw()
}
