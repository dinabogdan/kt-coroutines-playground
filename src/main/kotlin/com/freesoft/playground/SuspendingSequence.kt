package com.freesoft.playground

import kotlinx.coroutines.runBlocking
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.NoSuchElementException
import kotlin.coroutines.*

interface SuspendingIterator<out T> {
    suspend operator fun hasNext(): Boolean
    suspend operator fun next(): T
}

interface SuspendingSequence<out T> {
    operator fun iterator(): SuspendingIterator<T>
}

interface SuspendingSequenceScope<in T> {
    suspend fun yield(value: T)
}

fun <T> suspendingIterator(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend SuspendingSequenceScope<T>.() -> Unit
): SuspendingIterator<T> = SuspendingIteratorCoroutine<T>(context).apply {
    nextStep = block.createCoroutine(
            receiver = this,
            completion = this
    )
}

fun <T> suspendingSequence(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend SuspendingSequenceScope<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
    override fun iterator(): SuspendingIterator<T> {
        return suspendingIterator(context, block)
    }
}

@Suppress("UNCHECKED_CAST")
class SuspendingIteratorCoroutine<T>(
        override val context: CoroutineContext
) : SuspendingIterator<T>,
        SuspendingSequenceScope<T>,
        Continuation<Unit> {

    enum class State {
        INITIAL,
        COMPUTING_HAS_NEXT,
        COMPUTING_NEXT,
        COMPUTED,
        DONE
    }

    var state: State = State.INITIAL
    var nextValue: T? = null
    var nextStep: Continuation<Unit>? = null

    var computeContinuation: Continuation<*>? = null

    override suspend fun hasNext(): Boolean = when (state) {
        State.INITIAL -> computeHasNext()
        State.COMPUTED -> true
        State.DONE -> false
        else -> throw IllegalArgumentException("Recursive dependency detected. Already computing next!")
    }

    private suspend fun computeHasNext(): Boolean = suspendCoroutine { c ->
        state = State.COMPUTING_HAS_NEXT
        computeContinuation = c
        nextStep!!.resume(Unit)
    }

    override suspend fun next(): T = when (state) {
        State.INITIAL -> computeNext()
        State.COMPUTED -> {
            state = State.INITIAL
            nextValue as T
        }
        State.DONE -> throw NoSuchElementException()
        else -> throw IllegalArgumentException("Recursive dependency detected. Already computing next!")
    }

    private suspend fun computeNext(): T = suspendCoroutine { c ->
        state = State.COMPUTING_NEXT
        computeContinuation = c
        nextStep!!.resume(Unit)
    }

    override suspend fun yield(value: T): Unit = suspendCoroutine { c ->
        nextValue = value
        nextStep = c
        resumeIterator(true)
    }

    private fun resumeIterator(hasNext: Boolean) {
        when (state) {
            State.COMPUTING_HAS_NEXT -> {
                state = State.COMPUTED
                (computeContinuation as Continuation<Boolean>).resume(hasNext)
            }
            State.COMPUTING_NEXT -> {
                state = State.INITIAL
                (computeContinuation as Continuation<T>).resume(nextValue as T)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun resumeWith(result: Result<Unit>) {
        nextStep = null
        result.onSuccess { resumeIterator(false) }
                .onFailure { exception ->
                    state = State.DONE
                    computeContinuation?.resumeWithException(exception)
                }
    }
}

fun main() {
    val context = newSingleThreadContext("async-sequence")
    runBlocking(context) {
        val seq = suspendingSequence<Int>(context) {
            println("Starting generator")
            for (i in 1..10) {
                println("Generator yields $i")
                yield(i)
                println("Generator goes to sleep for 500 ms")
                delay(500)
            }
            println("Generator is done")
        }

        val random = Random()
        for (value in seq) {
            println("Consumer got value = $value")
            val consumerSleep = random.nextInt(1000).toLong()
            println("Consumer goes to sleep for $consumerSleep ms")
            delay(consumerSleep)
        }
    }

}