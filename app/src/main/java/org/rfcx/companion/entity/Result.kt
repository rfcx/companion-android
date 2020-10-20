package org.rfcx.companion.entity

/**
 * [Result] is a type that represents either success ([Ok]) or failure ([Err]).
 */
sealed class Result<out V, out E> {
    companion object {

        /**
         * Invokes a [function] and wraps it in a [Result], returning an [Err]
         * if an [Exception] was thrown, otherwise [Ok].
         */
        inline fun <V> of(function: () -> V): Result<V, Exception> {
            return try {
                Ok(function.invoke())
            } catch (ex: Exception) {
                Err(ex)
            }
        }
    }
}

/**
 * Represents a successful [Result], containing a [value].
 */
data class Ok<out V>(val value: V) : Result<V, Nothing>()

/**
 * Represents a failed [Result], containing an [error].
 */
data class Err<out E>(val error: E) : Result<Nothing, E>()

/**
 * Converts a nullable of type [V] to a [Result]. Returns [Ok] if the value is
 * non-null, otherwise the supplied [error].
 */
inline infix fun <V, E> V?.toResultOr(error: () -> E): Result<V, E> {
    return when (this) {
        null -> Err(error())
        else -> Ok(this)
    }
}
