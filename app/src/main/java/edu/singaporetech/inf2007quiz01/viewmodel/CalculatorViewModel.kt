package edu.singaporetech.inf2007quiz01.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.singaporetech.inf2007quiz01.FunctionMap
import edu.singaporetech.inf2007quiz01.GpuBridge
import edu.singaporetech.inf2007quiz01.RustBridge
import edu.singaporetech.inf2007quiz01.audio.ResultSonificationEngine
import edu.singaporetech.inf2007quiz01.consensus.FibonacciConsensusEngine
import edu.singaporetech.inf2007quiz01.data.ExpressionHistory
import edu.singaporetech.inf2007quiz01.data.ExpressionHistoryDao
import edu.singaporetech.inf2007quiz01.data.MathJsApi
import edu.singaporetech.inf2007quiz01.data.PreferencesManager
import edu.singaporetech.inf2007quiz01.navigation.CalculatorRoute
import edu.singaporetech.inf2007quiz01.personality.CalBotPersonalityEngine
import edu.singaporetech.inf2007quiz01.service.CalculationMicroservice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * CalculatorViewModel — The Orchestration Layer
 *
 * Previously a simple ViewModel that did math. Now the central nervous system
 * of a distributed computation platform that routes arithmetic through:
 *   - A Channel-based microservice
 *   - The Hilt-injected Operator Strategy Engine
 *   - Individual Rust JNI calls per operator
 *   - Proof-of-Work mining
 *   - Blockchain recording
 *   - CalBot personality commentary
 *
 * For Fibonacci, uses the Byzantine Fault Tolerant Consensus Engine that
 * runs 5 implementations in parallel across 3 languages + assembly + WASM.
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val dao: ExpressionHistoryDao,
    private val mathJsApi: MathJsApi,
    private val preferencesManager: PreferencesManager,
    private val calculationMicroservice: CalculationMicroservice,
    private val fibonacciConsensusEngine: FibonacciConsensusEngine,
    private val calBotPersonalityEngine: CalBotPersonalityEngine,
    private val sonificationEngine: ResultSonificationEngine
) : ViewModel() {

    data class BlockRecord(
        val index: Int,
        val expression: String,
        val result: String,
        val hash: String,
        val pqSeal: String,
        val zkpProof: String,
        val zkpVerified: Boolean = false
    )

    data class BlockVerification(
        val index: Int,
        val hashValid: Boolean,
        val chainLinkValid: Boolean,
        val pqSealValid: Boolean,
        val zkpValid: Boolean
    ) {
        val allPassed: Boolean get() = hashValid && chainLinkValid && pqSealValid && zkpValid
    }

    sealed class ChainVerificationState {
        object Idle : ChainVerificationState()
        data class InProgress(
            val currentIndex: Int,
            val totalBlocks: Int,
            val results: List<BlockVerification>
        ) : ChainVerificationState()
        data class Complete(
            val results: List<BlockVerification>,
            val allPassed: Boolean
        ) : ChainVerificationState()
    }

    // ── Genetic Algorithm ──────────────────────────────────────────────────
    data class GeneticResult(val evolvedResult: Int, val generations: Int, val populationSize: Int, val converged: Boolean)

    // ── Monte Carlo ──────────────────────────────────────────────────────
    data class MonteCarloResult(
        val estimate: Double, val exact: Int, val samples: Int,
        val method: String, val errorPct: Double
    )

    // ── GPU Compute ──────────────────────────────────────────────────────
    data class GpuVerificationResult(val result: Int, val device: String, val dispatchUs: Int, val verified: Boolean)

    // ── Sonification ─────────────────────────────────────────────────────
    data class SonificationInfo(val scaleType: String, val noteCount: Int)

    // ── Byzantine Consensus Visualization ────────────────────────────────
    data class NodeResult(
        val name: String,
        val result: Int,
        val agreedWithConsensus: Boolean,
        val isByzantine: Boolean
    )
    data class ConsensusVisualization(
        val nodes: List<NodeResult>,
        val consensusValue: Int,
        val unanimous: Boolean
    )

    companion object {
        private const val TAG = "CalculatorVM"
    }

    init {
        // Initialise blockchain persistence before any blockchain operations.
        // Rust will load the existing chain from disk (or create a fresh one).
        RustBridge.initBlockchain(context.filesDir.absolutePath)
    }

    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _historyList = MutableStateFlow<List<String>>(emptyList())
    val historyList: StateFlow<List<String>> = _historyList.asStateFlow()

    private val _isApiToggled = MutableStateFlow(false)
    val isApiToggled: StateFlow<Boolean> = _isApiToggled.asStateFlow()

    private val _catchPhrase = MutableStateFlow("")
    val catchPhrase: StateFlow<String> = _catchPhrase.asStateFlow()

    private val _blockchainRecords = MutableStateFlow<List<BlockRecord>>(emptyList())
    val blockchainRecords: StateFlow<List<BlockRecord>> = _blockchainRecords.asStateFlow()

    private val _raytracedImage = MutableStateFlow<ByteArray?>(null)
    val raytracedImage: StateFlow<ByteArray?> = _raytracedImage.asStateFlow()

    private val _lambdaInfo = MutableStateFlow("")
    val lambdaInfo: StateFlow<String> = _lambdaInfo.asStateFlow()

    private val _calBotOrder = MutableStateFlow(
        savedStateHandle.get<ArrayList<Int>>("calBotOrder") ?: ArrayList((1..30).toList())
    )
    val calBotOrder: StateFlow<List<Int>> = _calBotOrder.asStateFlow()

    private val _chainVerificationState = MutableStateFlow<ChainVerificationState>(ChainVerificationState.Idle)
    val chainVerificationState: StateFlow<ChainVerificationState> = _chainVerificationState.asStateFlow()

    private val _geneticResult = MutableStateFlow<GeneticResult?>(null)
    val geneticResult: StateFlow<GeneticResult?> = _geneticResult.asStateFlow()

    private val _monteCarloResult = MutableStateFlow<MonteCarloResult?>(null)
    val monteCarloResult: StateFlow<MonteCarloResult?> = _monteCarloResult.asStateFlow()

    private val _gpuVerification = MutableStateFlow<GpuVerificationResult?>(null)
    val gpuVerification: StateFlow<GpuVerificationResult?> = _gpuVerification.asStateFlow()

    private val _sonificationInfo = MutableStateFlow<SonificationInfo?>(null)
    val sonificationInfo: StateFlow<SonificationInfo?> = _sonificationInfo.asStateFlow()

    private val _consensusViz = MutableStateFlow<ConsensusVisualization?>(null)
    val consensusViz: StateFlow<ConsensusVisualization?> = _consensusViz.asStateFlow()

    private var currentCalBotId: Int = -1
    private var hasComputed: Boolean = false

    // Track current route so backstack can be rebuilt after rotation
    var currentCalBotRoute: CalculatorRoute? = null
        private set

    private var historyJob: Job? = null
    private var toggleJob: Job? = null

    // Set the current CalBot and load its history + toggle state
    fun setCalBot(id: Int, name: String) {
        currentCalBotId = id
        currentCalBotRoute = CalculatorRoute(id, name)
        hasComputed = false
        _displayText.value = ""

        // Log CalBot personality on selection
        val personality = calBotPersonalityEngine.getPersonality(id)
        _catchPhrase.value = personality.catchPhrase
        Log.d(TAG, "Selected CalBot $id (${personality.codename}) — " +
                "Mood: ${personality.mood}, Lucky #: ${personality.luckyNumber}")

        viewModelScope.launch { refreshBlockchain() }

        historyJob?.cancel()
        toggleJob?.cancel()

        historyJob = viewModelScope.launch {
            dao.getHistoryByCalBot(id).collect { historyEntities ->
                _historyList.value = historyEntities.map { it.expression }
            }
        }

        toggleJob = viewModelScope.launch {
            preferencesManager.getApiToggle(id).collect { toggled ->
                _isApiToggled.value = toggled
            }
        }
    }

    fun onDigitPress(digit: String) {
        _displayText.value += digit
    }

    fun onOperatorPress(op: String) {
        _displayText.value += op
    }

    fun onEquals() {
        val expression = _displayText.value
        if (expression.isBlank()) return

        viewModelScope.launch {
            try {
                val result = if (_isApiToggled.value) {
                    withContext(Dispatchers.IO) {
                        val response = mathJsApi.calculate(expression)
                        if (response.isSuccessful) {
                            response.body()?.string()?.trim() ?: "Error"
                        } else {
                            "Error"
                        }
                    }
                } else {
                    // Route through the Calculation Microservice
                    // (Channel → Strategy Engine → Operator → Rust JNI → Proof-of-Work → Blockchain)
                    calculationMicroservice.compute(expression, currentCalBotId)
                }

                addToHistory(expression)
                _displayText.value = result
                hasComputed = true
                refreshBlockchain()
                viewModelScope.launch(Dispatchers.Default) {
                    _raytracedImage.value = RustBridge.renderScene(result)
                }
                // Lambda calculus verification — compute via Church numerals
                viewModelScope.launch(Dispatchers.Default) {
                    val lambdaResult = RustBridge.lambdaCompute(expression)
                    val reductions = Regex(""""reductions":(\d+)""")
                        .find(lambdaResult)?.groupValues?.get(1) ?: "?"
                    val error = Regex(""""error":"([^"]+)"""")
                        .find(lambdaResult)?.groupValues?.get(1)
                    _lambdaInfo.value = if (error != null) {
                        "λ: $error"
                    } else {
                        "λ-verified via $reductions β-reductions"
                    }
                    Log.d(TAG, "Lambda calculus: $lambdaResult")
                }
                // Genetic Algorithm — evolve the result
                viewModelScope.launch(Dispatchers.Default) {
                    val numResult = result.toIntOrNull() ?: return@launch
                    val gaJson = RustBridge.geneticEvolve(expression, numResult)
                    val gens = Regex(""""generations":(\d+)""").find(gaJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val pop = Regex(""""population_size":(\d+)""").find(gaJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val converged = Regex(""""converged":(true|false)""").find(gaJson)?.groupValues?.get(1) == "true"
                    val evolved = Regex(""""evolved_result":(-?\d+)""").find(gaJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    _geneticResult.value = GeneticResult(evolved, gens, pop, converged)
                    Log.d(TAG, "Genetic algorithm: $gaJson")
                }
                // Monte Carlo verification
                viewModelScope.launch(Dispatchers.Default) {
                    val mcJson = RustBridge.monteCarloVerify(expression)
                    val estimate = Regex(""""estimate":([-\d.]+)""").find(mcJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    val exact = Regex(""""exact":(-?\d+)""").find(mcJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val samples = Regex(""""samples":(\d+)""").find(mcJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val method = Regex(""""method":"([^"]+)"""").find(mcJson)?.groupValues?.get(1) ?: ""
                    val errorPct = Regex(""""error_pct":([\d.]+)""").find(mcJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    if (samples > 0) _monteCarloResult.value = MonteCarloResult(estimate, exact, samples, method, errorPct)
                    Log.d(TAG, "Monte Carlo: $mcJson")
                }
                // GPU Compute Shader verification (addition only)
                viewModelScope.launch(Dispatchers.Default) {
                    val match = Regex("""(-?\d+)\+(-?\d+)""").find(expression) ?: return@launch
                    val a = match.groupValues[1].toIntOrNull() ?: return@launch
                    val b = match.groupValues[2].toIntOrNull() ?: return@launch
                    try {
                        if (!GpuBridge.isVulkanAvailable()) return@launch
                        val gpuJson = GpuBridge.gpuAdd(a, b)
                        val gpuResult = Regex(""""result":(-?\d+)""").find(gpuJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val device = Regex(""""device":"([^"]+)"""").find(gpuJson)?.groupValues?.get(1) ?: "unknown"
                        val dispatchUs = Regex(""""dispatch_us":(\d+)""").find(gpuJson)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val verified = Regex(""""verified":(true|false)""").find(gpuJson)?.groupValues?.get(1) == "true"
                        _gpuVerification.value = GpuVerificationResult(gpuResult, device, dispatchUs, verified)
                        Log.d(TAG, "GPU compute: $gpuJson")
                    } catch (e: Exception) {
                        Log.w(TAG, "GPU compute unavailable: ${e.message}")
                    }
                }
                // Sonification — play the result as music
                viewModelScope.launch(Dispatchers.IO) {
                    val info = sonificationEngine.sonify(result)
                    _sonificationInfo.value = SonificationInfo(info.scaleType, info.noteCount)
                }
            } catch (e: Exception) {
                _displayText.value = "Error"
            }
        }
    }

    private suspend fun addToHistory(expression: String) {
        dao.insert(ExpressionHistory(calBotId = currentCalBotId, expression = expression))
        if (dao.countForCalBot(currentCalBotId) > 20) {
            dao.deleteOldestForCalBot(currentCalBotId)
        }
    }

    fun onAC() {
        _displayText.value = ""
        _raytracedImage.value = null
        _lambdaInfo.value = ""
        _geneticResult.value = null
        _monteCarloResult.value = null
        _gpuVerification.value = null
        _sonificationInfo.value = null
        _consensusViz.value = null
        sonificationEngine.stop()
    }

    fun onDEL() {
        _displayText.value = _displayText.value.dropLast(1)
    }

    fun onFIB() {
        val input = _displayText.value.toIntOrNull() ?: return
        val fibExpression = "fib($input)"

        // Add to history immediately
        viewModelScope.launch {
            addToHistory(fibExpression)
        }

        // Run the Byzantine Fault Tolerant Fibonacci Consensus Engine
        // All 5 implementations execute in parallel, majority vote determines result
        viewModelScope.launch(Dispatchers.Default) {
            val consensus = fibonacciConsensusEngine.compute(input)
            Log.d(TAG, "Fibonacci consensus: ${consensus.value} " +
                    "(unanimous: ${consensus.unanimous}, votes: ${consensus.votes})")

            // Byzantine Consensus Visualization
            _consensusViz.value = ConsensusVisualization(
                nodes = consensus.votes.map { (name, result) ->
                    NodeResult(
                        name = name,
                        result = result,
                        agreedWithConsensus = result == consensus.value,
                        isByzantine = name in consensus.byzantineNodes
                    )
                },
                consensusValue = consensus.value,
                unanimous = consensus.unanimous
            )

            // Mine proof-of-work for the fibonacci computation
            val pow = RustBridge.proofOfWork("fib($input)=${consensus.value}")
            Log.d(TAG, "Fibonacci PoW: $pow")

            // Record in blockchain
            RustBridge.recordCalculation(fibExpression, consensus.value.toString())

            _displayText.value = consensus.value.toString()
            hasComputed = true
            refreshBlockchain()
            _raytracedImage.value = RustBridge.renderScene(consensus.value.toString())

            // Sonification for Fibonacci result
            viewModelScope.launch(Dispatchers.IO) {
                val info = sonificationEngine.sonify(consensus.value.toString())
                _sonificationInfo.value = SonificationInfo(info.scaleType, info.noteCount)
            }
        }
    }

    fun toggleAPI() {
        val newValue = !_isApiToggled.value
        _isApiToggled.value = newValue
        viewModelScope.launch {
            preferencesManager.setApiToggle(currentCalBotId, newValue)
        }
    }

    fun getPersonality(id: Int): CalBotPersonalityEngine.Personality =
        calBotPersonalityEngine.getPersonality(id)

    private fun parseBlockRecord(json: String): BlockRecord? {
        if (json.isBlank()) return null
        val index = Regex(""""index":(\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val expression = Regex(""""expression":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: return null
        val result = Regex(""""result":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: return null
        val hash = Regex(""""hash":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: return null
        val pqSeal = Regex(""""pq_seal":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        val zkpProof = Regex(""""zkp_proof":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        return BlockRecord(index, expression, result, hash, pqSeal, zkpProof)
    }

    private suspend fun refreshBlockchain() = withContext(Dispatchers.Default) {
        val length = RustBridge.getChainLength()
        _blockchainRecords.value = (0 until length).mapNotNull { i ->
            val record = parseBlockRecord(RustBridge.getBlockInfo(i)) ?: return@mapNotNull null
            val verified = record.zkpProof.isNotEmpty() &&
                    RustBridge.zkpVerify(record.expression, record.result, record.zkpProof)
            record.copy(zkpVerified = verified)
        }
    }

    fun startChainVerification() {
        viewModelScope.launch(Dispatchers.Default) {
            val totalBlocks = RustBridge.getChainLength()
            val results = mutableListOf<BlockVerification>()
            for (i in 0 until totalBlocks) {
                _chainVerificationState.value = ChainVerificationState.InProgress(i, totalBlocks, results.toList())
                delay(200L)
                val json = RustBridge.verifyBlockDetailed(i)
                val verification = parseBlockVerification(i, json)
                if (verification != null) results.add(verification)
            }
            _chainVerificationState.value = ChainVerificationState.Complete(
                results = results,
                allPassed = results.all { it.allPassed }
            )
        }
    }

    fun resetVerification() {
        _chainVerificationState.value = ChainVerificationState.Idle
    }

    private fun parseBlockVerification(index: Int, json: String): BlockVerification? {
        if (json.isBlank()) return null
        val hashValid = Regex(""""hash_valid":(true|false)""").find(json)?.groupValues?.get(1) == "true"
        val chainLinkValid = Regex(""""chain_link_valid":(true|false)""").find(json)?.groupValues?.get(1) == "true"
        val pqSealValid = Regex(""""pq_seal_valid":(true|false)""").find(json)?.groupValues?.get(1) == "true"
        val zkpValid = Regex(""""zkp_valid":(true|false)""").find(json)?.groupValues?.get(1) == "true"
        return BlockVerification(index, hashValid, chainLinkValid, pqSealValid, zkpValid)
    }

    // Called when navigating back from the calculator screen
    fun onBackFromCalculator(calBotId: Int) {
        currentCalBotRoute = null
        if (hasComputed) {
            val currentOrder = ArrayList<Int>(_calBotOrder.value)
            currentOrder.remove(calBotId)
            currentOrder.add(0, calBotId)
            _calBotOrder.value = currentOrder
            savedStateHandle["calBotOrder"] = currentOrder
        }
        hasComputed = false
    }
}
