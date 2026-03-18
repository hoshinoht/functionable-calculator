package edu.singaporetech.inf2007quiz01

import kotlin.math.sqrt

object FunctionMap {

    fun half(x:Int):Int = x/2

    fun fib(x:Int):Int = when(x){
            0 -> 0
            1 -> 1
            else -> fastfibonacci(x)
        }

    fun fastfibonacci(x:Int): Int {
        if (x<= 1) return x

        var prev1 = 1
        var prev2 = 0

        for (i in 2..x) {
            var current = prev1 + prev2
            prev2 = prev1
            prev1 = current
        }

        return prev1
    }

    fun self(x:Int):Int {
        var y = x.toDouble()
        repeat(1000000000) {
            y = sqrt(y) * sqrt(y)
        }
        return y.toInt()
    }

    /**
     * Native C++ Fibonacci via JNI matrix exponentiation.
     * Because crossing a language boundary for basic arithmetic
     * is the hallmark of enterprise software.
     */
    fun nativeFib(x: Int): Int = NativeFibonacci.fib(x)

    val functionMap = mapOf <String, (Int)->Int> (
        "half" to :: half,
        "fib" to ::fib,
        "self" to ::self,
        "nativeFib" to ::nativeFib
            )
}