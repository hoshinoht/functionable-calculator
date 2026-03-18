package edu.singaporetech.inf2007quiz01.service

import android.util.Log
import edu.singaporetech.inf2007quiz01.RustBridge
import edu.singaporetech.inf2007quiz01.operator.OperatorStrategyEngine
import edu.singaporetech.inf2007quiz01.personality.CalBotPersonalityEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculation Microservice — Channel-Based Local Service Mesh
 *
 * This "microservice" receives calculation requests via a Kotlin Channel,
 * processes them through the Operator Strategy Engine, mines a proof-of-work
 * block, records to the blockchain, and returns the result via a
 * CompletableDeferred.
 *
 * All of this happens within the same process, on the same device,
 * in the same thread pool. The Channel adds exactly zero value beyond
 * making the architecture diagram look like a real microservice.
 *
 * Call chain:
 *   ViewModel → Channel.send(request)
 *     → Microservice coroutine picks up request
 *       → OperatorStrategyEngine parses & dispatches
 *         → ArithmeticOperator calls RustBridge
 *           → JNI → Rust computes
 *       → Proof-of-Work mining
 *       → Blockchain recording
 *     → CompletableDeferred.complete(result)
 *   ← ViewModel receives result
 *
 * Total indirection layers: 7
 * Necessary indirection layers: 0
 */
@Singleton
class CalculationMicroservice @Inject constructor(
    private val strategyEngine: OperatorStrategyEngine,
    private val personalityEngine: CalBotPersonalityEngine
) {
    companion object {
        private const val TAG = "CalcMicroservice"
    }

    data class CalculationRequest(
        val expression: String,
        val calBotId: Int,
        val result: CompletableDeferred<String>
    )

    private val requestChannel = Channel<CalculationRequest>(Channel.BUFFERED)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Launch the "microservice" — a coroutine pretending to be a service
        serviceScope.launch {
            Log.d(TAG, "CalculationMicroservice online. Awaiting requests.")
            for (request in requestChannel) {
                processRequest(request)
            }
        }
    }

    private fun processRequest(request: CalculationRequest) {
        try {
            // Get CalBot personality for logging flavor
            val personality = personalityEngine.getPersonality(request.calBotId)
            Log.d(TAG, "Processing request from CalBot ${request.calBotId} " +
                    "(mood: ${personality.mood}, prefers: '${personality.preferredOperator}')")

            // Route through the Strategy Engine
            val result = strategyEngine.compute(request.expression)

            // Mine a proof-of-work block (because we can)
            val pow = RustBridge.proofOfWork("${request.expression}=$result")
            Log.d(TAG, "Proof-of-Work mined: $pow")

            // Record in blockchain
            val blockHash = RustBridge.recordCalculation(request.expression, result)
            Log.d(TAG, "Block recorded: $blockHash")

            // CalBot personality commentary
            Log.d(TAG, "CalBot ${request.calBotId} says: \"${personality.catchPhrase}\"")

            request.result.complete(result)
        } catch (e: Exception) {
            Log.e(TAG, "Microservice computation failed", e)
            request.result.complete("Error")
        }
    }

    /**
     * Submit a calculation request to the microservice.
     * The request travels through a Channel, gets processed by the
     * service coroutine, and the result comes back via CompletableDeferred.
     *
     * This is functionally identical to calling strategyEngine.compute()
     * directly, but with extra steps.
     */
    suspend fun compute(expression: String, calBotId: Int): String {
        val deferred = CompletableDeferred<String>()
        requestChannel.send(CalculationRequest(expression, calBotId, deferred))
        return deferred.await()
    }
}
