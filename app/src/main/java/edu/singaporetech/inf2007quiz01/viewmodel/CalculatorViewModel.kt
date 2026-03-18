package edu.singaporetech.inf2007quiz01.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.singaporetech.inf2007quiz01.FunctionMap
import edu.singaporetech.inf2007quiz01.data.ExpressionHistory
import edu.singaporetech.inf2007quiz01.data.ExpressionHistoryDao
import edu.singaporetech.inf2007quiz01.data.MathJsApi
import edu.singaporetech.inf2007quiz01.data.PreferencesManager
import edu.singaporetech.inf2007quiz01.navigation.CalculatorRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dao: ExpressionHistoryDao,
    private val mathJsApi: MathJsApi,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

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
                    computeLocally(expression)
                }

                addToHistory(expression)
                _displayText.value = result
                hasComputed = true
            } catch (e: Exception) {
                _displayText.value = "Error"
            }
        }
    }

    private fun computeLocally(expression: String): String {
        // Find the operator position (skip the first character to allow negative first operand)
        val regex = Regex("""(-?\d+)([+\-*/])(-?\d+)""")
        val match = regex.find(expression) ?: return "Error"

        val num1 = match.groupValues[1].toInt()
        val op = match.groupValues[2]
        val num2 = match.groupValues[3].toInt()

        val result = when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> if (num2 != 0) num1 / num2 else return "Error"
            else -> return "Error"
        }

        return result.toString()
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

        // Compute on background thread so UI remains responsive
        viewModelScope.launch(Dispatchers.Default) {
            val result = FunctionMap.functionMap["fib"]!!(input)
            _displayText.value = result.toString()
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
