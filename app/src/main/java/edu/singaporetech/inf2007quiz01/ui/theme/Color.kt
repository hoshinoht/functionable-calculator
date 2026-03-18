package edu.singaporetech.inf2007quiz01.ui.theme

import androidx.compose.ui.graphics.Color

// Core Material 3 Expressive palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Calculator button colors
val DigitButtonColor = Color(0xFF2D2D3A)
val DigitButtonColorLight = Color(0xFFE8E0F0)
val OperatorColor = Color(0xFF7C4DFF)
val OperatorColorLight = Color(0xFF6750A4)
val EqualsColor = Color(0xFFE91E63)
val ACColor = Color(0xFFEF5350)
val DELColor = Color(0xFFFF9800)
val FIBColor = Color(0xFF00897B)
val ApiToggleBg = Color(0xFF3A3A4A)
val ApiToggleBgLight = Color(0xFFE0D6EC)

// Display / History
val DisplayBgDark = Color(0xFF1A1A2E)
val DisplayBgLight = Color(0xFFF5F0FF)
val HistoryBgDark = Color(0xFF16213E)
val HistoryBgLight = Color(0xFFEDE7F6)
val HistoryItemBgDark = Color(0xFF1E2A45)
val HistoryItemBgLight = Color(0xFFE0D6EC)

// Gradient
val GradientPurple = Color(0xFF6750A4)
val GradientPink = Color(0xFFE91E63)
val GradientTeal = Color(0xFF00BCD4)

// CalBot accent — unique hue per CalBot
fun calBotAccent(id: Int): Color {
    val hue = ((id - 1) * 12f) % 360f
    return Color.hsl(hue, 0.72f, 0.58f)
}
