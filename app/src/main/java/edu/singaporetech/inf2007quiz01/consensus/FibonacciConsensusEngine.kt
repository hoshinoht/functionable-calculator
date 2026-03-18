package edu.singaporetech.inf2007quiz01.consensus

import android.util.Log
import edu.singaporetech.inf2007quiz01.FunctionMap
import edu.singaporetech.inf2007quiz01.NativeFibonacci
import edu.singaporetech.inf2007quiz01.RustBridge
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fibonacci Consensus Engine — Byzantine Fault Tolerant Fibonacci
 *
 * Runs ALL FIVE Fibonacci implementations in parallel and takes a majority
 * vote. If any implementation disagrees, it is flagged as a "Byzantine node"
 * and logged for investigation.
 *
 * Implementations:
 *   1. Kotlin iterative (legacy)
 *   2. C++ matrix exponentiation (JNI)
 *   3. Rust matrix exponentiation (JNI)
 *   4. ARM64 assembly / C++ fallback (JNI)
 *   5. WebAssembly VM inside Rust inside JNI (inception)
 *
 * All five produce identical results. All five are necessary.
 * The redundancy ensures Five Nines (99.999%) Fibonacci availability.
 * This is not a joke. (It is absolutely a joke.)
 */
@Singleton
class FibonacciConsensusEngine @Inject constructor() {

    companion object {
        private const val TAG = "FibConsensus"
    }

    data class ConsensusResult(
        val value: Int,
        val unanimous: Boolean,
        val votes: Map<String, Int>,
        val byzantineNodes: List<String>
    )

    /**
     * Computes fibonacci(n) using all 5 implementations in parallel,
     * then takes a majority vote.
     *
     * In practice, they always agree. We run all 5 anyway because
     * distributed consensus is non-negotiable in enterprise software.
     */
    suspend fun compute(n: Int): ConsensusResult = coroutineScope {
        val kotlinResult = async { runCatching { FunctionMap.ktFib(n) }.getOrDefault(-1) }
        val cppResult = async { runCatching { NativeFibonacci.fib(n) }.getOrDefault(-1) }
        val rustResult = async { runCatching { RustBridge.fibonacci(n) }.getOrDefault(-1) }
        val asmResult = async { runCatching { NativeFibonacci.asmFib(n) }.getOrDefault(-1) }
        val wasmResult = async { runCatching { RustBridge.wasmFibonacci(n) }.getOrDefault(-1) }

        val votes = mapOf(
            "Kotlin" to kotlinResult.await(),
            "C++" to cppResult.await(),
            "Rust" to rustResult.await(),
            "ARM-ASM" to asmResult.await(),
            "WASM" to wasmResult.await()
        )

        // Majority vote
        val tally = votes.values.groupingBy { it }.eachCount()
        val consensusValue = tally.maxByOrNull { it.value }!!.key

        // Identify Byzantine nodes (disagreeing implementations)
        val byzantineNodes = votes.filter { it.value != consensusValue }.keys.toList()

        if (byzantineNodes.isNotEmpty()) {
            Log.e(TAG, "CONSENSUS FAILURE for fib($n)!")
            Log.e(TAG, "Consensus value: $consensusValue")
            byzantineNodes.forEach { node ->
                Log.e(TAG, "Byzantine node '$node' returned ${votes[node]} instead of $consensusValue")
            }
        } else {
            Log.d(TAG, "Unanimous consensus for fib($n) = $consensusValue [5/5 nodes agree]")
        }

        ConsensusResult(
            value = consensusValue,
            unanimous = byzantineNodes.isEmpty(),
            votes = votes,
            byzantineNodes = byzantineNodes
        )
    }
}
