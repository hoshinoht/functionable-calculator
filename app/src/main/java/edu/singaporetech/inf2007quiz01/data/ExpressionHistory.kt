package edu.singaporetech.inf2007quiz01.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expression_history")
data class ExpressionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val calBotId: Int,
    val expression: String
)
