package edu.singaporetech.inf2007quiz01.ui

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.singaporetech.inf2007quiz01.CalculatorButton
import edu.singaporetech.inf2007quiz01.CalculatorPadRow
import edu.singaporetech.inf2007quiz01.ui.theme.ACColor
import edu.singaporetech.inf2007quiz01.ui.theme.ApiToggleBg
import edu.singaporetech.inf2007quiz01.ui.theme.ApiToggleBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.DELColor
import edu.singaporetech.inf2007quiz01.ui.theme.DigitButtonColor
import edu.singaporetech.inf2007quiz01.ui.theme.DigitButtonColorLight
import edu.singaporetech.inf2007quiz01.ui.theme.DisplayBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.DisplayBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.EqualsColor
import edu.singaporetech.inf2007quiz01.ui.theme.FIBColor
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryItemBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryItemBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.OperatorColor
import edu.singaporetech.inf2007quiz01.ui.theme.OperatorColorLight
import edu.singaporetech.inf2007quiz01.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    calBotName: String,
    onBack: (() -> Unit)? = null
) {
    val displayText by viewModel.displayText.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val isApiToggled by viewModel.isApiToggled.collectAsState()
    val configuration = LocalConfiguration.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        calBotName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HistoryPanel(
                        historyList = historyList,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        DisplayField(displayText, isDark)
                        CalculatorPad(
                            viewModel = viewModel,
                            isApiToggled = isApiToggled,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize()
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DisplayField(displayText, isDark)
                    Spacer(modifier = Modifier.weight(1.0f))
                    Column(modifier = Modifier.weight(3f)) {
                        HistoryPanel(
                            historyList = historyList,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorPad(
                            viewModel = viewModel,
                            isApiToggled = isApiToggled,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize()
                        )
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
fun DisplayField(displayText: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isDark) DisplayBgDark else DisplayBgLight,
                        if (isDark) DisplayBgDark.copy(alpha = 0.8f) else DisplayBgLight.copy(alpha = 0.8f),
                    )
                )
            )
    ) {
        TextField(
            value = displayText,
            onValueChange = {},
            textStyle = TextStyle(
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Black,
                lineHeight = 640.sp,
                fontSize = 60.sp,
                color = if (isDark) Color.White else Color(0xFF1C1B1F)
            ),
            maxLines = 2,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display")
        )
    }
}

@Composable
fun HistoryPanel(
    historyList: List<String>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) HistoryBgDark else HistoryBgLight)
    ) {
        if (historyList.isEmpty()) {
            Text(
                text = "No history yet",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("history"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(historyList) { expression ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) HistoryItemBgDark else HistoryItemBgLight)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = expression,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF4A3780)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorPad(
    viewModel: CalculatorViewModel,
    isApiToggled: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonSpacing = 4.dp
    Column(
        modifier = modifier
            .then(Modifier.padding(4.dp))
            .testTag("calculatorPad"),
        verticalArrangement = Arrangement.spacedBy(buttonSpacing)
    ) {
        for (row in 0..3) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                for (btn in CalculatorPadRow[row])
                    ExpressiveButton(btn, viewModel, isDark, Modifier.weight(1f))
            }
        }

        // 5th row with API toggle
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) ApiToggleBg else ApiToggleBgLight
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "API",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.9f)
                        else Color(0xFF4A3780),
                    )
                    Switch(
                        checked = isApiToggled,
                        modifier = Modifier.testTag("toggleAPI"),
                        onCheckedChange = { viewModel.toggleAPI() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) Color(0xFF555555) else Color(0xFFBBBBBB),
                        )
                    )
                }
            }

            for (btn in CalculatorPadRow[4])
                ExpressiveButton(btn, viewModel, isDark, Modifier.weight(1f))
        }
    }
}

@Composable
fun ExpressiveButton(
    button: CalculatorButton,
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        scale.animateTo(
            if (isPressed) 0.86f else 1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = if (isPressed) Spring.StiffnessMedium else Spring.StiffnessLow
            )
        )
    }

    val (bgColor, textColor) = buttonStyle(button, isDark)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(bgColor, bgColor.copy(alpha = 0.82f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                when (button.text) {
                    "AC" -> viewModel.onAC()
                    "DEL" -> viewModel.onDEL()
                    "FIB" -> viewModel.onFIB()
                    "=" -> viewModel.onEquals()
                    "+", "-", "*", "/" -> viewModel.onOperatorPress(button.text)
                    else -> viewModel.onDigitPress(button.text)
                }
            }
            .testTag("button${button.text}")
            .then(modifier)
    ) {
        Text(
            text = button.text,
            fontSize = if (button.text.length > 2) 22.sp else 30.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun buttonStyle(button: CalculatorButton, isDark: Boolean): Pair<Color, Color> {
    return when (button.text) {
        "AC" -> ACColor to Color.White
        "DEL" -> DELColor to Color.White
        "FIB" -> FIBColor to Color.White
        "=" -> EqualsColor to Color.White
        "+", "-", "*", "/" -> Pair(
            if (isDark) OperatorColor else OperatorColorLight,
            Color.White
        )
        else -> Pair(
            if (isDark) DigitButtonColor else DigitButtonColorLight,
            if (isDark) Color.White else Color(0xFF1C1B1F)
        )
    }
}
