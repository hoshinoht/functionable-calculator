package edu.singaporetech.inf2007quiz01.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ContactRoute

@Serializable
data class CalculatorRoute(val calBotId: Int, val calBotName: String)
