package edu.singaporetech.inf2007quiz01.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.singaporetech.inf2007quiz01.FunctionMap
import edu.singaporetech.inf2007quiz01.RustBridge
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
    private val savedStateHandle: SavedStateHandle,
    private val dao: ExpressionHistoryDao,
    private val mathJsApi: MathJsApi,
    private val preferencesManager: PreferencesManager,
    private val calculationMicroservice: CalculationMicroservice,
    private val fibonacciConsensusEngine: FibonacciConsensusEngine,
    private val calBotPersonalityEngine: CalBotPersonalityEngine
) : ViewModel() {

    companion object {
        private const val TAG = "CalculatorVM"
    }

    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _historyList = MutableStateFlow<List<String>>(emptyList())
    val historyList: StateFlow<List<String>> = _historyList.asStateFlow()

    private val _isApiToggled = MutableStateFlow(false)
    val isApiToggled: StateFlow<Boolean> = _isApiToggled.asStateFlow()

    private val _calBotOrder = MutableStateFlow(
        savedStateHandle.get<ArrayList<Int>>("calBotOrder") ?: ArrayList((1..30).toList())
    )
    val calBotOrder: StateFlow<List<Int>> = _calBotOrder.asStateFlow()

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
        Log.d(TAG, "Selected CalBot $id (${personality.codename}) — " +
                "Mood: ${personality.mood}, Lucky #: ${personality.luckyNumber}")

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

            // Mine proof-of-work for the fibonacci computation
            val pow = RustBridge.proofOfWork("fib($input)=${consensus.value}")
            Log.d(TAG, "Fibonacci PoW: $pow")

            // Record in blockchain
            RustBridge.recordCalculation(fibExpression, consensus.value.toString())

            _displayText.value = consensus.value.toString()
            hasComputed = true
        }
    }

    fun toggleAPI() {
        val newValue = !_isApiToggled.value
        _isApiToggled.value = newValue
        viewModelScope.launch {
            preferencesManager.setApiToggle(currentCalBotId, newValue)
        }
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
