package edu.singaporetech.inf2007quiz01.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import edu.singaporetech.inf2007quiz01.operator.AdditionOperator
import edu.singaporetech.inf2007quiz01.operator.ArithmeticOperator
import edu.singaporetech.inf2007quiz01.operator.DivisionOperator
import edu.singaporetech.inf2007quiz01.operator.MultiplicationOperator
import edu.singaporetech.inf2007quiz01.operator.SubtractionOperator

/**
 * Hilt Module for Arithmetic Operator Dependency Injection.
 *
 * This module binds each arithmetic operator implementation into a Map
 * keyed by operator symbol. The OperatorStrategyEngine receives this map
 * via constructor injection and uses it to look up the correct strategy
 * at runtime.
 *
 * Four classes, one interface, one module, one multibinding map,
 * all to achieve what `when(op) { "+" -> a + b }` does in one line.
 *
 * But that one line doesn't have dependency injection, does it?
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OperatorModule {

    @Binds
    @IntoMap
    @StringKey("+")
    abstract fun bindAddition(op: AdditionOperator): ArithmeticOperator

    @Binds
    @IntoMap
    @StringKey("-")
    abstract fun bindSubtraction(op: SubtractionOperator): ArithmeticOperator

    @Binds
    @IntoMap
    @StringKey("*")
    abstract fun bindMultiplication(op: MultiplicationOperator): ArithmeticOperator

    @Binds
    @IntoMap
    @StringKey("/")
    abstract fun bindDivision(op: DivisionOperator): ArithmeticOperator
}
