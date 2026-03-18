package edu.singaporetech.inf2007quiz01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import edu.singaporetech.inf2007quiz01.navigation.CalculatorRoute
import edu.singaporetech.inf2007quiz01.navigation.ContactRoute
import edu.singaporetech.inf2007quiz01.ui.CalculatorScreen
import edu.singaporetech.inf2007quiz01.ui.ContactScreen
import edu.singaporetech.inf2007quiz01.ui.theme.Inf2007quiz01Theme
import edu.singaporetech.inf2007quiz01.viewmodel.CalculatorViewModel

// For displaying the calculator pad
data class CalculatorButton(
    val text: String = "",
    val isDigit: Boolean = true
)

// 5 rows
val CalculatorPadRow = arrayListOf(
    arrayListOf(CalculatorButton("AC", false), CalculatorButton("DEL", false), CalculatorButton("FIB", false), CalculatorButton("/", false)),
    arrayListOf(CalculatorButton("7"), CalculatorButton("8"), CalculatorButton("9"), CalculatorButton("*", false)),
    arrayListOf(CalculatorButton("4"), CalculatorButton("5"), CalculatorButton("6"), CalculatorButton("-", false)),
    arrayListOf(CalculatorButton("1"), CalculatorButton("2"), CalculatorButton("3"), CalculatorButton("+", false)),
    arrayListOf(CalculatorButton("0"), CalculatorButton("=", false)),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Inf2007quiz01Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: CalculatorViewModel = hiltViewModel()
                    val calBotOrder by viewModel.calBotOrder.collectAsState()
                    val backStack = remember {
                        mutableStateListOf<Any>(ContactRoute).apply {
                            viewModel.currentCalBotRoute?.let { add(it) }
                        }
                    }

                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            val currentRoute = backStack.lastOrNull()
                            if (currentRoute is CalculatorRoute) {
                                viewModel.onBackFromCalculator(currentRoute.calBotId)
                            }
                            backStack.removeLastOrNull()
                        },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        entryProvider = entryProvider {
                            entry<ContactRoute> {
                                ContactScreen(
                                    calBotOrder = calBotOrder,
                                    onCalBotClick = { id, name ->
                                        viewModel.setCalBot(id, name)
                                        backStack.add(CalculatorRoute(id, name))
                                    },
                                    getPersonality = viewModel::getPersonality
                                )
                            }

                            entry<CalculatorRoute> { route ->
                                CalculatorScreen(
                                    viewModel = viewModel,
                                    calBotName = route.calBotName
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
