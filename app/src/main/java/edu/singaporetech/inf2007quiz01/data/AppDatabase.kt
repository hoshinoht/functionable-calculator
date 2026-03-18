package edu.singaporetech.inf2007quiz01.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ExpressionHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expressionHistoryDao(): ExpressionHistoryDao
}
