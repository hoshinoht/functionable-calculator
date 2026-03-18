/// WASM-in-Rust Fibonacci Module
///
/// This module embeds a WebAssembly virtual machine (wasmi) inside the Rust
/// native library, which is loaded via JNI from Kotlin, which runs on the
/// Android Runtime (ART), which runs on Linux.
///
/// The call chain for computing fib(10):
///   Kotlin → JNI → Rust → WASM VM → WASM bytecode → result
///   → back through WASM VM → Rust → JNI → Kotlin → Compose UI
///
/// Four layers of virtual machines. Five language boundaries.
/// All to compute the number 55.

use std::sync::Mutex;
use wasmi::*;

/// The Fibonacci function implemented in WebAssembly Text format.
/// This is compiled to WASM bytecode at runtime because ahead-of-time
/// compilation would be too efficient.
const FIBONACCI_WAT: &str = r#"
(module
  (func $fib (export "fib") (param $n i32) (result i32)
    (local $a i32)
    (local $b i32)
    (local $tmp i32)
    (if (i32.le_s (local.get $n) (i32.const 0))
      (then (return (i32.const 0)))
    )
    (if (i32.eq (local.get $n) (i32.const 1))
      (then (return (i32.const 1)))
    )
    (local.set $a (i32.const 0))
    (local.set $b (i32.const 1))
    (local.set $n (i32.sub (local.get $n) (i32.const 1)))
    (block $break
      (loop $loop
        (br_if $break (i32.le_s (local.get $n) (i32.const 0)))
        (local.set $tmp (i32.add (local.get $a) (local.get $b)))
        (local.set $a (local.get $b))
        (local.set $b (local.get $tmp))
        (local.set $n (i32.sub (local.get $n) (i32.const 1)))
        (br $loop)
      )
    )
    (local.get $b)
  )
)
"#;

/// Cached WASM instance — we're overengineered, not wasteful.
/// The Engine, Module, and Store are created once and reused.
struct WasmFibInstance {
    engine: Engine,
    module: Module,
}

static WASM_INSTANCE: Mutex<Option<WasmFibInstance>> = Mutex::new(None);

fn get_or_init_wasm() -> (Engine, Module) {
    let mut guard = WASM_INSTANCE.lock().unwrap();
    if guard.is_none() {
        let engine = Engine::default();
        let wasm_bytes = wat::parse_str(FIBONACCI_WAT).expect("Failed to parse WAT");
        let module = Module::new(&engine, &wasm_bytes[..]).expect("Failed to compile WASM");
        *guard = Some(WasmFibInstance {
            engine: engine.clone(),
            module: module.clone(),
        });
    }
    let instance = guard.as_ref().unwrap();
    (instance.engine.clone(), instance.module.clone())
}

/// Computes fibonacci by:
/// 1. Parsing WAT source code into WASM bytecode
/// 2. Instantiating a WebAssembly virtual machine
/// 3. Loading the module into the VM
/// 4. Calling the exported "fib" function
/// 5. Extracting the i32 result
///
/// All of this happens inside a Rust native library loaded via JNI.
/// The number of abstraction layers is truly staggering.
pub fn fibonacci(n: i32) -> i32 {
    let (engine, module) = get_or_init_wasm();
    let mut store = Store::new(&engine, ());
    let linker = <Linker<()>>::new(&engine);
    let instance = linker
        .instantiate(&mut store, &module)
        .expect("Failed to instantiate WASM module")
        .start(&mut store)
        .expect("Failed to start WASM instance");

    let fib_func = instance
        .get_typed_func::<i32, i32>(&store, "fib")
        .expect("Failed to find 'fib' export in WASM module");

    fib_func
        .call(&mut store, n)
        .expect("WASM fibonacci computation failed")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_wasm_fib_base() {
        assert_eq!(fibonacci(0), 0);
        assert_eq!(fibonacci(1), 1);
    }

    #[test]
    fn test_wasm_fib_10() {
        assert_eq!(fibonacci(10), 55);
    }

    #[test]
    fn test_wasm_fib_44() {
        assert_eq!(fibonacci(44), 701408733);
    }
}
