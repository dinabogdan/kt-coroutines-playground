package com.freesoft.playground

import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*

fun fibonacciSeq(): Sequence<Int> = sequence {
    yield(1)
    var cur = 1
    var next = 1

    while (true) {
        yield(next)
        val tmp = cur + next
        cur = next
        next = tmp
    }
}

suspend fun <T> CompletableFuture<T>.await(): T = suspendCoroutine<T> { cont: Continuation<T> ->
    whenComplete { result, exception ->
        if (exception == null) {
            cont.resume(result)
        } else cont.resumeWithException(exception)
    }
}

class CompletableFutureCoroutine<T>(override val context: CoroutineContext) : CompletableFuture<T>(), Continuation<T> {
    override fun resumeWith(result: Result<T>) {
        result
                .onSuccess { complete(it) }
                .onFailure { completeExceptionally(it) }
    }
}

fun <T> future(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): CompletableFuture<T> =
        CompletableFutureCoroutine<T>(context).also { block.startCoroutine(completion = it) }

fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
        block.startCoroutine(Continuation(context) { result ->
            result.onFailure { exception ->
                val currentThread = Thread.currentThread()
                currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
            }
        })

fun main() {
    println(fibonacciSeq()
            .take(10)
            .joinToString(",")
    )

    launch {
        val value = CompletableFuture.completedFuture("Some value").await()
    }

    runBlocking {
        val value = CompletableFuture.completedFuture("Some value").await()
        println(value)
    }

}