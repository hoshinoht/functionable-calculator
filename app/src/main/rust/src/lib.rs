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

mod arithmetic;
mod blockchain;
mod fibonacci;
mod wasm_fibonacci;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring};
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
