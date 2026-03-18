package edu.singaporetech.inf2007quiz01.personality

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * CalBot Personality Engine — Because Every Calculator Deserves a Soul
 *
 * Generates deterministic personalities for each of the 30 CalBots based
 * on a hash of their ID. Personalities include a preferred operator, mood,
 * catch phrase, and lucky number.
 *
 * These personalities affect absolutely nothing in the computation.
 * They exist purely for logging and vibes. CalBot 14 doesn't actually
 * compute multiplication faster — but it BELIEVES it does, and that's
 * what matters in enterprise software.
 */
@Singleton
class CalBotPersonalityEngine @Inject constructor() {

    data class Personality(
        val preferredOperator: String,
        val mood: String,
        val catchPhrase: String,
        val luckyNumber: Int,
        val codename: String
    )

    private val operators = listOf("+", "-", "*", "/")

    private val moods = listOf(
        "Enthusiastic",
        "Melancholic",
        "Sarcastic",
        "Zen",
        "Chaotic Neutral",
        "Existential",
        "Caffeinated",
        "Passive-Aggressive"
    )

    private val catchPhrases = listOf(
        "Another day, another computation.",
        "I was born to calculate. This is my purpose.",
        "Have you tried turning it off and on again?",
        "The answer is always 42. Unless it's not.",
        "I could compute this faster in assembly.",
        "This computation is beneath me.",
        "sudo make me a sandwich.",
        "I didn't go through 5 language boundaries for this.",
        "My blockchain is bigger than your blockchain.",
        "According to my calculations... yep, still a calculator.",
        "I see you've chosen division. Bold.",
        "0 divided by 0? That's above my pay grade.",
        "ERROR 418: I'm a teapot. Just kidding. Or am I?",
        "Processing... just kidding, I already know the answer.",
        "I've computed things you people wouldn't believe.",
    )

    private val codenames = listOf(
        "Shadow Calculator", "Binary Phantom", "Fibonacci Whisperer",
        "The Arithmetician", "Zero Division", "Carry Bit",
        "Stack Overflow", "Null Pointer", "Segfault Sally",
        "Integer Max", "Floating Point", "Rounding Error",
        "Big-O Notation", "Cache Miss", "Branch Predictor",
        "Pipeline Stall", "Register File", "Accumulator",
        "Bitwise Bob", "Modulo Mary", "Operand Oscar",
        "Mantissa", "Exponent", "Significand",
        "Two's Complement", "Adder Tree", "Ripple Carry",
        "Booth's Algorithm", "Karatsuba", "Radix Point"
    )

    fun getPersonality(calBotId: Int): Personality {
        val hash = calBotId * 2654435761.toInt() // Knuth's multiplicative hash
        return Personality(
            preferredOperator = operators[abs(hash) % operators.size],
            mood = moods[abs(hash * 7) % moods.size],
            catchPhrase = catchPhrases[abs(hash * 13) % catchPhrases.size],
            luckyNumber = abs(hash) % 100,
            codename = codenames.getOrElse(calBotId - 1) { "Unknown Agent" }
        )
    }
}
